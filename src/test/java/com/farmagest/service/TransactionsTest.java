package com.farmagest.service;

import com.farmagest.exception.*;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionsTest extends ServiceTestSupport {
    @Test void returnsWorkResultAndCommits() throws Exception {
        assertEquals(42, Transactions.<Integer>run(c -> { assertSame(connection, c); return 42; }).intValue()); committed();
    }
    @Test void rollsBackBusinessExceptionAndPreservesIdentity() throws Exception {
        BusinessException failure = new BusinessException("failure");
        assertSame(failure, assertThrows(BusinessException.class, () -> Transactions.run(c -> { throw failure; })));
        rolledBack();
    }
    @Test void translatesSqlConstraintViolationAfterRollback() throws Exception {
        SQLException failure = new SQLException("duplicate", "23000");
        BusinessException result = assertThrows(BusinessException.class, () -> Transactions.run(c -> { throw failure; }));
        assertSame(failure, result.getCause().getCause()); rolledBack();
    }
    @Test void translatesWrappedConstraintViolation() throws Exception {
        DatabaseException failure = new DatabaseException("duplicate", new SQLException("duplicate", "23000"));
        assertSame(failure, assertThrows(BusinessException.class, () -> Transactions.run(c -> { throw failure; })).getCause());
        rolledBack();
    }
    @Test void preservesUnexpectedDatabaseException() throws Exception {
        DatabaseException failure = new DatabaseException("offline");
        assertSame(failure, assertThrows(DatabaseException.class, () -> Transactions.run(c -> { throw failure; })));
        rolledBack();
    }
    @Test void rollsBackCommitFailure() throws Exception {
        SQLException failure = new SQLException("commit failed"); doThrow(failure).when(connection).commit();
        assertSame(failure, assertThrows(DatabaseException.class, () -> Transactions.run(c -> 42)).getCause());
        var order = inOrder(connection);
        order.verify(connection).setAutoCommit(false); order.verify(connection).commit();
        order.verify(connection).rollback(); order.verify(connection).setAutoCommit(true); order.verify(connection).close();
    }
    @Test void neverEnablesAutoCommitWhenRollbackFails() throws Exception {
        BusinessException original = new BusinessException("original"); SQLException rollback = new SQLException("rollback");
        doThrow(rollback).when(connection).rollback();
        var result = assertThrows(BusinessException.class, () -> Transactions.run(c -> { throw original; }));
        assertSame(original, result); assertArrayEquals(new Throwable[]{rollback}, result.getSuppressed());
        verify(connection, never()).setAutoCommit(true); verify(connection, never()).commit(); verify(connection).close();
    }
    @Test void suppressesRestoreFailureWithoutHidingOriginalFailure() throws Exception {
        BusinessException original = new BusinessException("original"); SQLException restore = new SQLException("restore");
        doThrow(restore).when(connection).setAutoCommit(true);
        var result = assertThrows(BusinessException.class, () -> Transactions.run(c -> { throw original; }));
        assertSame(original, result); assertArrayEquals(new Throwable[]{restore}, result.getSuppressed());
        verify(connection).rollback(); verify(connection).close();
    }
    @Test void reportsRestoreFailureAfterSuccessfulCommit() throws Exception {
        SQLException restore = new SQLException("restore"); doThrow(restore).when(connection).setAutoCommit(true);
        assertSame(restore, assertThrows(DatabaseException.class, () -> Transactions.run(c -> 42)).getCause());
        verify(connection).commit(); verify(connection, never()).rollback(); verify(connection).close();
    }
    @Test void restoresOriginallyDisabledAutoCommit() throws Exception {
        when(connection.getAutoCommit()).thenReturn(false);
        assertEquals(42, Transactions.<Integer>execute(connection, c -> 42).intValue());
        verify(connection, times(2)).setAutoCommit(false); verify(connection, never()).setAutoCommit(true);
        verify(connection, never()).close();
    }
    @Test void rollsBackErrorsToo() throws Exception {
        AssertionError failure = new AssertionError("failure");
        assertSame(failure, assertThrows(AssertionError.class, () -> Transactions.run(c -> { throw failure; }))); rolledBack();
    }
}
