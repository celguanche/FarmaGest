package com.farmagest;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

final class Form {

    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final GridPane grid = new GridPane();
    private final Label error = new Label();
    private final ButtonType save = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
    private int row;

    Form(Window owner, String title) {
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(480);

        grid.setHgap(12);
        grid.setVgap(9);
        grid.setPadding(new Insets(12));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(590);
        dialog.getDialogPane().setContent(scroll);

        error.setWrapText(true);
        error.setStyle("-fx-text-fill: #a32020;");
    }

    private <T extends Control> T add(String label, T control) {
        grid.add(new Label(label), 0, row);
        grid.add(control, 1, row++);
        GridPane.setHgrow(control, Priority.ALWAYS);
        control.setMaxWidth(Double.MAX_VALUE);
        return control;
    }

    TextField text(String label, Object value) {
        return add(label, new TextField(value == null ? "" : value.toString()));
    }

    PasswordField password(String label) {
        return add(label, new PasswordField());
    }

    CheckBox check(String label, boolean selected) {
        CheckBox box = new CheckBox();
        box.setSelected(selected);
        return add(label, box);
    }

    DatePicker date(String label) {
        DatePicker field = new DatePicker();
        field.setEditable(false);
        return add(label, field);
    }

    <T> ComboBox<T> choice(String label, List<T> values, T selected,
                           Function<T, String> display) {
        ComboBox<T> box = new ComboBox<>();
        box.getItems().setAll(values);
        box.setCellFactory(list -> cell(display));
        box.setButtonCell(cell(display));
        box.setValue(selected);
        return add(label, box);
    }

    private static <T> ListCell<T> cell(Function<T, String> display) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : display.apply(item));
            }
        };
    }

    <T> Optional<T> read(Supplier<T> builder) {
        grid.add(error, 0, row, 2, 1);
        Button button = (Button) dialog.getDialogPane().lookupButton(save);
        class Result {
            T value;
        }
        Result result = new Result();

        button.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                result.value = builder.get();
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
                event.consume();
            }
        });

        return dialog.showAndWait()
                .filter(save::equals)
                .map(ignored -> result.value);
    }

    static String required(TextField field, String label) {
        String value = field.getText().strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Completa el campo «" + label + "».");
        }
        return value;
    }

    static String optional(TextField field) {
        String value = field.getText().strip();
        return value.isEmpty() ? null : value;
    }

    static int integer(TextField field, String label) {
        try {
            return Integer.parseInt(required(field, label));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("«" + label + "» debe ser un número entero.");
        }
    }

    static BigDecimal decimal(TextField field, String label) {
        try {
            return new BigDecimal(required(field, label).replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Importe incorrecto en «" + label + "».");
        }
    }

    static <T> T selected(ComboBox<T> field, String label) {
        if (field.getValue() == null) {
            throw new IllegalArgumentException("Selecciona «" + label + "».");
        }
        return field.getValue();
    }
}