package client;

import common.Credentials;
import common.Protocol;
import common.Request;
import common.RequestType;
import common.Response;
import common.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.ExecutionException;

/**
 * The login screen: a username box, a hidden password box, a "Log in" button and a line for messages.
 * Talking to the server happens in the background (SwingWorker), so the window never freezes.
 */
@SuppressWarnings("serial") // JFrame is Serializable, but windows are never sent over the network
public final class LoginWindow extends JFrame {

    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JButton loginButton = new JButton("Log in");
    private final JLabel messageLabel = new JLabel(" ");

    /** The connection to the server: made at the first attempt and kept for retries. Null when not connected. */
    private ServerConnection connection;

    /** Builds the login window. Call setVisible(true) to show it. */
    public LoginWindow() {
        super("Store Chain - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Names let the tests find the parts of the window.
        usernameField.setName("username");
        passwordField.setName("password");
        loginButton.setName("login");
        messageLabel.setName("message");
        messageLabel.setForeground(new Color(0xB00020)); // dark red
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel fields = new JPanel(new GridLayout(2, 2, 8, 8));
        fields.add(new JLabel("Username:"));
        fields.add(usernameField);
        fields.add(new JLabel("Password:"));
        fields.add(passwordField);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        content.add(fields, BorderLayout.NORTH);
        content.add(loginButton, BorderLayout.CENTER);
        content.add(messageLabel, BorderLayout.SOUTH);
        setContentPane(content);

        loginButton.addActionListener(event -> logIn());
        getRootPane().setDefaultButton(loginButton); // pressing Enter = clicking "Log in"

        pack();
        setResizable(false);
        setLocationRelativeTo(null); // middle of the screen
    }

    /** Runs when "Log in" is clicked. */
    private void logIn() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter a username and a password");
            return;
        }

        setBusy(true);
        showMessage("Logging in...");

        // doInBackground() runs on a background thread (it may wait for the network);
        // done() runs afterwards on the window's own thread, where it is safe to change the window.
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws IOException {
                if (connection == null) {
                    connection = new ServerConnection(Protocol.HOST, Protocol.PORT);
                }
                return connection.send(new Request(RequestType.LOGIN, new Credentials(username, password)));
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        openMainWindow((User) response.getData());
                    } else {
                        passwordField.setText("");
                        showMessage(response.getMessage());
                    }
                } catch (ExecutionException e) {
                    // A network problem: forget the connection, so the next attempt connects again.
                    closeConnection();
                    showMessage(e.getCause() instanceof ConnectException
                            ? "Cannot reach the server. Is it running?"
                            : "Lost the connection to the server. Please try again.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void openMainWindow(User user) {
        new MainWindow(user, connection).setVisible(true);
        connection = null; // the main window owns the connection now
        dispose();
    }

    /** While waiting for the server, the boxes and the button are disabled (no double clicks). */
    private void setBusy(boolean busy) {
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        loginButton.setEnabled(!busy);
    }

    private void showMessage(String text) {
        messageLabel.setText(text);
        pack(); // resize the window so a long message fits
    }

    private void closeConnection() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}
