package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.PurchaseItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseItemDAO {

    public PurchaseItem findById(long id) {

        String sql = "SELECT id, purchase_id, product_id, quantity, " +
                "unit_cost, vat_rate, subtotal, tax_amount, " +
                "line_total, created_at " +
                "FROM purchase_items " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToPurchaseItem(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar la línea de compra por id",
                    e
            );
        }
    }

    public List<PurchaseItem> findByPurchaseId(long purchaseId) {

        String sql = "SELECT id, purchase_id, product_id, quantity, " +
                "unit_cost, vat_rate, subtotal, tax_amount, " +
                "line_total, created_at " +
                "FROM purchase_items " +
                "WHERE purchase_id = ? " +
                "ORDER BY id";

        List<PurchaseItem> items = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, purchaseId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    items.add(
                            mapResultSetToPurchaseItem(resultSet)
                    );
                }
            }

            return items;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener las líneas de la compra",
                    e
            );
        }
    }

    public boolean insert(
            Connection connection,
            PurchaseItem purchaseItem
    ) throws SQLException {

        String sql = "INSERT INTO purchase_items " +
                "(purchase_id, product_id, quantity, unit_cost, " +
                "vat_rate, subtotal, tax_amount, line_total) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            statement.setLong(
                    1,
                    purchaseItem.getPurchaseId()
            );

            statement.setLong(
                    2,
                    purchaseItem.getProductId()
            );

            statement.setInt(
                    3,
                    purchaseItem.getQuantity()
            );

            statement.setBigDecimal(
                    4,
                    purchaseItem.getUnitCost()
            );

            statement.setBigDecimal(
                    5,
                    purchaseItem.getVatRate()
            );

            statement.setBigDecimal(
                    6,
                    purchaseItem.getSubtotal()
            );

            statement.setBigDecimal(
                    7,
                    purchaseItem.getTaxAmount()
            );

            statement.setBigDecimal(
                    8,
                    purchaseItem.getLineTotal()
            );

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    purchaseItem.setId(
                            generatedKeys.getLong(1)
                    );
                }
            }

            return true;
        }
    }

    private PurchaseItem mapResultSetToPurchaseItem(
            ResultSet resultSet
    ) throws SQLException {

        return new PurchaseItem(
                resultSet.getLong("id"),
                resultSet.getLong("purchase_id"),
                resultSet.getLong("product_id"),
                resultSet.getInt("quantity"),
                resultSet.getBigDecimal("unit_cost"),
                resultSet.getBigDecimal("vat_rate"),
                resultSet.getBigDecimal("subtotal"),
                resultSet.getBigDecimal("tax_amount"),
                resultSet.getBigDecimal("line_total"),
                resultSet.getTimestamp("created_at").toLocalDateTime()
        );
    }

    public List<PurchaseItem> findByPurchaseId(Connection connection, long purchaseId) throws SQLException {

        String sql = "SELECT id, purchase_id, product_id, quantity, " +
                "unit_cost, vat_rate, subtotal, tax_amount, " +
                "line_total, created_at " +
                "FROM purchase_items " +
                "WHERE purchase_id = ? " +
                "ORDER BY id";

        List<PurchaseItem> items = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, purchaseId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    items.add(
                            mapResultSetToPurchaseItem(resultSet)
                    );
                }
            }

            return items;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener las líneas de la compra",
                    e
            );
        }
    }

}
