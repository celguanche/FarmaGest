package com.farmagest.service;

import com.farmagest.dao.*;
import com.farmagest.model.*;
import com.farmagest.model.enums.InventoryMovementType;

import java.time.LocalDate;
import java.util.List;

public class InventoryService {
    private final InventoryDAO inventory = new InventoryDAO();
    private final ProductBatchDAO batches = new ProductBatchDAO();
    private final InventoryMovementDAO movements = new InventoryMovementDAO();

    public ProductStock findStockByProductId(long id) {
        Checks.id(id);
        return Checks.exists(inventory.findStockByProductId(id));
    }

    public List<ProductStock> findAllStock() {
        return inventory.findAllStock();
    }

    public List<LowStockProduct> findLowStockProducts() {
        return inventory.findLowStockProducts();
    }

    public List<ProductBatch> findBatches(long productId) {
        Checks.id(productId);
        return batches.findByProductId(productId);
    }

    public List<InventoryMovement> findMovements(long batchId) {
        Checks.id(batchId);
        return movements.findByBatchId(batchId);
    }

    public void adjustStock(long batchId, long userId, int quantityChange, String reason) {
        Checks.id(batchId);
        Checks.require(quantityChange != 0, "El ajuste no puede ser cero");
        String checkedReason = Checks.text(reason, 255);
        Transactions.run(c -> {
            StockOperations.actor(c, userId);
            ProductBatch b = Checks.exists(batches.findByIdForUpdate(c, batchId));
            Checks.require(b.isActive(), "Lote inactivo");
            Checks.require(quantityChange < 0 || b.getExpiryDate() == null || !b.getExpiryDate().isBefore(LocalDate.now()), "No se puede añadir stock caducado");
            StockOperations.move(c, b, userId, quantityChange, quantityChange > 0 ? InventoryMovementType.ADJUSTMENT_IN : InventoryMovementType.ADJUSTMENT_OUT, null, null, checkedReason);
            return null;
        });
    }

    public void expireBatch(long batchId, long userId) {
        Checks.id(batchId);
        Transactions.run(c -> {
            StockOperations.actor(c, userId);
            ProductBatch b = Checks.exists(batches.findByIdForUpdate(c, batchId));
            Checks.require(b.getExpiryDate() != null && b.getExpiryDate().isBefore(LocalDate.now()), "El lote no está caducado");
            Checks.require(b.getAvailableQuantity() > 0, "Lote sin stock");
            StockOperations.move(c, b, userId, -b.getAvailableQuantity(), InventoryMovementType.EXPIRED, null, null, "Retirada por caducidad");
            return null;
        });
    }
}
