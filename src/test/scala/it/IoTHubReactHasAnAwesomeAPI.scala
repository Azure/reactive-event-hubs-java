// Copyright (c) Microsoft. All rights reserved.

package it

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.{Sink, Source}
import com.microsoft.azure.reactiveeventhubs.EventHubMessage
import com.microsoft.azure.reactiveeventhubs.scaladsl.EventHub
import org.scalatest._

class IoTHubReactHasAnAwesomeAPI extends FeatureSpec with GivenWhenThen {

  info("As a client of Azure IoT hub")
  info("I want to be able to receive device messages as a stream")
  info("So I can process them asynchronously and at scale")

  Feature("IoT Hub React has an awesome API") {

    Scenario("Developer wants to retrieve IoT messages") {

      Given("An IoT hub is configured")
      val hub = EventHub()

      When("A developer wants to fetch messages from Azure IoT hub")
      val messagesFromAllPartitions: Source[EventHubMessage, NotUsed] = hub.source()
      val messagesFromNowOn: Source[EventHubMessage, NotUsed] = hub.source(Instant.now())

      Then("The messages are presented as a stream")
      messagesFromAllPartitions.to(Sink.ignore)
      messagesFromNowOn.to(Sink.ignore)

      hub.close()
    }
  }
}
