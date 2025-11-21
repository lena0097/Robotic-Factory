package fr.tp.inf112.projects.robotsim.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;

import fr.tp.inf112.projects.canvas.model.Canvas;

/**
 * Handles one client request: read or persist Canvas (Factory) objects
 */
public class RequestProcessor implements Runnable {

    private final Socket socket;
    private final File storageDir;

    public RequestProcessor(final Socket socket, final File storageDir) {
        this.socket = socket;
        this.storageDir = storageDir;
    }

    @Override
    public void run() {
        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // flush header
            oos.flush();

            final Object request = ois.readObject();

            if (request instanceof String) {
                final String cmd = (String) request;
                if ("__LIST__".equals(cmd)) {
                    final String[] names = storageDir.list();
                    oos.writeObject(names);
                    oos.flush();
                    return;
                }

                // Sanitize incoming id (client may send a path); use only the base name
                final String safeName = cmd == null ? null : new File(cmd).getName();
                final File file = new File(storageDir, safeName);
                if (file.exists()) {
                    try (ObjectInputStream fis = new ObjectInputStream(new FileInputStream(file))) {
                        final Object loaded = fis.readObject();
                        oos.writeObject(loaded);
                        oos.flush();
                    } catch (final Exception e) {
                        // error reading file: return null to client
                        oos.writeObject(null);
                        oos.flush();
                    }
                }
                else {
                    oos.writeObject(null);
                    oos.flush();
                }
            }
            else if (request instanceof Canvas) {
                final Canvas canvas = (Canvas) request;
                final String id = canvas.getId() == null ? String.valueOf(System.currentTimeMillis()) : new File(canvas.getId()).getName();
                final File file = new File(storageDir, id);
                try (ObjectOutputStream fos = new ObjectOutputStream(new FileOutputStream(file))) {
                    fos.writeObject(canvas);
                    fos.flush();
                    // Log the saved file so the server console confirms the write
                    System.out.println("Saved canvas id='" + id + "' -> " + file.getAbsolutePath());
                    oos.writeObject(Boolean.TRUE);
                    oos.flush();
                } catch (final Exception e) {
                    System.err.println("Failed to persist canvas: " + e.getMessage());
                    try {
                        oos.writeObject(Boolean.FALSE);
                        oos.flush();
                    } catch (final Exception ignore) {}
                }
            }
            else {
                oos.writeObject(Boolean.FALSE);
                oos.flush();
            }
        }
        catch (final Exception ex) {
            System.err.println("RequestProcessor error: " + ex.getMessage());
        }
        finally {
            try { socket.close(); } catch (final IOException ignore) {}
        }
    }
}
