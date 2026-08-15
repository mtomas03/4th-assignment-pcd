package pcd.alarm.system.actors

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.*
import pcd.alarm.system.domain.{CborSerializable, Pin, Zone}

/**
 * Top-level guardian managing a keypad node and its lifecycle.
 */
object KeypadNode {

  sealed trait Command extends CborSerializable

  final case class ArmFull(pin: Pin) extends Command

  final case class ArmPartial(pin: Pin, zones: Set[Zone]) extends Command

  final case class EnterPin(pin: Pin) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      ctx.log.info("Keypad node starting up")
      val keypad = ctx.spawn(KeypadActor(), "keypad")

      Behaviors.receiveMessage {
        case ArmFull(pin) =>
          keypad ! KeypadActor.ArmFull(pin)
          Behaviors.same
        case ArmPartial(pin, zones) =>
          keypad ! KeypadActor.ArmPartial(pin, zones)
          Behaviors.same
        case EnterPin(pin) =>
          keypad ! KeypadActor.EnterPin(pin)
          Behaviors.same
      }
    }
}