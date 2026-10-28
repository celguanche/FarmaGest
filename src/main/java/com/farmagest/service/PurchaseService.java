package com.farmagest.service;

import com.farmagest.dao.*;
import com.farmagest.model.*;
import com.farmagest.model.enums.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class PurchaseService {
    private final PurchaseDAO purchases = new PurchaseDAO();
    private final PurchaseItemDAO lines = new PurchaseItemDAO();
    private final ProductBatchDAO batches = new ProductBatchDAO();

    public Purchase findById(long id) {
        Checks.id(id);
        return Checks.exists(purchases.findById(id));
    }

    public List<Purchase> findAll() {
        return purchases.findAll();
    }

    public List<PurchaseItem> findItems(long id) {
        findById(id);
        return lines.findByPurchaseId(id);
    }

    public Purchase createPurchase(Purchase purchase, List<PurchaseItem> items) {
        Checks.require(purchase != null && purchase.getId() == 0, "Compra nueva requerida");
        Checks.items(items);
        List<PurchaseItem> ordered = new ArrayList<>(items);
        for (PurchaseItem i : ordered) {
            Checks.id(i.getProductId());
            Checks.require(i.getId() == 0, "Línea nueva requerida");
        }
        ordered.sort(Comparator.comparingLong(PurchaseItem::getProductId));
        return Transactions.run(c -> {
            StockOperations.actor(c, purchase.getCreatedByUserId());
            Checks.id(purchase.getSupplierId());
            Checks.require(Checks.exists(new SupplierDAO().findByIdForUpdate(c, purchase.getSupplierId())).isActive(), "Proveedor inactivo");
            BigDecimal subtotal = BigDecimal.ZERO, tax = BigDecimal.ZERO;
            for (PurchaseItem i : ordered) {
                StockOperations.product(c, i.getProductId());
                i.setUnitCost(Checks.money(i.getUnitCost(), 10));
                i.setVatRate(Checks.vat(i.getVatRate()));
                i.setSubtotal(Checks.subtotal(i.getUnitCost(), i.getQuantity()));
                i.setTaxAmount(Checks.tax(i.getSubtotal(), i.getVatRate()));
                i.setLineTotal(Checks.money(i.getSubtotal().add(i.getTaxAmount()), 12));
                subtotal = subtotal.add(i.getSubtotal());
                tax = tax.add(i.getTaxAmount());
            }
            purchase.setSubtotal(Checks.money(subtotal, 12));
            purchase.setTaxAmount(Checks.money(tax, 12));
            purchase.setTotalAmount(Checks.money(subtotal.add(tax), 12));
            purchase.setStatus(PurchaseStatus.ORDERED);
            purchase.setOrderedAt(LocalDateTime.now());
            purchase.setReceivedAt(null);
            purchase.setCancelledAt(null);
            purchase.setCancelledByUserId(null);
            purchase.setCancellationReason(null);
            Checks.saved(purchases.insert(c, purchase));
            for (PurchaseItem i : ordered) {
                i.setPurchaseId(purchase.getId());
                Checks.saved(lines.insert(c, i));
            }
            return purchase;
        });
    }

    /**
     * Complete reception; each batch must identify a persisted purchase item.
     * Multiple batches per item are allowed, with their quantities summing to the ordered quantity.
     */
    public void receivePurchase(long id, long userId, List<ProductBatch> receivedBatches) {
        Checks.id(id);
        Checks.items(receivedBatches);
        List<ProductBatch> received = new ArrayList<>(receivedBatches);
        Transactions.run(c -> {
            StockOperations.actor(c, userId);
            Purchase purchase = Checks.exists(purchases.findByIdForUpdate(c, id));
            Checks.require(purchase.getStatus() == PurchaseStatus.ORDERED, "La compra debe estar pendiente");
            List<PurchaseItem> items = lines.findByPurchaseId(c, id);
            Checks.items(items);
            Map<Long, PurchaseItem> byId = new HashMap<>();
            Map<Long, Long> totals = new HashMap<>();
            for (PurchaseItem i : items) byId.put(i.getId(), i);
            for (ProductBatch b : received) {
                Checks.require(b.getId() == 0 && b.getPurchaseItemId() != null, "Lote nuevo con línea de compra requerido");
                PurchaseItem i = Checks.exists(byId.get(b.getPurchaseItemId()));
                Checks.require(b.getProductId() == i.getProductId(), "El lote no corresponde al producto de la compra");
                Checks.text(b.getBatchNumber(), 100);
                Checks.require(b.getInitialQuantity() > 0, "Cantidad de lote inválida");
                Checks.require(b.getExpiryDate() == null || !b.getExpiryDate().isBefore(LocalDate.now()), "Lote caducado");
                totals.merge(i.getId(), (long) b.getInitialQuantity(), Long::sum);
            }
            for (PurchaseItem i : items)
                Checks.require(totals.getOrDefault(i.getId(), 0L) == i.getQuantity(), "La recepción debe cubrir exactamente cada línea");
            received.sort(Comparator.comparingLong(ProductBatch::getProductId).thenComparing(ProductBatch::getBatchNumber));
            for (ProductBatch b : received) {
                StockOperations.product(c, b.getProductId());
                b.setUnitCost(byId.get(b.getPurchaseItemId()).getUnitCost());
                b.setActive(true);
                b.setReceivedAt(LocalDateTime.now());
                b.setAvailableQuantity(0);
                Checks.saved(batches.insert(c, b));
                StockOperations.move(c, b, userId, b.getInitialQuantity(), InventoryMovementType.PURCHASE_RECEIPT, null, id, "Recepción #" + id);
            }
            Checks.saved(purchases.markAsReceived(c, id));
            return null;
        });
    }

    /**
     * The current schema permits cancellation of pending orders only.
     */
    public void cancelPurchase(long id, long userId, String reason) {
        Checks.id(id);
        String checkedReason = Checks.text(reason, 255);
        Transactions.run(c -> {
            StockOperations.actor(c, userId);
            Purchase p = Checks.exists(purchases.findByIdForUpdate(c, id));
            Checks.require(p.getStatus() == PurchaseStatus.ORDERED, "Solo se pueden cancelar compras pendientes");
            Checks.saved(purchases.cancel(c, id, userId, checkedReason));
            return null;
        });
    }
}
