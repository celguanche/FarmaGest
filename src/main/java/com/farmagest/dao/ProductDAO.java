package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public Product findById(long id) {

        String sql = "SELECT id, name, description, barcode, category_id, manufacturer_id, " +
                "requires_prescription, sale_price, vat_rate, minimum_stock, active, " +
                "created_at, updated_at " +
                "FROM products WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToProduct(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar el producto por id", e);
        }
    }

    public Product findByBarcode(String barcode) {

        String sql = "SELECT id, name, description, barcode, category_id, manufacturer_id, " +
                "requires_prescription, sale_price, vat_rate, minimum_stock, active, " +
                "created_at, updated_at " +
                "FROM products WHERE barcode = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, barcode);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToProduct(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar el producto por código de barras", e);
        }
    }

    public List<Product> findAll() {

        String sql = "SELECT id, name, description, barcode, category_id, manufacturer_id, " +
                "requires_prescription, sale_price, vat_rate, minimum_stock, active, " +
                "created_at, updated_at " +
                "FROM products ORDER BY name";

        List<Product> products = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {
                products.add(mapResultSetToProduct(resultSet));
            }

            return products;

        } catch (SQLException e) {
            throw new DatabaseException("Error al obtener los productos", e);
        }
    }

    public boolean insert(Product product) {

        String sql = "INSERT INTO products " +
                "(name, description, barcode, category_id, manufacturer_id, " +
                "requires_prescription, sale_price, vat_rate, minimum_stock, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {

            setProductParameters(statement, product);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getLong(1));
                }
            }

            return true;

        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar el producto", e);
        }
    }

    public boolean update(Product product) {

        String sql = "UPDATE products SET " +
                "name = ?, description = ?, barcode = ?, category_id = ?, manufacturer_id = ?, " +
                "requires_prescription = ?, sale_price = ?, vat_rate = ?, minimum_stock = ?, active = ? " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            setProductParameters(statement, product);
            statement.setLong(11, product.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar el producto", e);
        }
    }

    public boolean deactivate(long id) {

        String sql = "UPDATE products SET active = FALSE WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error al desactivar el producto", e);
        }
    }

    private void setProductParameters(
            PreparedStatement statement,
            Product product
    ) throws SQLException {

        statement.setString(1, product.getName());
        statement.setString(2, product.getDescription());
        statement.setString(3, product.getBarcode());
        statement.setLong(4, product.getCategoryId());

        if (product.getManufacturerId() == null) {
            statement.setNull(5, Types.BIGINT);
        } else {
            statement.setLong(5, product.getManufacturerId());
        }

        statement.setBoolean(6, product.isRequiresPrescription());
        statement.setBigDecimal(7, product.getSalePrice());
        statement.setBigDecimal(8, product.getVatRate());
        statement.setInt(9, product.getMinimumStock());
        statement.setBoolean(10, product.isActive());
    }

    private Product mapResultSetToProduct(ResultSet resultSet) throws SQLException {

        Long manufacturerId = resultSet.getObject("manufacturer_id", Long.class);

        return new Product(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getString("barcode"),
                resultSet.getLong("category_id"),
                manufacturerId,
                resultSet.getBoolean("requires_prescription"),
                resultSet.getBigDecimal("sale_price"),
                resultSet.getBigDecimal("vat_rate"),
                resultSet.getInt("minimum_stock"),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    public Product findByIdForUpdate(Connection connection, long id) throws SQLException {

        String sql = "SELECT id, name, description, barcode, category_id, manufacturer_id, " +
                "requires_prescription, sale_price, vat_rate, minimum_stock, active, " +
                "created_at, updated_at " +
                "FROM products WHERE id = ?" + " FOR UPDATE";

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToProduct(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar el producto por id", e);
        }
    }

}
