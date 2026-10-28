package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.User;
import com.farmagest.model.enums.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findByUsername(String username) {

        String sql = "SELECT id, username, password_hash, first_name, last_name, " +
                "email, role, active, created_at, updated_at, last_login_at " +
                "FROM users " +
                "WHERE username = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar el usuario por username",
                    e
            );
        }
    }

    public User findById(long id) {

        String sql = "SELECT id, username, password_hash, first_name, last_name, " +
                "email, role, active, created_at, updated_at, last_login_at " +
                "FROM users " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar el usuario por id",
                    e
            );
        }
    }

    public List<User> findAll() {

        String sql = "SELECT id, username, password_hash, first_name, last_name, " +
                "email, role, active, created_at, updated_at, last_login_at " +
                "FROM users " +
                "ORDER BY id";

        List<User> users = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {
                users.add(mapResultSetToUser(resultSet));
            }

            return users;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener los usuarios",
                    e
            );
        }
    }

    public boolean insert(User user) {

        String sql = "INSERT INTO users " +
                "(username, password_hash, first_name, last_name, email, role, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getFirstName());
            statement.setString(4, user.getLastName());
            statement.setString(5, user.getEmail());
            statement.setString(6, user.getRole().name());
            statement.setBoolean(7, user.isActive());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
            }

            return true;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al insertar el usuario",
                    e
            );
        }
    }

    public boolean update(User user) {

        String sql = "UPDATE users " +
                "SET username = ?, " +
                "password_hash = ?, " +
                "first_name = ?, " +
                "last_name = ?, " +
                "email = ?, " +
                "role = ?, " +
                "active = ? " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getFirstName());
            statement.setString(4, user.getLastName());
            statement.setString(5, user.getEmail());
            statement.setString(6, user.getRole().name());
            statement.setBoolean(7, user.isActive());
            statement.setLong(8, user.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al actualizar el usuario",
                    e
            );
        }
    }

    public boolean deactivate(long id) {

        String sql = "UPDATE users " +
                "SET active = FALSE " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al desactivar el usuario",
                    e
            );
        }
    }

    public boolean updateLastLogin(long id) {

        String sql = "UPDATE users " +
                "SET last_login_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al actualizar el último inicio de sesión",
                    e
            );
        }
    }

    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {

        return new User(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("email"),
                Role.valueOf(resultSet.getString("role")),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime(),
                resultSet.getTimestamp("last_login_at") != null
                        ? resultSet.getTimestamp("last_login_at").toLocalDateTime()
                        : null
        );
    }

    public User findByIdForUpdate(Connection connection, long id) throws SQLException {

        String sql = "SELECT id, username, password_hash, first_name, last_name, " +
                "email, role, active, created_at, updated_at, last_login_at " +
                "FROM users " +
                "WHERE id = ?" + " FOR UPDATE";

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar el usuario por id",
                    e
            );
        }
    }


    public User findByUsername(Connection connection, String username) throws SQLException {

        String sql = "SELECT id, username, password_hash, first_name, last_name, " +
                "email, role, active, created_at, updated_at, last_login_at " +
                "FROM users " +
                "WHERE username = ?";

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar el usuario por username",
                    e
            );
        }
    }


    public boolean updateLastLogin(Connection connection, long id) throws SQLException {

        String sql = "UPDATE users " +
                "SET last_login_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al actualizar el último inicio de sesión",
                    e
            );
        }
    }


    public boolean insert(Connection connection, User user) throws SQLException {

        String sql = "INSERT INTO users " +
                "(username, password_hash, first_name, last_name, email, role, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getFirstName());
            statement.setString(4, user.getLastName());
            statement.setString(5, user.getEmail());
            statement.setString(6, user.getRole().name());
            statement.setBoolean(7, user.isActive());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
            }

            return true;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al insertar el usuario",
                    e
            );
        }
    }


    public boolean update(Connection connection, User user) throws SQLException {

        String sql = "UPDATE users " +
                "SET username = ?, " +
                "password_hash = ?, " +
                "first_name = ?, " +
                "last_name = ?, " +
                "email = ?, " +
                "role = ?, " +
                "active = ? " +
                "WHERE id = ?";

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getFirstName());
            statement.setString(4, user.getLastName());
            statement.setString(5, user.getEmail());
            statement.setString(6, user.getRole().name());
            statement.setBoolean(7, user.isActive());
            statement.setLong(8, user.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al actualizar el usuario",
                    e
            );
        }
    }

}
