package com.farmagest.service;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;

import java.sql.*;

final class Transactions {
    private Transactions() {
    }

    @FunctionalInterface
    interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    static <T> T run(Work<T> work) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return execute(connection, work);
        } catch (SQLException e) {
            throw ServiceErrors.translate(new DatabaseException("Error en la transacción", e));
        } catch (DatabaseException e) {
            throw ServiceErrors.translate(e);
        }
    }

    static <T> T execute(Connection connection, Work<T> work) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        boolean ended = false;
        Throwable failure = null;
        try {
            T result = work.run(connection);
            connection.commit();
            ended = true;
            return result;
        } catch (SQLException | RuntimeException | Error e) {
            failure = e;
            try {
                connection.rollback();
                ended = true;
            } catch (SQLException rollback) {
                e.addSuppressed(rollback);
            }
            throw e;
        } finally {
            // Never enable auto-commit if rollback failed: that could commit partial work.
            if (ended) {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException restore) {
                    if (failure != null) failure.addSuppressed(restore);
                    else throw restore;
                }
            }
        }
    }
}
