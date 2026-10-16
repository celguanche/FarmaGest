package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.Manufacturer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ManufacturerDAO {

    public Manufacturer findById(long id) {

        String sql = "SELECT id, name, phone, email, website, address, active, created_at, updated_at " +
                "FROM manufacturers WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToManufacturer(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar el fabricante por id", e);
        }
    }

    public List<Manufacturer> findAll() {

        String sql = "SELECT id, name, phone, email, website, address, active, created_at, updated_at " +
                "FROM manufacturers ORDER BY name";

        List<Manufacturer> manufacturers = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {
                manufacturers.add(mapResultSetToManufacturer(resultSet));
            }

            return manufacturers;

        } catch (SQLException e) {
            throw new DatabaseException("Error al obtener los fabricantes", e);
        }
    }

    public boolean insert(Manufacturer manufacturer) {

        String sql = "INSERT INTO manufacturers " +
                "(name, phone, email, website, address, active) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {

            statement.setString(1, manufacturer.getName());
            statement.setString(2, manufacturer.getPhone());
            statement.setString(3, manufacturer.getEmail());
            statement.setString(4, manufacturer.getWebsite());
            statement.setString(5, manufacturer.getAddress());
            statement.setBoolean(6, manufacturer.isActive());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    manufacturer.setId(generatedKeys.getLong(1));
                }
            }

            return true;

        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar el fabricante", e);
        }
    }

    public boolean update(Manufacturer manufacturer) {

        String sql = "UPDATE manufacturers " +
                "SET name = ?, phone = ?, email = ?, website = ?, address = ?, active = ? " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, manufacturer.getName());
            statement.setString(2, manufacturer.getPhone());
            statement.setString(3, manufacturer.getEmail());
            statement.setString(4, manufacturer.getWebsite());
            statement.setString(5, manufacturer.getAddress());
            statement.setBoolean(6, manufacturer.isActive());
            statement.setLong(7, manufacturer.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar el fabricante", e);
        }
    }

    public boolean deactivate(long id) {

        String sql = "UPDATE manufacturers SET active = FALSE WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error al desactivar el fabricante", e);
        }
    }

    private Manufacturer mapResultSetToManufacturer(ResultSet resultSet) throws SQLException {

        return new Manufacturer(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getString("website"),
                resultSet.getString("address"),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}