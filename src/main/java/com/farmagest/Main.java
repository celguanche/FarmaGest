package com.farmagest;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.model.User;
import com.farmagest.model.enums.Role;
import com.farmagest.service.AuthService;
import com.farmagest.service.UserService;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
        TextField username = new TextField(setup ? "admin" : "");
        username.setEditable(!setup);

        PasswordField password = new PasswordField();
        TextField email = new TextField();
        Label status = new Label();
        status.setWrapText(true);

        Button enter = new Button(setup ? "Crear administrador y entrar" : "Entrar");
        enter.setDefaultButton(true);
        enter.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label(setup ? "Primer administrador" : "FarmaGest");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        VBox form = new VBox(10, title, new Label("Usuario"), username,
                new Label("Contraseña"), password);

        if (setup) {
            form.getChildren().addAll(new Label("Correo del administrador"), email);
        }

        form.getChildren().addAll(enter, status);
        form.setPadding(new Insets(24));
        form.setMaxSize(380, Region.USE_PREF_SIZE);
        window.setScene(new Scene(new StackPane(form), 900, 580));

        enter.setOnAction(event -> {
            String name = username.getText().trim();
            String address = email.getText().trim();
            char[] secret = password.getText().toCharArray();
            password.clear();

            execute(form, status, () -> {
                try {
                    if (setup) {
                        createAdmin(address, secret);
                    }
                    return auth.login(name, secret);
                } finally {
                    Arrays.fill(secret, '\0');
                }
            }, user -> new PharmacyWindow(window, user, auth,
                    () -> showLogin(false)).show());
        });
    }

    private void createAdmin(String email, char[] password) {
        UserService users = new UserService();

        if (!users.findAll().isEmpty()) {
            throw new IllegalStateException(
                    "Ya existen usuarios. Inicia la aplicación sin --init-admin."
            );
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
            Throwable error = task.getException();
            String message = error.getMessage();
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