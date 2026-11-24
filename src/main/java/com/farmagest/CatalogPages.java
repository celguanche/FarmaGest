package com.farmagest;

import com.farmagest.model.*;
import com.farmagest.model.enums.Role;
import com.farmagest.service.*;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.*;

import static com.farmagest.PharmacyWindow.*;

final class CatalogPages {

    private final PharmacyWindow ui;
    private final ProductService products = new ProductService();
    private final CategoryService categories = new CategoryService();
    private final ManufacturerService manufacturers = new ManufacturerService();
    private final SupplierService suppliers = new SupplierService();
    private final CustomerService customers = new CustomerService();
    private final UserService users = new UserService();

    CatalogPages(PharmacyWindow ui) {
        this.ui = ui;
    }

    @SafeVarargs
    private final <T> void master(String title, String entity, Role[] readRoles,
                                  Role[] writeRoles, Callable<List<T>> read,
                                  ToLongFunction<T> id, Consumer<T> edit,
                                  LongConsumer deactivate, Runnable refresh,
                                  Column<T>... columns) {
        ui.load(readRoles, read, rows -> {
            FlowPane actions = new FlowPane();
            TableView<T> table = ui.page(title, rows, actions, columns);
            actions.getChildren().add(button("Actualizar", refresh));

            if (!Arrays.asList(writeRoles).contains(ui.user.getRole())) return;

            actions.getChildren().addAll(
                    button("Nuevo", () -> edit.accept(null)),
                    button("Editar", () -> ui.selected(table, edit)),
                    button("Desactivar", () -> ui.selected(table, record -> {
                        long key = id.applyAsLong(record);
                        if (entity.equals("users") && key == ui.user.getId()) {
                            ui.message("No puedes desactivar tu propia sesión.");
                            return;
                        }
                        if (ui.confirm("¿Desactivar el registro " + key + "?")) {
                            ui.save(writeRoles, "DEACTIVATE", entity, () -> {
                                deactivate.accept(key);
                                return key;
                            }, value -> value, refresh);
                        }
                    }))
            );
        });
    }

    private <T> void persist(T old, T value, String entity, Role[] roles,
                             Function<T, T> insert, Function<T, T> update,
                             Function<T, Long> id, Runnable refresh) {
        ui.save(roles, old == null ? "CREATE" : "UPDATE", entity,
                () -> old == null ? insert.apply(value) : update.apply(value),
                id, refresh);
    }

    void categories() {
        master("Categorías", "categories", ALL, MANAGERS, categories::findAll,
                Category::getId, this::categoryForm, categories::deactivate, this::categories,
                new Column<>("ID", Category::getId),
                new Column<>("Nombre", Category::getName),
                new Column<>("Descripción", Category::getDescription),
                new Column<>("Activa", c -> yes(c.isActive())));
    }

    private void categoryForm(Category old) {
        Form f = new Form(ui.stage, "Categoría");
        TextField name = f.text("Nombre", old == null ? null : old.getName());
        TextField description = f.text("Descripción", old == null ? null : old.getDescription());

        f.read(() -> new Category(old == null ? 0 : old.getId(),
                        Form.required(name, "Nombre"), Form.optional(description),
                        old == null || old.isActive(), null, null))
                .ifPresent(value -> persist(old, value, "categories", MANAGERS,
                        categories::create, categories::update, Category::getId, this::categories));
    }

    void manufacturers() {
        master("Fabricantes", "manufacturers", ALL, MANAGERS, manufacturers::findAll,
                Manufacturer::getId, this::manufacturerForm, manufacturers::deactivate,
                this::manufacturers, new Column<>("ID", Manufacturer::getId),
                new Column<>("Nombre", Manufacturer::getName),
                new Column<>("Teléfono", Manufacturer::getPhone),
                new Column<>("Correo", Manufacturer::getEmail),
                new Column<>("Activo", m -> yes(m.isActive())));
    }

