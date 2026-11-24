package com.farmagest;

import com.farmagest.model.AuditLog;
import com.farmagest.model.User;
import com.farmagest.model.enums.Role;
import com.farmagest.service.AuditService;
import com.farmagest.service.AuthService;
import com.farmagest.service.UserService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

final class PharmacyWindow {

    static final Role[] ALL = {Role.WORKER, Role.ADMIN, Role.DEVELOPER};
    static final Role[] MANAGERS = {Role.ADMIN, Role.DEVELOPER};

    final Stage stage;
    final User user;
    final AuthService auth;

    private final Runnable logout;
    private final BorderPane root = new BorderPane();
    private final Label status = new Label();
    private final VBox menu = new VBox(5);

    PharmacyWindow(Stage stage, User user, AuthService auth, Runnable logout) {
        this.stage = stage;
        this.user = user;
        this.auth = auth;
        this.logout = logout;
    }

    void show() {
        CatalogPages catalog = new CatalogPages(this);
        OperationsPages operations = new OperationsPages(this);

        menu.setPadding(new Insets(12));
        menu.setPrefWidth(175);

        nav("Productos", catalog::products);
        nav("Categorías", catalog::categories);
        nav("Fabricantes", catalog::manufacturers);
        nav("Proveedores", catalog::suppliers);
        nav("Clientes", catalog::customers);
        nav("Ventas", operations::sales);
        nav("Inventario", operations::stock);
        nav("Stock bajo", operations::lowStock);

        if (isManager()) {
            nav("Compras", operations::purchases);
            nav("Usuarios", catalog::users);
            nav("Auditoría", operations::audit);
        }

        nav("Mi contraseña", this::changePassword);
        nav("Cerrar sesión", logout);

        ScrollPane navigation = new ScrollPane(menu);
        navigation.setFitToWidth(true);
        root.setLeft(navigation);

        Label header = new Label("FarmaGest    ·    " + user.getUsername()
                + "    ·    " + user.getRole());
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        root.setTop(header);

        status.setPadding(new Insets(8, 16, 10, 16));
        status.setWrapText(true);
        root.setBottom(status);

        stage.setScene(new Scene(root, 1120, 680));
        catalog.products();
    }

    boolean isManager() {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.DEVELOPER;
    }

    private void nav(String name, Runnable action) {
        Button button = button(name, action);
        button.setMaxWidth(Double.MAX_VALUE);
        menu.getChildren().add(button);
    }

    static Button button(String name, Runnable action) {
        Button button = new Button(name);
        button.setOnAction(event -> action.run());
        return button;
    }

    void message(String text) {
        status.setText(text);
    }

    <T> void load(Role[] roles, Callable<T> work, Consumer<T> success) {
        Main.execute(root, status, () -> {
            auth.requireRole(user, roles);
            return work.call();
        }, success);
    }

    <T> void save(Role[] roles, String action, String entity,
                  Callable<T> work, Function<T, Long> entityId, Runnable refresh) {
        record Outcome(String warning) {}

        load(roles, () -> {
            T result = work.call();
            try {
                new AuditService().record(new AuditLog(
                        0, user.getId(), action, entity, entityId.apply(result),
                        null, null, null, null
                ));
                return new Outcome(null);
            } catch (RuntimeException ex) {
                return new Outcome("La operación ya se guardó. Falló el registro de auditoría: "
                        + ex.getMessage());
            }
        }, result -> {
            if (result.warning() != null) {
                Alert warning = new Alert(Alert.AlertType.WARNING, result.warning(), ButtonType.OK);
                warning.initOwner(stage);
                warning.showAndWait();
            }
            refresh.run();
        });
    }

    boolean confirm(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text,
                ButtonType.CANCEL, ButtonType.OK);
        alert.initOwner(stage);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    record Column<T>(String title, Function<T, ?> value) {
        String text(T row) {
            Object result = value.apply(row);
            return result == null ? "" : result.toString();
        }
    }

    @SafeVarargs
    final <T> TableView<T> page(String title, List<T> data, FlowPane actions,
                                Column<T>... columns) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 21px; -fx-font-weight: bold;");

        TextField search = new TextField();
        search.setPromptText("Buscar en esta tabla");

        TableView<T> table = new TableView<>();
        table.setPlaceholder(new Label("Sin registros"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        for (Column<T> definition : columns) {
            TableColumn<T, String> column = new TableColumn<>(definition.title());
            column.setCellValueFactory(cell ->
                    new ReadOnlyStringWrapper(definition.text(cell.getValue())));
            column.setSortable(false);
            table.getColumns().add(column);
        }

        var rows = new FilteredList<>(FXCollections.observableArrayList(data));
        table.setItems(rows);
        search.textProperty().addListener((obs, previous, value) -> {
            String query = value.strip().toLowerCase(Locale.ROOT);
            rows.setPredicate(row -> Arrays.stream(columns).anyMatch(column ->
                    column.text(row).toLowerCase(Locale.ROOT).contains(query)));
        });

        actions.setHgap(7);
        actions.setVgap(7);
        VBox body = new VBox(10, heading, actions, search, table);
        body.setPadding(new Insets(14));
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(body);
        message(data.size() + " registros");
        return table;
    }

    <T> void selected(TableView<T> table, Consumer<T> action) {
        T value = table.getSelectionModel().getSelectedItem();
        if (value == null) {
            message("Selecciona primero una fila.");
        } else {
            action.accept(value);
        }
    }

    private void changePassword() {
        Form form = new Form(stage, "Cambiar mi contraseña");
        PasswordField current = form.password("Contraseña actual");
        PasswordField next = form.password("Nueva contraseña");
        PasswordField repeat = form.password("Repetir contraseña");

        form.read(() -> {
            if (next.getText().length() < 12 || !next.getText().equals(repeat.getText())) {
                throw new IllegalArgumentException(
                        "La contraseña debe tener al menos 12 caracteres y coincidir en ambos campos.");
            }
            return new char[][] {
                    current.getText().toCharArray(), next.getText().toCharArray()
            };
        }).ifPresent(pair -> Main.execute(root, status, () -> {
            try {
                auth.requireRole(user, ALL);
                new UserService().changePassword(user.getId(), pair[0], pair[1]);
                return true;
            } finally {
                Arrays.fill(pair[0], '\0');
                Arrays.fill(pair[1], '\0');
            }
        }, ignored -> message("Contraseña actualizada.")));

        current.clear();
        next.clear();
        repeat.clear();
    }
}