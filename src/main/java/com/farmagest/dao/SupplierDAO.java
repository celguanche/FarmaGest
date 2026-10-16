package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.Supplier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {

    public Supplier findById(long id) {

        String sql = "SELECT id, business_name, tax_id, contact_name, phone, email, address, " +
                "active, created_at, updated_at " +
                "FROM suppliers WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToSupplier(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar el proveedor por id", e);
        }
    }

    public List<Supplier> findAll() {

        String sql = "SELECT id, business_name, tax_id, contact_name, phone, email, address, " +
                "active, created_at, updated_at " +
                "FROM suppliers ORDER BY business_name";

        List<Supplier> suppliers = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {
                suppliers.add(mapResultSetToSupplier(resultSet));
            }

            return suppliers;

        } catch (SQLException e) {
            throw new DatabaseException("Error al obtener los proveedores", e);
        }
    }

    public boolean insert(Supplier supplier) {

        String sql = "INSERT INTO suppliers " +
                "(business_name, tax_id, contact_name, phone, email, address, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {

            statement.setString(1, supplier.getBusinessName());
            statement.setString(2, supplier.getTaxId());
            statement.setString(3, supplier.getContactName());
            statement.setString(4, supplier.getPhone());
            statement.setString(5, supplier.getEmail());
            statement.setString(6, supplier.getAddress());
            statement.setBoolean(7, supplier.isActive());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    supplier.setId(generatedKeys.getLong(1));
                }
            }

            return true;

        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar el proveedor", e);
        }
    }

    public boolean update(Supplier supplier) {

        String sql = "UPDATE suppliers " +
                "SET business_name = ?, tax_id = ?, contact_name = ?, phone = ?, " +
                "email = ?, address = ?, active = ? " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, supplier.getBusinessName());
            statement.setString(2, supplier.getTaxId());
            statement.setString(3, supplier.getContactName());
            statement.setString(4, supplier.getPhone());
            statement.setString(5, supplier.getEmail());
            statement.setString(6, supplier.getAddress());
            statement.setBoolean(7, supplier.isActive());
            statement.setLong(8, supplier.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar el proveedor", e);
        }
    }

    public boolean deactivate(long id) {

        String sql = "UPDATE suppliers SET active = FALSE WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error al desactivar el proveedor", e);
        }
    }

    private Supplier mapResultSetToSupplier(ResultSet resultSet) throws SQLException {

        return new Supplier(
                resultSet.getLong("id"),
                resultSet.getString("business_name"),
                resultSet.getString("tax_id"),
                resultSet.getString("contact_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getString("address"),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}