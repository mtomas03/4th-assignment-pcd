package pcd.alarm.system.domain

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import scala.concurrent.duration.FiniteDuration

type Pin = String
type Zone = String
type SensorId = String

/**
 * Types extending this marker trait are mapped to the Jackson CBOR serializer.
 */
trait CborSerializable

/**
 * Configuration parameters for the alarm system.
 */
final case class SystemConfig(
                               pin: Pin,
                               exitDelay: FiniteDuration,
                               entryDelay: FiniteDuration
                             )

/**
 * Defines the arming strategy for monitoring zones when armed.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[ArmingMode.PartialArm], name = "PartialArm"),
  new JsonSubTypes.Type(value = classOf[ArmingMode.FullArm.type], name = "FullArm")
))
sealed trait ArmingMode extends CborSerializable

object ArmingMode {

  /**
   * Arms only a specified subset of zones.
   *
   * @param zones The active zones to monitor. Events from sensors outside this set are ignored while armed.
   */
  final case class PartialArm(zones: Set[Zone]) extends ArmingMode

  /**
   * Arms all system zones. Any sensor activity across the system will initiate an entry delay.
   */
  case object FullArm extends ArmingMode
}

/**
 * The observable state of the alarm control unit,
 * exposed only for monitoring/testing purposes.
 */
sealed trait AlarmState extends CborSerializable

object AlarmState {
  case object Disarmed extends AlarmState
  case object ExitDelay extends AlarmState
  final case class Armed(activeZones: Option[Set[Zone]]) extends AlarmState
  case object EntryDelay extends AlarmState
  case object Alarm extends AlarmState
  case object SafeRecovery extends AlarmState
}