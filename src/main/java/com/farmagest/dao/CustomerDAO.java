package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public Customer findById(long id) {

        String sql = "SELECT id, first_name, last_name, phone, email, active, created_at, updated_at " +
                "FROM customers WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToCustomer(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar el cliente por id", e);
        }
    }

    public List<Customer> findAll() {

        String sql = "SELECT id, first_name, last_name, phone, email, active, created_at, updated_at " +
                "FROM customers ORDER BY last_name, first_name";

        List<Customer> customers = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {
                customers.add(mapResultSetToCustomer(resultSet));
            }

            return customers;

        } catch (SQLException e) {
            throw new DatabaseException("Error al obtener los clientes", e);
        }
    }

    public boolean insert(Customer customer) {

        String sql = "INSERT INTO customers " +
                "(first_name, last_name, phone, email, active) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {

            statement.setString(1, customer.getFirstName());
            statement.setString(2, customer.getLastName());
            statement.setString(3, customer.getPhone());
            statement.setString(4, customer.getEmail());
            statement.setBoolean(5, customer.isActive());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    customer.setId(generatedKeys.getLong(1));
                }
            }

            return true;

        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar el cliente", e);
        }
    }

    public boolean update(Customer customer) {

        String sql = "UPDATE customers SET " +
                "first_name = ?, last_name = ?, phone = ?, email = ?, active = ? " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, customer.getFirstName());
            statement.setString(2, customer.getLastName());
            statement.setString(3, customer.getPhone());
            statement.setString(4, customer.getEmail());
            statement.setBoolean(5, customer.isActive());
            statement.setLong(6, customer.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar el cliente", e);
        }
    }

    public boolean deactivate(long id) {

        String sql = "UPDATE customers SET active = FALSE WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error al desactivar el cliente", e);
        }
    }

    private Customer mapResultSetToCustomer(ResultSet resultSet) throws SQLException {

        return new Customer(
                resultSet.getLong("id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    public Customer findByIdForUpdate(Connection connection, long id) throws SQLException {

        String sql = "SELECT id, first_name, last_name, phone, email, active, created_at, updated_at " +
                "FROM customers WHERE id = ?" + " FOR UPDATE";

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToCustomer(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar el cliente por id", e);
        }
    }

}
