import java.sql.*;
import java.util.*;
import java.nio.file.*;

public class Phase5Fixture {
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || !List.of("create", "cleanup", "inspect", "simulate-paid").contains(args[0])) {
            throw new IllegalArgumentException("Usage: Phase5Fixture create|cleanup|inspect|simulate-paid [changeId]");
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
                    s.executeUpdate("UPDATE motorbikes SET status='AVAILABLE' WHERE motorbike_id=7");
                    s.executeUpdate("INSERT INTO rental_orders(order_code,customer_id,motorbike_id,pickup_date,return_date,days,total_price,rental_fee,daily_rate,deposit_due,deposit_paid,balance_paid,delivery_fee,pickup_fee,return_fee,security_deposit,pickup_type,return_type,status,created_at,updated_at) " +
                            "SELECT 'PHASE5-UI-20260929',user_id,9,DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 2 DAY),3,300000,300000,100000,150000,150000,150000,0,0,0,1000000,'STORE','STORE','HANDED_OVER',DATE_SUB(NOW(),INTERVAL 1 DAY),NOW() " +
                            "FROM users WHERE email='customer_ui_verify@example.test'");
                    s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code) " +
                            "SELECT order_id,150000,150000,'PAYOS','DEPOSIT','BOOKING_PAYMENT','IN','PAID',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),'PHASE5-BOOKING' " +
                            "FROM rental_orders WHERE order_code='PHASE5-UI-20260929'");
                    s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code) " +
                            "SELECT order_id,150000,150000,'CASH','BALANCE','RENTAL_REMAINING','IN','PAID',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),'PHASE5-REM' " +
                            "FROM rental_orders WHERE order_code='PHASE5-UI-20260929'");
                    s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code) " +
                            "SELECT order_id,1000000,1000000,'CASH','SECURITY','ASSET_DEPOSIT','IN','PAID',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),'PHASE5-SEC' " +
                            "FROM rental_orders WHERE order_code='PHASE5-UI-20260929'");
                    s.executeUpdate("INSERT INTO rental_contracts(order_id,contract_code,handed_over_at,handover_staff_id,created_at) " +
                            "SELECT order_id,'HD-PHASE5',DATE_SUB(NOW(),INTERVAL 1 DAY),(SELECT user_id FROM users WHERE email='staff_demo@motorent.vn'),NOW() " +
                            "FROM rental_orders WHERE order_code='PHASE5-UI-20260929'");
                }
                if (args[0].equals("simulate-paid") && args.length > 1) {
                    long changeId = Long.parseLong(args[1]);
                    try (var rs = s.executeQuery("SELECT order_id, new_planned_return_time, new_total, new_rental_fee, additional_payment, extra_rental_amount, vehicle_change_fee FROM rental_vehicle_changes WHERE id=" + changeId)) {
                        if (rs.next()) {
                            long orderId = rs.getLong(1);
                            Timestamp newReturn = rs.getTimestamp(2);
                            int newTotal = rs.getInt(3);
                            int newRentalFee = rs.getInt(4);
                            int addPayment = rs.getInt(5);
                            s.executeUpdate("UPDATE rental_vehicle_changes SET extension_start_time=DATE_SUB(NOW(),INTERVAL 1 SECOND), status='READY_FOR_EXCHANGE', financial_approved_at=NOW() WHERE id=" + changeId);
                            s.executeUpdate("UPDATE rental_orders SET return_date='" + newReturn + "', total_price=" + newTotal + ", rental_fee=" + newRentalFee + ", balance_paid=balance_paid + " + addPayment + ", status='EXTENDED' WHERE order_id=" + orderId);
                            s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code,source_type,source_id,idempotency_key) VALUES(" +
                                    orderId + "," + addPayment + "," + addPayment + ",'PAYOS','EXTENSION_PAYMENT','EXTENSION_PAYMENT','IN','PAID',NOW(),NOW(),'PAYOS-SIMULATED','DURING_RENTAL'," + changeId + ",'DURING_RENTAL:" + changeId + "')");
                            System.out.println("Simulated payment success for changeId=" + changeId);
                        }
                    }
                }
                if (args[0].equals("cleanup")) {
                    s.executeUpdate("DELETE n FROM notifications n JOIN rental_orders r ON n.link=CONCAT('/customer/rental-detail/',r.order_id) WHERE r.order_code='PHASE5-UI-20260929'");
                    s.executeUpdate("DELETE c FROM rental_contracts c JOIN rental_orders r ON c.order_id=r.order_id WHERE r.order_code='PHASE5-UI-20260929'");
                    s.executeUpdate("DELETE v FROM rental_vehicle_changes v JOIN rental_orders r ON v.order_id=r.order_id WHERE r.order_code='PHASE5-UI-20260929'");
                    s.executeUpdate("DELETE p FROM payments p JOIN rental_orders r ON p.order_id=r.order_id WHERE r.order_code='PHASE5-UI-20260929'");
                    s.executeUpdate("DELETE FROM rental_orders WHERE order_code='PHASE5-UI-20260929'");
                    s.executeUpdate("UPDATE motorbikes SET status='AVAILABLE' WHERE motorbike_id IN (7, 9)");
                }
                try (var r = s.executeQuery("SELECT order_id,status,total_price,deposit_paid,balance_paid,security_deposit,motorbike_id FROM rental_orders WHERE order_code='PHASE5-UI-20260929'")) {
                    while (r.next()) {
                        System.out.println("Phase5 UI order=" + r.getLong(1) + " status=" + r.getString(2) + " total=" + r.getInt(3) + " depPaid=" + r.getInt(4) + " balPaid=" + r.getInt(5) + " secDep=" + r.getInt(6) + " bike=" + r.getLong(7));
                    }
                }
                c.commit();
                System.out.println("Clone fixture " + args[0] + " done");
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }
}
