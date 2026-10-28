package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.Purchase;
import com.farmagest.model.enums.PurchaseStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDAO {

    public Purchase findById(long id) {

        String sql = "SELECT id, supplier_id, created_by_user_id, status, " +
                "subtotal, tax_amount, total_amount, ordered_at, received_at, " +
                "cancelled_at, cancelled_by_user_id, cancellation_reason, notes, " +
                "created_at, updated_at " +
                "FROM purchases " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToPurchase(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar la compra por id",
                    e
            );
        }
    }

    public List<Purchase> findAll() {

        String sql = "SELECT id, supplier_id, created_by_user_id, status, " +
                "subtotal, tax_amount, total_amount, ordered_at, received_at, " +
                "cancelled_at, cancelled_by_user_id, cancellation_reason, notes, " +
                "created_at, updated_at " +
                "FROM purchases " +
                "ORDER BY ordered_at DESC";

        List<Purchase> purchases = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {
                purchases.add(mapResultSetToPurchase(resultSet));
            }

            return purchases;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener las compras",
                    e
            );
        }
    }

    public boolean insert(Connection connection, Purchase purchase)
            throws SQLException {

        String sql = "INSERT INTO purchases " +
                "(supplier_id, created_by_user_id, status, subtotal, tax_amount, " +
                "total_amount, ordered_at, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            statement.setLong(1, purchase.getSupplierId());
            statement.setLong(2, purchase.getCreatedByUserId());
            statement.setString(3, purchase.getStatus().name());
            statement.setBigDecimal(4, purchase.getSubtotal());
            statement.setBigDecimal(5, purchase.getTaxAmount());
            statement.setBigDecimal(6, purchase.getTotalAmount());
            statement.setTimestamp(
                    7,
                    Timestamp.valueOf(purchase.getOrderedAt())
            );
            statement.setString(8, purchase.getNotes());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    purchase.setId(generatedKeys.getLong(1));
                }
            }

            return true;
        }
    }

    public boolean markAsReceived(
            Connection connection,
            long purchaseId
    ) throws SQLException {

        String sql = "UPDATE purchases " +
                "SET status = 'RECEIVED', " +
                "received_at = CURRENT_TIMESTAMP " +
                "WHERE id = ? AND status = 'ORDERED'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, purchaseId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean cancel(
            Connection connection,
            long purchaseId,
            long userId,
            String reason
    ) throws SQLException {

        String sql = "UPDATE purchases " +
                "SET status = 'CANCELLED', " +
                "cancelled_at = CURRENT_TIMESTAMP, " +
                "cancelled_by_user_id = ?, " +
                "cancellation_reason = ? " +
                "WHERE id = ? AND status = 'ORDERED'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);
            statement.setString(2, reason);
            statement.setLong(3, purchaseId);

            return statement.executeUpdate() > 0;
        }
    }

    private Purchase mapResultSetToPurchase(ResultSet resultSet)
            throws SQLException {

        Timestamp receivedAt =
                resultSet.getTimestamp("received_at");

        Timestamp cancelledAt =
                resultSet.getTimestamp("cancelled_at");

        Long cancelledByUserId =
                resultSet.getObject(
                        "cancelled_by_user_id",
                        Long.class
                );

        return new Purchase(
                resultSet.getLong("id"),
                resultSet.getLong("supplier_id"),
                resultSet.getLong("created_by_user_id"),
                PurchaseStatus.valueOf(
                        resultSet.getString("status")
                ),
                resultSet.getBigDecimal("subtotal"),
                resultSet.getBigDecimal("tax_amount"),
                resultSet.getBigDecimal("total_amount"),
                resultSet.getTimestamp("ordered_at")
                        .toLocalDateTime(),

                receivedAt != null
                        ? receivedAt.toLocalDateTime()
                        : null,

                cancelledAt != null
                        ? cancelledAt.toLocalDateTime()
                        : null,

                cancelledByUserId,
                resultSet.getString("cancellation_reason"),
                resultSet.getString("notes"),
                resultSet.getTimestamp("created_at")
                        .toLocalDateTime(),
                resultSet.getTimestamp("updated_at")
                        .toLocalDateTime()
        );
    }

    public Purchase findByIdForUpdate(Connection connection, long id) throws SQLException {

        String sql = "SELECT id, supplier_id, created_by_user_id, status, " +
                "subtotal, tax_amount, total_amount, ordered_at, received_at, " +
                "cancelled_at, cancelled_by_user_id, cancellation_reason, notes, " +
                "created_at, updated_at " +
                "FROM purchases " +
                "WHERE id = ?" + " FOR UPDATE";

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToPurchase(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar la compra por id",
                    e
            );
        }
    }

}
