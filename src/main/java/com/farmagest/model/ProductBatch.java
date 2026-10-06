package com.farmagest.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProductBatch {

    private long id;
    private long productId;
    private Long purchaseItemId;

    private String batchNumber;
    private LocalDate expiryDate;

    private int initialQuantity;
    private int availableQuantity;

    private BigDecimal unitCost;

    private LocalDateTime receivedAt;

    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductBatch(long id, long productId, Long purchaseItemId,
                        String batchNumber, LocalDate expiryDate,
                        int initialQuantity, int availableQuantity,
                        BigDecimal unitCost, LocalDateTime receivedAt,
                        boolean active, LocalDateTime createdAt,
                        LocalDateTime updatedAt) {

        this.id = id;
        this.productId = productId;
        this.purchaseItemId = purchaseItemId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.initialQuantity = initialQuantity;
        this.availableQuantity = availableQuantity;
        this.unitCost = unitCost;
        this.receivedAt = receivedAt;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ProductBatch{" +
                "id=" + id +
                ", productId=" + productId +
                ", batchNumber='" + batchNumber + '\'' +
                ", expiryDate=" + expiryDate +
                ", availableQuantity=" + availableQuantity +
                ", active=" + active +
                '}';
    }

    public long getId() {
        return id;
    }

    public long getProductId() {
        return productId;
    }

    public Long getPurchaseItemId() {
        return purchaseItemId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public int getInitialQuantity() {
        return initialQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public void setPurchaseItemId(Long purchaseItemId) {
        this.purchaseItemId = purchaseItemId;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setInitialQuantity(int initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}