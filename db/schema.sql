-- ============================================================
-- FarmaGest - Initial Database Schema
-- MySQL 8.4+
-- ============================================================

CREATE DATABASE IF NOT EXISTS farmagest
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE farmagest;


-- ============================================================
-- USERS
-- Application users and roles.
-- ============================================================

CREATE TABLE users (
                       id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                       username VARCHAR(50) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,

                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(150) NOT NULL,
                       email VARCHAR(150) NOT NULL,

                       role ENUM(
        'WORKER',
        'ADMIN',
        'DEVELOPER'
    ) NOT NULL,

                       active BOOLEAN NOT NULL DEFAULT TRUE,

                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       updated_at DATETIME NOT NULL
                           DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

                       last_login_at DATETIME NULL,

                       CONSTRAINT uq_users_username
                           UNIQUE (username),

                       CONSTRAINT uq_users_email
                           UNIQUE (email),

                       INDEX idx_users_role_active (role, active)
) ENGINE = InnoDB;


-- ============================================================
-- CATEGORIES
-- Product categories managed by administrators.
-- ============================================================

CREATE TABLE categories (
                            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                            name VARCHAR(100) NOT NULL,
                            description VARCHAR(255) NULL,

                            active BOOLEAN NOT NULL DEFAULT TRUE,

                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            updated_at DATETIME NOT NULL
                                DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,

                            CONSTRAINT uq_categories_name
                                UNIQUE (name)
) ENGINE = InnoDB;


-- ============================================================
-- MANUFACTURERS
-- Product manufacturers / laboratories.
-- ============================================================

CREATE TABLE manufacturers (
                               id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                               name VARCHAR(150) NOT NULL,

                               phone VARCHAR(30) NULL,
                               email VARCHAR(150) NULL,
                               website VARCHAR(255) NULL,
                               address VARCHAR(255) NULL,

                               active BOOLEAN NOT NULL DEFAULT TRUE,

                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               updated_at DATETIME NOT NULL
                                   DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP,

                               CONSTRAINT uq_manufacturers_name
                                   UNIQUE (name)
) ENGINE = InnoDB;


-- ============================================================
-- SUPPLIERS
-- Companies that supply products to the pharmacy.
-- ============================================================

CREATE TABLE suppliers (
                           id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                           business_name VARCHAR(150) NOT NULL,
                           tax_id VARCHAR(30) NULL,

                           contact_name VARCHAR(150) NULL,
                           phone VARCHAR(30) NULL,
                           email VARCHAR(150) NULL,
                           address VARCHAR(255) NULL,

                           active BOOLEAN NOT NULL DEFAULT TRUE,

                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           updated_at DATETIME NOT NULL
                               DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,

                           CONSTRAINT uq_suppliers_business_name
                               UNIQUE (business_name),

                           CONSTRAINT uq_suppliers_tax_id
                               UNIQUE (tax_id)
) ENGINE = InnoDB;


-- ============================================================
-- PRODUCTS
-- Products sold by the pharmacy.
-- Stock is NOT stored here. Stock is calculated from batches.
-- ============================================================

CREATE TABLE products (
                          id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                          name VARCHAR(150) NOT NULL,
                          description TEXT NULL,

                          barcode VARCHAR(50) NOT NULL,

                          category_id BIGINT UNSIGNED NOT NULL,
                          manufacturer_id BIGINT UNSIGNED NULL,

                          requires_prescription BOOLEAN NOT NULL DEFAULT FALSE,

                          sale_price DECIMAL(10, 2) NOT NULL,
                          vat_rate DECIMAL(5, 2) NOT NULL,

                          minimum_stock INT UNSIGNED NOT NULL DEFAULT 0,

                          active BOOLEAN NOT NULL DEFAULT TRUE,

                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          updated_at DATETIME NOT NULL
                                                                 DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,

                          CONSTRAINT uq_products_barcode
                              UNIQUE (barcode),

                          CONSTRAINT chk_products_sale_price
                              CHECK (sale_price >= 0),

                          CONSTRAINT chk_products_vat_rate
                              CHECK (vat_rate >= 0 AND vat_rate <= 100),

                          CONSTRAINT fk_products_category
                              FOREIGN KEY (category_id)
                                  REFERENCES categories(id)
                                  ON UPDATE CASCADE
                                  ON DELETE RESTRICT,

                          CONSTRAINT fk_products_manufacturer
                              FOREIGN KEY (manufacturer_id)
                                  REFERENCES manufacturers(id)
                                  ON UPDATE CASCADE
                                  ON DELETE SET NULL,

                          INDEX idx_products_name (name),
                          INDEX idx_products_category (category_id),
                          INDEX idx_products_manufacturer (manufacturer_id),
                          INDEX idx_products_active (active)
) ENGINE = InnoDB;


