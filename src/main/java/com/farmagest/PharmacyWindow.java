package com.farmagest;

import com.farmagest.dao.DashboardDAO;
import com.farmagest.model.AuditLog;
import com.farmagest.model.Supplier;
import com.farmagest.model.User;
import com.farmagest.model.enums.Role;
import com.farmagest.service.AuditService;
import com.farmagest.service.AuthService;
import com.farmagest.service.UserService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
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
    private final View shell = new View("workspace");
    private final BorderPane root = (BorderPane) shell.root;
    private final Label status = shell.get("status", Label.class);
    private final VBox menu = shell.get("menu", VBox.class);

    PharmacyWindow(Stage stage, User user, AuthService auth, Runnable logout) {
        this.stage = stage;
        this.user = user;
        this.auth = auth;
        this.logout = logout;
    }

    void show() {
        CatalogPages catalog = new CatalogPages(this);
        OperationsPages operations = new OperationsPages(this);

        addPage("Inicio", this::dashboard);
        addPage("Productos", catalog::products);
        addPage("Categorías", catalog::categories);
        addPage("Fabricantes", catalog::manufacturers);
        addPage("Proveedores", catalog::suppliers);
        addPage("Clientes", catalog::customers);
        addPage("Ventas", operations::sales);
        addPage("Inventario", operations::stock);
        addPage("Stock bajo", operations::lowStock);

        if (isManager()) {
            addPage("Compras", operations::purchases);
            addPage("Usuarios", catalog::users);
            addPage("Auditoría", operations::audit);
        }

        addMenuAction("Mi contraseña", this::changePassword);
        addMenuAction("Cerrar sesión", logout);

        shell.get("identity", Label.class)
                .setText(user.getUsername() + " · " + user.getRole());

        stage.setScene(new Scene(root, 1160, 730));
        activate((Button) menu.getChildren().get(0));
        dashboard();
    }

    boolean isManager() {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.DEVELOPER;
    }

    private Button addMenuAction(String name, Runnable action) {
        Button button = button(name, action);
        button.getStyleClass().add("nav");
        button.setMaxWidth(Double.MAX_VALUE);
        menu.getChildren().add(button);
        return button;
    }

    private void addPage(String name, Runnable action) {
        Button button = addMenuAction(name, action);
        button.setOnAction(event -> {
            activate(button);
            action.run();
        });
    }

    private void activate(Button current) {
        PseudoClass state = PseudoClass.getPseudoClass("active-page");
        menu.getChildren().forEach(node ->
                node.pseudoClassStateChanged(state, node == current));
    }

    static Button button(String name, Runnable action) {
        Button button = new Button(name);
        button.setOnAction(event -> action.run());

        if (name.startsWith("Nuev") || name.startsWith("Recibir")) {
            button.getStyleClass().add("primary");
        }
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

    private void dashboard() {
        load(ALL, new DashboardDAO()::read, data -> {
            View view = new View("dashboard");

            view.get("active", Label.class).setText(String.valueOf(data.products()));
            view.get("low", Label.class).setText(String.valueOf(data.lowStock()));
            view.get("expiring", Label.class).setText(String.valueOf(data.expiring()));

            TableView<DashboardDAO.Notice> table = table(view, "alerts");

            configure(table,
                    new Column<>("Producto", DashboardDAO.Notice::product),
                    new Column<>("Tipo de alerta", DashboardDAO.Notice::detail),
                    new Column<>("Prioridad", DashboardDAO.Notice::priority),
                    new Column<>("Revisión / caducidad", DashboardDAO.Notice::date));

            table.setPlaceholder(new Label("Sin alertas de stock ni de caducidad"));
            table.getItems().setAll(data.alerts());

            root.setCenter(view.root);
            message("Caducidades previstas en los próximos 30 días");
        });
    }

    <T> void save(Role[] roles, String action, String entity,
                  Callable<T> work, Function<T, Long> entityId, Runnable refresh) {
        load(roles, () -> {
            T result = work.call();

            try {
                new AuditService().record(new AuditLog(
                        0, user.getId(), action, entity, entityId.apply(result),
                        null, null, null, null
                ));
                return "";
            } catch (RuntimeException ex) {
                return "La operación ya se guardó. Falló el registro de auditoría: "
                        + ex.getMessage();
            }
        }, warning -> {
            if (!warning.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, warning, ButtonType.OK);
                style(alert);
                alert.showAndWait();
            }
            refresh.run();
        });
    }

    boolean confirm(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text,
                ButtonType.CANCEL, ButtonType.OK);
        style(alert);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void style(Dialog<?> dialog) {
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(View.css());
    }

    record Column<T>(String title, Function<T, ?> value) {
        String text(T row) {
            Object result = value.apply(row);
            return result == null ? "" : result.toString();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> TableView<T> table(View view, String id) {
        return (TableView<T>) view.get(id, TableView.class);
    }

    @SafeVarargs
    private static <T> void configure(TableView<T> table, Column<T>... definitions) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        for (Column<T> definition : definitions) {
            TableColumn<T, String> column = new TableColumn<>(definition.title());
            column.setCellValueFactory(cell ->
                    new ReadOnlyStringWrapper(definition.text(cell.getValue())));
            column.setSortable(false);
            table.getColumns().add(column);
        }
    }

    @SafeVarargs
    final <T> TableView<T> page(String title, List<T> data,
                                FlowPane actions, Column<T>... columns) {
        View view = new View("list");
        view.get("heading", Label.class).setText(title);

        TextField search = view.get("search", TextField.class);
        TableView<T> table = table(view, "table");
        configure(table, columns);
        table.setPlaceholder(new Label("Sin registros"));

        actions.setHgap(7);
        actions.setVgap(7);
        view.get("toolbar", FlowPane.class).getChildren().add(actions);

        var rows = new FilteredList<>(FXCollections.observableArrayList(data));
        table.setItems(rows);

        search.textProperty().addListener((obs, old, value) -> {
            String query = value.strip().toLowerCase(Locale.ROOT);
            rows.setPredicate(row -> Arrays.stream(columns).anyMatch(column ->
                    column.text(row).toLowerCase(Locale.ROOT).contains(query)));
        });

        if (title.equals("Proveedores")) {
            search.setPromptText("Buscar proveedor por razón social, NIF o contacto…");
            search.setMaxWidth(Double.MAX_VALUE);

            Label subtitle = view.get("subtitle", Label.class);
            subtitle.setText("Gestión de proveedores de la farmacia.");
            subtitle.setVisible(true);
            subtitle.setManaged(true);

            supplierDetail(view, table, actions);
            if (!rows.isEmpty()) table.getSelectionModel().selectFirst();
        }

        root.setCenter(view.root);
        message(data.size() + " registros");
        return table;
    }

    private <T> void supplierDetail(View page, TableView<T> table, FlowPane actions) {
        View detail = new View("supplier-detail");
        StackPane target = page.get("detail", StackPane.class);
        target.getChildren().setAll(detail.root);

        // CatalogPages adds its buttons after page() returns.
        Platform.runLater(() -> {
            for (var node : List.copyOf(actions.getChildren())) {
                if (!(node instanceof Button command)) continue;

                if (command.getText().equals("Nuevo")) {
                    actions.getChildren().remove(command);
                    command.setText("+  Nuevo proveedor");
                    page.get("primaryActions", HBox.class).getChildren().add(command);
                } else if (command.getText().equals("Editar")) {
                    actions.getChildren().remove(command);
                    command.setText("Editar proveedor");
                    command.getStyleClass().add("primary");
                    detail.get("editActions", HBox.class).getChildren().add(command);
                }
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            boolean selected = row instanceof Supplier;
            target.setManaged(selected);
            target.setVisible(selected);

            if (row instanceof Supplier supplier) {
                detail.get("company", Label.class).setText(supplier.getBusinessName());
                detail.get("tax", Label.class).setText(supplier.getTaxId());
                detail.get("contact", Label.class).setText(supplier.getContactName());
                detail.get("phone", Label.class).setText(supplier.getPhone());
                detail.get("address", Label.class).setText(supplier.getAddress());
                detail.get("email", Label.class).setText(supplier.getEmail());

                Label state = detail.get("state", Label.class);
                state.setText(supplier.isActive() ? "Activo" : "Inactivo");
                state.pseudoClassStateChanged(
                        PseudoClass.getPseudoClass("inactive"), !supplier.isActive());
            }
        });
    }

    <T> void selected(TableView<T> table, Consumer<T> action) {
        T row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            message("Selecciona primero una fila.");
        } else {
            action.accept(row);
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
                        "La nueva contraseña debe tener al menos 12 caracteres "
                                + "y coincidir en ambos campos.");
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