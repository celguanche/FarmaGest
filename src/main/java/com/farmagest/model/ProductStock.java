package com.farmagest.model;

public class ProductStock {

    private long productId;
    private String productName;

    private int minimumStock;
    private int totalStock;
    private int saleableStock;
    private int expiredStock;

    public ProductStock(long productId, String productName,
                        int minimumStock, int totalStock,
                        int saleableStock, int expiredStock) {

        this.productId = productId;
        this.productName = productName;
        this.minimumStock = minimumStock;
        this.totalStock = totalStock;
        this.saleableStock = saleableStock;
        this.expiredStock = expiredStock;
    }

    @Override
    public String toString() {
        return "ProductStock{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", minimumStock=" + minimumStock +
                ", totalStock=" + totalStock +
                ", saleableStock=" + saleableStock +
                ", expiredStock=" + expiredStock +
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

    public int getTotalStock() {
        return totalStock;
    }

    public int getSaleableStock() {
        return saleableStock;
    }

    public int getExpiredStock() {
        return expiredStock;
    }
}