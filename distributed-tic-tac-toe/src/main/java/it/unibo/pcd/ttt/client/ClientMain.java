package it.unibo.pcd.ttt.client;

import it.unibo.pcd.ttt.client.controller.GameController;
import it.unibo.pcd.ttt.client.model.ClientGameModel;
import it.unibo.pcd.ttt.client.view.MainWindow;
import it.unibo.pcd.ttt.server.GameManager;

import javax.swing.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Entry point of the client.
 */
public final class ClientMain {

    private static final int DEFAULT_PORT = 1099;

    private ClientMain() {
    }

    /**
     * Starts the client.
     *
     * @param args optional arguments: {@code [host [port]]} of the RMI
     *             registry to connect to (defaults to {@code localhost} and
     *             {@value #DEFAULT_PORT})
     * @throws Exception if the remote {@link GameManager} cannot be located
     */
    public static void main(final String[] args) throws Exception {
        final String host = args.length > 0 ? args[0] : "localhost";
        final int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        final Registry registry = LocateRegistry.getRegistry(host, port);
        final GameManager gameManager = (GameManager) registry.lookup(GameManager.REGISTRY_NAME);

        final ClientGameModel model = new ClientGameModel();
        final GameController controller = new GameController(gameManager, model);

        SwingUtilities.invokeLater(() -> new MainWindow(controller, model).setVisible(true));
    }
}
