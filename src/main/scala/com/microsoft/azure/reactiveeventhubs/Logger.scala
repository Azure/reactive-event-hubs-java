// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.reactiveeventhubs

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging, LoggingAdapter}
import com.typesafe.config.{Config, ConfigFactory}

private[reactiveeventhubs] object Logger {
  val shConfig = ConfigFactory.empty()
  val actorSystem = ActorSystem("EventHubReact", shConfig)
}

/** Common logger via Akka
  *
  * @see http://doc.akka.io/docs/akka/2.4.10/scala/logging.html
  */
private[reactiveeventhubs] trait Logger {

  implicit val logSource = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName

    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }

  val log = Logging(Logger.actorSystem, this)
}

