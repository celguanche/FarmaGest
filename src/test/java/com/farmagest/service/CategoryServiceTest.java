package com.farmagest.service;

import com.farmagest.model.Category;
import com.farmagest.exception.*;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceTest extends ServiceTestSupport {
    @Test void createsValidRecord() {
        Category value = category();
        when(categories.insert(value)).thenReturn(true);
        assertSame(value, new CategoryService().create(value));
        verify(categories).insert(value);
    }
    @Test void updatesExistingRecord() {
        Category value = category(); value.setId(7);
        when(categories.findById(7)).thenReturn(value);
        when(categories.update(value)).thenReturn(true);
        assertSame(value, new CategoryService().update(value));
    }
    @Test void findsRecords() {
        Category value = category();
        when(categories.findById(7)).thenReturn(value);
        when(categories.findAll()).thenReturn(List.of(value));
        var service = new CategoryService();
        assertSame(value, service.findById(7));
        assertEquals(List.of(value), service.findAll());
    }
    @Test void deactivatesExistingRecord() {
        when(categories.findById(7)).thenReturn(category());
        when(categories.deactivate(7)).thenReturn(true);
        new CategoryService().deactivate(7);
        verify(categories).deactivate(7);
    }
    @Test void rejectsNullAndBlankName() {
        var service = new CategoryService();
        assertThrows(ValidationException.class, () -> service.create(null));
        Category value = category(); value.setName(" ");
        assertThrows(ValidationException.class, () -> service.create(value));
        verifyNoInteractions(categories);
    }
    @Test void rejectsInvalidIdAndMissingRecord() {
        var service = new CategoryService();
        assertThrows(ValidationException.class, () -> service.findById(0));
        assertThrows(BusinessException.class, () -> service.findById(7));
        assertThrows(BusinessException.class, () -> service.deactivate(7));
        verify(categories, never()).deactivate(anyLong());
    }
    @Test void rejectsExistingIdOnCreate() {
        Category value = category(); value.setId(7);
        assertThrows(ValidationException.class, () -> new CategoryService().create(value));
        verify(categories, never()).insert(any());
    }
    @Test void rejectsUpdateOfMissingRecord() {
        Category value = category(); value.setId(7);
        assertThrows(BusinessException.class, () -> new CategoryService().update(value));
        verify(categories, never()).update(any());
    }
    @Test void rejectsUnsuccessfulWrites() {
        Category value = category();
        var service = new CategoryService();
        assertThrows(BusinessException.class, () -> service.create(value));
        value.setId(7); when(categories.findById(7)).thenReturn(value);
        assertThrows(BusinessException.class, () -> service.update(value));
        assertThrows(BusinessException.class, () -> service.deactivate(7));
    }
    @Test void translatesConstraintViolation() {
        Category value = category();
        DatabaseException failure = new DatabaseException("duplicate", new SQLException("duplicate", "23000"));
        when(categories.insert(value)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> new CategoryService().create(value)).getCause());
        value.setId(7); when(categories.findById(7)).thenReturn(value);
        when(categories.update(value)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> new CategoryService().update(value)).getCause());
    }
    @Test void preservesUnexpectedDatabaseFailure() {
        Category value = category();
        DatabaseException failure = new DatabaseException("unavailable");
        when(categories.insert(value)).thenThrow(failure);
        assertSame(failure, assertThrows(DatabaseException.class, () -> new CategoryService().create(value)));
    }
}
