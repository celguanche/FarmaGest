package com.farmagest.model;

import com.farmagest.model.enums.InventoryMovementType;

import java.time.LocalDateTime;

public class InventoryMovement {

    private long id;

    private long batchId;
    private long userId;

    private InventoryMovementType movementType;

    private int quantityChange;

    private int stockBefore;
    private int stockAfter;

    private Long saleId;
    private Long purchaseId;

    private String reason;

    private LocalDateTime createdAt;

    public InventoryMovement(long id, long batchId, long userId,
                             InventoryMovementType movementType,
                             int quantityChange, int stockBefore,
                             int stockAfter, Long saleId,
                             Long purchaseId, String reason,
                             LocalDateTime createdAt) {

        this.id = id;
        this.batchId = batchId;
        this.userId = userId;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.saleId = saleId;
        this.purchaseId = purchaseId;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "InventoryMovement{" +
                "id=" + id +
                ", batchId=" + batchId +
                ", userId=" + userId +
                ", movementType=" + movementType +
                ", quantityChange=" + quantityChange +
                ", stockBefore=" + stockBefore +
                ", stockAfter=" + stockAfter +
                ", createdAt=" + createdAt +
                '}';
    }

    public long getId() {
        return id;
    }

    public long getBatchId() {
        return batchId;
    }

    public long getUserId() {
        return userId;
    }

    public InventoryMovementType getMovementType() {
        return movementType;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public int getStockBefore() {
        return stockBefore;
    }

    public int getStockAfter() {
        return stockAfter;
    }

    public Long getSaleId() {
        return saleId;
    }

    public Long getPurchaseId() {
        return purchaseId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setBatchId(long batchId) {
        this.batchId = batchId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setMovementType(InventoryMovementType movementType) {
        this.movementType = movementType;
    }

    public void setQuantityChange(int quantityChange) {
        this.quantityChange = quantityChange;
    }

    public void setStockBefore(int stockBefore) {
        this.stockBefore = stockBefore;
    }

    public void setStockAfter(int stockAfter) {
        this.stockAfter = stockAfter;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}