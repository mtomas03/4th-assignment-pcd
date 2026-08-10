package it.unibo.pcd.ttt.client.model;

/**
 * Listener notified whenever the observable state of a {@link ClientGameModel}
 * changes. Implemented by the view layer, which is the only thing that
 * reacts to model changes (the classic Observer role of MVC).
 */
@FunctionalInterface
public interface GameModelListener {

    /**
     * Called after the model's state has been updated.
     *
     * @param model the model whose state changed
     */
    void onModelChanged(ClientGameModel model);
}
