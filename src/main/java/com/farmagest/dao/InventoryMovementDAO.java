package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.InventoryMovement;
import com.farmagest.model.enums.InventoryMovementType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class InventoryMovementDAO {

    public InventoryMovement findById(long id) {

        String sql = "SELECT id, batch_id, user_id, movement_type, " +
                "quantity_change, stock_before, stock_after, " +
                "sale_id, purchase_id, reason, created_at " +
                "FROM inventory_movements " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToInventoryMovement(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar el movimiento de inventario por id",
                    e
            );
        }
    }

    public List<InventoryMovement> findByBatchId(long batchId) {

        String sql = "SELECT id, batch_id, user_id, movement_type, " +
                "quantity_change, stock_before, stock_after, " +
                "sale_id, purchase_id, reason, created_at " +
                "FROM inventory_movements " +
                "WHERE batch_id = ? " +
                "ORDER BY created_at DESC";

        List<InventoryMovement> movements = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, batchId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    movements.add(
                            mapResultSetToInventoryMovement(resultSet)
                    );
                }
            }

            return movements;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los movimientos del lote",
                    e
            );
        }
    }

    public List<InventoryMovement> findBySaleId(long saleId) {

        String sql = "SELECT id, batch_id, user_id, movement_type, " +
                "quantity_change, stock_before, stock_after, " +
                "sale_id, purchase_id, reason, created_at " +
                "FROM inventory_movements " +
                "WHERE sale_id = ? " +
                "ORDER BY created_at";

        List<InventoryMovement> movements = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, saleId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    movements.add(
                            mapResultSetToInventoryMovement(resultSet)
                    );
                }
            }

            return movements;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los movimientos de la venta",
                    e
            );
        }
    }

    public List<InventoryMovement> findByPurchaseId(long purchaseId) {

        String sql = "SELECT id, batch_id, user_id, movement_type, " +
                "quantity_change, stock_before, stock_after, " +
                "sale_id, purchase_id, reason, created_at " +
                "FROM inventory_movements " +
                "WHERE purchase_id = ? " +
                "ORDER BY created_at";

        List<InventoryMovement> movements = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, purchaseId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    movements.add(
                            mapResultSetToInventoryMovement(resultSet)
                    );
                }
            }

            return movements;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los movimientos de la compra",
                    e
            );
        }
    }

    public boolean insert(
            Connection connection,
            InventoryMovement movement
    ) throws SQLException {

        String sql = "INSERT INTO inventory_movements " +
                "(batch_id, user_id, movement_type, quantity_change, " +
                "stock_before, stock_after, sale_id, purchase_id, reason) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            statement.setLong(
                    1,
                    movement.getBatchId()
            );

            statement.setLong(
                    2,
                    movement.getUserId()
            );

            statement.setString(
                    3,
                    movement.getMovementType().name()
            );

            statement.setInt(
                    4,
                    movement.getQuantityChange()
            );

            statement.setInt(
                    5,
                    movement.getStockBefore()
            );

            statement.setInt(
                    6,
                    movement.getStockAfter()
            );

            if (movement.getSaleId() == null) {
                statement.setNull(7, java.sql.Types.BIGINT);
            } else {
                statement.setLong(
                        7,
                        movement.getSaleId()
                );
            }

            if (movement.getPurchaseId() == null) {
                statement.setNull(8, java.sql.Types.BIGINT);
            } else {
                statement.setLong(
                        8,
                        movement.getPurchaseId()
                );
            }

            statement.setString(
                    9,
                    movement.getReason()
            );

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    movement.setId(
                            generatedKeys.getLong(1)
                    );
                }
            }

            return true;
        }
    }

    private InventoryMovement mapResultSetToInventoryMovement(
            ResultSet resultSet
    ) throws SQLException {

        Long saleId =
                resultSet.getObject("sale_id", Long.class);

        Long purchaseId =
                resultSet.getObject("purchase_id", Long.class);

        return new InventoryMovement(
                resultSet.getLong("id"),
                resultSet.getLong("batch_id"),
                resultSet.getLong("user_id"),

                InventoryMovementType.valueOf(
                        resultSet.getString("movement_type")
                ),

                resultSet.getInt("quantity_change"),
                resultSet.getInt("stock_before"),
                resultSet.getInt("stock_after"),
                saleId,
                purchaseId,
                resultSet.getString("reason"),

                resultSet.getTimestamp("created_at")
                        .toLocalDateTime()
        );
    }
}