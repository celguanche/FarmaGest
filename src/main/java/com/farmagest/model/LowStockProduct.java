package com.farmagest.model;

public class LowStockProduct {

    private long productId;
    private String productName;
    private int minimumStock;
    private int saleableStock;

    public LowStockProduct(long productId, String productName,
                           int minimumStock, int saleableStock) {

        this.productId = productId;
        this.productName = productName;
        this.minimumStock = minimumStock;
        this.saleableStock = saleableStock;
    }

    @Override
    public String toString() {
        return "LowStockProduct{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", minimumStock=" + minimumStock +
                ", saleableStock=" + saleableStock +
                '}';
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public int getSaleableStock() {
        return saleableStock;
    }
}