package pcd.alarm.system.actors

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.*
import pcd.alarm.system.domain.{SensorId, Zone}

/**
 * SensorActor wraps one sensor (motion detector, door/window contact).
 */
object SensorActor {

  def apply(sensorId: SensorId, zone: Zone, acu: ActorRef[ControlUnitActor.Command]): Behavior[Command] =
    Behaviors.receiveMessage {
      case Triggered =>
        acu ! ControlUnitActor.SensorEvent(sensorId, zone)
        Behaviors.same
    }

  sealed trait Command

  case object Triggered extends Command
}