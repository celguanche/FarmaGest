package com.farmagest;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.model.User;
import com.farmagest.model.enums.Role;
import com.farmagest.service.AuthService;
import com.farmagest.service.UserService;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class Main extends Application {

    private final AuthService auth = new AuthService();
    private Stage window;

    @Override
    public void start(Stage stage) {
        window = stage;
        window.setTitle("FarmaGest");
        showLogin(getParameters().getRaw().contains("--init-admin"));
        window.show();
    }

    private void showLogin(boolean setup) {
        View view = new View("access");
        StackPane root = (StackPane) view.root;

        TextField username = view.get("username", TextField.class);
        PasswordField password = view.get("password", PasswordField.class);
        TextField visiblePassword = view.get("visiblePassword", TextField.class);
        ToggleButton reveal = view.get("reveal", ToggleButton.class);
        TextField email = view.get("email", TextField.class);
        Button enter = view.get("enter", Button.class);
        Label status = view.get("status", Label.class);

        visiblePassword.textProperty().bindBidirectional(password.textProperty());

        reveal.selectedProperty().addListener((obs, old, visible) -> {
            visiblePassword.setManaged(visible);
            visiblePassword.setVisible(visible);
            password.setManaged(!visible);
            password.setVisible(!visible);
            reveal.setText(visible ? "Ocultar" : "Ver");
        });

        if (setup) {
            username.setText("admin");
            username.setEditable(false);
            view.get("title", Label.class).setText("Primer administrador");
            view.get("subtitle", Label.class)
                    .setText("Elige una contraseña de al menos 12 caracteres.");

            VBox emailRow = view.get("emailRow", VBox.class);
            emailRow.setVisible(true);
            emailRow.setManaged(true);
            enter.setText("Crear administrador y entrar");
        }

        enter.setOnAction(event -> {
            String name = username.getText().strip();
            String address = email.getText().strip();
            char[] secret = password.getText().toCharArray();
            password.clear();

            execute(root, status, () -> {
                try {
                    if (setup) createAdmin(address, secret);
                    return auth.login(name, secret);
                } finally {
                    Arrays.fill(secret, '\0');
                }
            }, user -> new PharmacyWindow(window, user, auth, () -> showLogin(false)).show());
        });

        window.setScene(new Scene(root, 1024, 680));
    }

    private void createAdmin(String email, char[] password) {
        UserService users = new UserService();

        if (!users.findAll().isEmpty()) {
            throw new IllegalStateException("Ya existen usuarios. Inicia sin --init-admin.");
        }

        User admin = new User(
                0, "admin", null, "Administrador", "FarmaGest",
                email, Role.ADMIN, true, null, null, null
        );
        users.create(admin, password);
    }

    static <T> void execute(Pane pane, Label status, Callable<T> work,
                            Consumer<T> success) {
        pane.setDisable(true);
        status.setText("Procesando…");

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(event -> {
            pane.setDisable(false);
            status.setText("");
            success.accept(task.getValue());
        });

        task.setOnFailed(event -> {
            pane.setDisable(false);
            String message = task.getException().getMessage();
            status.setText(message == null ? "Error al realizar la operación." : message);
        });

        Thread thread = new Thread(task, "farmagest-db");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void stop() {
        DatabaseConnection.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}