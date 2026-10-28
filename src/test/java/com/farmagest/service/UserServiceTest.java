package com.farmagest.service;

import com.farmagest.exception.*;
import com.farmagest.model.User;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest extends ServiceTestSupport {
    static final char[] PASSWORD = "correct-password".toCharArray();
    static final String HASH = PasswordHasher.hash(PASSWORD);
    @Test void createsUserWithVerifiableHashAndClearsReturnedHash() throws Exception {
        User u = user(); u.setId(0);
        when(users.insert(connection, u)).thenAnswer(call -> {
            assertTrue(PasswordHasher.verify(PASSWORD, u.getPasswordHash()));
            u.setId(9); return true;
        });
        assertSame(u, new UserService().create(u, PASSWORD));
        assertEquals(9, u.getId()); assertNull(u.getPasswordHash()); committed();
    }
    @Test void clearsHashAndRollsBackWhenInsertFails() throws Exception {
        User u = user(); u.setId(0);
        when(users.insert(connection, u)).thenThrow(new SQLException("duplicate", "23000"));
        assertThrows(BusinessException.class, () -> new UserService().create(u, PASSWORD));
        assertNull(u.getPasswordHash()); rolledBack();
    }
    @Test void updatesProfilePreservingStoredHashDuringWrite() throws Exception {
        User current = user(); current.setPasswordHash(HASH);
        User update = user(); update.setPasswordHash("untrusted");
        when(users.findByIdForUpdate(connection, 1)).thenReturn(current);
        when(users.update(connection, update)).thenAnswer(call -> {
            assertEquals(HASH, update.getPasswordHash()); return true;
        });
        assertSame(update, new UserService().update(update));
        assertNull(update.getPasswordHash()); committed();
    }
    @Test void clearsHashOnFailedProfileUpdate() throws Exception {
        User u = user(); u.setPasswordHash(HASH);
        when(users.findByIdForUpdate(connection, 1)).thenReturn(u);
        when(users.update(connection, u)).thenThrow(new SQLException("offline"));
        assertThrows(DatabaseException.class, () -> new UserService().update(u));
        assertNull(u.getPasswordHash()); rolledBack();
    }
    @Test void rejectsUpdateOfMissingUser() throws Exception {
        assertThrows(BusinessException.class, () -> new UserService().update(user())); rolledBack();
        verify(users, never()).update(any(), any());
    }
    @Test void returnsPublicUsersOnly() {
        User first = user(); first.setPasswordHash(HASH);
        when(users.findById(1)).thenReturn(first);
        User second = user(); second.setPasswordHash(HASH);
        when(users.findAll()).thenReturn(List.of(second));
        UserService service = new UserService();
        assertNull(service.findById(1).getPasswordHash());
        assertNull(service.findAll().getFirst().getPasswordHash());
    }
    @Test void deactivatesExistingUser() {
        when(users.findById(1)).thenReturn(user()); when(users.deactivate(1)).thenReturn(true);
        new UserService().deactivate(1); verify(users).deactivate(1);
    }
    @Test void rejectsMissingInvalidAndUnsuccessfulDeactivation() {
        UserService service = new UserService();
        assertThrows(ValidationException.class, () -> service.findById(0));
        assertThrows(BusinessException.class, () -> service.findById(1));
        when(users.findById(1)).thenReturn(user());
        assertThrows(BusinessException.class, () -> service.deactivate(1));
    }
    @Test void changesPasswordAfterVerifyingCurrentPassword() throws Exception {
        User u = user(); u.setPasswordHash(HASH);
        when(users.findByIdForUpdate(connection, 1)).thenReturn(u);
        when(users.update(connection, u)).thenAnswer(call -> {
            assertTrue(PasswordHasher.verify("new-password-123".toCharArray(), u.getPasswordHash()));
            assertNotEquals(HASH, u.getPasswordHash()); return true;
        });
        new UserService().changePassword(1, PASSWORD, "new-password-123".toCharArray()); committed();
    }
    @Test void rejectsWrongCurrentPassword() throws Exception {
        User u = user(); u.setPasswordHash(HASH);
        when(users.findByIdForUpdate(connection, 1)).thenReturn(u);
        assertThrows(ValidationException.class, () -> new UserService().changePassword(1, "wrong".toCharArray(), PASSWORD));
        verify(users, never()).update(any(), any()); rolledBack();
    }
    @Test void rejectsInactivePasswordChange() throws Exception {
        User u = user(); u.setActive(false); u.setPasswordHash(HASH);
        when(users.findByIdForUpdate(connection, 1)).thenReturn(u);
        assertThrows(ValidationException.class, () -> new UserService().changePassword(1, PASSWORD, PASSWORD)); rolledBack();
    }
    @Test void rollsBackUnsuccessfulPasswordWrite() throws Exception {
        User u = user(); u.setPasswordHash(HASH);
        when(users.findByIdForUpdate(connection, 1)).thenReturn(u);
        assertThrows(BusinessException.class, () -> new UserService().changePassword(1, PASSWORD, PASSWORD)); rolledBack();
    }
    @Test void rejectsPasswordChangeForMissingUser() throws Exception {
        assertThrows(BusinessException.class, () -> new UserService().changePassword(1, PASSWORD, PASSWORD));
        verify(users, never()).update(any(), any()); rolledBack();
    }
    @Test void rejectsInvalidUserFieldsAndShortPasswordBeforeTransaction() {
        UserService service = new UserService(); User u = user(); u.setId(0);
        assertThrows(ValidationException.class, () -> service.create(null, PASSWORD));
        u.setEmail("invalid"); assertThrows(ValidationException.class, () -> service.create(u, PASSWORD));
        u.setEmail("valid@example.com"); u.setRole(null);
        assertThrows(ValidationException.class, () -> service.create(u, PASSWORD));
        u.setRole(com.farmagest.model.enums.Role.WORKER);
        assertThrows(ValidationException.class, () -> service.create(u, "short".toCharArray()));
        u.setId(1); assertThrows(ValidationException.class, () -> service.create(u, PASSWORD));
        verifyNoInteractions(connection);
    }
}
