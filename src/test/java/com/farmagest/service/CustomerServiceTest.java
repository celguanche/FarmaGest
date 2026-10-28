package com.farmagest.service;

import com.farmagest.model.Customer;
import com.farmagest.exception.*;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerServiceTest extends ServiceTestSupport {
    @Test void createsValidRecord() {
        Customer value = customer();
        when(customers.insert(value)).thenReturn(true);
        assertSame(value, new CustomerService().create(value));
        verify(customers).insert(value);
    }
    @Test void updatesExistingRecord() {
        Customer value = customer(); value.setId(7);
        when(customers.findById(7)).thenReturn(value);
        when(customers.update(value)).thenReturn(true);
        assertSame(value, new CustomerService().update(value));
    }
    @Test void findsRecords() {
        Customer value = customer();
        when(customers.findById(7)).thenReturn(value);
        when(customers.findAll()).thenReturn(List.of(value));
        var service = new CustomerService();
        assertSame(value, service.findById(7));
        assertEquals(List.of(value), service.findAll());
    }
    @Test void deactivatesExistingRecord() {
        when(customers.findById(7)).thenReturn(customer());
        when(customers.deactivate(7)).thenReturn(true);
        new CustomerService().deactivate(7);
        verify(customers).deactivate(7);
    }
    @Test void rejectsNullAndBlankName() {
        var service = new CustomerService();
        assertThrows(ValidationException.class, () -> service.create(null));
        Customer value = customer(); value.setFirstName(" ");
        assertThrows(ValidationException.class, () -> service.create(value));
        verifyNoInteractions(customers);
    }
    @Test void rejectsInvalidIdAndMissingRecord() {
        var service = new CustomerService();
        assertThrows(ValidationException.class, () -> service.findById(0));
        assertThrows(BusinessException.class, () -> service.findById(7));
        assertThrows(BusinessException.class, () -> service.deactivate(7));
        verify(customers, never()).deactivate(anyLong());
    }
    @Test void rejectsExistingIdOnCreate() {
        Customer value = customer(); value.setId(7);
        assertThrows(ValidationException.class, () -> new CustomerService().create(value));
        verify(customers, never()).insert(any());
    }
    @Test void rejectsUpdateOfMissingRecord() {
        Customer value = customer(); value.setId(7);
        assertThrows(BusinessException.class, () -> new CustomerService().update(value));
        verify(customers, never()).update(any());
    }
    @Test void rejectsUnsuccessfulWrites() {
        Customer value = customer();
        var service = new CustomerService();
        assertThrows(BusinessException.class, () -> service.create(value));
        value.setId(7); when(customers.findById(7)).thenReturn(value);
        assertThrows(BusinessException.class, () -> service.update(value));
        assertThrows(BusinessException.class, () -> service.deactivate(7));
    }
    @Test void translatesConstraintViolation() {
        Customer value = customer();
        DatabaseException failure = new DatabaseException("duplicate", new SQLException("duplicate", "23000"));
        when(customers.insert(value)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> new CustomerService().create(value)).getCause());
        value.setId(7); when(customers.findById(7)).thenReturn(value);
        when(customers.update(value)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> new CustomerService().update(value)).getCause());
    }
    @Test void preservesUnexpectedDatabaseFailure() {
        Customer value = customer();
        DatabaseException failure = new DatabaseException("unavailable");
        when(customers.insert(value)).thenThrow(failure);
        assertSame(failure, assertThrows(DatabaseException.class, () -> new CustomerService().create(value)));
    }
}
