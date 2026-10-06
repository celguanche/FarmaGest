package com.farmagest.model;

import com.farmagest.model.enums.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Purchase {

    private long id;
    private long supplierId;
    private long createdByUserId;
    private PurchaseStatus status;

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private LocalDateTime orderedAt;
    private LocalDateTime receivedAt;

    private LocalDateTime cancelledAt;
    private Long cancelledByUserId;
    private String cancellationReason;

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Purchase(long id, long supplierId, long createdByUserId,
                    PurchaseStatus status, BigDecimal subtotal,
                    BigDecimal taxAmount, BigDecimal totalAmount,
                    LocalDateTime orderedAt, LocalDateTime receivedAt,
                    LocalDateTime cancelledAt, Long cancelledByUserId,
                    String cancellationReason, String notes,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {

        this.id = id;
        this.supplierId = supplierId;
        this.createdByUserId = createdByUserId;
        this.status = status;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.receivedAt = receivedAt;
        this.cancelledAt = cancelledAt;
        this.cancelledByUserId = cancelledByUserId;
        this.cancellationReason = cancellationReason;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Purchase{" +
                "id=" + id +
                ", supplierId=" + supplierId +
                ", createdByUserId=" + createdByUserId +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", orderedAt=" + orderedAt +
                '}';
    }

    public long getId() {
        return id;
    }

    public long getSupplierId() {
        return supplierId;
    }

    public long getCreatedByUserId() {
        return createdByUserId;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Long getCancelledByUserId() {
        return cancelledByUserId;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getNotes() {
        return notes;
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

    public void setSupplierId(long supplierId) {
        this.supplierId = supplierId;
    }

    public void setCreatedByUserId(long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public void setCancelledByUserId(Long cancelledByUserId) {
        this.cancelledByUserId = cancelledByUserId;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}