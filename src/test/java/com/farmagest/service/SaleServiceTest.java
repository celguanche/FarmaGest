package com.farmagest.service;

import com.farmagest.exception.*;
import com.farmagest.model.*;
import com.farmagest.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SaleServiceTest extends ServiceTestSupport {
    SaleService service;
    @BeforeEach void prepare() throws Exception {
        allowStockOperations(); service = new SaleService();
        when(sales.insert(eq(connection), any())).thenAnswer(call -> { call.<Sale>getArgument(1).setId(7); return true; });
        when(saleItems.insert(eq(connection), any())).thenAnswer(call -> { call.<SaleItem>getArgument(1).setId(8); return true; });
        when(allocations.insert(eq(connection), any())).thenReturn(true);
    }
    @Test void calculatesTotalsAndAllocatesAcrossBatchesInDaoOrder() throws Exception {
        ProductBatch first = batch(); first.setAvailableQuantity(2);
        ProductBatch second = batch(); second.setId(5);
        when(batches.findAvailableByProductIdForUpdate(connection, 2)).thenReturn(List.of(first, second));
        Sale s = sale(); SaleItem line = saleItem();
        line.setUnitPrice(money("0.01")); line.setVatRate(money("0.00"));
        assertSame(s, service.createSale(s, List.of(line)));
        assertEquals(money("30.00"), s.getSubtotal()); assertEquals(money("6.30"), s.getTaxAmount());
        assertEquals(money("36.30"), s.getTotalAmount()); assertEquals(SaleStatus.COMPLETED, s.getStatus());
        assertNotNull(s.getSoldAt()); assertEquals(7, line.getSaleId());
        assertEquals(money("10.00"), line.getUnitPrice()); assertEquals(money("36.30"), line.getLineTotal());
        var order = inOrder(batches, movements, allocations);
        order.verify(batches).findAvailableByProductIdForUpdate(connection, 2);
        order.verify(batches).updateAvailableQuantity(connection, 4, 0);
        order.verify(movements).insert(eq(connection), argThat(m -> m.getQuantityChange() == -2 && m.getSaleId() == 7));
        order.verify(allocations).insert(eq(connection), argThat(a -> a.getSaleItemId() == 8 && a.getBatchId() == 4 && a.getQuantity() == 2));
        order.verify(batches).updateAvailableQuantity(connection, 5, 9);
        order.verify(movements).insert(eq(connection), argThat(m -> m.getQuantityChange() == -1));
        order.verify(allocations).insert(eq(connection), argThat(a -> a.getBatchId() == 5 && a.getQuantity() == 1));
        committed();
    }
    @Test void stopsAllocatingOnceQuantityIsSatisfied() throws Exception {
        ProductBatch unused = batch(); unused.setId(99);
        when(batches.findAvailableByProductIdForUpdate(connection, 2)).thenReturn(List.of(batch(), unused));
        service.createSale(sale(), List.of(saleItem()));
        verify(batches, never()).updateAvailableQuantity(eq(connection), eq(99L), anyInt()); committed();
    }
    @Test void sortsProductsAndSumsTotalsAcrossLines() throws Exception {
        when(products.findByIdForUpdate(connection, 3)).thenReturn(product());
        ProductBatch other = batch(); other.setId(5); other.setProductId(3);
        when(batches.findAvailableByProductIdForUpdate(connection, 2)).thenReturn(List.of(batch()));
        when(batches.findAvailableByProductIdForUpdate(connection, 3)).thenReturn(List.of(other));
        SaleItem higher = saleItem(); higher.setProductId(3); higher.setQuantity(2);
        Sale s = service.createSale(sale(), List.of(higher, saleItem()));
        assertEquals(money("50.00"), s.getSubtotal()); assertEquals(money("10.50"), s.getTaxAmount());
        assertEquals(money("60.50"), s.getTotalAmount());
        var order = inOrder(products);
        order.verify(products).findByIdForUpdate(connection, 2);
        order.verify(products).findByIdForUpdate(connection, 3); committed();
    }
    @Test void rollsBackInsufficientStockAfterPartialAllocation() throws Exception {
        ProductBatch b = batch(); b.setAvailableQuantity(2);
        when(batches.findAvailableByProductIdForUpdate(connection, 2)).thenReturn(List.of(b));
        assertThrows(ValidationException.class, () -> service.createSale(sale(), List.of(saleItem())));
        verify(batches).updateAvailableQuantity(connection, 4, 0); rolledBack();
    }
    @Test void rollsBackWhenNoStockExists() throws Exception {
        assertThrows(ValidationException.class, () -> service.createSale(sale(), List.of(saleItem()))); rolledBack();
    }
    @Test void rollsBackAllocationDaoFailure() throws Exception {
        when(batches.findAvailableByProductIdForUpdate(connection, 2)).thenReturn(List.of(batch()));
        when(allocations.insert(eq(connection), any())).thenThrow(new SQLException("offline", "08000"));
        assertThrows(DatabaseException.class, () -> service.createSale(sale(), List.of(saleItem())));
        verify(movements).insert(eq(connection), any()); rolledBack();
    }
    @Test void rejectsInactiveCustomer() throws Exception {
        Sale s = sale(); s.setCustomerId(6L); var customer = customer(); customer.setActive(false);
        when(customers.findByIdForUpdate(connection, 6)).thenReturn(customer);
        assertThrows(ValidationException.class, () -> service.createSale(s, List.of(saleItem())));
        verify(sales, never()).insert(any(), any()); rolledBack();
    }
    @Test void acceptsActiveCustomer() throws Exception {
        Sale s = sale(); s.setCustomerId(6L);
        when(customers.findByIdForUpdate(connection, 6)).thenReturn(customer());
        when(batches.findAvailableByProductIdForUpdate(connection, 2)).thenReturn(List.of(batch()));
        service.createSale(s, List.of(saleItem())); committed();
    }
    @Test void rejectsInactiveProduct() throws Exception {
        Product p = product(); p.setActive(false); when(products.findByIdForUpdate(connection, 2)).thenReturn(p);
        assertThrows(ValidationException.class, () -> service.createSale(sale(), List.of(saleItem()))); rolledBack();
    }
    @Test void rejectsInvalidInputBeforeTransaction() {
        Sale s = sale(); SaleItem line = saleItem();
        assertThrows(ValidationException.class, () -> service.createSale(null, List.of(line)));
        assertThrows(ValidationException.class, () -> service.createSale(s, List.of()));
        assertThrows(ValidationException.class, () -> service.createSale(s, List.of(line, line)));
        s.setPaymentMethod(null); assertThrows(ValidationException.class, () -> service.createSale(s, List.of(line)));
        s.setPaymentMethod(PaymentMethod.CARD); line.setQuantity(0);
        assertThrows(ValidationException.class, () -> service.createSale(s, List.of(line)));
        verifyNoInteractions(connection);
    }
    void allowCancellation() throws Exception {
        Sale s = sale(); s.setId(7); s.setStatus(SaleStatus.COMPLETED);
        when(sales.findByIdForUpdate(connection, 7)).thenReturn(s);
        SaleItem first = saleItem(); first.setId(8); first.setQuantity(2);
        SaleItem second = saleItem(); second.setId(9); second.setQuantity(1);
        when(saleItems.findBySaleId(connection, 7)).thenReturn(List.of(first, second));
        when(allocations.findBySaleItemId(connection, 8)).thenReturn(List.of(new SaleItemBatch(8, 4, 2, null)));
        when(allocations.findBySaleItemId(connection, 9)).thenReturn(List.of(new SaleItemBatch(9, 4, 1, null)));
        ProductBatch b = batch(); b.setAvailableQuantity(7);
        when(batches.findByIdForUpdate(connection, 4)).thenReturn(b);
        when(sales.cancel(connection, 7, 1, "Return")).thenReturn(true);
    }
    @Test void cancellationAggregatesSharedBatchAndRestoresOriginalStock() throws Exception {
        allowCancellation(); service.cancelSale(7, 1, " Return ");
        verify(batches, times(1)).updateAvailableQuantity(connection, 4, 10);
        verify(movements).insert(eq(connection), argThat(m -> m.getMovementType() == InventoryMovementType.SALE_CANCELLATION
                && m.getQuantityChange() == 3 && m.getSaleId() == 7));
        verify(sales).cancel(connection, 7, 1, "Return"); committed();
    }
    @Test void rejectsCancellationOfCancelledSale() throws Exception {
        Sale s = sale(); s.setStatus(SaleStatus.CANCELLED); when(sales.findByIdForUpdate(connection, 7)).thenReturn(s);
        assertThrows(ValidationException.class, () -> service.cancelSale(7, 1, "Return"));
        verify(sales, never()).cancel(any(), anyLong(), anyLong(), anyString()); rolledBack();
    }
    @Test void restoresOriginalBatchEvenIfItHasExpiredSinceSale() throws Exception {
        allowCancellation(); ProductBatch b = batch(); b.setAvailableQuantity(7);
        b.setExpiryDate(java.time.LocalDate.now().minusDays(1)); b.setActive(false);
        when(batches.findByIdForUpdate(connection, 4)).thenReturn(b);
        service.cancelSale(7, 1, "Return");
        assertEquals(10, b.getAvailableQuantity()); committed();
    }
    @Test void rejectsInconsistentAllocations() throws Exception {
        allowCancellation(); when(allocations.findBySaleItemId(connection, 8)).thenReturn(List.of());
        assertThrows(ValidationException.class, () -> service.cancelSale(7, 1, "Return"));
        verify(batches, never()).updateAvailableQuantity(any(), anyLong(), anyInt()); rolledBack();
    }
    @Test void rejectsCancellationWithoutLines() throws Exception {
        allowCancellation(); when(saleItems.findBySaleId(connection, 7)).thenReturn(List.of());
        assertThrows(ValidationException.class, () -> service.cancelSale(7, 1, "Return")); rolledBack();
    }
    @Test void rollsBackCancellationDaoFailureAfterRestoration() throws Exception {
        allowCancellation(); when(sales.cancel(connection, 7, 1, "Return")).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.cancelSale(7, 1, "Return"));
        verify(batches).updateAvailableQuantity(connection, 4, 10); rolledBack();
    }
    @Test void queriesSaleAndItems() {
        Sale s = sale(); SaleItem line = saleItem();
        when(sales.findById(7)).thenReturn(s); when(sales.findAll()).thenReturn(List.of(s));
        when(saleItems.findBySaleId(7)).thenReturn(List.of(line));
        assertSame(s, service.findById(7)); assertEquals(List.of(s), service.findAll());
        assertEquals(List.of(line), service.findItems(7));
    }
    @Test void rejectsMissingSaleAndInvalidQueryId() {
        assertThrows(ValidationException.class, () -> service.findById(0));
        assertThrows(BusinessException.class, () -> service.findItems(7));
        verify(saleItems, never()).findBySaleId(anyLong());
    }
}
