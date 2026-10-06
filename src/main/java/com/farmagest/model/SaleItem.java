package com.farmagest.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleItem {

    private long id;
    private long saleId;
    private long productId;

    private int quantity;

    private BigDecimal unitPrice;
    private BigDecimal vatRate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;

    private LocalDateTime createdAt;

    public SaleItem(long id, long saleId, long productId,
                    int quantity, BigDecimal unitPrice,
                    BigDecimal vatRate, BigDecimal subtotal,
                    BigDecimal taxAmount, BigDecimal lineTotal,
                    LocalDateTime createdAt) {

        this.id = id;
        this.saleId = saleId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.vatRate = vatRate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.lineTotal = lineTotal;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SaleItem{" +
                "id=" + id +
                ", saleId=" + saleId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", lineTotal=" + lineTotal +
                '}';
    }

    public long getId() {
        return id;
    }

    public long getSaleId() {
        return saleId;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
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

    public void setSaleId(long saleId) {
        this.saleId = saleId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
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