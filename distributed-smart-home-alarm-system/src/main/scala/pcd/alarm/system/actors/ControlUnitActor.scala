package pcd.alarm.system.actors

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.*
import pcd.alarm.system.*
import pcd.alarm.system.actors.SirenActor
import pcd.alarm.system.domain.*
import pcd.alarm.system.domain.ArmingMode.{FullArm, PartialArm}

/**
 * ControlUnitActor is the central FSM actor with zone-based arming support.
 */
object ControlUnitActor {

  def apply(config: SystemConfig, siren: ActorRef[SirenActor.Command]): Behavior[Command] =
    Behaviors.withTimers { timers =>
      disarmed(config, siren, timers)
    }

  /**
   * Returns true when the specified zone should be monitored, given the current arming mode.
   */
  private def inActiveZone(zone: Zone, activeZones: Option[Set[Zone]]): Boolean =
    activeZones.forall(_.contains(zone))

  private def disarmed(
                        config: SystemConfig,
                        siren: ActorRef[SirenActor.Command],
                        timers: TimerScheduler[Command]
                      ): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case ArmRequest(pin, _) if pin != config.pin =>
          ctx.log.info("[DISARMED] Wrong PIN for arm request")
          Behaviors.same

        case ArmRequest(_, mode) =>
          val activeZones: Option[Set[Zone]] = mode match {
            case FullArm => None
            case PartialArm(zones) => Some(zones)
          }
          val zoneDesc = activeZones.fold("ALL zones")(_.mkString(", "))
          ctx.log.info("[DISARMED] Arming - active zones: {} - exit delay {} s", zoneDesc, config.exitDelay.toSeconds)
          timers.startSingleTimer(ExitTimerKey, ExitDelayTimeout, config.exitDelay)
          exitDelay(config, siren, timers, activeZones)

        case PinEntered(_) =>
          ctx.log.debug("[DISARMED] Already disarmed - ignoring PIN")
          Behaviors.same

        case SensorEvent(id, zone) =>
          ctx.log.debug("[DISARMED] Sensor {}/{} fired - ignored while disarmed", id, zone)
          Behaviors.same

        case _ => Behaviors.unhandled
      }
    }

  private def exitDelay(
                         config: SystemConfig,
                         siren: ActorRef[SirenActor.Command],
                         timers: TimerScheduler[Command],
                         activeZones: Option[Set[Zone]]
                       ): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case ExitDelayTimeout =>
          val zoneDesc = activeZones.fold("ALL")(_.mkString(", "))
          ctx.log.info("[EXIT DELAY] Elapsed - system ARMED (active zones: {})", zoneDesc)
          armed(config, siren, timers, activeZones)

        case PinEntered(pin) if pin == config.pin =>
          ctx.log.info("[EXIT DELAY] Arming canceled by valid PIN - returning to disarmed")
          timers.cancel(ExitTimerKey)
          disarmed(config, siren, timers)

        case PinEntered(_) =>
          ctx.log.info("[EXIT DELAY] Wrong PIN entered during exit delay")
          Behaviors.same

        case SensorEvent(id, zone) =>
          ctx.log.debug("[EXIT DELAY] Sensor {}/{} - ignored during exit window", id, zone)
          Behaviors.same

        case _ => Behaviors.unhandled
      }
    }

  private def armed(
                     config: SystemConfig,
                     siren: ActorRef[SirenActor.Command],
                     timers: TimerScheduler[Command],
                     activeZones: Option[Set[Zone]]
                   ): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        // Sensors in an ACTIVE zone start entry delay
        case SensorEvent(id, zone) if inActiveZone(zone, activeZones) =>
          ctx.log.info("[ARMED] Intrusion: {}/{} - entry delay {} s", id, zone, config.entryDelay.toSeconds)
          timers.startSingleTimer(EntryTimerKey, EntryDelayTimeout, config.entryDelay)
          entryDelay(config, siren, timers, activeZones)

        // Sensors in an INACTIVE zone are silently ignored (partial arm)
        case SensorEvent(id, zone) =>
          ctx.log.info("[ARMED] Sensor {}/{} fired - zone not active (partial arm), ignoring", id, zone)
          Behaviors.same

        case PinEntered(pin) if pin == config.pin =>
          ctx.log.info("[ARMED] Correct PIN - disarming immediately")
          disarmed(config, siren, timers)

        case PinEntered(_) =>
          ctx.log.info("[ARMED] Wrong PIN")
          Behaviors.same

        case _ => Behaviors.unhandled
      }
    }

  private def entryDelay(
                          config: SystemConfig,
                          siren: ActorRef[SirenActor.Command],
                          timers: TimerScheduler[Command],
                          activeZones: Option[Set[Zone]]
                        ): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case PinEntered(pin) if pin == config.pin =>
          timers.cancel(EntryTimerKey)
          ctx.log.info("[ENTRY DELAY] Correct PIN in time - disarming")
          disarmed(config, siren, timers)

        case PinEntered(_) =>
          ctx.log.info("[ENTRY DELAY] Wrong PIN - countdown continues")
          Behaviors.same

        case EntryDelayTimeout =>
          ctx.log.info("[ENTRY DELAY] Timeout expired - Alarm triggered!")
          siren ! SirenActor.Activate
          alarm(config, siren, timers)

        case SensorEvent(id, zone) if inActiveZone(zone, activeZones) =>
          ctx.log.info("[ENTRY DELAY] Extra sensor {}/{} - already counting down", id, zone)
          Behaviors.same

        case SensorEvent(id, zone) =>
          ctx.log.debug("[ENTRY DELAY] Sensor {}/{} in inactive zone - ignored", id, zone)
          Behaviors.same

        case _ => Behaviors.unhandled
      }
    }

  private def alarm(
                     config: SystemConfig,
                     siren: ActorRef[SirenActor.Command],
                     timers: TimerScheduler[Command]
                   ): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case PinEntered(pin) if pin == config.pin =>
          siren ! SirenActor.Deactivate
          ctx.log.info("[ALARM] Correct PIN - Siren off, system disarmed")
          disarmed(config, siren, timers)

        case PinEntered(_) =>
          ctx.log.info("[ALARM] Wrong PIN - Siren continues")
          Behaviors.same

        case SensorEvent(_, _) =>
          ctx.log.debug("[ALARM] Sensor event during alarm - already triggered")
          Behaviors.same

        case _ => Behaviors.unhandled
      }
    }

  sealed trait Command

  final case class ArmRequest(pin: Pin, mode: ArmingMode) extends Command

  final case class PinEntered(pin: Pin) extends Command

  final case class SensorEvent(sensorId: SensorId, zone: Zone) extends Command

  private[system] case object ExitDelayTimeout extends Command

  private[system] case object EntryDelayTimeout extends Command

  private case object ExitTimerKey

  private case object EntryTimerKey
}