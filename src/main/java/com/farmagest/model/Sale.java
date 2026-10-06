package com.farmagest.model;

import com.farmagest.model.enums.PaymentMethod;
import com.farmagest.model.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Sale {

    private long id;

    private long employeeUserId;
    private Long customerId;

    private PaymentMethod paymentMethod;
    private SaleStatus status;

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private LocalDateTime soldAt;

    private LocalDateTime cancelledAt;
    private Long cancelledByUserId;
    private String cancellationReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Sale(long id, long employeeUserId, Long customerId,
                PaymentMethod paymentMethod, SaleStatus status,
                BigDecimal subtotal, BigDecimal taxAmount,
                BigDecimal totalAmount, LocalDateTime soldAt,
                LocalDateTime cancelledAt, Long cancelledByUserId,
                String cancellationReason, LocalDateTime createdAt,
                LocalDateTime updatedAt) {

        this.id = id;
        this.employeeUserId = employeeUserId;
        this.customerId = customerId;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.soldAt = soldAt;
        this.cancelledAt = cancelledAt;
        this.cancelledByUserId = cancelledByUserId;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", employeeUserId=" + employeeUserId +
                ", customerId=" + customerId +
                ", paymentMethod=" + paymentMethod +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", soldAt=" + soldAt +
                '}';
    }

    public long getId() {
        return id;
    }

    public long getEmployeeUserId() {
        return employeeUserId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public SaleStatus getStatus() {
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

    public LocalDateTime getSoldAt() {
        return soldAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setEmployeeUserId(long employeeUserId) {
        this.employeeUserId = employeeUserId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setStatus(SaleStatus status) {
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

    public void setSoldAt(LocalDateTime soldAt) {
        this.soldAt = soldAt;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}