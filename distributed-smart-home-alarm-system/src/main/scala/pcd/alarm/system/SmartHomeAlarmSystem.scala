package pcd.alarm.system

import org.apache.pekko.actor.typed.ActorSystem
import pcd.alarm.system.actors.UserGuardian
import pcd.alarm.system.domain.SystemConfig

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
 * Entry point for the application.
 */
object SmartHomeAlarmSystem {
  @main def main(args: String*): Unit = {
    val config = SystemConfig(
      pin = "1234",
      exitDelay = 20.seconds,
      entryDelay = 15.seconds
    )

    val system = ActorSystem[UserGuardian.Command](UserGuardian(config), "SmartHomeAlarmSystem")

    Await.result(system.whenTerminated, 60.seconds)
  }
}