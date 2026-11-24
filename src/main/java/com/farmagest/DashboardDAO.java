package com.farmagest.dao;

import com.farmagest.config.DatabaseConnection;
import com.farmagest.exception.DatabaseException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class DashboardDAO {

    public record Notice(String product, String detail, String priority, LocalDate date) {}

    public record Summary(int products, int lowStock, int expiring, List<Notice> alerts) {}

    public Summary read() {
        String sql = """
                SELECT p.id, p.name, s.saleable_stock, s.minimum_stock,
                       b.batch_number, b.expiry_date, CURRENT_DATE AS today
                FROM products p
                JOIN v_product_stock s ON s.product_id = p.id
                LEFT JOIN product_batches b ON b.product_id = p.id
                  AND b.active = TRUE
                  AND b.available_quantity > 0
                  AND b.expiry_date BETWEEN CURRENT_DATE
                                        AND DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY)
                WHERE p.active = TRUE
                ORDER BY p.id, b.expiry_date, b.id
                """;

        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(sql);
             var rows = statement.executeQuery()) {

            int productCount = 0;
            int lowCount = 0;
            int expiryCount = 0;
            long previousProduct = -1;
            List<Notice> notices = new ArrayList<>();

            while (rows.next()) {
                long id = rows.getLong("id");
                String name = rows.getString("name");

                if (id != previousProduct) {
                    previousProduct = id;
                    productCount++;

                    int units = rows.getInt("saleable_stock");
                    if (units <= rows.getInt("minimum_stock")) {
                        lowCount++;
                        notices.add(new Notice(
                                name,
                                "Stock bajo · " + units + " uds.",
                                units == 0 ? "Alta" : "Media",
                                rows.getDate("today").toLocalDate()
                        ));
                    }
                }

                var expiry = rows.getDate("expiry_date");
                if (expiry != null) {
                    expiryCount++;
                    notices.add(new Notice(
                            name,
                            "Caducidad · lote " + rows.getString("batch_number"),
                            "Revisar",
                            expiry.toLocalDate()
                    ));
                }
            }

            return new Summary(productCount, lowCount, expiryCount, List.copyOf(notices));

        } catch (SQLException ex) {
            throw new DatabaseException("No se pudo cargar el resumen de la farmacia.", ex);
        }
    }
}