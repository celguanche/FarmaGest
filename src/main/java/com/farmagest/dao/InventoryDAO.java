package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.LowStockProduct;
import com.farmagest.model.ProductStock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class InventoryDAO {

    public ProductStock findStockByProductId(long productId) {

        String sql = "SELECT product_id, product_name, minimum_stock, " +
                "total_stock, saleable_stock, expired_stock " +
                "FROM v_product_stock " +
                "WHERE product_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, productId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToProductStock(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al consultar el stock del producto",
                    e
            );
        }
    }

    public List<ProductStock> findAllStock() {

        String sql = "SELECT product_id, product_name, minimum_stock, " +
                "total_stock, saleable_stock, expired_stock " +
                "FROM v_product_stock " +
                "ORDER BY product_name";

        List<ProductStock> stock = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {
                stock.add(
                        mapResultSetToProductStock(resultSet)
                );
            }

            return stock;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener el stock",
                    e
            );
        }
    }

    public List<LowStockProduct> findLowStockProducts() {

        String sql = "SELECT product_id, product_name, minimum_stock, saleable_stock " +
                "FROM v_low_stock_products " +
                "ORDER BY saleable_stock, product_name";

        List<LowStockProduct> products = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {

                products.add(
                        new LowStockProduct(
                                resultSet.getLong("product_id"),
                                resultSet.getString("product_name"),
                                resultSet.getInt("minimum_stock"),
                                resultSet.getInt("saleable_stock")
                        )
                );
            }

            return products;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los productos con stock bajo",
                    e
            );
        }
    }

    private ProductStock mapResultSetToProductStock(
            ResultSet resultSet
    ) throws SQLException {

        return new ProductStock(
                resultSet.getLong("product_id"),
                resultSet.getString("product_name"),
                resultSet.getInt("minimum_stock"),
                resultSet.getInt("total_stock"),
                resultSet.getInt("saleable_stock"),
                resultSet.getInt("expired_stock")
        );
    }
}