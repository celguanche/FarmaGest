package com.farmagest.service;

import com.farmagest.exception.*;
import com.farmagest.model.*;
import com.farmagest.model.enums.InventoryMovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest extends ServiceTestSupport {
    InventoryService service;
    ProductBatch batch;
    @BeforeEach void prepare() throws Exception {
        allowStockOperations(); service = new InventoryService(); batch = batch();
        when(batches.findByIdForUpdate(connection, 4)).thenReturn(batch);
    }
    @ParameterizedTest @ValueSource(ints = {5, -3})
    void adjustsStockAndRecordsBeforeAndAfter(int change) throws Exception {
        service.adjustStock(4, 1, change, " Count correction ");
        assertEquals(10 + change, batch.getAvailableQuantity());
        verify(batches).updateAvailableQuantity(connection, 4, 10 + change);
        ArgumentCaptor<InventoryMovement> capture = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movements).insert(eq(connection), capture.capture());
        InventoryMovement m = capture.getValue();
        assertEquals(10, m.getStockBefore()); assertEquals(10 + change, m.getStockAfter());
        assertEquals(change, m.getQuantityChange()); assertEquals(4, m.getBatchId()); assertEquals(1, m.getUserId());
        assertEquals(change > 0 ? InventoryMovementType.ADJUSTMENT_IN : InventoryMovementType.ADJUSTMENT_OUT, m.getMovementType());
        assertEquals("Count correction", m.getReason()); committed();
    }
    @Test void rejectsInsufficientStock() throws Exception {
        assertThrows(ValidationException.class, () -> service.adjustStock(4, 1, -11, "Correction"));
        verify(batches, never()).updateAvailableQuantity(any(), anyLong(), anyInt());
        verifyNoInteractions(movements); rolledBack();
    }
    @Test void rejectsOverflow() throws Exception {
        batch.setAvailableQuantity(Integer.MAX_VALUE);
        assertThrows(ValidationException.class, () -> service.adjustStock(4, 1, 1, "Correction")); rolledBack();
    }
    @Test void rejectsInactiveBatch() throws Exception {
        batch.setActive(false);
        assertThrows(ValidationException.class, () -> service.adjustStock(4, 1, 1, "Correction")); rolledBack();
    }
    @Test void rejectsAddingExpiredStock() throws Exception {
        batch.setExpiryDate(LocalDate.now().minusDays(1));
        assertThrows(ValidationException.class, () -> service.adjustStock(4, 1, 1, "Correction")); rolledBack();
    }
    @Test void allowsRemovingExpiredStock() throws Exception {
        batch.setExpiryDate(LocalDate.now().minusDays(1));
        service.adjustStock(4, 1, -1, "Removal"); committed();
    }
    @Test void allowsAddingStockWithoutExpiryDate() throws Exception {
        batch.setExpiryDate(null); service.adjustStock(4, 1, 1, "Correction"); committed();
    }
    @Test void expiresEntireBatch() throws Exception {
        batch.setExpiryDate(LocalDate.now().minusDays(1)); service.expireBatch(4, 1);
        assertEquals(0, batch.getAvailableQuantity());
        verify(movements).insert(eq(connection), argThat(m -> m.getMovementType() == InventoryMovementType.EXPIRED
                && m.getQuantityChange() == -10 && m.getStockAfter() == 0)); committed();
    }
    @Test void rejectsExpiryOfUnexpiredBatch() throws Exception {
        batch.setExpiryDate(LocalDate.now());
        assertThrows(ValidationException.class, () -> service.expireBatch(4, 1)); rolledBack();
    }
    @Test void rejectsExpiryWithoutExpiryDate() throws Exception {
        batch.setExpiryDate(null);
        assertThrows(ValidationException.class, () -> service.expireBatch(4, 1)); rolledBack();
    }
    @Test void rejectsExpiryWithoutStock() throws Exception {
        batch.setExpiryDate(LocalDate.now().minusDays(1)); batch.setAvailableQuantity(0);
        assertThrows(ValidationException.class, () -> service.expireBatch(4, 1)); rolledBack();
    }
    @Test void rollsBackWhenMovementInsertFailsAfterStockWrite() throws Exception {
        when(movements.insert(eq(connection), any())).thenThrow(new SQLException("offline", "08000"));
        assertThrows(DatabaseException.class, () -> service.adjustStock(4, 1, 2, "Correction"));
        verify(batches).updateAvailableQuantity(connection, 4, 12); rolledBack();
    }
    @Test void rollsBackWhenStockUpdateReturnsFalse() throws Exception {
        when(batches.updateAvailableQuantity(connection, 4, 12)).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.adjustStock(4, 1, 2, "Correction"));
        verifyNoInteractions(movements); rolledBack();
    }
    @Test void rejectsMissingBatch() throws Exception {
        when(batches.findByIdForUpdate(connection, 4)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.adjustStock(4, 1, 2, "Correction")); rolledBack();
    }
    @Test void rejectsInactiveActor() throws Exception {
        var actor = user(); actor.setActive(false); when(users.findByIdForUpdate(connection, 1)).thenReturn(actor);
        assertThrows(ValidationException.class, () -> service.adjustStock(4, 1, 2, "Correction"));
        verify(batches, never()).findByIdForUpdate(any(), anyLong()); rolledBack();
    }
    @Test void rejectsInvalidAdjustmentBeforeTransaction() {
        assertThrows(ValidationException.class, () -> service.adjustStock(0, 1, 2, "Correction"));
        assertThrows(ValidationException.class, () -> service.adjustStock(4, 1, 0, "Correction"));
        assertThrows(ValidationException.class, () -> service.adjustStock(4, 1, 2, " "));
        verifyNoInteractions(connection);
    }
    @Test void readsInventoryViewsBatchesAndMovements() {
        ProductStock stock = new ProductStock(2, "Medicine", 5, 10, 10, 0);
        LowStockProduct low = new LowStockProduct(3, "Other", 5, 1);
        InventoryMovement movement = new InventoryMovement(1, 4, 1, InventoryMovementType.SALE, -1, 10, 9, 7L, null, "Sale", null);
        when(inventory.findStockByProductId(2)).thenReturn(stock);
        when(inventory.findAllStock()).thenReturn(List.of(stock));
        when(inventory.findLowStockProducts()).thenReturn(List.of(low));
        when(batches.findByProductId(2)).thenReturn(List.of(batch));
        when(movements.findByBatchId(4)).thenReturn(List.of(movement));
        assertSame(stock, service.findStockByProductId(2));
        assertEquals(List.of(stock), service.findAllStock()); assertEquals(List.of(low), service.findLowStockProducts());
        assertEquals(List.of(batch), service.findBatches(2)); assertEquals(List.of(movement), service.findMovements(4));
    }
    @Test void rejectsMissingStockAndInvalidQueryIds() {
        assertThrows(BusinessException.class, () -> service.findStockByProductId(2));
        assertThrows(ValidationException.class, () -> service.findStockByProductId(0));
        assertThrows(ValidationException.class, () -> service.findBatches(0));
        assertThrows(ValidationException.class, () -> service.findMovements(0));
    }
}
