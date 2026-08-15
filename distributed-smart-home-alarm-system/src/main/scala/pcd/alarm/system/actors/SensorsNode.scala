package pcd.alarm.system.actors

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.*
import pcd.alarm.system.domain.{CborSerializable, SensorId, Zone}

/**
 * Top-level guardian managing a sensors node and its lifecycle.
 */
object SensorsNode {

  sealed trait Command extends CborSerializable

  final case class Trigger(sensorId: SensorId) extends Command

  def apply(sensors: List[(SensorId, Zone)]): Behavior[Command] =
    Behaviors.setup { ctx =>
      ctx.log.info("Sensors node starting up with {} sensor(s)", sensors.size)

      val sensorRefs: Map[SensorId, ActorRef[SensorActor.Command]] =
        sensors.map { case (id, zone) =>
          id -> ctx.spawn(SensorActor(id, zone), s"sensor-$id")
        }.toMap

      Behaviors.receiveMessage {
        case Trigger(sensorId) =>
          sensorRefs.get(sensorId) match {
            case Some(ref) => ref ! SensorActor.Triggered
            case None => ctx.log.warn("Unknown sensor id [{}] on this node", sensorId)
          }
          Behaviors.same
      }
    }
}