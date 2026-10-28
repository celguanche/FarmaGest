package com.farmagest.service;

import com.farmagest.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void usesRandomSaltAndVerifiesWithoutMutatingPassword() {
        char[] password = "correct-password".toCharArray(); char[] original = password.clone();
        String first = PasswordHasher.hash(password), second = PasswordHasher.hash(password);
        assertNotEquals(first, second); assertTrue(PasswordHasher.verify(password, first));
        assertFalse(PasswordHasher.verify("incorrect".toCharArray(), first));
        assertArrayEquals(original, password);
    }
    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {"plain", "pbkdf2-sha256$1$a$b", "pbkdf2-sha256$3000000$a$b", "pbkdf2-sha256$abc$a$b", "pbkdf2-sha256$600000$!$!", "pbkdf2-sha256$600000$YQ==$Yg==", "unknown$600000$a$b"})
    void safelyRejectsMalformedHashes(String encoded) {
        assertFalse(PasswordHasher.verify("correct-password".toCharArray(), encoded));
    }
    @Test void rejectsInvalidPasswordLengthsAndNulls() {
        assertThrows(ValidationException.class, () -> PasswordHasher.hash(null));
        assertThrows(ValidationException.class, () -> PasswordHasher.hash("short".toCharArray()));
        assertThrows(ValidationException.class, () -> PasswordHasher.hash(new char[1025]));
        assertFalse(PasswordHasher.verify(null, "hash"));
        assertFalse(PasswordHasher.verify(new char[1025], "hash"));
    }
}
