package pcd.alarm.system

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.ActorSystem
import pcd.alarm.system.actors.{ControlUnitNode, KeypadNode, SensorsNode}
import pcd.alarm.system.domain.SystemConfig

import scala.concurrent.Await
import scala.concurrent.duration.*

/**
 * Entry point for every cluster node. All nodes share the same artefact, which role a given JVM plays
 * is selected by the first command line argument, while its network identity comes from
 * the environment variables read by `application.conf`.
 *
 * - Demo scenario driven by the "keypad" and "sensors" nodes once the cluster has formed:
 *   t= 5s, ArmFull -> ExitDelay starts (20s)
 *   t=10s, DS-FRONT sensor fires -> ignored (still exit delay)
 *   t=26s, MS-UPPER-01 sensor fires -> EntryDelay starts (15s)
 *   t=30s, wrong PIN -> countdown continues
 *   t=42s, EntryDelay has expired -> Alarm sounding
 *          correct PIN -> siren off: system Disarmed
 *   t=50s, the control unit deliberately crashes (SimulateCrash) and
 *          is respawned in Safe Recovery
 *   t=55s, MS-UPPER-01 fires again -> ignored (Safe Recovery)
 *   t=60s, correct PIN -> system back to Disarmed
 */
object SmartHomeAlarmSystem {

  private val alarmConfig = SystemConfig(
    pin = "1234",
    exitDelay = 20.seconds,
    entryDelay = 15.seconds
  )

  def main(args: Array[String]): Unit = {
    val role = args.headOption.getOrElse(
      throw new IllegalArgumentException("Missing role argument. " +
        "Expected: control-unit, keypad, sensors")
    )

    val config = ConfigFactory.load()
    val clusterName = config.getString("cluster.name")

    role match {
      case "control-unit" =>
        val system = ActorSystem[Nothing](
          ControlUnitNode(alarmConfig, simulateCrashAfter = Some(50.seconds)),
          clusterName,
          config
        )
        Await.result(system.whenTerminated, Duration.Inf)

      case "keypad" =>
        val system = ActorSystem(KeypadNode(), clusterName, config)

        scheduleScenario(system) { schedule =>
          schedule(5.seconds, KeypadNode.ArmFull("1234"))
          schedule(30.seconds, KeypadNode.EnterPin("0000"))
          schedule(42.seconds, KeypadNode.EnterPin("1234"))
          schedule(60.seconds, KeypadNode.EnterPin("1234"))
        }

        Await.result(system.whenTerminated, Duration.Inf)

      case "sensors" =>
        val sensors = List(
          "DS-FRONT" -> "perimeter",
          "MS-GROUND-01" -> "ground-floor",
          "MS-UPPER-01" -> "upper-floor"
        )
        val system = ActorSystem(SensorsNode(sensors), clusterName, config)

        scheduleScenario(system) { schedule =>
          schedule(10.seconds, SensorsNode.Trigger("DS-FRONT"))
          schedule(26.seconds, SensorsNode.Trigger("MS-UPPER-01"))
          schedule(55.seconds, SensorsNode.Trigger("MS-UPPER-01"))
        }

        Await.result(system.whenTerminated, Duration.Inf)

      case other =>
        throw new IllegalArgumentException(
          s"Unknown role '$other'. Expected one of: control-unit, keypad, sensors"
        )
    }
  }

  /**
   * Utility helper that schedules a sequence of messages on an ActorSystem
   * to build declarative timeline scripts.
   *
   * @tparam T The type of messages handled by the actor system.
   * @param system The target actor system used for message dispatching and timer scheduling.
   * @param register A builder function that receives a registration closure `(delay, message)` to define scheduled events.
   */
  private def scheduleScenario[T](system: ActorSystem[T])(register: ((FiniteDuration, T) => Unit) => Unit): Unit = {
    import system.executionContext
    // Let cluster membership converge before the scenario starts sending commands.
    val gracePeriod = 5.seconds
    register { (delay, message) =>
      val task: Runnable = () => system ! message
      system.scheduler.scheduleOnce(gracePeriod + delay, task)
    }
  }
}
