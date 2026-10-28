package com.farmagest.service;

import com.farmagest.dao.*;
import com.farmagest.model.*;
import com.farmagest.exception.*;

import java.util.List;

public class ManufacturerService {
    private final ManufacturerDAO dao = new ManufacturerDAO();

    public Manufacturer findById(long id) {
        Checks.id(id);
        return Checks.exists(dao.findById(id));
    }

    public List<Manufacturer> findAll() {
        return dao.findAll();
    }

    public Manufacturer create(Manufacturer value) {
        validate(value);
        Checks.require(value.getId() == 0, "Registro nuevo requerido");
        try {
            Checks.saved(dao.insert(value));
        } catch (DatabaseException e) {
            throw translate(e);
        }
        return value;
    }

    public Manufacturer update(Manufacturer value) {
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

    private void validate(Manufacturer value) {
        Checks.require(value != null, "Registro obligatorio");
        Checks.text(value.getName(), 150);
        Checks.optional(value.getPhone(), 30);
        Checks.email(value.getEmail(), false);
        Checks.optional(value.getWebsite(), 255);
        Checks.optional(value.getAddress(), 255);
    }

    private RuntimeException translate(DatabaseException error) {
        return ServiceErrors.translate(error);
    }
}
