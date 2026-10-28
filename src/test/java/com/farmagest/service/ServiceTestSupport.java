package com.farmagest.service;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.dao.*;
import com.farmagest.model.*;
import com.farmagest.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.delegatesTo;

/** Intercepts every DAO construction, including those inside StockOperations.
 * Real services, calculations and transaction management still execute. */
abstract class ServiceTestSupport {
    final UserDAO users = mock(UserDAO.class);
    final ProductDAO products = mock(ProductDAO.class);
    final CategoryDAO categories = mock(CategoryDAO.class);
    final ManufacturerDAO manufacturers = mock(ManufacturerDAO.class);
    final SupplierDAO suppliers = mock(SupplierDAO.class);
    final CustomerDAO customers = mock(CustomerDAO.class);
    final PurchaseDAO purchases = mock(PurchaseDAO.class);
    final PurchaseItemDAO purchaseItems = mock(PurchaseItemDAO.class);
    final SaleDAO sales = mock(SaleDAO.class);
    final SaleItemDAO saleItems = mock(SaleItemDAO.class);
    final SaleItemBatchDAO allocations = mock(SaleItemBatchDAO.class);
    final ProductBatchDAO batches = mock(ProductBatchDAO.class);
    final InventoryDAO inventory = mock(InventoryDAO.class);
    final InventoryMovementDAO movements = mock(InventoryMovementDAO.class);
    final AuditLogDAO logs = mock(AuditLogDAO.class);
    final Connection connection = mock(Connection.class);
    private final List<AutoCloseable> scopes = new ArrayList<>();

    @BeforeEach void isolatePersistence() throws Exception {
        intercept(UserDAO.class, users); intercept(ProductDAO.class, products);
        intercept(CategoryDAO.class, categories); intercept(ManufacturerDAO.class, manufacturers);
        intercept(SupplierDAO.class, suppliers); intercept(CustomerDAO.class, customers);
        intercept(PurchaseDAO.class, purchases); intercept(PurchaseItemDAO.class, purchaseItems);
        intercept(SaleDAO.class, sales); intercept(SaleItemDAO.class, saleItems);
        intercept(SaleItemBatchDAO.class, allocations); intercept(ProductBatchDAO.class, batches);
        intercept(InventoryDAO.class, inventory); intercept(InventoryMovementDAO.class, movements);
        intercept(AuditLogDAO.class, logs);
        MockedStatic<DatabaseConnection> database = mockStatic(DatabaseConnection.class);
        scopes.add(database);
        database.when(DatabaseConnection::getConnection).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
    }

    private <T> void intercept(Class<T> type, T target) {
        scopes.add(mockConstructionWithAnswer(type, delegatesTo(target)));
    }

    @AfterEach void releaseMocks() throws Exception {
        for (int i = scopes.size() - 1; i >= 0; i--) scopes.get(i).close();
    }

    void allowStockOperations() throws Exception {
        when(users.findByIdForUpdate(connection, 1)).thenReturn(user());
        when(products.findByIdForUpdate(connection, 2)).thenReturn(product());
        when(batches.updateAvailableQuantity(eq(connection), anyLong(), anyInt())).thenReturn(true);
        when(movements.insert(eq(connection), any(InventoryMovement.class))).thenReturn(true);
    }

    void committed() throws Exception {
        var order = inOrder(connection);
        order.verify(connection).setAutoCommit(false);
        order.verify(connection).commit();
        order.verify(connection).setAutoCommit(true);
        order.verify(connection).close();
        verify(connection, never()).rollback();
    }

    void rolledBack() throws Exception {
        var order = inOrder(connection);
        order.verify(connection).setAutoCommit(false);
        order.verify(connection).rollback();
        order.verify(connection).setAutoCommit(true);
        order.verify(connection).close();
        verify(connection, never()).commit();
    }

    static BigDecimal money(String value) { return new BigDecimal(value); }
    static User user() { return new User(1, "worker", null, "First", "Last", "worker@example.com", Role.WORKER, true, null, null, null); }
    static Category category() { return new Category(0, "Medicines", null, true, null, null); }
    static Manufacturer manufacturer() { return new Manufacturer(0, "Laboratory", null, null, null, null, true, null, null); }
    static Supplier supplier() { return new Supplier(0, "Supplier", null, null, null, null, null, true, null, null); }
    static Customer customer() { return new Customer(0, "First", "Last", null, null, true, null, null); }
    static Product product() { return new Product(0, "Medicine", null, "123456", 3, null, false, money("10.00"), money("21.00"), 5, true, null, null); }
    static ProductBatch batch() { return new ProductBatch(4, 2, 8L, "LOT-1", LocalDate.now().plusDays(30), 10, 10, money("4.00"), null, true, null, null); }
    static Purchase purchase() { return new Purchase(0, 5, 1, null, null, null, null, null, null, null, null, null, null, null, null); }
    static PurchaseItem purchaseItem() { return new PurchaseItem(0, 0, 2, 3, money("4.00"), money("21.00"), null, null, null, null); }
    static Sale sale() { return new Sale(0, 1, null, PaymentMethod.CASH, null, null, null, null, null, null, null, null, null, null); }
    static SaleItem saleItem() { return new SaleItem(0, 0, 2, 3, null, null, null, null, null, null); }
    static AuditLog log() { return new AuditLog(0, null, "CREATE", "product", 2L, null, "{}", "Created product", null); }
}
