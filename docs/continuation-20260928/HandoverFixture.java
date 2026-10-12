import java.sql.*;import java.nio.file.*;import java.util.*;
class HandoverFixture {
 public static void main(String[] args)throws Exception{
 var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
 String url=p.getProperty("spring.datasource.url").replaceAll("(?<=3306/)[^?]+","motorent_verify_20260926");
 try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"));var s=c.createStatement()){
 if(args.length==0){
 s.executeUpdate("INSERT INTO rental_orders(order_code,customer_id,motorbike_id,pickup_date,return_date,days,total_price,deposit_paid,balance_paid,security_deposit,status,pickup_type,return_type,created_at,updated_at) SELECT 'UI-HANDOVER-20260928',user_id,9,NOW()-INTERVAL 1 HOUR,NOW()+INTERVAL 1 DAY,1,100000,50000,0,1000000,'READY_FOR_HANDOVER','STORE','STORE',NOW(),NOW() FROM users WHERE username='customer_demo'");
 }else if("cleanup".equals(args[0])){
 c.setAutoCommit(false);
 for(String table:List.of("payments","rental_contracts"))s.executeUpdate("DELETE FROM "+table+" WHERE order_id IN (SELECT order_id FROM rental_orders WHERE order_code='UI-HANDOVER-20260928')");
 s.executeUpdate("DELETE FROM notifications WHERE title='Cập nhật đơn UI-HANDOVER-20260928'");
 s.executeUpdate("DELETE FROM rental_orders WHERE order_code='UI-HANDOVER-20260928'");
 s.executeUpdate("UPDATE motorbikes SET status='AVAILABLE' WHERE motorbike_id=9 AND status='RENTED' AND NOT EXISTS (SELECT 1 FROM rental_orders WHERE motorbike_id=9 AND status IN ('HANDED_OVER','EXTENDED','OVERDUE','RETURN_REQUESTED','RETURNED'))");
 c.commit();System.out.println("CLONE_FIXTURE_CLEANED");
 }else{
 try(var r=s.executeQuery("SELECT o.status,o.balance_paid,c.identity_checked,c.payment_received,c.handed_over_at,c.handover_staff_id,(SELECT SUM(p.amount) FROM payments p WHERE p.order_id=o.order_id AND p.payment_method='BANK_TRANSFER') FROM rental_orders o LEFT JOIN rental_contracts c ON c.order_id=o.order_id WHERE o.order_code='UI-HANDOVER-20260928'")){while(r.next())System.out.println("status="+r.getString(1)+" balance="+r.getInt(2)+" identity="+r.getBoolean(3)+" payment="+r.getBoolean(4)+" audit="+(r.getTimestamp(5)!=null&&r.getString(6)!=null)+" transfer="+r.getInt(7));}
 }
 }
 }
}

