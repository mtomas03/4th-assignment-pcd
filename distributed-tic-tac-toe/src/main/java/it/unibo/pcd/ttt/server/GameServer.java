package it.unibo.pcd.ttt.server;

import it.unibo.pcd.ttt.shared.GameManager;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Entry point of the server.
 */
public final class GameServer {

    private static final int DEFAULT_PORT = 1099;

    private GameServer() {
    }

    /**
     * Starts the server.
     *
     * @param args optional single argument: the TCP port the RMI registry
     *             should listen on (defaults to {@value #DEFAULT_PORT})
     * @throws Exception if the registry cannot be created or the manager
     *                   cannot be bound
     */
    public static void main(final String[] args) throws Exception {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        final Registry registry = LocateRegistry.createRegistry(port);
        final GameManagerImpl manager = new GameManagerImpl();
        registry.rebind(GameManager.REGISTRY_NAME, manager);

        System.out.println("Distributed Tic-Tac-Toe server ready on port " + port
                + ", bound as '" + GameManager.REGISTRY_NAME + "'.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                registry.unbind(GameManager.REGISTRY_NAME);
                UnicastRemoteObject.unexportObject(manager, true);
            } catch (final Exception ignored) {
                // Best-effort clean-up on shutdown.
            }
        }, "ttt-server-shutdown"));
    }
}
