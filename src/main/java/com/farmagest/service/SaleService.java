package com.farmagest.service;

import com.farmagest.dao.*;
import com.farmagest.model.*;
import com.farmagest.model.enums.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class SaleService {
    private final SaleDAO sales = new SaleDAO();
    private final SaleItemDAO lines = new SaleItemDAO();
    private final SaleItemBatchDAO allocations = new SaleItemBatchDAO();
    private final ProductBatchDAO batches = new ProductBatchDAO();

    public Sale findById(long id) {
        Checks.id(id);
        return Checks.exists(sales.findById(id));
    }

    public List<Sale> findAll() {
        return sales.findAll();
    }

    public List<SaleItem> findItems(long id) {
        findById(id);
        return lines.findBySaleId(id);
    }

    public Sale createSale(Sale sale, List<SaleItem> items) {
        Checks.require(sale != null && sale.getId() == 0, "Venta nueva requerida");
        Checks.items(items);
        Checks.require(sale.getPaymentMethod() != null, "Método de pago obligatorio");
        List<SaleItem> ordered = new ArrayList<>(items);
        for (SaleItem i : ordered) {
            Checks.id(i.getProductId());
            Checks.require(i.getQuantity() > 0 && i.getId() == 0, "Línea inválida");
        }
        ordered.sort(Comparator.comparingLong(SaleItem::getProductId));
        return Transactions.run(c -> {
            StockOperations.actor(c, sale.getEmployeeUserId());
            if (sale.getCustomerId() != null) {
                Checks.id(sale.getCustomerId());
                Checks.require(Checks.exists(new CustomerDAO().findByIdForUpdate(c, sale.getCustomerId())).isActive(), "Cliente inactivo");
            }
            BigDecimal subtotal = BigDecimal.ZERO, tax = BigDecimal.ZERO;
            for (SaleItem i : ordered) {
                Product p = StockOperations.product(c, i.getProductId());
                i.setUnitPrice(Checks.money(p.getSalePrice(), 10));
                i.setVatRate(Checks.vat(p.getVatRate()));
                i.setSubtotal(Checks.subtotal(i.getUnitPrice(), i.getQuantity()));
                i.setTaxAmount(Checks.tax(i.getSubtotal(), i.getVatRate()));
                i.setLineTotal(Checks.money(i.getSubtotal().add(i.getTaxAmount()), 12));
                subtotal = subtotal.add(i.getSubtotal());
                tax = tax.add(i.getTaxAmount());
            }
            sale.setSubtotal(Checks.money(subtotal, 12));
            sale.setTaxAmount(Checks.money(tax, 12));
            sale.setTotalAmount(Checks.money(subtotal.add(tax), 12));
            sale.setStatus(SaleStatus.COMPLETED);
            sale.setSoldAt(LocalDateTime.now());
            sale.setCancelledAt(null);
            sale.setCancelledByUserId(null);
            sale.setCancellationReason(null);
            Checks.saved(sales.insert(c, sale));
            for (SaleItem i : ordered) {
                i.setSaleId(sale.getId());
                Checks.saved(lines.insert(c, i));
                int remaining = i.getQuantity();
                for (ProductBatch b : batches.findAvailableByProductIdForUpdate(c, i.getProductId())) {
                    if (remaining == 0) break;
                    int take = Math.min(remaining, b.getAvailableQuantity());
                    StockOperations.move(c, b, sale.getEmployeeUserId(), -take, InventoryMovementType.SALE, sale.getId(), null, "Venta #" + sale.getId());
                    Checks.saved(allocations.insert(c, new SaleItemBatch(i.getId(), b.getId(), take, null)));
                    remaining -= take;
                }
                Checks.require(remaining == 0, "Stock insuficiente para producto " + i.getProductId());
            }
            return sale;
        });
    }

    public void cancelSale(long id, long userId, String reason) {
        Checks.id(id);
        String checkedReason = Checks.text(reason, 255);
        Transactions.run(c -> {
            StockOperations.actor(c, userId);
            Sale sale = Checks.exists(sales.findByIdForUpdate(c, id));
            Checks.require(sale.getStatus() == SaleStatus.COMPLETED, "Solo se pueden cancelar ventas completadas");
            // Aggregate allocations: a product may appear on more than one sale line.
            Map<Long, Integer> quantities = new TreeMap<>();
            for (SaleItem i : lines.findBySaleId(c, id)) {
                long allocated = 0;
                for (SaleItemBatch a : allocations.findBySaleItemId(c, i.getId())) {
                    allocated += a.getQuantity();
                    quantities.merge(a.getBatchId(), a.getQuantity(), Math::addExact);
                }
                Checks.require(allocated == i.getQuantity(), "Asignación de lotes inconsistente");
            }
            Checks.require(!quantities.isEmpty(), "Venta sin lotes");
            for (var e : quantities.entrySet()) {
                ProductBatch b = Checks.exists(batches.findByIdForUpdate(c, e.getKey()));
                StockOperations.move(c, b, userId, e.getValue(), InventoryMovementType.SALE_CANCELLATION, id, null, checkedReason);
            }
            Checks.saved(sales.cancel(c, id, userId, checkedReason));
            return null;
        });
    }
}
