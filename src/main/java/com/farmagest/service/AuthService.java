package com.farmagest.service;

import com.farmagest.dao.UserDAO;
import com.farmagest.exception.BusinessException;
import com.farmagest.model.User;
import com.farmagest.model.enums.Role;

import java.time.LocalDateTime;

public class AuthService {
    private final UserDAO users = new UserDAO();
    private static final String DUMMY = PasswordHasher.hash("dummy-password-never-used".toCharArray());

    public User login(String username, char[] password) {
        String name = Checks.text(username, 50);
        return Transactions.run(c -> {
            User user = users.findByUsername(c, name);
            boolean valid = PasswordHasher.verify(password, user == null ? DUMMY : user.getPasswordHash());
            if (!valid || user == null || !user.isActive()) throw new BusinessException("Credenciales inválidas");
            // Recheck under lock so deactivation cannot race with a successful login.
            user = users.findByIdForUpdate(c, user.getId());
            if (user == null || !user.isActive() || !PasswordHasher.verify(password, user.getPasswordHash()))
                throw new BusinessException("Credenciales inválidas");
            Checks.saved(users.updateLastLogin(c, user.getId()));
            user.setLastLoginAt(LocalDateTime.now());
            user.setPasswordHash(null);
            return user;
        });
    }

    public void requireRole(User user, Role... allowed) {
        if (user == null || allowed == null) throw new BusinessException("Acceso denegado");
        Checks.id(user.getId());
        User current = Checks.exists(users.findById(user.getId()));
        if (current.isActive()) for (Role role : allowed) if (current.getRole() == role) return;
        throw new BusinessException("Acceso denegado");
    }
}
