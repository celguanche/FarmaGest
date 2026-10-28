package com.farmagest.service;

import com.farmagest.dao.*;
import com.farmagest.model.*;
import com.farmagest.exception.*;

import java.util.List;

public class CustomerService {
    private final CustomerDAO dao = new CustomerDAO();

    public Customer findById(long id) {
        Checks.id(id);
        return Checks.exists(dao.findById(id));
    }

    public List<Customer> findAll() {
        return dao.findAll();
    }

    public Customer create(Customer value) {
        validate(value);
        Checks.require(value.getId() == 0, "Registro nuevo requerido");
        try {
            Checks.saved(dao.insert(value));
        } catch (DatabaseException e) {
            throw translate(e);
        }
        return value;
    }

    public Customer update(Customer value) {
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

    private void validate(Customer value) {
        Checks.require(value != null, "Registro obligatorio");
        Checks.text(value.getFirstName(), 100);
        Checks.text(value.getLastName(), 150);
        Checks.optional(value.getPhone(), 30);
        Checks.email(value.getEmail(), false);
    }

    private RuntimeException translate(DatabaseException error) {
        return ServiceErrors.translate(error);
    }
}
