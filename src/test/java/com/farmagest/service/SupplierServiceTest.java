package com.farmagest.service;

import com.farmagest.model.Supplier;
import com.farmagest.exception.*;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupplierServiceTest extends ServiceTestSupport {
    @Test void createsValidRecord() {
        Supplier value = supplier();
        when(suppliers.insert(value)).thenReturn(true);
        assertSame(value, new SupplierService().create(value));
        verify(suppliers).insert(value);
    }
    @Test void updatesExistingRecord() {
        Supplier value = supplier(); value.setId(7);
        when(suppliers.findById(7)).thenReturn(value);
        when(suppliers.update(value)).thenReturn(true);
        assertSame(value, new SupplierService().update(value));
    }
    @Test void findsRecords() {
        Supplier value = supplier();
        when(suppliers.findById(7)).thenReturn(value);
        when(suppliers.findAll()).thenReturn(List.of(value));
        var service = new SupplierService();
        assertSame(value, service.findById(7));
        assertEquals(List.of(value), service.findAll());
    }
    @Test void deactivatesExistingRecord() {
        when(suppliers.findById(7)).thenReturn(supplier());
        when(suppliers.deactivate(7)).thenReturn(true);
        new SupplierService().deactivate(7);
        verify(suppliers).deactivate(7);
    }
    @Test void rejectsNullAndBlankName() {
        var service = new SupplierService();
        assertThrows(ValidationException.class, () -> service.create(null));
        Supplier value = supplier(); value.setBusinessName(" ");
        assertThrows(ValidationException.class, () -> service.create(value));
        verifyNoInteractions(suppliers);
    }
    @Test void rejectsInvalidIdAndMissingRecord() {
        var service = new SupplierService();
        assertThrows(ValidationException.class, () -> service.findById(0));
        assertThrows(BusinessException.class, () -> service.findById(7));
        assertThrows(BusinessException.class, () -> service.deactivate(7));
        verify(suppliers, never()).deactivate(anyLong());
    }
    @Test void rejectsExistingIdOnCreate() {
        Supplier value = supplier(); value.setId(7);
        assertThrows(ValidationException.class, () -> new SupplierService().create(value));
        verify(suppliers, never()).insert(any());
    }
    @Test void rejectsUpdateOfMissingRecord() {
        Supplier value = supplier(); value.setId(7);
        assertThrows(BusinessException.class, () -> new SupplierService().update(value));
        verify(suppliers, never()).update(any());
    }
    @Test void rejectsUnsuccessfulWrites() {
        Supplier value = supplier();
        var service = new SupplierService();
        assertThrows(BusinessException.class, () -> service.create(value));
        value.setId(7); when(suppliers.findById(7)).thenReturn(value);
        assertThrows(BusinessException.class, () -> service.update(value));
        assertThrows(BusinessException.class, () -> service.deactivate(7));
    }
    @Test void translatesConstraintViolation() {
        Supplier value = supplier();
        DatabaseException failure = new DatabaseException("duplicate", new SQLException("duplicate", "23000"));
        when(suppliers.insert(value)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> new SupplierService().create(value)).getCause());
        value.setId(7); when(suppliers.findById(7)).thenReturn(value);
        when(suppliers.update(value)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> new SupplierService().update(value)).getCause());
    }
    @Test void preservesUnexpectedDatabaseFailure() {
        Supplier value = supplier();
        DatabaseException failure = new DatabaseException("unavailable");
        when(suppliers.insert(value)).thenThrow(failure);
        assertSame(failure, assertThrows(DatabaseException.class, () -> new SupplierService().create(value)));
    }
}
