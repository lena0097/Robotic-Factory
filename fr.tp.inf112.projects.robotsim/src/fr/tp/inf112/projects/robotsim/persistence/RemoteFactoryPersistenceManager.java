package fr.tp.inf112.projects.robotsim.persistence;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.canvas.model.impl.AbstractCanvasPersistenceManager;

/**
 * Remote persistence manager: sends Canvas objects or ids to a remote persistence server.
 */
public class RemoteFactoryPersistenceManager extends AbstractCanvasPersistenceManager {

    private final String host;
    private final int port;
    private final int timeoutMs = 3000;

    public RemoteFactoryPersistenceManager(final CanvasChooser chooser, final String host, final int port) {
        super(chooser);
        this.host = host;
        this.port = port;
    }

    @Override
    public Canvas read(final String canvasId) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                // send only the base name to avoid sending client paths to the server
                final String safeId = canvasId == null ? null : new java.io.File(canvasId).getName();
                oos.flush();
                oos.writeObject(safeId);
                oos.flush();

                final Object resp = ois.readObject();
                if (resp instanceof Canvas) {
                    return (Canvas) resp;
                }
                return null;
            }
            catch (final ClassNotFoundException ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public void persist(final Canvas canvasModel) throws IOException {
        // If the canvas has no id, generate a readable filename so the server will store the model
        if (canvasModel.getId() == null || canvasModel.getId().trim().isEmpty()) {
            canvasModel.setId("factory-" + System.currentTimeMillis() + ".ser");
        }

        // Sanitize id: send only the base filename (strip any path components) so the server
        // stores files inside its storageDir and doesn't try to write arbitrary client paths.
        final String safeId = new java.io.File(canvasModel.getId()).getName();
        canvasModel.setId(safeId);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                oos.flush();
                oos.writeObject(canvasModel);
                oos.flush();

                final Object resp = ois.readObject();
                if (resp instanceof Boolean && Boolean.TRUE.equals(resp)) {
                    return;
                }
                else {
                    throw new IOException("Remote persist failed");
                }
            }
            catch (final ClassNotFoundException ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public boolean delete(final Canvas canvasModel) throws IOException {
        return false;
    }
}
