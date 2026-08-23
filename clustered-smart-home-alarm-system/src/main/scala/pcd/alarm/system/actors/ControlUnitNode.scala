package pcd.alarm.system.actors

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.*
import pcd.alarm.system.domain.SystemConfig

import scala.concurrent.duration.FiniteDuration

/**
 * Top-level guardian for a control unit cluster node.
 */
object ControlUnitNode {

  def apply(config: SystemConfig, simulateCrashAfter: Option[FiniteDuration] = None): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>
      ctx.log.info("Control-unit node starting up")
      val siren = ctx.spawn(SirenActor(), "siren")
      var recoveryCount = 0

      def spawnControlUnit(startInRecovery: Boolean, armCrashTimer: Boolean): ActorRef[ControlUnitActor.Command] = {
        val crashDelay = if (armCrashTimer) simulateCrashAfter else None
        val childName = if (startInRecovery) { recoveryCount += 1; s"control-unit-recovery-$recoveryCount" } else "control-unit-initial"
        val ref = ctx.spawn(
          ControlUnitActor(config, siren, startInRecovery = startInRecovery, simulateCrashAfter = crashDelay),
          childName
        )
        ctx.watch(ref)
        ref
      }

      // First boot: normal startup, no recovery assumptions needed.
      spawnControlUnit(startInRecovery = false, armCrashTimer = true)

      Behaviors.receiveSignal[Nothing] {
        case (context, Terminated(deadRef)) =>
          context.log.error("Control unit [{}] terminated unexpectedly! " +
            "Restarting into Safe Recovery", deadRef.path.name)
          siren ! SirenActor.Deactivate
          spawnControlUnit(startInRecovery = true, armCrashTimer = false)
          Behaviors.same
      }
    }
}
