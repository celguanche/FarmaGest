package com.farmagest.service;

import com.farmagest.exception.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContactValidationTest extends ServiceTestSupport {
    @ParameterizedTest @ValueSource(strings = {"invalid", "", "a b@example.com"})
    void rejectsMalformedContactEmail(String email) {
        var m = manufacturer(); m.setEmail(email);
        var s = supplier(); s.setEmail(email);
        var c = customer(); c.setEmail(email);
        assertThrows(ValidationException.class, () -> new ManufacturerService().create(m));
        assertThrows(ValidationException.class, () -> new SupplierService().create(s));
        assertThrows(ValidationException.class, () -> new CustomerService().create(c));
        verifyNoInteractions(manufacturers, suppliers, customers);
    }
    @ParameterizedTest @ValueSource(ints = {31, 100})
    void rejectsOversizedPhoneNumbers(int length) {
        var m = manufacturer(); m.setPhone("1".repeat(length));
        var s = supplier(); s.setPhone("1".repeat(length));
        var c = customer(); c.setPhone("1".repeat(length));
        assertThrows(ValidationException.class, () -> new ManufacturerService().create(m));
        assertThrows(ValidationException.class, () -> new SupplierService().create(s));
        assertThrows(ValidationException.class, () -> new CustomerService().create(c));
    }
    @ParameterizedTest @ValueSource(ints = {151, 500})
    void rejectsOversizedNames(int length) {
        var m = manufacturer(); m.setName("a".repeat(length));
        var s = supplier(); s.setBusinessName("a".repeat(length));
        var c = customer(); c.setLastName("a".repeat(length));
        var category = category(); category.setName("a".repeat(length));
        assertThrows(ValidationException.class, () -> new ManufacturerService().create(m));
        assertThrows(ValidationException.class, () -> new SupplierService().create(s));
        assertThrows(ValidationException.class, () -> new CustomerService().create(c));
        assertThrows(ValidationException.class, () -> new CategoryService().create(category));
    }
}
