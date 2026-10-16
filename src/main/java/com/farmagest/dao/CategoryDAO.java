package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;
import com.farmagest.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    public Category findById(long id) {

        String sql = "SELECT id, name, description, active, created_at, updated_at " +
                "FROM categories " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToCategory(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al buscar la categoría por id",
                    e
            );
        }
    }

    public List<Category> findAll() {

        String sql = "SELECT id, name, description, active, created_at, updated_at " +
                "FROM categories " +
                "ORDER BY name";

        List<Category> categories = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {
                categories.add(mapResultSetToCategory(resultSet));
            }

            return categories;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al obtener las categorías",
                    e
            );
        }
    }

    public boolean insert(Category category) {

        String sql = "INSERT INTO categories " +
                "(name, description, active) " +
                "VALUES (?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.setBoolean(3, category.isActive());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    category.setId(generatedKeys.getLong(1));
                }
            }

            return true;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al insertar la categoría",
                    e
            );
        }
    }

    public boolean update(Category category) {

        String sql = "UPDATE categories " +
                "SET name = ?, " +
                "description = ?, " +
                "active = ? " +
                "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.setBoolean(3, category.isActive());
            statement.setLong(4, category.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error al actualizar la categoría",
                    e
            );
        }
    }

    public boolean deactivate(long id) {

        String sql = "UPDATE categories " +
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
                    "Error al desactivar la categoría",
                    e
            );
        }
    }

    private Category mapResultSetToCategory(ResultSet resultSet) throws SQLException {

        return new Category(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}