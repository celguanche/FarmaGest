package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.ProductSupplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class ProductSupplierDAO {

    public ProductSupplier find(long productId, long supplierId) {

        String sql = "SELECT product_id, supplier_id, supplier_product_code, " +
                "active, created_at, updated_at " +
                "FROM product_suppliers " +
                "WHERE product_id = ? AND supplier_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, productId);
            statement.setLong(2, supplierId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToProductSupplier(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar la relación producto-proveedor",
                    e
            );
        }
    }

    public List<ProductSupplier> findByProductId(long productId) {

        String sql = "SELECT product_id, supplier_id, supplier_product_code, " +
                "active, created_at, updated_at " +
                "FROM product_suppliers " +
                "WHERE product_id = ? " +
                "ORDER BY supplier_id";

        List<ProductSupplier> suppliers = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, productId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    suppliers.add(
                            mapResultSetToProductSupplier(resultSet)
                    );
                }
            }

            return suppliers;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los proveedores del producto",
                    e
            );
        }
    }

    public List<ProductSupplier> findBySupplierId(long supplierId) {

        String sql = "SELECT product_id, supplier_id, supplier_product_code, " +
                "active, created_at, updated_at " +
                "FROM product_suppliers " +
                "WHERE supplier_id = ? " +
                "ORDER BY product_id";

        List<ProductSupplier> products = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, supplierId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    products.add(
                            mapResultSetToProductSupplier(resultSet)
                    );
                }
            }

            return products;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los productos del proveedor",
                    e
            );
        }
    }

    public boolean insert(ProductSupplier productSupplier) {

        String sql = "INSERT INTO product_suppliers " +
                "(product_id, supplier_id, supplier_product_code, active) " +
                "VALUES (?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, productSupplier.getProductId());
            statement.setLong(2, productSupplier.getSupplierId());
            statement.setString(3, productSupplier.getSupplierProductCode());
            statement.setBoolean(4, productSupplier.isActive());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al relacionar el producto con el proveedor",
                    e
            );
        }
    }

    public boolean update(ProductSupplier productSupplier) {

        String sql = "UPDATE product_suppliers " +
                "SET supplier_product_code = ?, active = ? " +
                "WHERE product_id = ? AND supplier_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    productSupplier.getSupplierProductCode()
            );

            statement.setBoolean(
                    2,
                    productSupplier.isActive()
            );

            statement.setLong(
                    3,
                    productSupplier.getProductId()
            );

            statement.setLong(
                    4,
                    productSupplier.getSupplierId()
            );

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al actualizar la relación producto-proveedor",
                    e
            );
        }
    }

    public boolean deactivate(long productId, long supplierId) {

        String sql = "UPDATE product_suppliers " +
                "SET active = FALSE " +
                "WHERE product_id = ? AND supplier_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, productId);
            statement.setLong(2, supplierId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al desactivar la relación producto-proveedor",
                    e
            );
        }
    }

    private ProductSupplier mapResultSetToProductSupplier(
            ResultSet resultSet
    ) throws SQLException {

        return new ProductSupplier(
                resultSet.getLong("product_id"),
                resultSet.getLong("supplier_id"),
                resultSet.getString("supplier_product_code"),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}