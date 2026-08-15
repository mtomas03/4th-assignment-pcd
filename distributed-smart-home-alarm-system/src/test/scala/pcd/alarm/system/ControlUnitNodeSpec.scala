package pcd.alarm.system

import org.apache.pekko.actor.testkit.typed.scaladsl.{FishingOutcomes, ScalaTestWithActorTestKit}
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import pcd.alarm.system.actors.{ControlUnitActor, ControlUnitNode}
import pcd.alarm.system.domain.{AlarmState, SystemConfig}

import scala.concurrent.duration.*

class ControlUnitNodeSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {

  private val testConfig = SystemConfig(
    pin = "1234",
    exitDelay = 150.milliseconds,
    entryDelay = 150.milliseconds
  )

  "ControlUnitNode" should {

    "restart its control unit into SAFE RECOVERY after a crash, under a new ActorRef" in {
      val listingProbe = createTestProbe[Receptionist.Listing]()
      val stateProbe = createTestProbe[AlarmState]()

      spawn(ControlUnitNode(testConfig, simulateCrashAfter = None))
      system.receptionist ! Receptionist.Subscribe(ControlUnitActor.ControlUnitKey, listingProbe.ref)

      /**
       * Waits until an active ControlUnitActor registration is emitted by the Receptionist.
       *
       * Uses `fishForMessage` to discard intermediate or stale listings until a listing containing
       * an `ActorRef` not present in `excluding` is received.
       *
       * @param excluding Set of previously known `ActorRef` instances to ignore, ensuring only newly
       *                  spawned instances are returned after a restart.
       * @return The newly registered `ActorRef` for the active [[ControlUnitActor]].
       */
      def awaitRegisteredRef(excluding: Set[ActorRef[ControlUnitActor.Command]]): ActorRef[ControlUnitActor.Command] = {
        val listings = listingProbe.fishForMessage(5.seconds) {
          case ControlUnitActor.ControlUnitKey.Listing(refs) if refs.diff(excluding).nonEmpty =>
            FishingOutcomes.complete
          case _ =>
            FishingOutcomes.continueAndIgnore
        }
        listings.last match {
          case ControlUnitActor.ControlUnitKey.Listing(refs) => refs.diff(excluding).head
        }
      }

      // Step 1 = Initial startup: node boots normally into DISARMED state
      val initialRef = awaitRegisteredRef(excluding = Set.empty)
      initialRef ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.Disarmed)

      // Step 2 = Trigger crash: uncaught exception terminates the child actor, prompting node guardian supervision
      initialRef ! ControlUnitActor.SimulateCrash

      // Step 3 = Re-discovery: verify replacement actor registers under a fresh ActorRef identity
      val recoveredRef = awaitRegisteredRef(excluding = Set(initialRef))
      recoveredRef should not be initialRef

      // Step 4 = Initial recovery assertion: replacement instance must enter SAFE RECOVERY
      recoveredRef ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.SafeRecovery)

      // Step 5 = Fault-tolerance assertion: sensor events during recovery must be ignored
      recoveredRef ! ControlUnitActor.SensorEvent("S1", "zone1")
      recoveredRef ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.SafeRecovery)

      // Step 6 = Security assertion: invalid PIN entries cannot exit recovery state
      recoveredRef ! ControlUnitActor.PinEntered("0000")
      recoveredRef ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.SafeRecovery)

      // Step 7 = Recovery exit assertion: entering the correct PIN transitions state back to DISARMED
      recoveredRef ! ControlUnitActor.PinEntered("1234")
      recoveredRef ! ControlUnitActor.GetState(stateProbe.ref)
      stateProbe.expectMessage(AlarmState.Disarmed)
    }
  }
}