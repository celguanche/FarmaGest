package com.farmagest.service;

import com.farmagest.exception.*;

import java.math.*;
import java.util.List;

final class Checks {
    private Checks() {
    }

    static void require(boolean valid, String message) {
        if (!valid) throw new ValidationException(message);
    }

    static void id(long id) {
        require(id > 0, "Identificador inválido");
    }

    static <T> T exists(T value) {
        if (value == null) throw new BusinessException("Registro no encontrado");
        return value;
    }

    static void saved(boolean ok) {
        if (!ok) throw new BusinessException("No se pudo guardar el registro");
    }

    static String text(String value, int length) {
        require(value != null && !value.isBlank() && value.length() <= length, "Texto obligatorio o demasiado largo (máximo " + length + ")");
        return value.trim();
    }

    static void optional(String value, int length) {
        require(value == null || value.length() <= length, "Texto demasiado largo");
    }

    static void email(String value, boolean required) {
        if (required) text(value, 150);
        else optional(value, 150);
        require(value == null || (!value.isBlank() && value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")), "Correo inválido");
    }

    static BigDecimal money(BigDecimal value, int precision) {
        require(value != null && value.signum() >= 0, "Importe negativo o ausente");
        try {
            value = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new ValidationException("Se admiten dos decimales");
        }
        require(value.precision() <= precision, "Importe fuera de rango");
        return value;
    }

    static BigDecimal vat(BigDecimal value) {
        value = money(value, 5);
        require(value.compareTo(BigDecimal.valueOf(100)) <= 0, "IVA fuera de rango");
        return value;
    }

    static BigDecimal subtotal(BigDecimal price, int quantity) {
        require(quantity > 0, "Cantidad inválida");
        return money(money(price, 10).multiply(BigDecimal.valueOf(quantity)), 12);
    }

    static BigDecimal tax(BigDecimal subtotal, BigDecimal rate) {
        return money(subtotal.multiply(vat(rate)).movePointLeft(2).setScale(2, RoundingMode.HALF_UP), 12);
    }

    static void items(List<?> items) {
        require(items != null && !items.isEmpty() && items.stream().noneMatch(x -> x == null), "Debe incluir líneas válidas");
        java.util.Set<Object> identities = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Object item : items)
            require(identities.add(item), "No se puede reutilizar la misma instancia en varias líneas");
    }
}