-- ============================================================
-- PRODUCT SUPPLIERS
-- Many-to-many relationship between products and suppliers.
-- ============================================================

CREATE TABLE product_suppliers (
                                   product_id BIGINT UNSIGNED NOT NULL,
                                   supplier_id BIGINT UNSIGNED NOT NULL,

                                   supplier_product_code VARCHAR(100) NULL,

                                   active BOOLEAN NOT NULL DEFAULT TRUE,

                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   updated_at DATETIME NOT NULL
                                       DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,

                                   PRIMARY KEY (product_id, supplier_id),

                                   CONSTRAINT uq_product_suppliers_code
                                       UNIQUE (supplier_id, supplier_product_code),

                                   CONSTRAINT fk_product_suppliers_product
                                       FOREIGN KEY (product_id)
                                           REFERENCES products(id)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT,

                                   CONSTRAINT fk_product_suppliers_supplier
                                       FOREIGN KEY (supplier_id)
                                           REFERENCES suppliers(id)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT
) ENGINE = InnoDB;


-- ============================================================
-- CUSTOMERS
-- Optional customers associated with sales.
-- ============================================================

CREATE TABLE customers (
                           id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                           first_name VARCHAR(100) NOT NULL,
                           last_name VARCHAR(150) NOT NULL,

                           phone VARCHAR(30) NULL,
                           email VARCHAR(150) NULL,

                           active BOOLEAN NOT NULL DEFAULT TRUE,

                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           updated_at DATETIME NOT NULL
                               DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,

                           CONSTRAINT uq_customers_email
                               UNIQUE (email),

                           INDEX idx_customers_name (last_name, first_name),
                           INDEX idx_customers_active (active)
) ENGINE = InnoDB;


-- ============================================================
-- PURCHASES
-- Orders placed with suppliers.
-- ============================================================

