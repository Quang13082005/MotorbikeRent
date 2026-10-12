import java.sql.*;
import java.util.*;
import java.nio.file.*;

public class Phase7Fixture {
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || !List.of("create", "cleanup", "inspect", "verify-baseline").contains(args[0])) {
            throw new IllegalArgumentException("Usage: Phase7Fixture create|cleanup|inspect|verify-baseline");
        }
        var p = new Properties();
        try (var r = Files.newBufferedReader(Path.of("src/main/resources/application.properties"))) {
            p.load(r);
        }
        String url = p.getProperty("spring.datasource.url").replaceAll("(?<=3306/)[^?]+", "motorent_verify_20260926");
        if (!url.contains("/motorent_verify_20260926")) {
            throw new IllegalStateException("Clone database only");
        }
        if (!url.contains("allowPublicKeyRetrieval=true")) {
            url += (url.contains("?") ? "&" : "?") + "allowPublicKeyRetrieval=true";
        }
        try (var c = DriverManager.getConnection(url, p.getProperty("spring.datasource.username"), p.getProperty("spring.datasource.password"))) {
            c.setAutoCommit(false);
            try (var s = c.createStatement()) {
                if (args[0].equals("create")) {
                    // Clean previous fixture if any
                    cleanup(s);

                    s.executeUpdate("UPDATE motorbikes SET status='RENTED' WHERE motorbike_id=9");
                    s.executeUpdate("INSERT INTO rental_orders(order_code,customer_id,motorbike_id,pickup_date,return_date,days,total_price,rental_fee,daily_rate,deposit_due,deposit_paid,balance_paid,delivery_fee,pickup_fee,return_fee,security_deposit,pickup_type,return_type,status,created_at,updated_at) " +
                            "SELECT 'PHASE7-UI-20260929',user_id,9,DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),2,200000,200000,100000,100000,100000,100000,0,0,0,2000000,'STORE','STORE','HANDED_OVER',DATE_SUB(NOW(),INTERVAL 3 DAY),NOW() " +
                            "FROM users WHERE email='customer_ui_verify@example.test'");
                    s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code) " +
                            "SELECT order_id,100000,100000,'PAYOS','DEPOSIT','BOOKING_PAYMENT','IN','PAID',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),'PHASE7-BOOKING' " +
                            "FROM rental_orders WHERE order_code='PHASE7-UI-20260929'");
                    s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code) " +
                            "SELECT order_id,100000,100000,'CASH','BALANCE','RENTAL_REMAINING','IN','PAID',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),'PHASE7-REM' " +
                            "FROM rental_orders WHERE order_code='PHASE7-UI-20260929'");
                    s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code) " +
                            "SELECT order_id,2000000,2000000,'CASH','SECURITY','ASSET_DEPOSIT','IN','PAID',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),'PHASE7-SEC' " +
                            "FROM rental_orders WHERE order_code='PHASE7-UI-20260929'");
                    s.executeUpdate("INSERT INTO rental_contracts(order_id,contract_code,handed_over_at,handover_staff_id,created_at) " +
                            "SELECT order_id,'HD-PHASE7',DATE_SUB(NOW(),INTERVAL 3 DAY),(SELECT user_id FROM users WHERE email='staff_demo@motorent.vn'),NOW() " +
                            "FROM rental_orders WHERE order_code='PHASE7-UI-20260929'");
                    c.commit();

                    try (var rs = s.executeQuery("SELECT order_id FROM rental_orders WHERE order_code='PHASE7-UI-20260929'")) {
                        if (rs.next()) {
                            System.out.println("Phase 7 fixture created successfully. OrderId=" + rs.getLong(1));
                        }
                    }
                }
                if (args[0].equals("cleanup")) {
                    cleanup(s);
                    c.commit();
                    System.out.println("Phase 7 fixture cleaned up successfully.");
                }
                if (args[0].equals("verify-baseline")) {
                    long orderCount, orderTotal;
                    try (ResultSet rsOrders = s.executeQuery("SELECT COUNT(*), COALESCE(SUM(total_price), 0) FROM rental_orders")) {
                        rsOrders.next();
                        orderCount = rsOrders.getLong(1);
                        orderTotal = rsOrders.getLong(2);
                    }
                    long paymentCount, paymentTotal;
                    try (ResultSet rsPayments = s.executeQuery("SELECT COUNT(*), COALESCE(SUM(amount), 0) FROM payments WHERE status = 'PAID'")) {
                        rsPayments.next();
                        paymentCount = rsPayments.getLong(1);
                        paymentTotal = rsPayments.getLong(2);
                    }

                    System.out.println("BASELINE CHECK: orders=" + orderCount + " (total=" + orderTotal + "), payments=" + paymentCount + " (total=" + paymentTotal + ")");
                    if (orderCount != 2 || paymentCount != 2 || paymentTotal != 110000) {
                        System.err.println("WARNING: Baseline mismatch! Expected 2 orders, 2 payments = 110,000 VND.");
                    } else {
                        System.out.println("BASELINE VERIFIED: Exactly 2 orders, 2 payments = 110,000 VND preserved.");
                    }
                }
            }
        }
    }

    private static void cleanup(Statement s) throws SQLException {
        s.executeUpdate("DELETE r FROM reviews r JOIN rental_orders o ON r.order_id=o.order_id WHERE o.order_code='PHASE7-UI-20260929'");
        s.executeUpdate("DELETE n FROM notifications n JOIN rental_orders o ON n.link=CONCAT('/customer/rental-detail/',o.order_id) WHERE o.order_code='PHASE7-UI-20260929'");
        s.executeUpdate("DELETE c FROM rental_contracts c JOIN rental_orders o ON c.order_id=o.order_id WHERE o.order_code='PHASE7-UI-20260929'");
        s.executeUpdate("DELETE v FROM rental_vehicle_changes v JOIN rental_orders o ON v.order_id=o.order_id WHERE o.order_code='PHASE7-UI-20260929'");
        s.executeUpdate("DELETE p FROM payments p JOIN rental_orders o ON p.order_id=o.order_id WHERE o.order_code='PHASE7-UI-20260929'");
        s.executeUpdate("DELETE FROM rental_orders WHERE order_code='PHASE7-UI-20260929'");
        s.executeUpdate("UPDATE motorbikes SET status='AVAILABLE' WHERE motorbike_id=9");
    }
}
