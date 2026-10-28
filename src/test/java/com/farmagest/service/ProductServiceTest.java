package com.farmagest.service;

import com.farmagest.exception.*;
import com.farmagest.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest extends ServiceTestSupport {
    ProductService service;
    @BeforeEach void prepare() {
        service = new ProductService();
        when(categories.findById(3)).thenReturn(category());
    }
    @Test void createsProductWithActiveManufacturer() {
        Product p = product(); p.setManufacturerId(6L);
        when(manufacturers.findById(6)).thenReturn(manufacturer());
        when(products.insert(p)).thenReturn(true);
        assertSame(p, service.create(p));
        verify(products).insert(p);
    }
    @Test void updatesProductKeepingItsBarcode() {
        Product p = product(); p.setId(2);
        when(products.findByBarcode(p.getBarcode())).thenReturn(p);
        when(products.findById(2)).thenReturn(p);
        when(products.update(p)).thenReturn(true);
        assertSame(p, service.update(p));
    }
    @Test void readsAndDeactivatesProduct() {
        Product p = product();
        when(products.findById(2)).thenReturn(p);
        when(products.findAll()).thenReturn(List.of(p));
        when(products.deactivate(2)).thenReturn(true);
        assertSame(p, service.findById(2));
        assertEquals(List.of(p), service.findAll());
        service.deactivate(2); verify(products).deactivate(2);
    }
    @Test void rejectsMissingProductAndInvalidId() {
        assertThrows(ValidationException.class, () -> service.findById(-1));
        assertThrows(BusinessException.class, () -> service.findById(2));
        Product p = product(); p.setId(2);
        assertThrows(BusinessException.class, () -> service.update(p));
        verify(products, never()).update(any());
    }
    @Test void rejectsNullBlankAndPersistedInput() {
        assertThrows(ValidationException.class, () -> service.create(null));
        Product p = product(); p.setName(" ");
        assertThrows(ValidationException.class, () -> service.create(p));
        p.setName("Medicine"); p.setBarcode("");
        assertThrows(ValidationException.class, () -> service.create(p));
        p.setBarcode("123456"); p.setId(2);
        assertThrows(ValidationException.class, () -> service.create(p));
        verify(products, never()).insert(any());
    }
    @Test void rejectsMissingAndInactiveRelations() {
        Product p = product();
        when(categories.findById(3)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.create(p));
        Category c = category(); c.setActive(false);
        when(categories.findById(3)).thenReturn(c);
        assertThrows(ValidationException.class, () -> service.create(p));
        when(categories.findById(3)).thenReturn(category()); p.setManufacturerId(6L);
        assertThrows(BusinessException.class, () -> service.create(p));
        Manufacturer m = manufacturer(); m.setActive(false);
        when(manufacturers.findById(6)).thenReturn(m);
        assertThrows(ValidationException.class, () -> service.create(p));
        verify(products, never()).insert(any());
    }
    @Test void rejectsDuplicateBarcode() {
        Product existing = product(); existing.setId(99);
        when(products.findByBarcode("123456")).thenReturn(existing);
        assertThrows(ValidationException.class, () -> service.create(product()));
    }
    @ParameterizedTest @ValueSource(strings = {"-0.01", "1.001", "100000000.00"})
    void rejectsInvalidPrices(String price) {
        Product p = product(); p.setSalePrice(money(price));
        assertThrows(ValidationException.class, () -> service.create(p));
        verify(products, never()).insert(any());
    }
    @ParameterizedTest @ValueSource(strings = {"-1", "100.01", "1.001"})
    void rejectsInvalidVat(String vat) {
        Product p = product(); p.setVatRate(money(vat));
        assertThrows(ValidationException.class, () -> service.create(p));
    }
    @Test void rejectsNegativeMinimumStock() {
        Product p = product(); p.setMinimumStock(-1);
        assertThrows(ValidationException.class, () -> service.create(p));
    }
    @Test void rejectsFailedWrites() {
        Product p = product();
        assertThrows(BusinessException.class, () -> service.create(p));
        p.setId(2); when(products.findById(2)).thenReturn(p);
        assertThrows(BusinessException.class, () -> service.update(p));
        assertThrows(BusinessException.class, () -> service.deactivate(2));
    }
    @Test void translatesConstraintErrorsAndPreservesOtherErrors() {
        Product p = product();
        DatabaseException constraint = new DatabaseException("duplicate", new SQLException("duplicate", "23000"));
        when(products.insert(p)).thenThrow(constraint);
        assertSame(constraint, assertThrows(BusinessException.class, () -> service.create(p)).getCause());
        p.setId(2); when(products.findById(2)).thenReturn(p);
        when(products.update(p)).thenThrow(constraint);
        assertSame(constraint, assertThrows(BusinessException.class, () -> service.update(p)).getCause());
        p.setId(0);
        DatabaseException unavailable = new DatabaseException("offline");
        doThrow(unavailable).when(products).insert(p);
        assertSame(unavailable, assertThrows(DatabaseException.class, () -> service.create(p)));
    }
}
