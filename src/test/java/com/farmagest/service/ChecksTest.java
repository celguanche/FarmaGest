package com.farmagest.service;

import com.farmagest.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChecksTest {
    @Test void trimsTextAndAcceptsExactLimit() {
        assertEquals("name", Checks.text(" name ", 6));
        assertDoesNotThrow(() -> Checks.text("a".repeat(100), 100));
        assertThrows(ValidationException.class, () -> Checks.text("a".repeat(101), 100));
        assertThrows(ValidationException.class, () -> Checks.text(null, 100));
    }
    @Test void validatesOptionalTextAndEmail() {
        assertDoesNotThrow(() -> Checks.optional(null, 3)); assertDoesNotThrow(() -> Checks.optional("abc", 3));
        assertThrows(ValidationException.class, () -> Checks.optional("abcd", 3));
        assertDoesNotThrow(() -> Checks.email(null, false)); assertDoesNotThrow(() -> Checks.email("user@example.com", true));
        assertThrows(ValidationException.class, () -> Checks.email(null, true));
    }
    @ParameterizedTest @ValueSource(strings = {"", " ", "user", "a@b", "a b@c.com", "a@@b.com"})
    void rejectsMalformedEmail(String value) { assertThrows(ValidationException.class, () -> Checks.email(value, false)); }
    @Test void normalizesMoneyAndChecksPrecision() {
        assertEquals(new BigDecimal("1.00"), Checks.money(BigDecimal.ONE, 10));
        assertEquals(new BigDecimal("99999999.99"), Checks.money(new BigDecimal("99999999.99"), 10));
        assertThrows(ValidationException.class, () -> Checks.money(null, 10));
        assertThrows(ValidationException.class, () -> Checks.money(new BigDecimal("100000000.00"), 10));
        assertThrows(ValidationException.class, () -> Checks.money(new BigDecimal("0.001"), 10));
        assertThrows(ValidationException.class, () -> Checks.money(new BigDecimal("-0.01"), 10));
    }
    @Test void roundsTaxHalfUpPerLine() {
        assertEquals(new BigDecimal("0.01"), Checks.tax(new BigDecimal("0.05"), BigDecimal.TEN));
        assertEquals(new BigDecimal("0.00"), Checks.tax(new BigDecimal("0.04"), BigDecimal.TEN));
        assertEquals(new BigDecimal("12.00"), Checks.subtotal(new BigDecimal("4.00"), 3));
        assertThrows(ValidationException.class, () -> Checks.subtotal(BigDecimal.ONE, 0));
        assertThrows(ValidationException.class, () -> Checks.vat(new BigDecimal("100.01")));
    }
    @Test void rejectsNullEmptyAndRepeatedLineInstances() {
        Object item = new Object();
        assertThrows(ValidationException.class, () -> Checks.items(null));
        assertThrows(ValidationException.class, () -> Checks.items(List.of()));
        assertThrows(ValidationException.class, () -> Checks.items(Arrays.asList(item, null)));
        assertThrows(ValidationException.class, () -> Checks.items(List.of(item, item)));
        assertDoesNotThrow(() -> Checks.items(List.of(item, new Object())));
    }
}
