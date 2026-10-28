package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.SaleItemBatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class SaleItemBatchDAO {

    public SaleItemBatch find(long saleItemId, long batchId) {

        String sql = "SELECT sale_item_id, batch_id, quantity, created_at " +
                "FROM sale_item_batches " +
                "WHERE sale_item_id = ? AND batch_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, saleItemId);
            statement.setLong(2, batchId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToSaleItemBatch(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar la relación entre línea de venta y lote",
                    e
            );
        }
    }

    public List<SaleItemBatch> findBySaleItemId(long saleItemId) {

        String sql = "SELECT sale_item_id, batch_id, quantity, created_at " +
                "FROM sale_item_batches " +
                "WHERE sale_item_id = ? " +
                "ORDER BY batch_id";

        List<SaleItemBatch> batches = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, saleItemId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    batches.add(
                            mapResultSetToSaleItemBatch(resultSet)
                    );
                }
            }

            return batches;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los lotes de la línea de venta",
                    e
            );
        }
    }

    public boolean insert(
            Connection connection,
            SaleItemBatch saleItemBatch
    ) throws SQLException {

        String sql = "INSERT INTO sale_item_batches " +
                "(sale_item_id, batch_id, quantity) " +
                "VALUES (?, ?, ?)";

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    saleItemBatch.getSaleItemId()
            );

            statement.setLong(
                    2,
                    saleItemBatch.getBatchId()
            );

            statement.setInt(
                    3,
                    saleItemBatch.getQuantity()
            );

            return statement.executeUpdate() > 0;
        }
    }

    public List<SaleItemBatch> findByBatchId(long batchId) {

        String sql = "SELECT sale_item_id, batch_id, quantity, created_at " +
                "FROM sale_item_batches " +
                "WHERE batch_id = ? " +
                "ORDER BY sale_item_id";

        List<SaleItemBatch> items = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, batchId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    items.add(
                            mapResultSetToSaleItemBatch(resultSet)
                    );
                }
            }

            return items;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener las ventas asociadas al lote",
                    e
            );
        }
    }

    private SaleItemBatch mapResultSetToSaleItemBatch(
            ResultSet resultSet
    ) throws SQLException {

        return new SaleItemBatch(
                resultSet.getLong("sale_item_id"),
                resultSet.getLong("batch_id"),
                resultSet.getInt("quantity"),
                resultSet.getTimestamp("created_at")
                        .toLocalDateTime()
        );
    }

    public List<SaleItemBatch> findBySaleItemId(Connection connection, long saleItemId) throws SQLException {

        String sql = "SELECT sale_item_id, batch_id, quantity, created_at " +
                "FROM sale_item_batches " +
                "WHERE sale_item_id = ? " +
                "ORDER BY batch_id";

        List<SaleItemBatch> batches = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, saleItemId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    batches.add(
                            mapResultSetToSaleItemBatch(resultSet)
                    );
                }
            }

            return batches;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los lotes de la línea de venta",
                    e
            );
        }
    }

}
