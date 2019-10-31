// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.reactiveeventhubs

import java.time.Instant
import java.util

import com.microsoft.azure.eventhubs.{EventData, ReceiverRuntimeInformation}

import scala.collection.JavaConverters._
import scala.language.{implicitConversions, postfixOps}

/* MessageFromDevice factory */
private object EventHubsMessage {

  /** Create a user friendly representation of the Event hub message from the raw
    * data coming from the storage
    *
    * @param rawData         Raw data retrieved from the EventHubs service
    * @param partitionNumber Storage partition where the message was retrieved from
    * @param partitionInfo   Information about the partition, e.g. about the last message
    *
    * @return
    */
  def apply(
      rawData: EventData,
      partitionNumber: Option[Int],
      partitionInfo: Option[ReceiverRuntimeInformation]): EventHubsMessage = {
    new EventHubsMessage(Some(rawData), partitionNumber, partitionInfo)
  }
}

/** Expose the Event hub message body and timestamp
  *
  * @param partNumber Storage partition where the message was retrieved from
  * @param partInfo   Information about the partition, e.g. about the last message
  */
class EventHubsMessage(
    data: Option[EventData],
    partNumber: Option[Int],
    partInfo: Option[ReceiverRuntimeInformation]) {

  // TODO: test properties over all protocols

  // Internal properties set by Event hub stoage
  private[this] lazy val systemProps = data.get.getSystemProperties

  lazy val property: String => AnyRef = (propName: => String) => systemProps.get(propName)

  // Meta properties set by the device
  lazy val properties: util.Map[String, String] = data.get.getProperties.asScala.map(x ⇒ (x._1, x._2.toString)).asJava

  // Time when the message was received by Event hub service. *NOT* the device time.
  lazy val received: Instant = systemProps.getEnqueuedTime

  // Event hub message offset, useful to store the current position in the stream
  lazy val offset: String = systemProps.getOffset

  // Event hub message sequence number
  lazy val sequenceNumber: Long = systemProps.getSequenceNumber

  // Event hub message content bytes
  lazy val content: Array[Byte] = data.get.getBytes

  // Event hub message content as string, e.g. JSON/XML/etc.
  lazy val contentAsString: String = new String(content)

  // Information about the partition containing the current message
  lazy val runtimeInfo: SourceRuntimeInfo =
    if (partNumber.isEmpty) {
      new SourceRuntimeInfo(new SourcePartitionInfo(None, None, None, None))
    } else if (partInfo.isEmpty) {
      new SourceRuntimeInfo(new SourcePartitionInfo(partNumber, None, None, None))
    } else {
      new SourceRuntimeInfo(new SourcePartitionInfo(
        partNumber,
        Some(partInfo.get.getLastSequenceNumber),
        Some(partInfo.get.getLastEnqueuedOffset),
        Some(partInfo.get.getLastEnqueuedTime)))
    }

}
