package com.farmagest.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String  DB_URL = System.getenv("DB_URL");
    private static final String  DB_USER = System.getenv("DB_USER");
    private static final String  DB_PASS = System.getenv("DB_PASS");

    private static HikariDataSource dataSource;

    private DatabaseConnection() {
    }

    private static synchronized void initialize() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        validateEnvironmentVariables();

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASS);

        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setPoolName("FarmaGestPool");

        dataSource = new HikariDataSource(config);
    }

    private static void validateEnvironmentVariables() {
        if (DB_URL == null || DB_URL.isBlank()) {
            throw new IllegalStateException("DB_URL environment variable is not configured.");
        }

        if (DB_USER == null || DB_USER.isBlank()) {
            throw new IllegalStateException("DB_USER environment variable is not configured.");
        }

        if (DB_PASS == null || DB_PASS.isBlank()) {
            throw new IllegalStateException("DB_PASS environment variable is not configured.");
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initialize();
        }

        return dataSource.getConnection();
    }

    public static void close(){
        if(dataSource != null && !dataSource.isClosed()){
            dataSource.close();
            dataSource = null;
        }
    }

}
