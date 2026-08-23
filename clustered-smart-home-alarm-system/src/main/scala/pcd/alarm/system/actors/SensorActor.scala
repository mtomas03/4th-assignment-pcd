package pcd.alarm.system.actors

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.*
import pcd.alarm.system.domain.{CborSerializable, SensorId, Zone}

/**
 * SensorActor wraps one sensor (motion detector, door/window contact).
 */
object SensorActor {

  def apply(sensorId: SensorId, zone: Zone): Behavior[Command] =
    Behaviors.setup { ctx =>
      val listingAdapter = ctx.messageAdapter[Receptionist.Listing](ControlUnitListing.apply)
      ctx.system.receptionist ! Receptionist.Subscribe(ControlUnitActor.ControlUnitKey, listingAdapter)
      awaitingControlUnit(sensorId, zone)
    }

  private def awaitingControlUnit(sensorId: SensorId, zone: Zone): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case ControlUnitListing(ControlUnitActor.ControlUnitKey.Listing(refs)) =>
          refs.headOption match {
            case Some(acu) =>
              ctx.log.info("Sensor {} discovered the control unit", sensorId)
              connected(sensorId, zone, acu)
            case None =>
              Behaviors.same
          }

        case Triggered =>
          ctx.log.warn("Sensor {} triggered but no control unit is reachable yet - event lost", sensorId)
          Behaviors.same

        case ControlUnitListing(_) =>
          Behaviors.same
      }
    }

  private def connected(sensorId: SensorId, zone: Zone, acu: ActorRef[ControlUnitActor.Command]): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case ControlUnitListing(ControlUnitActor.ControlUnitKey.Listing(refs)) =>
          refs.headOption match {
            case Some(newAcu) =>
              connected(sensorId, zone, newAcu)
            case None =>
              ctx.log.warn("Sensor {} lost the control unit - awaiting rediscovery", sensorId)
              awaitingControlUnit(sensorId, zone)
          }

        case Triggered =>
          ctx.log.info("Sensor {} ({}) triggered", sensorId, zone)
          acu ! ControlUnitActor.SensorEvent(sensorId, zone)
          Behaviors.same

        case ControlUnitListing(_) =>
          Behaviors.same
      }
    }

  sealed trait Command extends CborSerializable

  case object Triggered extends Command

  private final case class ControlUnitListing(listing: Receptionist.Listing) extends Command
}