    private void manufacturerForm(Manufacturer old) {
        Form f = new Form(ui.stage, "Fabricante");
        var name = f.text("Nombre", old == null ? null : old.getName());
        var phone = f.text("Teléfono", old == null ? null : old.getPhone());
        var email = f.text("Correo", old == null ? null : old.getEmail());
        var web = f.text("Web", old == null ? null : old.getWebsite());
        var address = f.text("Dirección", old == null ? null : old.getAddress());

        f.read(() -> new Manufacturer(old == null ? 0 : old.getId(),
                Form.required(name, "Nombre"), Form.optional(phone), Form.optional(email),
                Form.optional(web), Form.optional(address), old == null || old.isActive(),
                null, null)).ifPresent(value -> persist(old, value, "manufacturers", MANAGERS,
                manufacturers::create, manufacturers::update, Manufacturer::getId,
                this::manufacturers));
    }

    void suppliers() {
        master("Proveedores", "suppliers", ALL, MANAGERS, suppliers::findAll,
                Supplier::getId, this::supplierForm, suppliers::deactivate, this::suppliers,
                new Column<>("ID", Supplier::getId),
                new Column<>("Empresa", Supplier::getBusinessName),
                new Column<>("NIF", Supplier::getTaxId),
                new Column<>("Contacto", Supplier::getContactName),
                new Column<>("Activo", s -> yes(s.isActive())));
    }

    private void supplierForm(Supplier old) {
        Form f = new Form(ui.stage, "Proveedor");
        var name = f.text("Empresa", old == null ? null : old.getBusinessName());
        var tax = f.text("NIF", old == null ? null : old.getTaxId());
        var contact = f.text("Contacto", old == null ? null : old.getContactName());
        var phone = f.text("Teléfono", old == null ? null : old.getPhone());
        var email = f.text("Correo", old == null ? null : old.getEmail());
        var address = f.text("Dirección", old == null ? null : old.getAddress());

        f.read(() -> new Supplier(old == null ? 0 : old.getId(),
                        Form.required(name, "Empresa"), Form.optional(tax), Form.optional(contact),
                        Form.optional(phone), Form.optional(email), Form.optional(address),
                        old == null || old.isActive(), null, null))
                .ifPresent(value -> persist(old, value, "suppliers", MANAGERS,
                        suppliers::create, suppliers::update, Supplier::getId, this::suppliers));
    }

    void customers() {
        master("Clientes", "customers", ALL, ALL, customers::findAll, Customer::getId,
                this::customerForm, customers::deactivate, this::customers,
                new Column<>("ID", Customer::getId), new Column<>("Nombre", Customer::getFirstName),
                new Column<>("Apellidos", Customer::getLastName),
                new Column<>("Teléfono", Customer::getPhone),
                new Column<>("Correo", Customer::getEmail),
                new Column<>("Activo", c -> yes(c.isActive())));
    }

    private void customerForm(Customer old) {
        Form f = new Form(ui.stage, "Cliente");
        var name = f.text("Nombre", old == null ? null : old.getFirstName());
        var last = f.text("Apellidos", old == null ? null : old.getLastName());
        var phone = f.text("Teléfono", old == null ? null : old.getPhone());
        var email = f.text("Correo", old == null ? null : old.getEmail());

        f.read(() -> new Customer(old == null ? 0 : old.getId(),
                Form.required(name, "Nombre"), Form.required(last, "Apellidos"),
                Form.optional(phone), Form.optional(email), old == null || old.isActive(),
                null, null)).ifPresent(value -> persist(old, value, "customers", ALL,
                customers::create, customers::update, Customer::getId, this::customers));
    }

    void products() {
        master("Productos", "products", ALL, MANAGERS, products::findAll, Product::getId,
                this::productForm, products::deactivate, this::products,
                new Column<>("ID", Product::getId), new Column<>("Producto", Product::getName),
                new Column<>("Código", Product::getBarcode),
                new Column<>("Precio base", Product::getSalePrice),
                new Column<>("IVA %", Product::getVatRate),
                new Column<>("Stock mínimo", Product::getMinimumStock),
                new Column<>("Activo", p -> yes(p.isActive())));
    }

    private record ProductOptions(List<Category> categories, List<Manufacturer> makers) {}

