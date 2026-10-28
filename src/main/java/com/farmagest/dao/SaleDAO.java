package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.Sale;
import com.farmagest.model.enums.PaymentMethod;
import com.farmagest.model.enums.SaleStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    public Sale findById(long id) {

        String sql = "SELECT id, employee_user_id, customer_id, payment_method, status, " +
                "subtotal, tax_amount, total_amount, sold_at, " +
                "cancelled_at, cancelled_by_user_id, cancellation_reason, " +
                "created_at, updated_at " +
                "FROM sales " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToSale(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar la venta por id",
                    e
            );
        }
    }

    public List<Sale> findAll() {

        String sql = "SELECT id, employee_user_id, customer_id, payment_method, status, " +
                "subtotal, tax_amount, total_amount, sold_at, " +
                "cancelled_at, cancelled_by_user_id, cancellation_reason, " +
                "created_at, updated_at " +
                "FROM sales " +
                "ORDER BY sold_at DESC";

        List<Sale> sales = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {
                sales.add(mapResultSetToSale(resultSet));
            }

            return sales;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener las ventas",
                    e
            );
        }
    }

    public boolean insert(Connection connection, Sale sale)
            throws SQLException {

        String sql = "INSERT INTO sales " +
                "(employee_user_id, customer_id, payment_method, status, " +
                "subtotal, tax_amount, total_amount, sold_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            statement.setLong(
                    1,
                    sale.getEmployeeUserId()
            );

            if (sale.getCustomerId() == null) {
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(
                        2,
                        sale.getCustomerId()
                );
            }

            statement.setString(
                    3,
                    sale.getPaymentMethod().name()
            );

            statement.setString(
                    4,
                    sale.getStatus().name()
            );

            statement.setBigDecimal(
                    5,
                    sale.getSubtotal()
            );

            statement.setBigDecimal(
                    6,
                    sale.getTaxAmount()
            );

            statement.setBigDecimal(
                    7,
                    sale.getTotalAmount()
            );

            statement.setTimestamp(
                    8,
                    Timestamp.valueOf(sale.getSoldAt())
            );

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    sale.setId(
                            generatedKeys.getLong(1)
                    );
                }
            }

            return true;
        }
    }

    public boolean cancel(
            Connection connection,
            long saleId,
            long userId,
            String reason
    ) throws SQLException {

        String sql = "UPDATE sales " +
                "SET status = 'CANCELLED', " +
                "cancelled_at = CURRENT_TIMESTAMP, " +
                "cancelled_by_user_id = ?, " +
                "cancellation_reason = ? " +
                "WHERE id = ? " +
                "AND status = 'COMPLETED'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);
            statement.setString(2, reason);
            statement.setLong(3, saleId);

            return statement.executeUpdate() > 0;
        }
    }

    private Sale mapResultSetToSale(ResultSet resultSet)
            throws SQLException {

        Long customerId =
                resultSet.getObject("customer_id", Long.class);

        Long cancelledByUserId =
                resultSet.getObject(
                        "cancelled_by_user_id",
                        Long.class
                );

        Timestamp cancelledAt =
                resultSet.getTimestamp("cancelled_at");

        return new Sale(
                resultSet.getLong("id"),
                resultSet.getLong("employee_user_id"),
                customerId,

                PaymentMethod.valueOf(
                        resultSet.getString("payment_method")
                ),

                SaleStatus.valueOf(
                        resultSet.getString("status")
                ),

                resultSet.getBigDecimal("subtotal"),
                resultSet.getBigDecimal("tax_amount"),
                resultSet.getBigDecimal("total_amount"),

                resultSet.getTimestamp("sold_at")
                        .toLocalDateTime(),

                cancelledAt != null
                        ? cancelledAt.toLocalDateTime()
                        : null,

                cancelledByUserId,

                resultSet.getString("cancellation_reason"),

                resultSet.getTimestamp("created_at")
                        .toLocalDateTime(),

                resultSet.getTimestamp("updated_at")
                        .toLocalDateTime()
        );
    }

    public Sale findByIdForUpdate(Connection connection, long id) throws SQLException {

        String sql = "SELECT id, employee_user_id, customer_id, payment_method, status, " +
                "subtotal, tax_amount, total_amount, sold_at, " +
                "cancelled_at, cancelled_by_user_id, cancellation_reason, " +
                "created_at, updated_at " +
                "FROM sales " +
                "WHERE id = ?" + " FOR UPDATE";

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToSale(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar la venta por id",
                    e
            );
        }
    }

}
