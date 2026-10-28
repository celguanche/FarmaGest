package com.farmagest.service;

import com.farmagest.dao.*;
import com.farmagest.model.*;
import com.farmagest.exception.*;

import java.util.List;

public class CategoryService {
    private final CategoryDAO dao = new CategoryDAO();

    public Category findById(long id) {
        Checks.id(id);
        return Checks.exists(dao.findById(id));
    }

    public List<Category> findAll() {
        return dao.findAll();
    }

    public Category create(Category value) {
        validate(value);
        Checks.require(value.getId() == 0, "Registro nuevo requerido");
        try {
            Checks.saved(dao.insert(value));
        } catch (DatabaseException e) {
            throw translate(e);
        }
        return value;
    }

    public Category update(Category value) {
        validate(value);
        findById(value.getId());
        try {
            Checks.saved(dao.update(value));
        } catch (DatabaseException e) {
            throw translate(e);
        }
        return value;
    }

    public void deactivate(long id) {
        findById(id);
        Checks.saved(dao.deactivate(id));
    }

    private void validate(Category value) {
        Checks.require(value != null, "Registro obligatorio");
        Checks.text(value.getName(), 100);
        Checks.optional(value.getDescription(), 255);
    }

    private RuntimeException translate(DatabaseException error) {
        return ServiceErrors.translate(error);
    }
}
