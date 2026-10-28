package com.farmagest.service;

import com.farmagest.dao.*;
import com.farmagest.model.*;
import com.farmagest.exception.*;

import java.util.List;

public class SupplierService {
    private final SupplierDAO dao = new SupplierDAO();

    public Supplier findById(long id) {
        Checks.id(id);
        return Checks.exists(dao.findById(id));
    }

    public List<Supplier> findAll() {
        return dao.findAll();
    }

    public Supplier create(Supplier value) {
        validate(value);
        Checks.require(value.getId() == 0, "Registro nuevo requerido");
        try {
            Checks.saved(dao.insert(value));
        } catch (DatabaseException e) {
            throw translate(e);
        }
        return value;
    }

    public Supplier update(Supplier value) {
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

    private void validate(Supplier value) {
        Checks.require(value != null, "Registro obligatorio");
        Checks.text(value.getBusinessName(), 150);
        Checks.optional(value.getTaxId(), 30);
        Checks.optional(value.getContactName(), 150);
        Checks.optional(value.getPhone(), 30);
        Checks.email(value.getEmail(), false);
        Checks.optional(value.getAddress(), 255);
    }

    private RuntimeException translate(DatabaseException error) {
        return ServiceErrors.translate(error);
    }
}
