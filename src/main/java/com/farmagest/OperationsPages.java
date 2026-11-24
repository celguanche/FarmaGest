package com.farmagest;

import com.farmagest.model.*;
import com.farmagest.model.enums.PaymentMethod;
import com.farmagest.service.*;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.farmagest.PharmacyWindow.*;

final class OperationsPages {

    private final PharmacyWindow ui;
    private final PurchaseService purchases = new PurchaseService();
    private final SaleService sales = new SaleService();
    private final InventoryService inventory = new InventoryService();
    private final ProductService products = new ProductService();
    private final SupplierService suppliers = new SupplierService();
    private final CustomerService customers = new CustomerService();
    private final AuditService audit = new AuditService();

    OperationsPages(PharmacyWindow ui) {
        this.ui = ui;
    }

    private record PurchaseOptions(List<Product> products, List<Supplier> suppliers) {}
    private record SaleOptions(List<Product> products, List<Customer> customers) {}
    private record SaleHeader(Long customerId, PaymentMethod payment) {}
    private record Line(Product product, int quantity, BigDecimal cost, BigDecimal vat) {}

    private List<Product> activeProducts() {
        return products.findAll().stream().filter(Product::isActive).toList();
    }

    private String productLabel(Product product) {
        return product.getName() + " · " + product.getBarcode();
    }

    void purchases() {
        ui.load(MANAGERS, purchases::findAll, rows -> {
            FlowPane actions = new FlowPane();
            var table = ui.page("Compras", rows, actions,
                    new Column<>("Pedido", Purchase::getId),
                    new Column<>("Proveedor ID", Purchase::getSupplierId),
                    new Column<>("Fecha", Purchase::getOrderedAt),
                    new Column<>("Estado", Purchase::getStatus),
                    new Column<>("Base", Purchase::getSubtotal),
                    new Column<>("IVA", Purchase::getTaxAmount),
                    new Column<>("Total", Purchase::getTotalAmount));

            actions.getChildren().addAll(
                    button("Nuevo pedido", this::newPurchase),
                    button("Ver líneas", () -> ui.selected(table, this::purchaseDetails)),
                    button("Recibir pedido", () -> ui.selected(table, this::receive)),
                    button("Cancelar pedido", () -> ui.selected(table, p ->
                            reason("Cancelar pedido " + p.getId(), text ->
                                    ui.save(MANAGERS, "CANCEL", "purchases", () -> {
                                        purchases.cancelPurchase(p.getId(), ui.user.getId(), text);
                                        return p.getId();
                                    }, id -> id, this::purchases)))),
                    button("Actualizar", this::purchases)
            );
        });
    }

    private void newPurchase() {
        ui.load(MANAGERS, () -> new PurchaseOptions(activeProducts(),
                suppliers.findAll().stream().filter(Supplier::isActive).toList()), options -> {
            if (options.products().isEmpty() || options.suppliers().isEmpty()) {
                ui.message("Necesitas al menos un producto y un proveedor activos.");
                return;
            }

            Form f = new Form(ui.stage, "Nuevo pedido");
            var supplier = f.choice("Proveedor", options.suppliers(), null,
                    Supplier::getBusinessName);
            var notes = f.text("Notas", null);

            f.read(() -> new Purchase(0, Form.selected(supplier, "Proveedor").getId(),
                    ui.user.getId(), null, null, null, null, null, null, null,
                    null, null, Form.optional(notes), null, null)).ifPresent(purchase ->
                    collectLines(options.products(), true).ifPresent(lines -> {
                        List<PurchaseItem> items = lines.stream().map(line ->
                                new PurchaseItem(0, 0, line.product().getId(), line.quantity(),
                                        line.cost(), line.vat(), null, null, null, null)).toList();

                        ui.save(MANAGERS, "CREATE", "purchases",
                                () -> purchases.createPurchase(purchase, items),
                                Purchase::getId, this::purchases);
                    }));
        });
    }

    private void purchaseDetails(Purchase purchase) {
        ui.load(MANAGERS, () -> purchases.findItems(purchase.getId()), rows -> {
            FlowPane actions = new FlowPane(button("Volver a compras", this::purchases));
            ui.page("Líneas del pedido " + purchase.getId(), rows, actions,
                    new Column<>("Línea", PurchaseItem::getId),
                    new Column<>("Producto ID", PurchaseItem::getProductId),
                    new Column<>("Cantidad", PurchaseItem::getQuantity),
                    new Column<>("Coste", PurchaseItem::getUnitCost),
                    new Column<>("IVA %", PurchaseItem::getVatRate),
                    new Column<>("Total", PurchaseItem::getLineTotal));
        });
    }

