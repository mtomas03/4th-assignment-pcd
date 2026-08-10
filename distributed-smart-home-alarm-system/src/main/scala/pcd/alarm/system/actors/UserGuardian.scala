package pcd.alarm.system.actors

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import pcd.alarm.system.actors.{ControlUnitActor, KeypadActor, SensorActor, SirenActor}
import pcd.alarm.system.domain.SystemConfig

import scala.concurrent.duration.DurationInt

/**
 * User Guardian Actor responsible for instantiating the system actor hierarchy
 * and orchestrating scenario events.
 *
 * - Scenario A: Full arm, alarm fires, then silenced
 *   t= 1s, ArmFull -> ExitDelay starts
 *   t=23s, ExitDelay expires -> Armed (all zones)
 *   t=25s, Sensor MS-UPPER-01 fires -> EntryDelay starts
 *   (no correct PIN entered)
 *   t=40s, EntryDelay timeout -> Alarm ON
 *   t=44s, Correct PIN -> Siren OFF, disarmed
 *
 * - Scenario B: Night mode (PartialArm: perimeter + ground floor)
 *   t=1s, ArmPartial -> ExitDelay starts
 *   t=23s, ExitDelay expires -> Partially armed (perimeter + ground floor)
 *   t=25s, Sensor MS-UPPER-01 fires (upper-floor) -> Ignored because upper-floor is not armed
 *   t=27s, Sensor DS-FRONT fires (perimeter) -> EntryDelay starts (15 s)
 *   t=30s, Wrong PIN -> still armed
 *   t=35s, Correct PIN -> disarmed before timeout
 */
object UserGuardian {

  def apply(config: SystemConfig): Behavior[Command] = Behaviors.setup[Command] { ctx =>

    // Spawn all actors
    val siren = ctx.spawn(SirenActor(), "siren")
    val acu = ctx.spawn(ControlUnitActor(config, siren), "alarm-control-unit")
    val sensorFront = ctx.spawn(SensorActor("DS-FRONT", "perimeter", acu), "door-sensor-front")
    val sensorGround = ctx.spawn(SensorActor("MS-GROUND-01", "ground-floor", acu), "motion-sensor-ground")
    val sensorUpper = ctx.spawn(SensorActor("MS-UPPER-01", "upper-floor", acu), "motion-sensor-upper")
    val keypad = ctx.spawn(KeypadActor(acu), "keypad")

    // Scenario A: FullArm, alarm fires
    ctx.scheduleOnce(1.second, keypad, KeypadActor.ArmFull("1234"))
    ctx.scheduleOnce(25.seconds, sensorUpper, SensorActor.Triggered)
    ctx.scheduleOnce(44.seconds, keypad, KeypadActor.EnterPin("1234"))
    ctx.scheduleOnce(50.seconds, ctx.self, Shutdown)

    // Scenario B: Night mode partially armed
    //val nightZones = Set("perimeter", "ground-floor")

    //ctx.scheduleOnce(1.second, keypad, KeypadActor.ArmPartial("1234", nightZones))
    //ctx.scheduleOnce(25.seconds, sensorUpper, SensorActor.Triggered) // should be IGNORED
    //ctx.scheduleOnce(27.seconds, sensorFront, SensorActor.Triggered) // triggers entry delay
    //ctx.scheduleOnce(30.seconds, keypad, KeypadActor.EnterPin("0000")) // wrong PIN
    //ctx.scheduleOnce(35.seconds, keypad, KeypadActor.EnterPin("1234")) // disarmed
    //ctx.scheduleOnce(40.seconds, ctx.self, Shutdown)

    Behaviors.receiveMessage {
      case Shutdown =>
        ctx.log.info("Scenario completed. Initiating graceful shutdown")
        ctx.system.terminate()
        Behaviors.stopped
    }
  }

  sealed trait Command

  private case object Shutdown extends Command
}