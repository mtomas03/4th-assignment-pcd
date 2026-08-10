package pcd.alarm.system.domain

import scala.concurrent.duration.FiniteDuration

type Pin = String
type Zone = String
type SensorId = String

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
sealed trait ArmingMode

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