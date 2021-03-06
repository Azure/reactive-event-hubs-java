// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.reactiveeventhubs

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.microsoft.azure.eventhubs.{PartitionReceiver, ReceiverOptions, ReceiverRuntimeInformation}
import com.microsoft.azure.reactiveeventhubs.config.IConfiguration

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

private object EventHubsMessageSource {

  /** Create an instance of the messages source for the specified partition
    *
    * @param partition Event hub partition to read
    * @param offset    Starting position, offset of the first message
    *
    * @return A source returning the body of the message sent from a device.
    *         Deserialization is left to the consumer.
    */
  def apply(config: IConfiguration, partition: Int, offset: String): Source[EventHubsMessage, NotUsed] = {
    Source.fromGraph(new EventHubsMessageSource(config, partition, offset))
  }

  /** Create an instance of the messages source for the specified partition
    *
    * @param partition Event hub partition to read
    * @param startTime Starting position expressed in time
    *
    * @return A source returning the body of the message sent from a device.
    *         Deserialization is left to the consumer.
    */
  def apply(config: IConfiguration, partition: Int, startTime: Instant): Source[EventHubsMessage, NotUsed] = {
    Source.fromGraph(new EventHubsMessageSource(config, partition, startTime))
  }
}

/** Source of messages from one partition of the EventHubs service
  */
private class EventHubsMessageSource(config: IConfiguration) extends GraphStage[SourceShape[EventHubsMessage]] with Logger {

  /** Source of messages from one partition of the EventHubs service
    *
    * @param partition Partition number (0 to N-1) to read from
    * @param offset    Starting position
    */
  def this(config: IConfiguration, partition: Int, offset: String) {
    this(config)
    _partition = Some(partition)
    _offset = Some(offset)
    offsetType = SequenceOffset
  }

  /** Source of messages from one partition of the EventHubs service
    *
    * @param partition Partition number (0 to N-1) to read from
    * @param startTime Starting position
    */
  def this(config: IConfiguration, partition: Int, startTime: Instant) {
    this(config)
    _partition = Some(partition)
    _startTime = Some(startTime)
    offsetType = TimeOffset
  }

  // TODO: Refactor and use async methods, compare performance
  // TODO: Consider option to deserialize on the fly to [T], when JSON Content Type

  abstract class OffsetType

  case object UnspecifiedOffset extends OffsetType

  case object SequenceOffset extends OffsetType

  case object TimeOffset extends OffsetType

  // When retrieving messages, include the message with the initial offset
  private[this] val OffsetInclusive = true

  private[this] var _partition: Option[Int]     = None
  private[this] var _offset   : Option[String]  = None
  private[this] var _startTime: Option[Instant] = None
  private[this] var offsetType: OffsetType      = UnspecifiedOffset

  private[this] def partition: Int = _partition.get

  private[this] def offset: String = _offset.get

  private[this] def startTime: Instant = _startTime.get

  // Define the (sole) output port of this stage
  private[this] val out: Outlet[EventHubsMessage] = Outlet("EventHubsMessageSource")

  // Define the shape of this stage ⇒ SourceShape with the port defined above
  override val shape: SourceShape[EventHubsMessage] = SourceShape(out)

  // All state MUST be inside the GraphStageLogic, never inside the enclosing
  // GraphStage. This state is safe to read/write from all the callbacks
  // provided by GraphStageLogic and the registered handlers.
  override def createLogic(attr: Attributes): GraphStageLogic = {
    log.debug("Creating the Event hub source")
    new GraphStageLogic(shape) {

      val keepAliveSignal = new EventHubsMessage(None, None, None)
      val emptyResult     = List[EventHubsMessage](keepAliveSignal)

      lazy val receiver = getEventHubReceiver()

      setHandler(
        out, new OutHandler {
          log.debug("Defining the output handler")

          override def onPull(): Unit = {
            try {
              val messages = Retry(2, 1 seconds) {
                receiver.receiveSync(config.streaming.receiverBatchSize)
              }

              if (messages == null) {
                log.debug("Partition {} is empty", partition)
                this.onPull()
              } else {
                val partitionInfo: ReceiverRuntimeInformation = receiver.getRuntimeInformation
                val iterator = messages.asScala.map(e ⇒ EventHubsMessage(e, Some(partition), Some(partitionInfo))).toList
                log.debug("Emitting {} messages", iterator.size)
                emitMultiple(out, iterator)
              }
            } catch {
              case e: Exception ⇒ log.error(e, "Fatal error: " + e.getMessage)
            }
          }

          override def onDownstreamFinish(): Unit = {
            super.onDownstreamFinish()
            log.info("Closing partition {} receiver", partition)
            receiver.closeSync()
          }
        })

      /** Connect to the EventHubs service
        *
        * @return Event Hubs receiver
        */
      def getEventHubReceiver(): PartitionReceiver = Retry(3, 2 seconds) {
        val receiverOptions = new ReceiverOptions()
        receiverOptions.setReceiverRuntimeMetricEnabled(config.streaming.retrieveRuntimeInfo)
        offsetType match {

          case SequenceOffset ⇒
            log.info("Connecting to partition {} starting from offset '{}'", partition, offset)
            EventHubsConnector(config.connect)
              .createClient()
              .createReceiverSync(
                config.streaming.receiverConsumerGroup,
                partition.toString,
                offset,
                OffsetInclusive,
                receiverOptions)

          case TimeOffset ⇒
            log.info("Connecting to partition {} starting from time '{}'", partition, startTime)
            EventHubsConnector(config.connect)
              .createClient()
              .createReceiverSync(
                config.streaming.receiverConsumerGroup,
                partition.toString,
                startTime,
                receiverOptions)
        }
      }
    }
  }
}
