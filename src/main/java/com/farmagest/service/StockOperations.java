package com.farmagest.service;

import com.farmagest.dao.*;
import com.farmagest.model.*;
import com.farmagest.model.enums.*;

import java.sql.*;

final class StockOperations {
    private StockOperations() {
    }

    static User actor(Connection c, long id) throws SQLException {
        Checks.id(id);
        User u = Checks.exists(new UserDAO().findByIdForUpdate(c, id));
        Checks.require(u.isActive(), "Usuario inactivo");
        return u;
    }

    static Product product(Connection c, long id) throws SQLException {
        Checks.id(id);
        Product p = Checks.exists(new ProductDAO().findByIdForUpdate(c, id));
        Checks.require(p.isActive(), "Producto inactivo");
        return p;
    }

    static void move(Connection c, ProductBatch batch, long user, int change,
                     InventoryMovementType type, Long sale, Long purchase, String reason) throws SQLException {
        long after = (long) batch.getAvailableQuantity() + change;
        Checks.require(change != 0 && after >= 0 && after <= Integer.MAX_VALUE, "Stock insuficiente o fuera de rango");
        Checks.saved(new ProductBatchDAO().updateAvailableQuantity(c, batch.getId(), (int) after));
        Checks.saved(new InventoryMovementDAO().insert(c, new InventoryMovement(0, batch.getId(), user, type, change,
                batch.getAvailableQuantity(), (int) after, sale, purchase, reason, null)));
        batch.setAvailableQuantity((int) after);
    }
}
