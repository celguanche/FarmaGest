package com.farmagest.service;

import com.farmagest.model.Manufacturer;
import com.farmagest.exception.*;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManufacturerServiceTest extends ServiceTestSupport {
    @Test void createsValidRecord() {
        Manufacturer value = manufacturer();
        when(manufacturers.insert(value)).thenReturn(true);
        assertSame(value, new ManufacturerService().create(value));
        verify(manufacturers).insert(value);
    }
    @Test void updatesExistingRecord() {
        Manufacturer value = manufacturer(); value.setId(7);
        when(manufacturers.findById(7)).thenReturn(value);
        when(manufacturers.update(value)).thenReturn(true);
        assertSame(value, new ManufacturerService().update(value));
    }
    @Test void findsRecords() {
        Manufacturer value = manufacturer();
        when(manufacturers.findById(7)).thenReturn(value);
        when(manufacturers.findAll()).thenReturn(List.of(value));
        var service = new ManufacturerService();
        assertSame(value, service.findById(7));
        assertEquals(List.of(value), service.findAll());
    }
    @Test void deactivatesExistingRecord() {
        when(manufacturers.findById(7)).thenReturn(manufacturer());
        when(manufacturers.deactivate(7)).thenReturn(true);
        new ManufacturerService().deactivate(7);
        verify(manufacturers).deactivate(7);
    }
    @Test void rejectsNullAndBlankName() {
        var service = new ManufacturerService();
        assertThrows(ValidationException.class, () -> service.create(null));
        Manufacturer value = manufacturer(); value.setName(" ");
        assertThrows(ValidationException.class, () -> service.create(value));
        verifyNoInteractions(manufacturers);
    }
    @Test void rejectsInvalidIdAndMissingRecord() {
        var service = new ManufacturerService();
        assertThrows(ValidationException.class, () -> service.findById(0));
        assertThrows(BusinessException.class, () -> service.findById(7));
        assertThrows(BusinessException.class, () -> service.deactivate(7));
        verify(manufacturers, never()).deactivate(anyLong());
    }
    @Test void rejectsExistingIdOnCreate() {
        Manufacturer value = manufacturer(); value.setId(7);
        assertThrows(ValidationException.class, () -> new ManufacturerService().create(value));
        verify(manufacturers, never()).insert(any());
    }
    @Test void rejectsUpdateOfMissingRecord() {
        Manufacturer value = manufacturer(); value.setId(7);
        assertThrows(BusinessException.class, () -> new ManufacturerService().update(value));
        verify(manufacturers, never()).update(any());
    }
    @Test void rejectsUnsuccessfulWrites() {
        Manufacturer value = manufacturer();
        var service = new ManufacturerService();
        assertThrows(BusinessException.class, () -> service.create(value));
        value.setId(7); when(manufacturers.findById(7)).thenReturn(value);
        assertThrows(BusinessException.class, () -> service.update(value));
        assertThrows(BusinessException.class, () -> service.deactivate(7));
    }
    @Test void translatesConstraintViolation() {
        Manufacturer value = manufacturer();
        DatabaseException failure = new DatabaseException("duplicate", new SQLException("duplicate", "23000"));
        when(manufacturers.insert(value)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> new ManufacturerService().create(value)).getCause());
        value.setId(7); when(manufacturers.findById(7)).thenReturn(value);
        when(manufacturers.update(value)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> new ManufacturerService().update(value)).getCause());
    }
    @Test void preservesUnexpectedDatabaseFailure() {
        Manufacturer value = manufacturer();
        DatabaseException failure = new DatabaseException("unavailable");
        when(manufacturers.insert(value)).thenThrow(failure);
        assertSame(failure, assertThrows(DatabaseException.class, () -> new ManufacturerService().create(value)));
    }
}
