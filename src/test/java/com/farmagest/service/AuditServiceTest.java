package com.farmagest.service;

import com.farmagest.exception.*;
import com.farmagest.model.AuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditServiceTest extends ServiceTestSupport {
    @Test void recordsAnonymousAuditInOwnTransaction() throws Exception {
        AuditLog log = log(); when(logs.insert(connection, log)).thenReturn(true);
        when(connection.getAutoCommit()).thenReturn(true, false);
        assertSame(log, new AuditService().record(log)); committed();
    }
    @Test void joinsExistingTransactionWithoutCommittingOrClosingIt() throws Exception {
        allowStockOperations(); AuditLog log = log(); log.setUserId(1L);
        when(connection.getAutoCommit()).thenReturn(false); when(logs.insert(connection, log)).thenReturn(true);
        assertSame(log, new AuditService().record(connection, log));
        verify(users).findByIdForUpdate(connection, 1);
        verify(connection, never()).commit(); verify(connection, never()).rollback(); verify(connection, never()).close();
    }
    @Test void rejectsMissingOrNonTransactionalConnection() {
        AuditService service = new AuditService();
        assertThrows(ValidationException.class, () -> service.record(null, log()));
        assertThrows(ValidationException.class, () -> service.record(connection, log()));
        verifyNoInteractions(logs);
    }
    @Test void rollsBackAuditDaoFailure() throws Exception {
        when(connection.getAutoCommit()).thenReturn(true, false);
        when(logs.insert(eq(connection), any())).thenThrow(new SQLException("invalid JSON", "22032"));
        assertThrows(DatabaseException.class, () -> new AuditService().record(log())); rolledBack();
    }
    @Test void rejectsUnsuccessfulInsert() throws Exception {
        when(connection.getAutoCommit()).thenReturn(true, false);
        assertThrows(BusinessException.class, () -> new AuditService().record(log())); rolledBack();
    }
    @ParameterizedTest @ValueSource(strings = {"password", "PASSWORD", "contraseña", "pbkdf2", "$2a$", "$2b$"})
    void rejectsCredentialMarkersInAllTextFields(String marker) {
        AuditService service = new AuditService();
        AuditLog old = log(); old.setOldValues(marker);
        AuditLog updated = log(); updated.setNewValues(marker);
        AuditLog description = log(); description.setDescription(marker);
        assertThrows(ValidationException.class, () -> service.record(old));
        assertThrows(ValidationException.class, () -> service.record(updated));
        assertThrows(ValidationException.class, () -> service.record(description));
        verifyNoInteractions(connection, logs);
    }
    @ParameterizedTest @ValueSource(strings = {"user", "users", "USER", "Users"})
    void rejectsAllUserSnapshots(String entity) {
        AuditLog log = log(); log.setEntityType(entity);
        assertThrows(ValidationException.class, () -> new AuditService().record(log)); verifyNoInteractions(logs);
    }
    @Test void permitsUserAuditWithoutSnapshots() throws Exception {
        AuditLog log = log(); log.setEntityType("user"); log.setNewValues(null);
        when(connection.getAutoCommit()).thenReturn(true, false); when(logs.insert(connection, log)).thenReturn(true);
        new AuditService().record(log); committed();
    }
    @Test void rejectsNullPersistedAndInvalidLog() {
        AuditService service = new AuditService(); AuditLog log = log();
        assertThrows(ValidationException.class, () -> service.record((AuditLog) null));
        log.setId(1); assertThrows(ValidationException.class, () -> service.record(log));
        log.setId(0); log.setAction(" "); assertThrows(ValidationException.class, () -> service.record(log));
        log.setAction("CREATE"); log.setEntityId(0L); assertThrows(ValidationException.class, () -> service.record(log));
        verifyNoInteractions(connection);
    }
    @Test void queriesAuditHistory() {
        AuditLog log = log(); when(logs.findById(7)).thenReturn(log);
        when(logs.findByUserId(1)).thenReturn(List.of(log)); when(logs.findByEntity("product", 2)).thenReturn(List.of(log));
        AuditService service = new AuditService();
        assertSame(log, service.findById(7)); assertEquals(List.of(log), service.findByUserId(1));
        assertEquals(List.of(log), service.findByEntity(" product ", 2));
    }
    @Test void rejectsMissingAuditAndInvalidQueries() {
        AuditService service = new AuditService();
        assertThrows(BusinessException.class, () -> service.findById(7));
        assertThrows(ValidationException.class, () -> service.findById(0));
        assertThrows(ValidationException.class, () -> service.findByUserId(0));
        assertThrows(ValidationException.class, () -> service.findByEntity(" ", 2));
        assertThrows(ValidationException.class, () -> service.findByEntity("product", 0));
    }
}
