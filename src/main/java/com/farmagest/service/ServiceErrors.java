package com.farmagest.service;

import com.farmagest.exception.*;

import java.sql.SQLException;

final class ServiceErrors {
    private ServiceErrors() {
    }

    static RuntimeException translate(DatabaseException error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("23"))
                return new BusinessException("Registro duplicado o relación no válida", error);
        }
        return error;
    }
}