CREATE TABLE purchases (
                           id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                           supplier_id BIGINT UNSIGNED NOT NULL,
                           created_by_user_id BIGINT UNSIGNED NOT NULL,

                           status ENUM(
        'ORDERED',
        'RECEIVED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'ORDERED',

                           subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0,
                           tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
                           total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,

                           ordered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           received_at DATETIME NULL,

                           cancelled_at DATETIME NULL,
                           cancelled_by_user_id BIGINT UNSIGNED NULL,
                           cancellation_reason VARCHAR(255) NULL,

                           notes TEXT NULL,

                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           updated_at DATETIME NOT NULL
                               DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,

                           CONSTRAINT chk_purchases_subtotal
                               CHECK (subtotal >= 0),

                           CONSTRAINT chk_purchases_tax_amount
                               CHECK (tax_amount >= 0),

                           CONSTRAINT chk_purchases_total_amount
                               CHECK (total_amount >= 0),

                           CONSTRAINT fk_purchases_supplier
                               FOREIGN KEY (supplier_id)
                                   REFERENCES suppliers(id)
                                   ON UPDATE CASCADE
                                   ON DELETE RESTRICT,

                           CONSTRAINT fk_purchases_created_by
                               FOREIGN KEY (created_by_user_id)
                                   REFERENCES users(id)
                                   ON UPDATE CASCADE
                                   ON DELETE RESTRICT,

                           CONSTRAINT fk_purchases_cancelled_by
                               FOREIGN KEY (cancelled_by_user_id)
                                   REFERENCES users(id)
                                   ON UPDATE CASCADE
                                   ON DELETE SET NULL,

                           INDEX idx_purchases_supplier (supplier_id),
                           INDEX idx_purchases_status (status),
                           INDEX idx_purchases_ordered_at (ordered_at)
) ENGINE = InnoDB;


-- ============================================================
-- PURCHASE ITEMS
-- Products and historical costs included in a purchase.
-- ============================================================

CREATE TABLE purchase_items (
                                id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                purchase_id BIGINT UNSIGNED NOT NULL,
                                product_id BIGINT UNSIGNED NOT NULL,

                                quantity INT UNSIGNED NOT NULL,

                                unit_cost DECIMAL(10, 2) NOT NULL,
                                vat_rate DECIMAL(5, 2) NOT NULL,

                                subtotal DECIMAL(12, 2) NOT NULL,
                                tax_amount DECIMAL(12, 2) NOT NULL,
                                line_total DECIMAL(12, 2) NOT NULL,

                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT chk_purchase_items_quantity
                                    CHECK (quantity > 0),

                                CONSTRAINT chk_purchase_items_unit_cost
                                    CHECK (unit_cost >= 0),

                                CONSTRAINT chk_purchase_items_vat_rate
                                    CHECK (vat_rate >= 0 AND vat_rate <= 100),

                                CONSTRAINT chk_purchase_items_subtotal
                                    CHECK (subtotal >= 0),

                                CONSTRAINT chk_purchase_items_tax_amount
                                    CHECK (tax_amount >= 0),

                                CONSTRAINT chk_purchase_items_line_total
                                    CHECK (line_total >= 0),

                                CONSTRAINT fk_purchase_items_purchase
                                    FOREIGN KEY (purchase_id)
                                        REFERENCES purchases(id)
                                        ON UPDATE CASCADE
                                        ON DELETE RESTRICT,

                                CONSTRAINT fk_purchase_items_product
                                    FOREIGN KEY (product_id)
                                        REFERENCES products(id)
                                        ON UPDATE CASCADE
                                        ON DELETE RESTRICT,

                                INDEX idx_purchase_items_purchase (purchase_id),
                                INDEX idx_purchase_items_product (product_id)
) ENGINE = InnoDB;


-- ============================================================
-- PRODUCT BATCHES
-- Real product stock grouped by batch and expiry date.
-- ============================================================

CREATE TABLE product_batches (
                                 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                 product_id BIGINT UNSIGNED NOT NULL,
                                 purchase_item_id BIGINT UNSIGNED NULL,

                                 batch_number VARCHAR(100) NOT NULL,

                                 expiry_date DATE NULL,

                                 initial_quantity INT UNSIGNED NOT NULL,
                                 available_quantity INT UNSIGNED NOT NULL,

                                 unit_cost DECIMAL(10, 2) NOT NULL,

                                 received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 active BOOLEAN NOT NULL DEFAULT TRUE,

                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 updated_at DATETIME NOT NULL
                                                               DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,

                                 CONSTRAINT uq_product_batches
                                     UNIQUE (product_id, batch_number),

                                 CONSTRAINT chk_product_batches_initial_quantity
                                     CHECK (initial_quantity > 0),

                                 CONSTRAINT chk_product_batches_unit_cost
                                     CHECK (unit_cost >= 0),

                                 CONSTRAINT fk_product_batches_product
                                     FOREIGN KEY (product_id)
                                         REFERENCES products(id)
                                         ON UPDATE CASCADE
                                         ON DELETE RESTRICT,

                                 CONSTRAINT fk_product_batches_purchase_item
                                     FOREIGN KEY (purchase_item_id)
                                         REFERENCES purchase_items(id)
                                         ON UPDATE CASCADE
                                         ON DELETE SET NULL,

                                 INDEX idx_product_batches_product (product_id),
                                 INDEX idx_product_batches_expiry (expiry_date),
                                 INDEX idx_product_batches_available (available_quantity),
                                 INDEX idx_product_batches_active (active)
) ENGINE = InnoDB;


-- ============================================================
-- SALES
-- Sale header.
-- ============================================================

CREATE TABLE sales (
                       id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                       employee_user_id BIGINT UNSIGNED NOT NULL,

                       customer_id BIGINT UNSIGNED NULL,

                       payment_method ENUM(
        'CASH',
        'CARD'
    ) NOT NULL,

                       status ENUM(
        'COMPLETED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'COMPLETED',

                       subtotal DECIMAL(12, 2) NOT NULL,
                       tax_amount DECIMAL(12, 2) NOT NULL,
                       total_amount DECIMAL(12, 2) NOT NULL,

                       sold_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       cancelled_at DATETIME NULL,
                       cancelled_by_user_id BIGINT UNSIGNED NULL,
                       cancellation_reason VARCHAR(255) NULL,

                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       updated_at DATETIME NOT NULL
                           DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT chk_sales_subtotal
                           CHECK (subtotal >= 0),

                       CONSTRAINT chk_sales_tax_amount
                           CHECK (tax_amount >= 0),

                       CONSTRAINT chk_sales_total_amount
                           CHECK (total_amount >= 0),

                       CONSTRAINT fk_sales_employee
                           FOREIGN KEY (employee_user_id)
                               REFERENCES users(id)
                               ON UPDATE CASCADE
                               ON DELETE RESTRICT,

                       CONSTRAINT fk_sales_customer
                           FOREIGN KEY (customer_id)
                               REFERENCES customers(id)
                               ON UPDATE CASCADE
                               ON DELETE SET NULL,

                       CONSTRAINT fk_sales_cancelled_by
                           FOREIGN KEY (cancelled_by_user_id)
                               REFERENCES users(id)
                               ON UPDATE CASCADE
                               ON DELETE SET NULL,

                       INDEX idx_sales_employee (employee_user_id),
                       INDEX idx_sales_customer (customer_id),
                       INDEX idx_sales_status (status),
                       INDEX idx_sales_sold_at (sold_at)
) ENGINE = InnoDB;


-- ============================================================
-- SALE ITEMS
-- Products sold and their historical price / VAT.
-- ============================================================

CREATE TABLE sale_items (
                            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                            sale_id BIGINT UNSIGNED NOT NULL,
                            product_id BIGINT UNSIGNED NOT NULL,

                            quantity INT UNSIGNED NOT NULL,

                            unit_price DECIMAL(10, 2) NOT NULL,
                            vat_rate DECIMAL(5, 2) NOT NULL,

                            subtotal DECIMAL(12, 2) NOT NULL,
                            tax_amount DECIMAL(12, 2) NOT NULL,
                            line_total DECIMAL(12, 2) NOT NULL,

                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT chk_sale_items_quantity
                                CHECK (quantity > 0),

                            CONSTRAINT chk_sale_items_unit_price
                                CHECK (unit_price >= 0),

                            CONSTRAINT chk_sale_items_vat_rate
                                CHECK (vat_rate >= 0 AND vat_rate <= 100),

                            CONSTRAINT chk_sale_items_subtotal
                                CHECK (subtotal >= 0),

                            CONSTRAINT chk_sale_items_tax_amount
                                CHECK (tax_amount >= 0),

                            CONSTRAINT chk_sale_items_line_total
                                CHECK (line_total >= 0),

                            CONSTRAINT fk_sale_items_sale
                                FOREIGN KEY (sale_id)
                                    REFERENCES sales(id)
                                    ON UPDATE CASCADE
                                    ON DELETE RESTRICT,

                            CONSTRAINT fk_sale_items_product
                                FOREIGN KEY (product_id)
                                    REFERENCES products(id)
                                    ON UPDATE CASCADE
                                    ON DELETE RESTRICT,

                            INDEX idx_sale_items_sale (sale_id),
                            INDEX idx_sale_items_product (product_id)
) ENGINE = InnoDB;


-- ============================================================
-- SALE ITEM BATCHES
-- Indicates exactly which batches supplied each sale item.
-- ============================================================

CREATE TABLE sale_item_batches (
                                   sale_item_id BIGINT UNSIGNED NOT NULL,
                                   batch_id BIGINT UNSIGNED NOT NULL,

                                   quantity INT UNSIGNED NOT NULL,

                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   PRIMARY KEY (sale_item_id, batch_id),

                                   CONSTRAINT chk_sale_item_batches_quantity
                                       CHECK (quantity > 0),

                                   CONSTRAINT fk_sale_item_batches_sale_item
                                       FOREIGN KEY (sale_item_id)
                                           REFERENCES sale_items(id)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT,

                                   CONSTRAINT fk_sale_item_batches_batch
                                       FOREIGN KEY (batch_id)
                                           REFERENCES product_batches(id)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT,

                                   INDEX idx_sale_item_batches_batch (batch_id)
) ENGINE = InnoDB;


-- ============================================================
-- INVENTORY MOVEMENTS
-- Complete history of every stock change.
-- ============================================================

CREATE TABLE inventory_movements (
                                     id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                     batch_id BIGINT UNSIGNED NOT NULL,
                                     user_id BIGINT UNSIGNED NOT NULL,

                                     movement_type ENUM(
        'PURCHASE_RECEIPT',
        'SALE',
        'SALE_CANCELLATION',
        'ADJUSTMENT_IN',
        'ADJUSTMENT_OUT',
        'EXPIRED'
    ) NOT NULL,

                                     quantity_change INT NOT NULL,

                                     stock_before INT UNSIGNED NOT NULL,
                                     stock_after INT UNSIGNED NOT NULL,

                                     sale_id BIGINT UNSIGNED NULL,
                                     purchase_id BIGINT UNSIGNED NULL,

                                     reason VARCHAR(255) NULL,

                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT chk_inventory_quantity_change
                                         CHECK (quantity_change <> 0),

                                     CONSTRAINT fk_inventory_batch
                                         FOREIGN KEY (batch_id)
                                             REFERENCES product_batches(id)
                                             ON UPDATE CASCADE
                                             ON DELETE RESTRICT,

                                     CONSTRAINT fk_inventory_user
                                         FOREIGN KEY (user_id)
                                             REFERENCES users(id)
                                             ON UPDATE CASCADE
                                             ON DELETE RESTRICT,

                                     CONSTRAINT fk_inventory_sale
                                         FOREIGN KEY (sale_id)
                                             REFERENCES sales(id)
                                             ON UPDATE CASCADE
                                             ON DELETE RESTRICT,

                                     CONSTRAINT fk_inventory_purchase
                                         FOREIGN KEY (purchase_id)
                                             REFERENCES purchases(id)
                                             ON UPDATE CASCADE
                                             ON DELETE RESTRICT,

                                     INDEX idx_inventory_batch (batch_id),
                                     INDEX idx_inventory_user (user_id),
                                     INDEX idx_inventory_sale (sale_id),
                                     INDEX idx_inventory_purchase (purchase_id),
                                     INDEX idx_inventory_type (movement_type),
                                     INDEX idx_inventory_created_at (created_at)
) ENGINE = InnoDB;


-- ============================================================
-- AUDIT LOGS
-- Administrative audit trail.
--
-- Never store passwords or password hashes in old_values,
-- new_values or description.
-- ============================================================

CREATE TABLE audit_logs (
                            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                            user_id BIGINT UNSIGNED NULL,

                            action VARCHAR(50) NOT NULL,
                            entity_type VARCHAR(100) NOT NULL,
                            entity_id BIGINT UNSIGNED NULL,

                            old_values JSON NULL,
                            new_values JSON NULL,

                            description VARCHAR(500) NULL,

                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_audit_logs_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON UPDATE CASCADE
                                    ON DELETE SET NULL,

                            INDEX idx_audit_logs_user (user_id),
                            INDEX idx_audit_logs_entity (entity_type, entity_id),
                            INDEX idx_audit_logs_created_at (created_at)
) ENGINE = InnoDB;


-- ============================================================
-- PRODUCT STOCK VIEW
--
-- total_stock:
--     All available units, including expired batches.
--
-- saleable_stock:
--     Active and non-expired units that may be sold.
--
-- expired_stock:
--     Units currently remaining in expired batches.
-- ============================================================

CREATE OR REPLACE VIEW v_product_stock AS
SELECT
    p.id AS product_id,
    p.name AS product_name,
    p.minimum_stock,

    COALESCE(
            SUM(
                    CASE
                        WHEN pb.active = TRUE
                            THEN pb.available_quantity
                        ELSE 0
                        END
            ),
            0
    ) AS total_stock,

    COALESCE(
            SUM(
                    CASE
                        WHEN pb.active = TRUE
                            AND (
                                 pb.expiry_date IS NULL
                                     OR pb.expiry_date >= CURRENT_DATE
                                 )
                            THEN pb.available_quantity
                        ELSE 0
                        END
            ),
            0
    ) AS saleable_stock,

    COALESCE(
            SUM(
                    CASE
                        WHEN pb.active = TRUE
                            AND pb.expiry_date < CURRENT_DATE
                            THEN pb.available_quantity
                        ELSE 0
                        END
            ),
            0
    ) AS expired_stock

FROM products p

         LEFT JOIN product_batches pb
                   ON pb.product_id = p.id

WHERE p.active = TRUE

GROUP BY
    p.id,
    p.name,
    p.minimum_stock;


-- ============================================================
-- LOW STOCK VIEW
-- Products whose saleable stock is at or below their minimum.
-- ============================================================

CREATE OR REPLACE VIEW v_low_stock_products AS
SELECT
    product_id,
    product_name,
    minimum_stock,
    saleable_stock

FROM v_product_stock

WHERE saleable_stock <= minimum_stock;