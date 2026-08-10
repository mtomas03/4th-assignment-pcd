package pcd.alarm.system.actors

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.*

/**
 * SirenActor represents the siren device.
 */
object SirenActor {

  def apply(): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Activate =>
          ctx.log.info("Siren ON!")
          Behaviors.same
        case Deactivate =>
          ctx.log.info("Siren OFF!")
          Behaviors.same
      }
    }

  sealed trait Command

  case object Activate extends Command

  case object Deactivate extends Command
}