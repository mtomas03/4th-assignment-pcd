package pcd.alarm.system

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import pcd.alarm.system.actors.*
import pcd.alarm.system.domain.ArmingMode.{FullArm, PartialArm}
import pcd.alarm.system.domain.{AlarmState, SystemConfig}

import scala.concurrent.duration.*

class ControlUnitActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  val testConfig = SystemConfig(
    pin = "1234",
    exitDelay = 150.milliseconds,
    entryDelay = 150.milliseconds
  )

  "ControlUnitActor" should {

    "ignore arm requests with incorrect PIN" in {
      val sirenProbe = createTestProbe[SirenActor.Command]()
      val acu = spawn(ControlUnitActor(testConfig, sirenProbe.ref))

      acu ! ControlUnitActor.ArmRequest("0000", FullArm)

      // Sensors must not trigger the alarm since the system remained disarmed
      acu ! ControlUnitActor.SensorEvent("S1", "zone1")
      sirenProbe.expectNoMessage(250.milliseconds)
    }

    "arm system after exit delay and trigger alarm when intrusion occurs" in {
      val sirenProbe = createTestProbe[SirenActor.Command]()
      val acu = spawn(ControlUnitActor(testConfig, sirenProbe.ref))

      acu ! ControlUnitActor.ArmRequest("1234", FullArm)

      // Wait for the exit delay to elapse without triggering the siren
      sirenProbe.expectNoMessage(200.milliseconds)

      // Intrusion event during the Armed state
      acu ! ControlUnitActor.SensorEvent("S1", "zone1")

      // When the entry delay expires, the siren must be activated
      sirenProbe.expectMessage(250.milliseconds, SirenActor.Activate)
    }

    "allow canceling exit delay with valid PIN" in {
      val sirenProbe = createTestProbe[SirenActor.Command]()
      val acu = spawn(ControlUnitActor(testConfig, sirenProbe.ref))

      acu ! ControlUnitActor.ArmRequest("1234", FullArm)
      acu ! ControlUnitActor.PinEntered("1234") // Cancels arming during the exit delay

      // Wait beyond the original exit delay duration
      sirenProbe.expectNoMessage(200.milliseconds)
      acu ! ControlUnitActor.SensorEvent("S1", "zone1")

      sirenProbe.expectNoMessage(250.milliseconds)
    }

    "disarm during entry delay when correct PIN is entered" in {
      val sirenProbe = createTestProbe[SirenActor.Command]()
      val acu = spawn(ControlUnitActor(testConfig, sirenProbe.ref))

      acu ! ControlUnitActor.ArmRequest("1234", FullArm)
      sirenProbe.expectNoMessage(200.milliseconds) // System transitions to Armed

      acu ! ControlUnitActor.SensorEvent("S1", "zone1") // Starts entry delay
      acu ! ControlUnitActor.PinEntered("1234") // Disarms before timeout expires

      // Siren must not be activated
      sirenProbe.expectNoMessage(250.milliseconds)
    }

    "handle partial arming by ignoring inactive zones" in {
      val sirenProbe = createTestProbe[SirenActor.Command]()
      val acu = spawn(ControlUnitActor(testConfig, sirenProbe.ref))

      // Partial arming for the "perimeter" zone only
      acu ! ControlUnitActor.ArmRequest("1234", PartialArm(Set("perimeter")))
      sirenProbe.expectNoMessage(200.milliseconds) // System transitions to Armed

      // Event in an inactive zone -> ignored
      acu ! ControlUnitActor.SensorEvent("S1", "upper-floor")
      sirenProbe.expectNoMessage(200.milliseconds)

      // Event in an active zone -> triggers entry delay and then the alarm
      acu ! ControlUnitActor.SensorEvent("S2", "perimeter")
      sirenProbe.expectMessage(250.milliseconds, SirenActor.Activate)
    }

    "deactivate siren and disarm when correct PIN is entered in alarm state" in {
      val sirenProbe = createTestProbe[SirenActor.Command]()
      val acu = spawn(ControlUnitActor(testConfig, sirenProbe.ref))

      acu ! ControlUnitActor.ArmRequest("1234", FullArm)
      sirenProbe.expectNoMessage(200.milliseconds) // System transitions to Armed

      acu ! ControlUnitActor.SensorEvent("S1", "zone1")

      // Wait for siren activation after entry delay
      sirenProbe.expectMessage(250.milliseconds, SirenActor.Activate)

      // Correct PIN to deactivate the siren
      acu ! ControlUnitActor.PinEntered("1234")
      sirenProbe.expectMessage(SirenActor.Deactivate)
    }

    "report its current state through GetState" in {
      val sirenProbe = createTestProbe[SirenActor.Command]()
      val stateProbe = createTestProbe[AlarmState]()
      val acu = spawn(ControlUnitActor(testConfig, sirenProbe.ref))

      acu ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.Disarmed)

      acu ! ControlUnitActor.ArmRequest("1234", FullArm)
      acu ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.ExitDelay)
    }
  }

  "ControlUnitActor started in SAFE RECOVERY mode" should {

    "not assume the system is disarmed or armed, ignoring sensors and arm requests" in {
      val sirenProbe = createTestProbe[SirenActor.Command]()
      val stateProbe = createTestProbe[AlarmState]()
      val acu = spawn(ControlUnitActor(testConfig, sirenProbe.ref, startInRecovery = true))

      acu ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.SafeRecovery)

      // Neither sensor events nor arm requests are able to move the system
      // out of recovery: only a correct PIN can.
      acu ! ControlUnitActor.SensorEvent("S1", "zone1")
      acu ! ControlUnitActor.ArmRequest("1234", FullArm)
      sirenProbe.expectNoMessage(200.milliseconds)

      acu ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.SafeRecovery)
    }

    "stay in SAFE RECOVERY when a wrong PIN is entered" in {
      val sirenProbe = createTestProbe[SirenActor.Command]()
      val stateProbe = createTestProbe[AlarmState]()
      val acu = spawn(ControlUnitActor(testConfig, sirenProbe.ref, startInRecovery = true))

      acu ! ControlUnitActor.PinEntered("0000")

      acu ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.SafeRecovery)
    }

    "return to DISARMED only once the correct PIN is entered" in {
      val sirenProbe = createTestProbe[SirenActor.Command]()
      val stateProbe = createTestProbe[AlarmState]()
      val acu = spawn(ControlUnitActor(testConfig, sirenProbe.ref, startInRecovery = true))

      acu ! ControlUnitActor.PinEntered("1234")

      acu ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.Disarmed)

      // From here on the FSM behaves exactly as a freshly started unit would.
      acu ! ControlUnitActor.ArmRequest("1234", FullArm)
      acu ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.ExitDelay)
    }
  }
}