package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public AuditLog findById(long id) {

        String sql = "SELECT id, user_id, action, entity_type, entity_id, " +
                "old_values, new_values, description, created_at " +
                "FROM audit_logs " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToAuditLog(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar el registro de auditoría por id",
                    e
            );
        }
    }

    public List<AuditLog> findByUserId(long userId) {

        String sql = "SELECT id, user_id, action, entity_type, entity_id, " +
                "old_values, new_values, description, created_at " +
                "FROM audit_logs " +
                "WHERE user_id = ? " +
                "ORDER BY created_at DESC";

        List<AuditLog> logs = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    logs.add(
                            mapResultSetToAuditLog(resultSet)
                    );
                }
            }

            return logs;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los registros de auditoría del usuario",
                    e
            );
        }
    }

    public List<AuditLog> findByEntity(
            String entityType,
            long entityId
    ) {

        String sql = "SELECT id, user_id, action, entity_type, entity_id, " +
                "old_values, new_values, description, created_at " +
                "FROM audit_logs " +
                "WHERE entity_type = ? AND entity_id = ? " +
                "ORDER BY created_at DESC";

        List<AuditLog> logs = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, entityType);
            statement.setLong(2, entityId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    logs.add(
                            mapResultSetToAuditLog(resultSet)
                    );
                }
            }

            return logs;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los registros de auditoría de la entidad",
                    e
            );
        }
    }

    public boolean insert(
            Connection connection,
            AuditLog auditLog
    ) throws SQLException {

        String sql = "INSERT INTO audit_logs " +
                "(user_id, action, entity_type, entity_id, " +
                "old_values, new_values, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            if (auditLog.getUserId() == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(
                        1,
                        auditLog.getUserId()
                );
            }

            statement.setString(
                    2,
                    auditLog.getAction()
            );

            statement.setString(
                    3,
                    auditLog.getEntityType()
            );

            if (auditLog.getEntityId() == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(
                        4,
                        auditLog.getEntityId()
                );
            }

            if (auditLog.getOldValues() == null) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(
                        5,
                        auditLog.getOldValues()
                );
            }

            if (auditLog.getNewValues() == null) {
                statement.setNull(6, Types.VARCHAR);
            } else {
                statement.setString(
                        6,
                        auditLog.getNewValues()
                );
            }

            statement.setString(
                    7,
                    auditLog.getDescription()
            );

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    auditLog.setId(
                            generatedKeys.getLong(1)
                    );
                }
            }

            return true;
        }
    }

    private AuditLog mapResultSetToAuditLog(
            ResultSet resultSet
    ) throws SQLException {

        Long userId =
                resultSet.getObject("user_id", Long.class);

        Long entityId =
                resultSet.getObject("entity_id", Long.class);

        return new AuditLog(
                resultSet.getLong("id"),
                userId,
                resultSet.getString("action"),
                resultSet.getString("entity_type"),
                entityId,
                resultSet.getString("old_values"),
                resultSet.getString("new_values"),
                resultSet.getString("description"),
                resultSet.getTimestamp("created_at")
                        .toLocalDateTime()
        );
    }
}