    private void receive(Purchase purchase) {
        if (!"ORDERED".equals(purchase.getStatus().name())) {
            ui.message("Solo puedes recibir un pedido pendiente.");
            return;
        }

        ui.load(MANAGERS, () -> purchases.findItems(purchase.getId()), items -> {
            List<ProductBatch> batches = new ArrayList<>();

            for (PurchaseItem item : items) {
                int remaining = item.getQuantity();

                while (remaining > 0) {
                    Form f = new Form(ui.stage, "Recepción · producto " + item.getProductId()
                            + " · quedan " + remaining + " unidades");
                    var number = f.text("Número de lote", null);
                    var quantity = f.text("Unidades de este lote", remaining);
                    var expiry = f.date("Caducidad, si corresponde");
                    int maximum = remaining;

                    var result = f.read(() -> {
                        int units = Form.integer(quantity, "Unidades");
                        if (units < 1 || units > maximum) {
                            throw new IllegalArgumentException("Introduce entre 1 y " + maximum + " unidades.");
                        }
                        LocalDate date = expiry.getValue();
                        if (date != null && date.isBefore(LocalDate.now())) {
                            throw new IllegalArgumentException("El lote ya está caducado.");
                        }
                        return new ProductBatch(0, item.getProductId(), item.getId(),
                                Form.required(number, "Número de lote"), date, units, 0,
                                item.getUnitCost(), null, true, null, null);
                    });

                    if (result.isEmpty()) return;
                    ProductBatch batch = result.get();
                    batches.add(batch);
                    remaining -= batch.getInitialQuantity();
                }
            }

            if (ui.confirm("¿Confirmar la recepción de " + batches.size() + " lotes?")) {
                ui.save(MANAGERS, "RECEIVE", "purchases", () -> {
                    purchases.receivePurchase(purchase.getId(), ui.user.getId(), batches);
                    return purchase.getId();
                }, id -> id, this::purchases);
            }
        });
    }

    void sales() {
        ui.load(ALL, sales::findAll, rows -> {
            FlowPane actions = new FlowPane();
            var table = ui.page("Ventas", rows, actions,
                    new Column<>("Venta", Sale::getId), new Column<>("Fecha", Sale::getSoldAt),
                    new Column<>("Empleado ID", Sale::getEmployeeUserId),
                    new Column<>("Pago", Sale::getPaymentMethod),
                    new Column<>("Estado", Sale::getStatus),
                    new Column<>("Base", Sale::getSubtotal),
                    new Column<>("IVA", Sale::getTaxAmount),
                    new Column<>("Total", Sale::getTotalAmount));

            actions.getChildren().addAll(button("Nueva venta", this::newSale),
                    button("Ver líneas", () -> ui.selected(table, this::saleDetails)),
                    button("Actualizar", this::sales));

            if (ui.isManager()) {
                actions.getChildren().add(button("Cancelar venta", () -> ui.selected(table, sale ->
                        reason("Cancelar venta " + sale.getId(), text ->
                                ui.save(MANAGERS, "CANCEL", "sales", () -> {
                                    sales.cancelSale(sale.getId(), ui.user.getId(), text);
                                    return sale.getId();
                                }, id -> id, this::sales)))));
            }
        });
    }

    private void newSale() {
        ui.load(ALL, () -> new SaleOptions(activeProducts(),
                customers.findAll().stream().filter(Customer::isActive).toList()), options -> {
            if (options.products().isEmpty()) {
                ui.message("No hay productos activos para vender.");
                return;
            }

            Form f = new Form(ui.stage, "Nueva venta");
            var customer = f.choice("Cliente opcional", options.customers(), null,
                    c -> c.getFirstName() + " " + c.getLastName() + " · " + c.getId());
            var payment = f.choice("Forma de pago", List.of(PaymentMethod.values()),
                    PaymentMethod.CASH, p -> p == PaymentMethod.CASH ? "Efectivo" : "Tarjeta");

            f.read(() -> new SaleHeader(customer.getValue() == null ? null :
                            customer.getValue().getId(), Form.selected(payment, "Forma de pago")))
                    .ifPresent(header -> collectLines(options.products(), false).ifPresent(lines -> {
                        Sale sale = new Sale(0, ui.user.getId(), header.customerId(),
                                header.payment(), null, null, null, null, null, null,
                                null, null, null, null);

                        List<SaleItem> items = lines.stream().map(line ->
                                new SaleItem(0, 0, line.product().getId(), line.quantity(),
                                        null, null, null, null, null, null)).toList();

                        ui.save(ALL, "CREATE", "sales", () -> sales.createSale(sale, items),
                                Sale::getId, this::sales);
                    }));
        });
    }

