package com.farmagest.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {

    private long id;
    private String name;
    private String description;
    private String barcode;
    private long categoryId;
    private Long manufacturerId;
    private boolean requiresPrescription;
    private BigDecimal salePrice;
    private BigDecimal vatRate;
    private int minimumStock;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product(
            long id,
            String name,
            String description,
            String barcode,
            long categoryId,
            Long manufacturerId,
            boolean requiresPrescription,
            BigDecimal salePrice,
            BigDecimal vatRate,
            int minimumStock,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.barcode = barcode;
        this.categoryId = categoryId;
        this.manufacturerId = manufacturerId;
        this.requiresPrescription = requiresPrescription;
        this.salePrice = salePrice;
        this.vatRate = vatRate;
        this.minimumStock = minimumStock;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", barcode='" + barcode + '\'' +
                ", categoryId=" + categoryId +
                ", manufacturerId=" + manufacturerId +
                ", requiresPrescription=" + requiresPrescription +
                ", salePrice=" + salePrice +
                ", vatRate=" + vatRate +
                ", minimumStock=" + minimumStock +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getBarcode() {
        return barcode;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public Long getManufacturerId() {
        return manufacturerId;
    }

    public boolean isRequiresPrescription() {
        return requiresPrescription;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public int getMinimumStock() {
        return minimumStock;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public void setManufacturerId(Long manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public void setRequiresPrescription(boolean requiresPrescription) {
        this.requiresPrescription = requiresPrescription;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
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