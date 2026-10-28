package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.SaleItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleItemDAO {

    public SaleItem findById(long id) {

        String sql = "SELECT id, sale_id, product_id, quantity, " +
                "unit_price, vat_rate, subtotal, tax_amount, line_total, created_at " +
                "FROM sale_items " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToSaleItem(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar la línea de venta por id",
                    e
            );
        }
    }

    public List<SaleItem> findBySaleId(long saleId) {

        String sql = "SELECT id, sale_id, product_id, quantity, " +
                "unit_price, vat_rate, subtotal, tax_amount, line_total, created_at " +
                "FROM sale_items " +
                "WHERE sale_id = ? " +
                "ORDER BY id";

        List<SaleItem> items = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, saleId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    items.add(
                            mapResultSetToSaleItem(resultSet)
                    );
                }
            }

            return items;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener las líneas de la venta",
                    e
            );
        }
    }

    public boolean insert(
            Connection connection,
            SaleItem saleItem
    ) throws SQLException {

        String sql = "INSERT INTO sale_items " +
                "(sale_id, product_id, quantity, unit_price, vat_rate, " +
                "subtotal, tax_amount, line_total) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            statement.setLong(
                    1,
                    saleItem.getSaleId()
            );

            statement.setLong(
                    2,
                    saleItem.getProductId()
            );

            statement.setInt(
                    3,
                    saleItem.getQuantity()
            );

            statement.setBigDecimal(
                    4,
                    saleItem.getUnitPrice()
            );

            statement.setBigDecimal(
                    5,
                    saleItem.getVatRate()
            );

            statement.setBigDecimal(
                    6,
                    saleItem.getSubtotal()
            );

            statement.setBigDecimal(
                    7,
                    saleItem.getTaxAmount()
            );

            statement.setBigDecimal(
                    8,
                    saleItem.getLineTotal()
            );

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    saleItem.setId(
                            generatedKeys.getLong(1)
                    );
                }
            }

            return true;
        }
    }

    private SaleItem mapResultSetToSaleItem(
            ResultSet resultSet
    ) throws SQLException {

        return new SaleItem(
                resultSet.getLong("id"),
                resultSet.getLong("sale_id"),
                resultSet.getLong("product_id"),
                resultSet.getInt("quantity"),
                resultSet.getBigDecimal("unit_price"),
                resultSet.getBigDecimal("vat_rate"),
                resultSet.getBigDecimal("subtotal"),
                resultSet.getBigDecimal("tax_amount"),
                resultSet.getBigDecimal("line_total"),
                resultSet.getTimestamp("created_at")
                        .toLocalDateTime()
        );
    }

    public List<SaleItem> findBySaleId(Connection connection, long saleId) throws SQLException {

        String sql = "SELECT id, sale_id, product_id, quantity, " +
                "unit_price, vat_rate, subtotal, tax_amount, line_total, created_at " +
                "FROM sale_items " +
                "WHERE sale_id = ? " +
                "ORDER BY id";

        List<SaleItem> items = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, saleId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    items.add(
                            mapResultSetToSaleItem(resultSet)
                    );
                }
            }

            return items;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener las líneas de la venta",
                    e
            );
        }
    }

}
