package pcd.alarm.system.actors

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.*
import pcd.alarm.system.*
import pcd.alarm.system.domain.ArmingMode.{FullArm, PartialArm}
import pcd.alarm.system.domain.{Pin, Zone}

/**
 * KeypadActor represents the numeric keypad the user interacts with.
 */
object KeypadActor {

  def apply(acu: ActorRef[ControlUnitActor.Command]): Behavior[Command] =
    Behaviors.receiveMessage {

      // Arm every zone.
      case ArmFull(pin) =>
        acu ! ControlUnitActor.ArmRequest(pin, FullArm)
        Behaviors.same

      // Arm only the listed zones.
      case ArmPartial(pin, zones) =>
        acu ! ControlUnitActor.ArmRequest(pin, PartialArm(zones))
        Behaviors.same

      // Disarm / silence alarm.
      case EnterPin(pin) =>
        acu ! ControlUnitActor.PinEntered(pin)
        Behaviors.same
    }

  sealed trait Command

  final case class ArmFull(pin: Pin) extends Command

  final case class ArmPartial(pin: Pin, zones: Set[Zone]) extends Command

  final case class EnterPin(pin: Pin) extends Command
}