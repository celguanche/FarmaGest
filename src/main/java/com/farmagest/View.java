package com.farmagest;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

final class View {

    private final FXMLLoader loader;
    final Parent root;

    View(String name) {
        String path = "/fxml/" + name + ".fxml";
        loader = new FXMLLoader(
                Objects.requireNonNull(View.class.getResource(path), "Falta " + path)
        );

        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new UncheckedIOException("Error al cargar " + path, ex);
        }
    }

    <T> T get(String id, Class<T> type) {
        Object control = Objects.requireNonNull(
                loader.getNamespace().get(id), "Falta fx:id=\"" + id + "\""
        );
        return type.cast(control);
    }

    static String css() {
        return Objects.requireNonNull(
                View.class.getResource("/css/farmagest.css")
        ).toExternalForm();
    }
}