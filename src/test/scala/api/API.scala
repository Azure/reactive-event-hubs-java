// Copyright (c) Microsoft. All rights reserved.

// NOTE: Namespace chosen to avoid access to internal classes
package api

import com.microsoft.azure.reactiveeventhubs.SourceOptions
import com.microsoft.azure.reactiveeventhubs.checkpointing.Backends.CheckpointBackend

// NOTE: No global imports from the library, to make easier detecting breaking changes

class APIIsBackwardCompatible
  extends org.scalatest.FeatureSpec
    with org.scalatest.mockito.MockitoSugar {

  info("As a developer using Azure Event hub React")
  info("I want to be able to upgrade to new minor versions without changing my code")
  info("So I can benefit from improvements without excessive development costs")

  Feature("Version 0.x is backward compatible") {

    Scenario("Using MessageFromDevice") {
      import com.microsoft.azure.eventhubs.{EventData, ReceiverRuntimeInformation}
      import com.microsoft.azure.reactiveeventhubs.EventHubsMessage

      val data: Option[EventData] = None
      val partition: Option[Int] = Some(1)

      // Test properties
      val partitionInfo = Some(new ReceiverRuntimeInformation(partition.toString))
      val message1 = new EventHubsMessage(data, partition, partitionInfo)
      lazy val properties: java.util.Map[String, String] = message1.properties
      lazy val created: java.time.Instant = message1.received
      lazy val offset: String = message1.offset
      lazy val sequenceNumber: Long = message1.sequenceNumber
      lazy val content: Array[Byte] = message1.content
      lazy val contentAsString: String = message1.contentAsString
      lazy val extraProp: AnyRef = message1.property("test-prop")

      // Named parameters
      val message2: EventHubsMessage = new EventHubsMessage(data = data, partNumber = partition, partInfo = partitionInfo)

      // Keepalive
      val message3: EventHubsMessage = new EventHubsMessage(data, partNumber = None, partInfo = None)
    }

    Scenario("Using ResumeOnError") {
      import akka.actor.ActorSystem
      import akka.stream.ActorMaterializer
      import com.microsoft.azure.reactiveeventhubs.ResumeOnError._

      val as: ActorSystem = actorSystem
      val mat: ActorMaterializer = materializer
    }

    Scenario("Using StopOnError") {
      import akka.actor.ActorSystem
      import akka.stream.ActorMaterializer
      import com.microsoft.azure.reactiveeventhubs.StopOnError._

      val as: ActorSystem = actorSystem
      val mat: ActorMaterializer = materializer
    }

    Scenario("Using CheckpointBackend") {
      import com.microsoft.azure.reactiveeventhubs.checkpointing.ICPConfiguration
      import org.mockito.Mockito.when

      class CustomBackend extends CheckpointBackend {

        override def readOffset(partition: Int): String = {
          return ""
        }

        override def writeOffset(partition: Int, offset: String): Unit = {}
      }

      val backend: CustomBackend = new CustomBackend()

      val anyname = java.util.UUID.randomUUID.toString
      val cpconfig = mock[ICPConfiguration]
      when(cpconfig.storageNamespace).thenReturn(anyname)
      assert(backend.checkpointNamespace(cpconfig) == anyname)
    }

    Scenario("Using Scala DSL EventHub") {
      import java.time.Instant

      import akka.NotUsed
      import akka.stream.scaladsl.Source
      import com.microsoft.azure.reactiveeventhubs.EventHubsMessage
      import com.microsoft.azure.reactiveeventhubs.config.IConfiguration
      import com.microsoft.azure.reactiveeventhubs.scaladsl.EventHub

      val hub1: EventHub = EventHub()
      val hub2: EventHub = EventHub(mock[IConfiguration])

      val partitions = Seq(0, 1, 3)
      val options = SourceOptions()

      var source: Source[EventHubsMessage, NotUsed] = hub1.source()
      source = hub1.source(partitions)
      source = hub1.source(partitions = partitions)
      source = hub1.source(Instant.now())
      source = hub1.source(startTime = Instant.now())
      source = hub1.source(options)
      source = hub1.source(options = options)

      hub1.close()
      hub2.close()
    }

    Scenario("Using Java DSL EventHub") {
      import java.time.Instant

      import akka.NotUsed
      import akka.stream.javadsl.Source
      import com.microsoft.azure.reactiveeventhubs.EventHubsMessage
      import com.microsoft.azure.reactiveeventhubs.config.IConfiguration
      import com.microsoft.azure.reactiveeventhubs.javadsl.EventHub

      val hub1: EventHub = new EventHub()
      val hub2: EventHub = new EventHub(mock[IConfiguration])

      val partitions: java.util.List[java.lang.Integer] = java.util.Arrays.asList(0, 1, 3)
      val options = new SourceOptions()

      var source: Source[EventHubsMessage, NotUsed] = hub1.source()
      source = hub1.source(partitions)
      source = hub1.source(partitions = partitions)
      source = hub1.source(Instant.now())
      source = hub1.source(startTime = Instant.now())
      source = hub1.source(options)
      source = hub1.source(options = options)

      hub1.close()
      hub2.close()
    }

    Scenario("Using SourceOptions") {
      import com.microsoft.azure.reactiveeventhubs.SourceOptions

      val o: SourceOptions = SourceOptions()
        .fromStart
        .fromStart()
        .fromTime(java.time.Instant.now)
        .fromSavedOffsets()
        .fromSavedOffsets(java.time.Instant.now)
        .allPartitions
        .allPartitions()
        .partitions(0, 2, 4)
        .partitions(Seq(0, 1, 2))
        .partitions(Array(1, 2, 3, 4))
        .fromOffsets("1", "2")
        .fromOffsets(Seq("1", "2"))
        .fromOffsets(Array("1", "2"))
        .saveOffsets
        .saveOffsets()
        .withRuntimeInfo
        .withRuntimeInfo()
    }
  }
}
