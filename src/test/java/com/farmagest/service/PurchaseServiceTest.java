package com.farmagest.service;

import com.farmagest.exception.*;
import com.farmagest.model.*;
import com.farmagest.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseServiceTest extends ServiceTestSupport {
    PurchaseService service;
    @BeforeEach void prepare() throws Exception {
        allowStockOperations(); service = new PurchaseService();
        when(suppliers.findByIdForUpdate(connection, 5)).thenReturn(supplier());
        when(purchases.insert(eq(connection), any())).thenAnswer(call -> { call.<Purchase>getArgument(1).setId(7); return true; });
        when(purchaseItems.insert(eq(connection), any())).thenReturn(true);
        when(batches.insert(eq(connection), any())).thenAnswer(call -> { call.<ProductBatch>getArgument(1).setId(4); return true; });
    }
    @Test void createsOrderAndCalculatesTotals() throws Exception {
        Purchase p = purchase(); PurchaseItem line = purchaseItem();
        p.setCancellationReason("stale"); p.setCancelledByUserId(1L);
        assertSame(p, service.createPurchase(p, List.of(line)));
        assertEquals(PurchaseStatus.ORDERED, p.getStatus()); assertNotNull(p.getOrderedAt());
        assertNull(p.getReceivedAt()); assertNull(p.getCancellationReason()); assertNull(p.getCancelledByUserId());
        assertEquals(money("12.00"), p.getSubtotal()); assertEquals(money("2.52"), p.getTaxAmount());
        assertEquals(money("14.52"), p.getTotalAmount()); assertEquals(money("14.52"), line.getLineTotal());
        assertEquals(7, line.getPurchaseId()); verify(purchaseItems).insert(connection, line); committed();
    }
    @Test void locksProductsInIdOrderAndSumsMultipleLines() throws Exception {
        when(products.findByIdForUpdate(connection, 3)).thenReturn(product());
        PurchaseItem higher = purchaseItem(); higher.setProductId(3); higher.setQuantity(2);
        PurchaseItem lower = purchaseItem();
        Purchase p = service.createPurchase(purchase(), List.of(higher, lower));
        assertEquals(money("20.00"), p.getSubtotal()); assertEquals(money("4.20"), p.getTaxAmount());
        assertEquals(money("24.20"), p.getTotalAmount());
        var order = inOrder(products);
        order.verify(products).findByIdForUpdate(connection, 2);
        order.verify(products).findByIdForUpdate(connection, 3); committed();
    }
    @Test void rejectsInvalidInputBeforeTransaction() {
        Purchase p = purchase(); PurchaseItem line = purchaseItem();
        assertThrows(ValidationException.class, () -> service.createPurchase(null, List.of(line)));
        assertThrows(ValidationException.class, () -> service.createPurchase(p, List.of()));
        assertThrows(ValidationException.class, () -> service.createPurchase(p, List.of(line, line)));
        line.setId(8); assertThrows(ValidationException.class, () -> service.createPurchase(p, List.of(line)));
        verifyNoInteractions(connection);
    }
    @Test void rejectsInvalidQuantityAndCost() throws Exception {
        PurchaseItem line = purchaseItem(); line.setQuantity(0);
        assertThrows(ValidationException.class, () -> service.createPurchase(purchase(), List.of(line))); rolledBack();
    }
    @Test void rejectsNegativeCost() throws Exception {
        PurchaseItem line = purchaseItem(); line.setUnitCost(money("-1"));
        assertThrows(ValidationException.class, () -> service.createPurchase(purchase(), List.of(line))); rolledBack();
    }
    @Test void rejectsInactiveSupplier() throws Exception {
        var supplier = supplier(); supplier.setActive(false);
        when(suppliers.findByIdForUpdate(connection, 5)).thenReturn(supplier);
        assertThrows(ValidationException.class, () -> service.createPurchase(purchase(), List.of(purchaseItem())));
        verify(purchases, never()).insert(any(), any()); rolledBack();
    }
    @Test void rollsBackLineInsertFailureAfterHeaderWrite() throws Exception {
        when(purchaseItems.insert(eq(connection), any())).thenThrow(new SQLException("offline", "08000"));
        assertThrows(DatabaseException.class, () -> service.createPurchase(purchase(), List.of(purchaseItem())));
        verify(purchases).insert(eq(connection), any()); rolledBack();
    }
    void allowReception() throws Exception {
        Purchase p = purchase(); p.setId(7); p.setStatus(PurchaseStatus.ORDERED);
        when(purchases.findByIdForUpdate(connection, 7)).thenReturn(p);
        PurchaseItem item = purchaseItem(); item.setId(8); item.setPurchaseId(7);
        when(purchaseItems.findByPurchaseId(connection, 7)).thenReturn(List.of(item));
        when(purchases.markAsReceived(connection, 7)).thenReturn(true);
    }
    ProductBatch incoming(int quantity, String number) {
        ProductBatch b = batch(); b.setId(0); b.setInitialQuantity(quantity); b.setBatchNumber(number);
        b.setAvailableQuantity(999); b.setUnitCost(money("999.00")); return b;
    }
    @Test void receivesMultipleBatchesAndUsesPersistedCost() throws Exception {
        allowReception(); ProductBatch first = incoming(1, "A"); ProductBatch second = incoming(2, "B");
        service.receivePurchase(7, 1, List.of(first, second));
        assertEquals(money("4.00"), first.getUnitCost()); assertEquals(money("4.00"), second.getUnitCost());
        assertEquals(1, first.getAvailableQuantity()); assertEquals(2, second.getAvailableQuantity());
        assertTrue(first.isActive()); assertNotNull(first.getReceivedAt());
        verify(movements).insert(eq(connection), argThat(m -> m.getQuantityChange() == 1 && m.getStockBefore() == 0
                && m.getPurchaseId() == 7 && m.getMovementType() == InventoryMovementType.PURCHASE_RECEIPT));
        verify(movements).insert(eq(connection), argThat(m -> m.getQuantityChange() == 2 && m.getStockBefore() == 0));
        verify(purchases).markAsReceived(connection, 7); committed();
    }
    @Test void rejectsPartialReception() throws Exception {
        allowReception();
        assertThrows(ValidationException.class, () -> service.receivePurchase(7, 1, List.of(incoming(2, "A"))));
        verify(batches, never()).insert(any(), any()); rolledBack();
    }
    @Test void rejectsExcessReception() throws Exception {
        allowReception();
        assertThrows(ValidationException.class, () -> service.receivePurchase(7, 1, List.of(incoming(4, "A")))); rolledBack();
    }
    @Test void rejectsExpiredReception() throws Exception {
        allowReception(); ProductBatch b = incoming(3, "A"); b.setExpiryDate(LocalDate.now().minusDays(1));
        assertThrows(ValidationException.class, () -> service.receivePurchase(7, 1, List.of(b))); rolledBack();
    }
    @Test void rejectsBatchForDifferentProduct() throws Exception {
        allowReception(); ProductBatch b = incoming(3, "A"); b.setProductId(99);
        assertThrows(ValidationException.class, () -> service.receivePurchase(7, 1, List.of(b))); rolledBack();
    }
    @Test void rejectsUnknownPurchaseItem() throws Exception {
        allowReception(); ProductBatch b = incoming(3, "A"); b.setPurchaseItemId(99L);
        assertThrows(BusinessException.class, () -> service.receivePurchase(7, 1, List.of(b))); rolledBack();
    }
    @ParameterizedTest @ValueSource(strings = {"persisted", "missingItem", "zeroQuantity", "blankNumber"})
    void rejectsInvalidIncomingBatch(String invalidField) throws Exception {
        allowReception(); ProductBatch b = incoming(3, "A");
        switch (invalidField) {
            case "persisted" -> b.setId(4);
            case "missingItem" -> b.setPurchaseItemId(null);
            case "zeroQuantity" -> b.setInitialQuantity(0);
            case "blankNumber" -> b.setBatchNumber(" ");
            default -> throw new AssertionError(invalidField);
        }
        assertThrows(ValidationException.class, () -> service.receivePurchase(7, 1, List.of(b)));
        verify(batches, never()).insert(any(), any()); rolledBack();
    }
    @Test void rollsBackReceptionDaoFailure() throws Exception {
        allowReception(); when(purchases.markAsReceived(connection, 7)).thenThrow(new SQLException("offline"));
        assertThrows(DatabaseException.class, () -> service.receivePurchase(7, 1, List.of(incoming(3, "A"))));
        verify(movements).insert(eq(connection), any()); rolledBack();
    }
    @ParameterizedTest @EnumSource(value = PurchaseStatus.class, names = {"RECEIVED", "CANCELLED"})
    void rejectsReceptionOfNonPendingOrder(PurchaseStatus status) throws Exception {
        allowReception(); Purchase p = purchase(); p.setStatus(status);
        when(purchases.findByIdForUpdate(connection, 7)).thenReturn(p);
        assertThrows(ValidationException.class, () -> service.receivePurchase(7, 1, List.of(incoming(3, "A")))); rolledBack();
    }
    @Test void cancelsPendingOrder() throws Exception {
        allowReception(); when(purchases.cancel(connection, 7, 1, "Cancelled")).thenReturn(true);
        service.cancelPurchase(7, 1, " Cancelled ");
        verify(purchases).cancel(connection, 7, 1, "Cancelled"); verifyNoInteractions(movements); committed();
    }
    @ParameterizedTest @EnumSource(value = PurchaseStatus.class, names = {"RECEIVED", "CANCELLED"})
    void rejectsCancellationOfNonPendingOrder(PurchaseStatus status) throws Exception {
        Purchase p = purchase(); p.setStatus(status); when(purchases.findByIdForUpdate(connection, 7)).thenReturn(p);
        assertThrows(ValidationException.class, () -> service.cancelPurchase(7, 1, "Cancelled"));
        verify(purchases, never()).cancel(any(), anyLong(), anyLong(), anyString()); rolledBack();
    }
    @Test void rollsBackFailedCancellation() throws Exception {
        allowReception();
        assertThrows(BusinessException.class, () -> service.cancelPurchase(7, 1, "Cancelled")); rolledBack();
    }
    @Test void queriesPurchaseAndItems() {
        Purchase p = purchase(); PurchaseItem item = purchaseItem();
        when(purchases.findById(7)).thenReturn(p); when(purchases.findAll()).thenReturn(List.of(p));
        when(purchaseItems.findByPurchaseId(7)).thenReturn(List.of(item));
        assertSame(p, service.findById(7)); assertEquals(List.of(p), service.findAll());
        assertEquals(List.of(item), service.findItems(7));
    }
    @Test void rejectsInvalidIdAndMissingPurchase() {
        assertThrows(ValidationException.class, () -> service.findById(0));
        assertThrows(BusinessException.class, () -> service.findItems(7));
        verify(purchaseItems, never()).findByPurchaseId(anyLong());
    }
}
