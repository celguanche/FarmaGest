package com.farmagest.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PurchaseItem {

    private long id;
    private long purchaseId;
    private long productId;
    private int quantity;
    private BigDecimal unitCost;
    private BigDecimal vatRate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
    private LocalDateTime createdAt;

    public PurchaseItem(long id,
                        long purchaseId,
                        long productId,
                        int quantity,
                        BigDecimal unitCost,
                        BigDecimal vatRate,
                        BigDecimal subtotal,
                        BigDecimal taxAmount,
                        BigDecimal lineTotal,
                        LocalDateTime createdAt) {

        this.id = id;
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.vatRate = vatRate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.lineTotal = lineTotal;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PurchaseItem{" +
                "id=" + id +
                ", purchaseId=" + purchaseId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", unitCost=" + unitCost +
                ", vatRate=" + vatRate +
                ", subtotal=" + subtotal +
                ", taxAmount=" + taxAmount +
                ", lineTotal=" + lineTotal +
                ", createdAt=" + createdAt +
                '}';
    }

    public long getId() {
        return id;
    }

    public long getPurchaseId() {
        return purchaseId;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setPurchaseId(long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}