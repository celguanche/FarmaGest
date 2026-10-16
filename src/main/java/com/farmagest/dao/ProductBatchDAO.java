package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.ProductBatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class ProductBatchDAO {

    public ProductBatch findById(long id) {

        String sql = "SELECT id, product_id, purchase_item_id, batch_number, " +
                "expiry_date, initial_quantity, available_quantity, " +
                "unit_cost, received_at, active, created_at, updated_at " +
                "FROM product_batches " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToProductBatch(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar el lote por id",
                    e
            );
        }
    }

    public List<ProductBatch> findByProductId(long productId) {

        String sql = "SELECT id, product_id, purchase_item_id, batch_number, " +
                "expiry_date, initial_quantity, available_quantity, " +
                "unit_cost, received_at, active, created_at, updated_at " +
                "FROM product_batches " +
                "WHERE product_id = ? " +
                "ORDER BY expiry_date, received_at";

        List<ProductBatch> batches = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, productId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    batches.add(
                            mapResultSetToProductBatch(resultSet)
                    );
                }
            }

            return batches;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los lotes del producto",
                    e
            );
        }
    }

    public List<ProductBatch> findAvailableByProductId(long productId) {

        String sql = "SELECT id, product_id, purchase_item_id, batch_number, " +
                "expiry_date, initial_quantity, available_quantity, " +
                "unit_cost, received_at, active, created_at, updated_at " +
                "FROM product_batches " +
                "WHERE product_id = ? " +
                "AND active = TRUE " +
                "AND available_quantity > 0 " +
                "AND (expiry_date IS NULL OR expiry_date >= CURRENT_DATE) " +
                "ORDER BY expiry_date IS NULL, expiry_date, received_at";

        List<ProductBatch> batches = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, productId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    batches.add(
                            mapResultSetToProductBatch(resultSet)
                    );
                }
            }

            return batches;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los lotes disponibles",
                    e
            );
        }
    }

    private ProductBatch mapResultSetToProductBatch(
            ResultSet resultSet
    ) throws SQLException {

        Long purchaseItemId =
                resultSet.getObject("purchase_item_id", Long.class);

        return new ProductBatch(
                resultSet.getLong("id"),
                resultSet.getLong("product_id"),
                purchaseItemId,
                resultSet.getString("batch_number"),

                resultSet.getDate("expiry_date") != null
                        ? resultSet.getDate("expiry_date").toLocalDate()
                        : null,

                resultSet.getInt("initial_quantity"),
                resultSet.getInt("available_quantity"),
                resultSet.getBigDecimal("unit_cost"),
                resultSet.getTimestamp("received_at").toLocalDateTime(),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}