    private Optional<List<Line>> collectLines(List<Product> available, boolean purchase) {
        List<Line> lines = new ArrayList<>();
        ButtonType another = new ButtonType("Añadir otro producto");
        ButtonType finish = new ButtonType(purchase ? "Guardar pedido" : "Registrar venta");

        while (true) {
            Form f = new Form(ui.stage, "Línea " + (lines.size() + 1));
            var product = f.choice("Producto", available, null, this::productLabel);
            var quantity = f.text("Cantidad", 1);
            var cost = purchase ? f.text("Coste unitario sin IVA", null) : null;
            var vat = purchase ? f.text("IVA de compra (%)", null) : null;

            if (purchase) {
                product.valueProperty().addListener((obs, old, value) -> {
                    if (value != null) vat.setText(value.getVatRate().toPlainString());
                });
            }

            var result = f.read(() -> {
                Product selected = Form.selected(product, "Producto");
                int units = Form.integer(quantity, "Cantidad");
                if (units <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");

                BigDecimal base = purchase ? Form.decimal(cost, "Coste") : selected.getSalePrice();
                BigDecimal rate = purchase ? Form.decimal(vat, "IVA") : selected.getVatRate();
                if (base.signum() < 0 || rate.signum() < 0
                        || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException("Revisa el importe y el porcentaje de IVA.");
                }
                return new Line(selected, units, base, rate);
            });

            if (result.isEmpty()) return Optional.empty();
            lines.add(result.get());

            StringBuilder summary = new StringBuilder();
            for (Line line : lines) {
                summary.append(line.product().getName()).append(" · ")
                        .append(line.quantity()).append(" × ").append(line.cost())
                        .append(" + ").append(line.vat()).append("% IVA\n");
            }

            Alert review = new Alert(Alert.AlertType.CONFIRMATION, summary.toString(),
                    another, finish, ButtonType.CANCEL);
            review.initOwner(ui.stage);
            review.setHeaderText(purchase ? "Revisar pedido" : "Revisar venta");
            ButtonType choice = review.showAndWait().orElse(ButtonType.CANCEL);
            if (choice == finish) return Optional.of(lines);
            if (choice != another) return Optional.empty();
        }
    }

    private void saleDetails(Sale sale) {
        ui.load(ALL, () -> sales.findItems(sale.getId()), rows -> {
            FlowPane actions = new FlowPane(button("Volver a ventas", this::sales));
            ui.page("Líneas de la venta " + sale.getId(), rows, actions,
                    new Column<>("Línea", SaleItem::getId),
                    new Column<>("Producto ID", SaleItem::getProductId),
                    new Column<>("Cantidad", SaleItem::getQuantity),
                    new Column<>("Precio", SaleItem::getUnitPrice),
                    new Column<>("IVA %", SaleItem::getVatRate),
                    new Column<>("Total", SaleItem::getLineTotal));
        });
    }

    void stock() {
        ui.load(ALL, inventory::findAllStock, rows -> {
            FlowPane actions = new FlowPane();
            var table = ui.page("Inventario", rows, actions,
                    new Column<>("Producto ID", ProductStock::getProductId),
                    new Column<>("Nombre", ProductStock::getProductName),
                    new Column<>("Mínimo", ProductStock::getMinimumStock),
                    new Column<>("Total", ProductStock::getTotalStock),
                    new Column<>("Disponible", ProductStock::getSaleableStock),
                    new Column<>("Caducado", ProductStock::getExpiredStock));

            actions.getChildren().addAll(button("Actualizar", this::stock),
                    button("Ver lotes", () -> ui.selected(table, p -> batches(p.getProductId()))));
        });
    }

    void lowStock() {
        ui.load(ALL, inventory::findLowStockProducts, rows -> {
            FlowPane actions = new FlowPane(button("Actualizar", this::lowStock));
            ui.page("Productos en stock mínimo", rows, actions,
                    new Column<>("ID", LowStockProduct::getProductId),
                    new Column<>("Producto", LowStockProduct::getProductName),
                    new Column<>("Mínimo", LowStockProduct::getMinimumStock),
                    new Column<>("Disponible", LowStockProduct::getSaleableStock));
        });
    }

    private void batches(long productId) {
        ui.load(ALL, () -> inventory.findBatches(productId), rows -> {
            FlowPane actions = new FlowPane();
            var table = ui.page("Lotes del producto " + productId, rows, actions,
                    new Column<>("ID", ProductBatch::getId),
                    new Column<>("Lote", ProductBatch::getBatchNumber),
                    new Column<>("Caducidad", ProductBatch::getExpiryDate),
                    new Column<>("Inicial", ProductBatch::getInitialQuantity),
                    new Column<>("Disponible", ProductBatch::getAvailableQuantity),
                    new Column<>("Activo", b -> b.isActive() ? "Sí" : "No"));

            actions.getChildren().addAll(button("Volver", this::stock),
                    button("Movimientos", () -> ui.selected(table, this::movements)));

            if (ui.isManager()) {
                actions.getChildren().addAll(
                        button("Ajustar existencias", () -> ui.selected(table, this::adjust)),
                        button("Registrar caducidad", () -> ui.selected(table, batch -> {
                            if (ui.confirm("¿Registrar la caducidad del lote "
                                    + batch.getBatchNumber() + "?")) {
                                ui.save(MANAGERS, "EXPIRE", "product_batches", () -> {
                                    inventory.expireBatch(batch.getId(), ui.user.getId());
                                    return batch.getId();
                                }, id -> id, () -> batches(productId));
                            }
                        }))
                );
            }
        });
    }

    private void adjust(ProductBatch batch) {
        Form f = new Form(ui.stage, "Ajustar lote " + batch.getBatchNumber());
        var quantity = f.text("Cambio (+ entrada, − salida)", null);
        var reason = f.text("Motivo", null);
        record Adjustment(int units, String reason) {}

        f.read(() -> {
            int units = Form.integer(quantity, "Cambio");
            if (units == 0) throw new IllegalArgumentException("El ajuste debe ser distinto de cero.");
            return new Adjustment(units, Form.required(reason, "Motivo"));
        }).ifPresent(change -> ui.save(MANAGERS, "ADJUST", "product_batches", () -> {
            inventory.adjustStock(batch.getId(), ui.user.getId(), change.units(), change.reason());
            return batch.getId();
        }, id -> id, () -> batches(batch.getProductId())));
    }

    private void movements(ProductBatch batch) {
        ui.load(ALL, () -> inventory.findMovements(batch.getId()), rows -> {
            FlowPane actions = new FlowPane(button("Volver a lotes", () -> batches(batch.getProductId())));
            ui.page("Movimientos del lote " + batch.getBatchNumber(), rows, actions,
                    new Column<>("Fecha", InventoryMovement::getCreatedAt),
                    new Column<>("Tipo", InventoryMovement::getMovementType),
                    new Column<>("Cambio", InventoryMovement::getQuantityChange),
                    new Column<>("Antes", InventoryMovement::getStockBefore),
                    new Column<>("Después", InventoryMovement::getStockAfter),
                    new Column<>("Usuario ID", InventoryMovement::getUserId),
                    new Column<>("Motivo", InventoryMovement::getReason));
        });
    }

    void audit() {
        Form f = new Form(ui.stage, "Consultar auditoría");
        var userId = f.text("ID del usuario", ui.user.getId());

        f.read(() -> {
            try {
                long id = Long.parseLong(Form.required(userId, "ID del usuario"));
                if (id < 1) throw new NumberFormatException();
                return id;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Introduce un ID de usuario positivo.");
            }
        }).ifPresent(id -> ui.load(MANAGERS, () -> audit.findByUserId(id), rows -> {
            FlowPane actions = new FlowPane(button("Buscar otro usuario", this::audit));
            ui.page("Auditoría del usuario " + id, rows, actions,
                    new Column<>("Fecha", AuditLog::getCreatedAt),
                    new Column<>("Acción", AuditLog::getAction),
                    new Column<>("Entidad", AuditLog::getEntityType),
                    new Column<>("Registro ID", AuditLog::getEntityId),
                    new Column<>("Descripción", AuditLog::getDescription));
        }));
    }

    private void reason(String title, Consumer<String> action) {
        Form f = new Form(ui.stage, title);
        var text = f.text("Motivo", null);
        f.read(() -> Form.required(text, "Motivo")).ifPresent(action);
    }
}