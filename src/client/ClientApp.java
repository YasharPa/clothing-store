package client;

import javax.swing.SwingUtilities;

/** Starts the graphical client by opening the login window. (Client.java is the old text version, kept for testing.) */
public class ClientApp {

    /** Starts the graphical client. */
    public static void main(String[] args) {
        // Swing windows must be created on Swing's own thread; invokeLater puts the job there.
        SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
    }
}
