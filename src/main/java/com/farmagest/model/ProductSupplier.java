package com.farmagest.model;

import java.time.LocalDateTime;

public class ProductSupplier {

    private long productId;
    private long supplierId;
    private String supplierProductCode;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductSupplier(long productId, long supplierId,
                           String supplierProductCode, boolean active,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {

        this.productId = productId;
        this.supplierId = supplierId;
        this.supplierProductCode = supplierProductCode;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ProductSupplier{" +
                "productId=" + productId +
                ", supplierId=" + supplierId +
                ", supplierProductCode='" + supplierProductCode + '\'' +
                ", active=" + active +
                '}';
    }

    public long getProductId() {
        return productId;
    }

    public long getSupplierId() {
        return supplierId;
    }

    public String getSupplierProductCode() {
        return supplierProductCode;
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

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public void setSupplierId(long supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierProductCode(String supplierProductCode) {
        this.supplierProductCode = supplierProductCode;
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