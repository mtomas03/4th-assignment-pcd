package pcd.alarm.system.actors

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.*
import pcd.alarm.system.domain.ArmingMode.{FullArm, PartialArm}
import pcd.alarm.system.domain.{CborSerializable, Pin, Zone}

/**
 * KeypadActor represents the numeric keypad the user interacts with.
 */
object KeypadActor {

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      val listingAdapter = ctx.messageAdapter[Receptionist.Listing](ControlUnitListing.apply)
      ctx.system.receptionist ! Receptionist.Subscribe(ControlUnitActor.ControlUnitKey, listingAdapter)
      awaitingControlUnit()
    }

  private def awaitingControlUnit(): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case ControlUnitListing(ControlUnitActor.ControlUnitKey.Listing(refs)) =>
          refs.headOption match {
            case Some(acu) =>
              ctx.log.info("Keypad discovered the control unit")
              connected(acu)
            case None =>
              Behaviors.same
          }

        case _: ArmFull | _: ArmPartial | _: EnterPin =>
          ctx.log.warn("Keypad input ignored - no control unit reachable yet")
          Behaviors.same

        case ControlUnitListing(_) =>
          Behaviors.same
      }
    }

  private def connected(acu: ActorRef[ControlUnitActor.Command]): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case ControlUnitListing(ControlUnitActor.ControlUnitKey.Listing(refs)) =>
          refs.headOption match {
            case Some(newAcu) =>
              connected(newAcu)
            case None =>
              ctx.log.warn("Keypad lost the control unit - awaiting rediscovery")
              awaitingControlUnit()
          }

        // Arm every zone.
        case ArmFull(pin) =>
          acu ! ControlUnitActor.ArmRequest(pin, FullArm)
          Behaviors.same

        // Arm only the listed zones.
        case ArmPartial(pin, zones) =>
          acu ! ControlUnitActor.ArmRequest(pin, PartialArm(zones))
          Behaviors.same

        // Disarm / silence alarm / exit recovery mode.
        case EnterPin(pin) =>
          acu ! ControlUnitActor.PinEntered(pin)
          Behaviors.same

        case ControlUnitListing(_) =>
          Behaviors.same
      }
    }

  sealed trait Command extends CborSerializable

  final case class ArmFull(pin: Pin) extends Command

  final case class ArmPartial(pin: Pin, zones: Set[Zone]) extends Command

  final case class EnterPin(pin: Pin) extends Command

  private final case class ControlUnitListing(listing: Receptionist.Listing) extends Command
}