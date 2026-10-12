import java.sql.*;
import java.util.*;
import java.nio.file.*;

public class Phase6Fixture {
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || !List.of("create", "cleanup", "inspect").contains(args[0])) {
            throw new IllegalArgumentException("Usage: Phase6Fixture create|cleanup|inspect");
        }
        var p = new Properties();
        try (var r = Files.newBufferedReader(Path.of("src/main/resources/application.properties"))) {
            p.load(r);
        }
        String url = p.getProperty("spring.datasource.url").replaceAll("(?<=3306/)[^?]+", "motorent_verify_20260926");
        if (!url.contains("/motorent_verify_20260926?")) {
            throw new IllegalStateException("Clone database only");
        }
        try (var c = DriverManager.getConnection(url, p.getProperty("spring.datasource.username"), p.getProperty("spring.datasource.password"))) {
            c.setAutoCommit(false);
            try (var s = c.createStatement()) {
                if (args[0].equals("create")) {
                    s.executeUpdate("UPDATE motorbikes SET status='RENTED' WHERE motorbike_id=9");
                    s.executeUpdate("INSERT INTO rental_orders(order_code,customer_id,motorbike_id,pickup_date,return_date,days,total_price,rental_fee,daily_rate,deposit_due,deposit_paid,balance_paid,delivery_fee,pickup_fee,return_fee,security_deposit,pickup_type,return_type,status,created_at,updated_at) " +
                            "SELECT 'PHASE6-UI-20260929',user_id,9,DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 2 DAY),3,300000,300000,100000,150000,150000,150000,0,0,0,1000000,'STORE','STORE','HANDED_OVER',DATE_SUB(NOW(),INTERVAL 1 DAY),NOW() " +
                            "FROM users WHERE email='customer_ui_verify@example.test'");
                    s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code) " +
                            "SELECT order_id,150000,150000,'PAYOS','DEPOSIT','BOOKING_PAYMENT','IN','PAID',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),'PHASE6-BOOKING' " +
                            "FROM rental_orders WHERE order_code='PHASE6-UI-20260929'");
                    s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code) " +
                            "SELECT order_id,150000,150000,'CASH','BALANCE','RENTAL_REMAINING','IN','PAID',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),'PHASE6-REM' " +
                            "FROM rental_orders WHERE order_code='PHASE6-UI-20260929'");
                    s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code) " +
                            "SELECT order_id,1000000,1000000,'CASH','SECURITY','ASSET_DEPOSIT','IN','PAID',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),'PHASE6-SEC' " +
                            "FROM rental_orders WHERE order_code='PHASE6-UI-20260929'");
                    s.executeUpdate("INSERT INTO rental_contracts(order_id,contract_code,handed_over_at,handover_staff_id,created_at) " +
                            "SELECT order_id,'HD-PHASE6',DATE_SUB(NOW(),INTERVAL 1 DAY),(SELECT user_id FROM users WHERE email='staff_demo@motorent.vn'),NOW() " +
                            "FROM rental_orders WHERE order_code='PHASE6-UI-20260929'");
                    c.commit();
                    System.out.println("Phase 6 fixture created successfully.");
                }
                if (args[0].equals("cleanup")) {
                    s.executeUpdate("DELETE n FROM notifications n JOIN rental_orders r ON n.link=CONCAT('/customer/rental-detail/',r.order_id) WHERE r.order_code='PHASE6-UI-20260929'");
                    s.executeUpdate("DELETE c FROM rental_contracts c JOIN rental_orders r ON c.order_id=r.order_id WHERE r.order_code='PHASE6-UI-20260929'");
                    s.executeUpdate("DELETE v FROM rental_vehicle_changes v JOIN rental_orders r ON v.order_id=r.order_id WHERE r.order_code='PHASE6-UI-20260929'");
                    s.executeUpdate("DELETE p FROM payments p JOIN rental_orders r ON p.order_id=r.order_id WHERE r.order_code='PHASE6-UI-20260929'");
                    s.executeUpdate("DELETE FROM rental_orders WHERE order_code='PHASE6-UI-20260929'");
                    s.executeUpdate("UPDATE motorbikes SET status='AVAILABLE' WHERE motorbike_id=9");
                    c.commit();
                    System.out.println("Phase 6 fixture cleaned up successfully.");
                }
                if (args[0].equals("inspect")) {
                    try (var rs = s.executeQuery("SELECT r.order_id, r.order_code, r.status, r.total_price, m.status, c.early_return_refund_amount, c.early_return_reason FROM rental_orders r JOIN motorbikes m ON r.motorbike_id=m.motorbike_id LEFT JOIN rental_contracts c ON r.order_id=c.order_id WHERE r.order_code='PHASE6-UI-20260929'")) {
                        if (rs.next()) {
                            System.out.println("Order: id=" + rs.getLong(1) + ", code=" + rs.getString(2) + ", status=" + rs.getString(3) + ", total=" + rs.getInt(4) + ", bikeStatus=" + rs.getString(5) + ", refund=" + rs.getInt(6) + ", reason=" + rs.getString(7));
                        } else {
                            System.out.println("No Phase 6 order found.");
                        }
                    }
                    try (var rs = s.executeQuery("SELECT p.payment_id, p.amount, p.direction, p.transaction_type, p.status, p.payment_method FROM payments p JOIN rental_orders r ON p.order_id=r.order_id WHERE r.order_code='PHASE6-UI-20260929' ORDER BY p.payment_id")) {
                        while (rs.next()) {
                            System.out.println("Payment: id=" + rs.getLong(1) + ", amount=" + rs.getInt(2) + ", dir=" + rs.getString(3) + ", type=" + rs.getString(4) + ", status=" + rs.getString(5) + ", method=" + rs.getString(6));
                        }
                    }
                }
            }
        }
    }
}
