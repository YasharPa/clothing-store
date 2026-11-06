package client;

import common.Request;
import common.RequestType;
import common.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * The window after a successful login. For now it shows who is logged in and has a "Log out" button;
 * the shop's screens (inventory, customers, chat...) will be added here by the team.
 */
@SuppressWarnings("serial") // JFrame is Serializable, but windows are never sent over the network
public final class MainWindow extends JFrame {

    private final ServerConnection connection;
    private final JButton logoutButton = new JButton("Log out");

    /**
     * @param user       who logged in (from the server's login answer)
     * @param connection the open, logged-in connection; this window now owns it and closes it on log out
     */
    public MainWindow(User user, ServerConnection connection) {
        super("Store Chain - " + user.getUsername());
        this.connection = connection;

        // Closing the window ends the program; closing the connection first logs the user out on the server.
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                connection.close();
            }
        });

        JLabel welcome = new JLabel("Logged in as " + user.getUsername());
        welcome.setName("welcome");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 16f));
        JLabel details = new JLabel("Role: " + user.getRole() + "    Branch: " + user.getBranch());
        details.setName("details");
        JLabel placeholder = new JLabel("(The shop's screens will be added here.)");
        placeholder.setForeground(Color.GRAY);

        JPanel text = new JPanel(new GridLayout(3, 1, 0, 6));
        text.add(welcome);
        text.add(details);
        text.add(placeholder);

        logoutButton.setName("logout");
        logoutButton.addActionListener(event -> logOut());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(logoutButton);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        content.add(text, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);

        pack();
        setLocationRelativeTo(null);
    }

    /** Tells the server we are logging out, closes the connection, and goes back to the login window. */
    private void logOut() {
        logoutButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    connection.send(new Request(RequestType.LOGOUT, null));
                } catch (IOException e) {
                    // The connection is already broken; the server logs the user out when it notices.
                } finally {
                    connection.close();
                }
                return null;
            }

            @Override
            protected void done() {
                dispose();
                new LoginWindow().setVisible(true);
            }
        }.execute();
    }
}
