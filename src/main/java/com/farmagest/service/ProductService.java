package com.farmagest.service;

import com.farmagest.dao.*;
import com.farmagest.model.*;
import com.farmagest.exception.*;

import java.util.List;

public class ProductService {
    private final ProductDAO dao = new ProductDAO();

    public Product findById(long id) {
        Checks.id(id);
        return Checks.exists(dao.findById(id));
    }

    public List<Product> findAll() {
        return dao.findAll();
    }

    public Product create(Product value) {
        validate(value);
        Checks.require(value.getId() == 0, "Registro nuevo requerido");
        try {
            Checks.saved(dao.insert(value));
        } catch (DatabaseException e) {
            throw translate(e);
        }
        return value;
    }

    public Product update(Product value) {
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

    private void validate(Product value) {
        Checks.require(value != null, "Registro obligatorio");
        Checks.text(value.getName(), 150);
        Checks.text(value.getBarcode(), 50);
        Checks.id(value.getCategoryId());
        Checks.require(Checks.exists(new CategoryDAO().findById(value.getCategoryId())).isActive(), "Categoría inactiva");
        if (value.getManufacturerId() != null) {
            Checks.id(value.getManufacturerId());
            Checks.require(Checks.exists(new ManufacturerDAO().findById(value.getManufacturerId())).isActive(), "Fabricante inactivo");
        }
        Checks.money(value.getSalePrice(), 10);
        Checks.vat(value.getVatRate());
        Checks.require(value.getMinimumStock() >= 0, "Stock mínimo inválido");
        Product existing = dao.findByBarcode(value.getBarcode());
        Checks.require(existing == null || existing.getId() == value.getId(), "Código de barras duplicado");
    }

    private RuntimeException translate(DatabaseException error) {
        return ServiceErrors.translate(error);
    }
}