    private void productForm(Product old) {
        ui.load(MANAGERS, () -> new ProductOptions(
                categories.findAll().stream().filter(Category::isActive).toList(),
                manufacturers.findAll().stream().filter(Manufacturer::isActive).toList()
        ), options -> {
            if (options.categories().isEmpty()) {
                ui.message("Crea una categoría activa antes de dar de alta un producto.");
                return;
            }

            Form f = new Form(ui.stage, "Producto");
            var name = f.text("Nombre", old == null ? null : old.getName());
            var barcode = f.text("Código de barras", old == null ? null : old.getBarcode());
            var description = f.text("Descripción", old == null ? null : old.getDescription());

            Category selectedCategory = old == null ? null : options.categories().stream()
                    .filter(c -> c.getId() == old.getCategoryId()).findFirst().orElse(null);

            var category = f.choice("Categoría", options.categories(), selectedCategory,
                    Category::getName);

            Manufacturer selectedMaker = old == null || old.getManufacturerId() == null
                    ? null : options.makers().stream()
                    .filter(m -> m.getId() == old.getManufacturerId()).findFirst().orElse(null);

            var maker = f.choice("Fabricante", options.makers(), selectedMaker,
                    Manufacturer::getName);
            var clearMaker = f.check("Sin fabricante", selectedMaker == null);
            maker.disableProperty().bind(clearMaker.selectedProperty());

            var price = f.text("Precio sin IVA", old == null ? null : old.getSalePrice());
            var vat = f.text("IVA (%)", old == null ? null : old.getVatRate());
            var minimum = f.text("Stock mínimo", old == null ? 0 : old.getMinimumStock());
            var prescription = f.check("Requiere receta", old != null && old.isRequiresPrescription());

            f.read(() -> new Product(old == null ? 0 : old.getId(),
                            Form.required(name, "Nombre"), Form.optional(description),
                            Form.required(barcode, "Código de barras"),
                            Form.selected(category, "Categoría").getId(),
                            clearMaker.isSelected() ? null : Form.selected(maker, "Fabricante").getId(),
                            prescription.isSelected(), Form.decimal(price, "Precio"),
                            Form.decimal(vat, "IVA"), Form.integer(minimum, "Stock mínimo"),
                            old == null || old.isActive(), null, null))
                    .ifPresent(value -> persist(old, value, "products", MANAGERS,
                            products::create, products::update, Product::getId, this::products));
        });
    }

    void users() {
        master("Usuarios", "users", MANAGERS, MANAGERS, users::findAll, User::getId,
                this::userForm, users::deactivate, this::users,
                new Column<>("ID", User::getId), new Column<>("Usuario", User::getUsername),
                new Column<>("Nombre", User::getFirstName),
                new Column<>("Apellidos", User::getLastName),
                new Column<>("Rol", User::getRole),
                new Column<>("Activo", u -> yes(u.isActive())));
    }

    private void userForm(User old) {
        Form f = new Form(ui.stage, "Usuario");
        var username = f.text("Usuario", old == null ? null : old.getUsername());
        var name = f.text("Nombre", old == null ? null : old.getFirstName());
        var last = f.text("Apellidos", old == null ? null : old.getLastName());
        var email = f.text("Correo", old == null ? null : old.getEmail());
        var role = f.choice("Rol", List.of(Role.values()),
                old == null ? Role.WORKER : old.getRole(), Role::name);
        if (old != null && old.getId() == ui.user.getId()) role.setDisable(true);

        PasswordField secret = old == null ? f.password("Contraseña inicial") : null;

        f.read(() -> {
            if (secret != null && secret.getText().length() < 12) {
                throw new IllegalArgumentException("La contraseña requiere al menos 12 caracteres.");
            }
            return new User(old == null ? 0 : old.getId(), Form.required(username, "Usuario"),
                    null, Form.required(name, "Nombre"), Form.required(last, "Apellidos"),
                    Form.required(email, "Correo"), Form.selected(role, "Rol"),
                    old == null || old.isActive(), null, null, null);
        }).ifPresent(value -> {
            if (old != null) {
                ui.save(MANAGERS, "UPDATE", "users", () -> users.update(value),
                        User::getId, this::users);
            } else {
                char[] password = secret.getText().toCharArray();
                secret.clear();
                ui.save(MANAGERS, "CREATE", "users", () -> {
                    try {
                        return users.create(value, password);
                    } finally {
                        Arrays.fill(password, '\0');
                    }
                }, User::getId, this::users);
            }
        });
    }

    private static String yes(boolean value) {
        return value ? "Sí" : "No";
    }
}