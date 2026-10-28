package com.farmagest.service;

import com.farmagest.exception.*;
import com.farmagest.model.User;
import com.farmagest.model.enums.Role;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest extends ServiceTestSupport {
    static final char[] PASSWORD = "correct-password".toCharArray();
    static final String HASH = PasswordHasher.hash(PASSWORD);

    User allowLogin() throws Exception {
        User u = user(); u.setPasswordHash(HASH);
        when(users.findByUsername(connection, "worker")).thenReturn(u);
        when(users.findByIdForUpdate(connection, 1)).thenReturn(u);
        when(users.updateLastLogin(connection, 1)).thenReturn(true);
        return u;
    }
    @Test void logsInWithTrimmedUsernameAndRemovesHash() throws Exception {
        User u = allowLogin();
        assertSame(u, new AuthService().login(" worker ", PASSWORD));
        assertNull(u.getPasswordHash()); assertNotNull(u.getLastLoginAt());
        verify(users).updateLastLogin(connection, 1); committed();
    }
    @Test void rejectsUnknownUser() throws Exception {
        assertThrows(BusinessException.class, () -> new AuthService().login("unknown", PASSWORD));
        verify(users, never()).updateLastLogin(any(), anyLong()); rolledBack();
    }
    @Test void rejectsWrongPassword() throws Exception {
        allowLogin();
        assertThrows(BusinessException.class, () -> new AuthService().login("worker", "wrong".toCharArray()));
        verify(users, never()).findByIdForUpdate(any(), anyLong()); rolledBack();
    }
    @Test void rejectsInactiveUser() throws Exception {
        allowLogin().setActive(false);
        assertThrows(BusinessException.class, () -> new AuthService().login("worker", PASSWORD)); rolledBack();
    }
    @Test void rechecksDeactivationUnderLock() throws Exception {
        allowLogin(); User locked = user(); locked.setActive(false); locked.setPasswordHash(HASH);
        when(users.findByIdForUpdate(connection, 1)).thenReturn(locked);
        assertThrows(BusinessException.class, () -> new AuthService().login("worker", PASSWORD));
        verify(users, never()).updateLastLogin(any(), anyLong()); rolledBack();
    }
    @Test void rejectsUserDeletedBeforeLock() throws Exception {
        allowLogin(); when(users.findByIdForUpdate(connection, 1)).thenReturn(null);
        assertThrows(BusinessException.class, () -> new AuthService().login("worker", PASSWORD)); rolledBack();
    }
    @Test void rechecksChangedPasswordUnderLock() throws Exception {
        allowLogin(); User locked = user(); locked.setPasswordHash("invalid-format");
        when(users.findByIdForUpdate(connection, 1)).thenReturn(locked);
        assertThrows(BusinessException.class, () -> new AuthService().login("worker", PASSWORD)); rolledBack();
    }
    @Test void rollsBackLastLoginFailure() throws Exception {
        allowLogin(); when(users.updateLastLogin(connection, 1)).thenThrow(new SQLException("offline", "08000"));
        assertThrows(DatabaseException.class, () -> new AuthService().login("worker", PASSWORD)); rolledBack();
    }
    @Test void rejectsUnsuccessfulLastLoginUpdate() throws Exception {
        allowLogin(); when(users.updateLastLogin(connection, 1)).thenReturn(false);
        assertThrows(BusinessException.class, () -> new AuthService().login("worker", PASSWORD)); rolledBack();
    }
    @Test void rejectsBlankUsernameBeforeOpeningTransaction() {
        assertThrows(ValidationException.class, () -> new AuthService().login(" ", PASSWORD));
        verifyNoInteractions(connection, users);
    }
    @Test void usesPersistedRoleForAuthorization() {
        User supplied = user(); supplied.setRole(Role.ADMIN);
        when(users.findById(1)).thenReturn(user());
        AuthService service = new AuthService();
        assertThrows(BusinessException.class, () -> service.requireRole(supplied, Role.ADMIN));
        assertDoesNotThrow(() -> service.requireRole(supplied, Role.ADMIN, Role.WORKER));
    }
    @Test void deniesMissingInactiveAndUnspecifiedUsers() {
        AuthService service = new AuthService();
        assertThrows(BusinessException.class, () -> service.requireRole(null, Role.WORKER));
        assertThrows(BusinessException.class, () -> service.requireRole(user(), (Role[]) null));
        assertThrows(BusinessException.class, () -> service.requireRole(user(), Role.WORKER));
        User u = user(); u.setActive(false); when(users.findById(1)).thenReturn(u);
        assertThrows(BusinessException.class, () -> service.requireRole(user(), Role.WORKER));
        assertThrows(BusinessException.class, () -> service.requireRole(user()));
    }
}
