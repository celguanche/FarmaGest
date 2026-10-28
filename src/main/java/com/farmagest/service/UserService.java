package com.farmagest.service;

import com.farmagest.dao.UserDAO;
import com.farmagest.model.User;

import java.util.List;

public class UserService {
    private final UserDAO users = new UserDAO();

    private User publicUser(User user) {
        user.setPasswordHash(null);
        return user;
    }

    public User findById(long id) {
        Checks.id(id);
        return publicUser(Checks.exists(users.findById(id)));
    }

    public List<User> findAll() {
        List<User> result = users.findAll();
        result.forEach(this::publicUser);
        return result;
    }

    public User create(User user, char[] password) {
        validate(user);
        Checks.require(user.getId() == 0, "Usuario nuevo requerido");
        String hash = PasswordHasher.hash(password);
        return Transactions.run(c -> {
            user.setPasswordHash(hash);
            try {
                Checks.saved(users.insert(c, user));
            } finally {
                user.setPasswordHash(null);
            }
            return user;
        });
    }

    public User update(User user) {
        validate(user);
        Checks.id(user.getId());
        return Transactions.run(c -> {
            User current = Checks.exists(users.findByIdForUpdate(c, user.getId()));
            user.setPasswordHash(current.getPasswordHash());
            try {
                Checks.saved(users.update(c, user));
            } finally {
                user.setPasswordHash(null);
            }
            return user;
        });
    }

    public void deactivate(long id) {
        findById(id);
        Checks.saved(users.deactivate(id));
    }

    public void changePassword(long id, char[] currentPassword, char[] newPassword) {
        Checks.id(id);
        String hash = PasswordHasher.hash(newPassword);
        Transactions.run(c -> {
            User user = Checks.exists(users.findByIdForUpdate(c, id));
            Checks.require(user.isActive() && PasswordHasher.verify(currentPassword, user.getPasswordHash()), "Credenciales inválidas");
            user.setPasswordHash(hash);
            Checks.saved(users.update(c, user));
            return null;
        });
    }

    private void validate(User user) {
        Checks.require(user != null, "Usuario obligatorio");
        Checks.text(user.getUsername(), 50);
        Checks.text(user.getFirstName(), 100);
        Checks.text(user.getLastName(), 150);
        Checks.email(user.getEmail(), true);
        Checks.require(user.getRole() != null, "Rol obligatorio");
    }
}
