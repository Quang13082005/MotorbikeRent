import java.sql.*;import java.util.*;import java.nio.file.*;
class Phase2Fixture {
 public static void main(String[] args)throws Exception{
  if(args.length!=1||!List.of("create","cleanup","inspect").contains(args[0]))throw new IllegalArgumentException();
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
  String url=p.getProperty("spring.datasource.url").replaceAll("(?<=3306/)[^?]+","motorent_verify_20260926");
  if(!url.contains("/motorent_verify_20260926?"))throw new IllegalStateException();
  try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){c.setAutoCommit(false);try(var s=c.createStatement()){
   if(args[0].equals("create")){
    s.executeUpdate("INSERT INTO rental_orders(order_code,customer_id,motorbike_id,pickup_date,return_date,days,total_price,rental_fee,daily_rate,deposit_due,deposit_paid,balance_paid,delivery_fee,pickup_fee,return_fee,security_deposit,pickup_type,return_type,status,created_at,updated_at) SELECT 'PHASE2-UI-20260929',user_id,9,DATE_ADD(NOW(),INTERVAL 80 DAY),DATE_ADD(NOW(),INTERVAL 82 DAY),2,200000,200000,100000,100000,100000,0,0,0,0,1000000,'STORE','STORE','PENDING',NOW(),NOW() FROM users WHERE email='customer_ui_verify@example.test'");
    s.executeUpdate("INSERT INTO payments(order_id,amount,received_amount,payment_method,purpose,transaction_type,direction,status,created_at,paid_at,transaction_code) SELECT order_id,100000,100000,'CASH','DEPOSIT','BOOKING_PAYMENT','IN','PAID',NOW(),NOW(),'PHASE2-UI-BOOKING-20260929' FROM rental_orders WHERE order_code='PHASE2-UI-20260929'");
   }
   if(args[0].equals("cleanup")){
    s.executeUpdate("DELETE n FROM notifications n JOIN rental_orders r ON n.link=CONCAT('/customer/rental-detail/',r.order_id) WHERE r.order_code='PHASE2-UI-20260929'");
    s.executeUpdate("DELETE c FROM rental_vehicle_changes c JOIN rental_orders r ON c.order_id=r.order_id WHERE r.order_code='PHASE2-UI-20260929'");
    s.executeUpdate("DELETE p FROM payments p JOIN rental_orders r ON p.order_id=r.order_id WHERE r.order_code='PHASE2-UI-20260929'");
    s.executeUpdate("DELETE FROM rental_orders WHERE order_code='PHASE2-UI-20260929'");
   }
   try(var r=s.executeQuery("SELECT order_id,status,total_price,deposit_paid,motorbike_id FROM rental_orders WHERE order_code='PHASE2-UI-20260929'")){while(r.next())System.out.println("UI order="+r.getLong(1)+" status="+r.getString(2)+" total="+r.getInt(3)+" paid="+r.getInt(4)+" bike="+r.getLong(5));}
   c.commit();System.out.println("Clone fixture "+args[0]+" done");
  }catch(Exception e){c.rollback();throw e;}}
 }
}

