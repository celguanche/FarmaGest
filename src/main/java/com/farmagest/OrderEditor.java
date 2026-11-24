package com.farmagest;

import com.farmagest.model.Product;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

final class OrderEditor {

    private static final Locale SPANISH = Locale.forLanguageTag("es-ES");

    record Line(Product product, int quantity, BigDecimal cost, BigDecimal vat) {
        BigDecimal base() {
            return cost.multiply(BigDecimal.valueOf(quantity));
        }

        BigDecimal tax() {
            return base().multiply(vat).movePointLeft(2)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    private static String currency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(SPANISH).format(value);
    }

    private static void column(TableView<Line> table, String title,
                               Function<Line, String> format) {
        TableColumn<Line, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(format.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    @SuppressWarnings("unchecked")
    static Optional<List<Line>> open(Window owner, List<Product> products, boolean purchase) {
        View view = new View("order-lines");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setResizable(true);
        dialog.setTitle(purchase ? "Pedido de compra" : "Punto de venta");
        dialog.getDialogPane().setContent(view.root);
        dialog.getDialogPane().getStylesheets().add(View.css());

        view.get("heading", Label.class)
                .setText(purchase ? "Líneas del pedido" : "Productos de la venta");

        ButtonType finish = new ButtonType(
                purchase ? "Guardar pedido" : "Registrar venta",
                ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(finish, ButtonType.CANCEL);

        ComboBox<Product> product = view.get("product", ComboBox.class);
        var options = new FilteredList<>(FXCollections.observableArrayList(products));
        product.setItems(options);

        product.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product value) {
                return value == null ? "" : value.getName() + " · " + value.getBarcode();
            }

            @Override
            public Product fromString(String text) {
                throw new UnsupportedOperationException();
            }
        });

        TextField search = view.get("search", TextField.class);
        search.textProperty().addListener((obs, old, value) -> {
            String query = value.strip().toLowerCase(SPANISH);
            options.setPredicate(item ->
                    product.getConverter().toString(item).toLowerCase(SPANISH).contains(query));

            if (options.size() == 1) {
                product.setValue(options.get(0));
            } else if (!options.contains(product.getValue())) {
                product.setValue(null);
            }
        });

        TextField quantity = view.get("quantity", TextField.class);
        TextField price = view.get("price", TextField.class);
        TextField vat = view.get("vat", TextField.class);
        price.setEditable(purchase);
        vat.setEditable(purchase);

        product.valueProperty().addListener((obs, old, selected) -> {
            price.setText(selected == null || purchase
                    ? "" : selected.getSalePrice().toPlainString());
            vat.setText(selected == null ? "" : selected.getVatRate().toPlainString());
        });

        TableView<Line> lines = view.get("lines", TableView.class);
        lines.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        lines.setPlaceholder(new Label("Añade el primer producto"));

        column(lines, "Producto", line -> line.product().getName());
        column(lines, "Unidades", line -> String.valueOf(line.quantity()));
        column(lines, "Precio", line -> currency(line.cost()));
        column(lines, "IVA %", line -> line.vat().toPlainString());
        column(lines, "Importe", line -> currency(line.base().add(line.tax())));

        Button remove = view.get("remove", Button.class);
        remove.disableProperty().bind(lines.getSelectionModel().selectedItemProperty().isNull());
        remove.setOnAction(event -> {
            int index = lines.getSelectionModel().getSelectedIndex();
            if (index >= 0) lines.getItems().remove(index);
        });

        Label total = view.get("total", Label.class);
        Runnable updateTotal = () -> {
            BigDecimal base = BigDecimal.ZERO;
            BigDecimal tax = BigDecimal.ZERO;

            for (Line line : lines.getItems()) {
                base = base.add(line.base());
                tax = tax.add(line.tax());
            }

            total.setText("Base " + currency(base) + "     IVA " + currency(tax)
                    + "     Total " + currency(base.add(tax)));
        };

        lines.getItems().addListener((ListChangeListener<Line>) change -> updateTotal.run());
        updateTotal.run();

        Label error = view.get("error", Label.class);
        view.get("add", Button.class).setOnAction(event -> {
            try {
                Product selected = Form.selected(product, "Producto");
                int units = Form.integer(quantity, "Unidades");
                BigDecimal cost = Form.decimal(price, "Precio");
                BigDecimal rate = Form.decimal(vat, "IVA");

                if (units < 1 || cost.signum() < 0 || rate.signum() < 0
                        || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException("Revisa las unidades, el precio y el IVA.");
                }

                if (cost.stripTrailingZeros().scale() > 2
                        || rate.stripTrailingZeros().scale() > 2) {
                    throw new IllegalArgumentException("Usa un máximo de dos decimales.");
                }

                lines.getItems().add(new Line(selected, units, cost, rate));
                quantity.setText("1");
                error.setText("");

            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
            }
        });

        dialog.getDialogPane().lookupButton(finish).disableProperty()
                .bind(Bindings.isEmpty(lines.getItems()));

        return dialog.showAndWait()
                .filter(finish::equals)
                .map(ignored -> List.copyOf(lines.getItems()));
    }
}