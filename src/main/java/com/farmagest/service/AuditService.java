package com.farmagest.service;

import com.farmagest.dao.AuditLogDAO;
import com.farmagest.model.AuditLog;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Append-only administrative history. Never pass credentials to this service.
 */
public class AuditService {
    private final AuditLogDAO logs = new AuditLogDAO();

    public AuditLog findById(long id) {
        Checks.id(id);
        return Checks.exists(logs.findById(id));
    }

    public List<AuditLog> findByUserId(long userId) {
        Checks.id(userId);
        return logs.findByUserId(userId);
    }

    public List<AuditLog> findByEntity(String entityType, long entityId) {
        Checks.id(entityId);
        return logs.findByEntity(Checks.text(entityType, 100), entityId);
    }

    public AuditLog record(AuditLog log) {
        validate(log);
        return Transactions.run(connection -> record(connection, log));
    }

    /**
     * Joins the caller's transaction; does not commit or close its connection.
     */
    public AuditLog record(Connection connection, AuditLog log) throws SQLException {
        Checks.require(connection != null && !connection.getAutoCommit(),
                "La auditoría requiere una transacción activa");
        validate(log);
        if (log.getUserId() != null) StockOperations.actor(connection, log.getUserId());
        Checks.saved(logs.insert(connection, log));
        return log;
    }

    private void validate(AuditLog log) {
        Checks.require(log != null && log.getId() == 0, "Registro de auditoría nuevo requerido");
        Checks.text(log.getAction(), 50);
        Checks.text(log.getEntityType(), 100);
        Checks.optional(log.getDescription(), 500);
        if (log.getUserId() != null) Checks.id(log.getUserId());
        if (log.getEntityId() != null) Checks.id(log.getEntityId());
        // User snapshots may contain a hash under arbitrary or escaped JSON keys.
        // Record user changes through action/entity/id and a safe description instead.
        String entity = log.getEntityType().toLowerCase(Locale.ROOT);
        if (entity.equals("user") || entity.equals("users")) {
            Checks.require(log.getOldValues() == null && log.getNewValues() == null,
                    "La auditoría de usuarios no admite instantáneas con credenciales");
        }
        for (String value : new String[]{log.getOldValues(), log.getNewValues(), log.getDescription()}) {
            if (value != null) {
                String lower = value.toLowerCase(Locale.ROOT);
                Checks.require(!lower.contains("password") && !lower.contains("contraseña")
                                && !lower.contains("pbkdf2") && !lower.contains("$2a$")
                                && !lower.contains("$2b$"),
                        "No se admiten credenciales en auditoría");
            }
        }
        // MySQL validates the JSON columns; malformed JSON rolls back the transaction.
    }
}
