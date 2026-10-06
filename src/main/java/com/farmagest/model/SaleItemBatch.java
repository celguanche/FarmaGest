package com.farmagest.model;

import java.time.LocalDateTime;

public class SaleItemBatch {

    private long saleItemId;
    private long batchId;
    private int quantity;
    private LocalDateTime createdAt;

    public SaleItemBatch(long saleItemId, long batchId,
                         int quantity, LocalDateTime createdAt) {

        this.saleItemId = saleItemId;
        this.batchId = batchId;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SaleItemBatch{" +
                "saleItemId=" + saleItemId +
                ", batchId=" + batchId +
                ", quantity=" + quantity +
                '}';
    }

    public long getSaleItemId() {
        return saleItemId;
    }

    public long getBatchId() {
        return batchId;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setSaleItemId(long saleItemId) {
        this.saleItemId = saleItemId;
    }

    public void setBatchId(long batchId) {
        this.batchId = batchId;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}