import java.sql.*;
import java.nio.file.*;
import java.util.*;

/** Only for the demo data explicitly confirmed by the owner on 26 September 2026. */
class NormalizeDemoDatabase {
 static final String CLONE="motorent_verify_20260926";
 static final Path BACKUP=Path.of("docs/implementation-20260925/database-before-normalization-20260926.sql");
 static long scalar(Connection c,String sql) throws SQLException {try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void update(Connection c,String label,String sql) throws SQLException {try(var s=c.createStatement()){System.out.println(label+"="+s.executeUpdate(sql));}}
 static Map<String,String> columns(Connection c,String schema) throws SQLException {
  var m=new LinkedHashMap<String,String>();try(var s=c.prepareStatement("SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE FROM information_schema.columns WHERE table_schema=? ORDER BY table_name,ordinal_position")){
   s.setString(1,schema);try(var r=s.executeQuery()){while(r.next())m.put(r.getString(1)+"."+r.getString(2),r.getString(3)+"|"+r.getString(4));}}
  return m;
 }
 static void schema(Connection c,String target) throws Exception {
  var current=columns(c,target);var tested=columns(c,CLONE);var ddl=new ArrayList<String>();
  for(var e:tested.entrySet()){
   if(current.containsKey(e.getKey())){if(!current.get(e.getKey()).equals(e.getValue()))throw new IllegalStateException("Unexpected changed column: "+e.getKey());continue;}
   var key=e.getKey().split("\\.");var definition=e.getValue().split("\\|");
   if(!Set.of("payments","rental_orders","shop_settings","users").contains(key[0]) || !key[1].matches("[a-z_]+") || !definition[0].matches("(int|bigint|double|text|date|datetime\\(6\\)|bit\\(1\\)|varchar\\([0-9]+\\))") || !definition[1].equals("YES"))throw new IllegalStateException("Unreviewed schema change "+e.getKey());
   ddl.add("ALTER TABLE `"+key[0]+"` ADD COLUMN `"+key[1]+"` "+definition[0]+" NULL");
  }
  Files.write(Path.of("docs/implementation-20260925/additive-migration-"+target+".sql"),ddl.stream().map(s->s+";").toList());
  for(String sql:ddl){try(var s=c.createStatement()){s.executeUpdate(sql);}}
  // Match the tested entity's unique provider reference without guessing generated index names.
  if(scalar(c,"SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='payments' AND column_name='provider_code' AND non_unique=0")==0){
   try(var s=c.createStatement()){s.executeUpdate("ALTER TABLE payments ADD UNIQUE INDEX uk_payment_provider_code (provider_code)");}
  }
  System.out.println("ADDED_COLUMNS="+ddl.size());
 }
 static void samplePayment(Connection c,long id,int amount,String purpose,String reference,Timestamp at) throws SQLException {
  try(var s=c.prepareStatement("INSERT INTO payments (order_id,amount,received_amount,purpose,payment_method,status,transaction_code,paid_at,created_at) SELECT ?,?,?,?,'CASH','PAID',?,?,CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM payments WHERE transaction_code=?)")){
   s.setLong(1,id);s.setInt(2,amount);s.setInt(3,amount);s.setString(4,purpose);s.setString(5,reference);s.setTimestamp(6,at);s.setString(7,reference);s.executeUpdate();
  }
 }
 public static void main(String[] args) throws Exception {
  if(args.length!=2 || !args[1].equals("--apply"))throw new IllegalArgumentException("Usage: NormalizeDemoDatabase <motorent_db|motorent_verify_20260926> --apply");
  String target=args[0];if(!Set.of("motorent_db",CLONE).contains(target))throw new IllegalArgumentException("Unsupported target");
  if(!Files.exists(BACKUP)||Files.size(BACKUP)<1000||!Files.readString(BACKUP).endsWith("SET FOREIGN_KEY_CHECKS=1;\n"))throw new IllegalStateException("A complete original backup is required");
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
  String url=p.getProperty("spring.datasource.url");if(!url.startsWith("jdbc:mysql://localhost:")&&!url.startsWith("jdbc:mysql://127.0.0.1:"))throw new IllegalStateException("Local database only");
  url=url.replaceFirst("/(motorent_db)(?=[?]|$)","/"+target);
  try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
   if(!target.equals(c.getCatalog()))throw new IllegalStateException("Wrong catalog");
   if(scalar(c,"SELECT COUNT(*) FROM rental_orders WHERE status NOT IN ('COMPLETED','CANCELLED','REJECTED')")!=0)throw new IllegalStateException("Active orders appeared; refusing normalization");
   long bikes=scalar(c,"SELECT COUNT(*) FROM motorbikes"),orders=scalar(c,"SELECT COUNT(*) FROM rental_orders");
   schema(c,target); // MySQL DDL auto-commits; additive only, never drops or changes old columns.
   c.setAutoCommit(false);
   try{
    update(c,"DEMO_PLATES_ASSIGNED","UPDATE motorbikes SET license_plate=CONCAT('DEMO-MR',LPAD(motorbike_id,5,'0')) WHERE license_plate IS NULL OR TRIM(license_plate)=''");
    update(c,"RENTED_BIKES_RESET","UPDATE motorbikes SET status='AVAILABLE' WHERE status='RENTED' AND NOT EXISTS (SELECT 1 FROM rental_orders o WHERE o.motorbike_id=motorbikes.motorbike_id AND o.status NOT IN ('COMPLETED','CANCELLED','REJECTED'))");
    update(c,"DEMO_RETURN_DATES_FILLED","UPDATE rental_orders SET actual_return_date=return_date WHERE status='COMPLETED' AND actual_return_date IS NULL");
    update(c,"DEMO_ORDERS_LABELLED","UPDATE rental_orders SET note=CONCAT(COALESCE(note,''),' [DEMO-NORMALIZED-20260926: dữ liệu thử; ngày trả và khoản thu bổ sung chỉ dùng kiểm thử]') WHERE note IS NULL OR note NOT LIKE '%DEMO-NORMALIZED-20260926%'");
    try(var s=c.createStatement();var r=s.executeQuery("SELECT order_id,total_price,COALESCE(deposit_paid,0),status,COALESCE(actual_return_date,return_date) FROM rental_orders")){
     while(r.next()){
      long id=r.getLong(1);int total=r.getInt(2),deposit=r.getInt(3);String status=r.getString(4);Timestamp at=r.getTimestamp(5);
      if(deposit<0||deposit>total)throw new IllegalStateException("Unexpected demo amount for order "+id);
      if(scalar(c,"SELECT COUNT(*) FROM payments WHERE order_id="+id+" AND (transaction_code IS NULL OR transaction_code NOT LIKE 'DEMO-NORM-%')")>0)throw new IllegalStateException("Non-normalization payment exists; preserve it for review");
      if(deposit>0)samplePayment(c,id,deposit,"DEPOSIT","DEMO-NORM-"+id+"-DEPOSIT",at);
      if(status.equals("COMPLETED")){
       int balance=total-deposit;if(balance>0)samplePayment(c,id,balance,"BALANCE","DEMO-NORM-"+id+"-BALANCE",at);
       try(var u=c.prepareStatement("UPDATE rental_orders SET balance_paid=? WHERE order_id=?")){u.setInt(1,balance);u.setLong(2,id);u.executeUpdate();}
      }else if(deposit>0)samplePayment(c,id,-deposit,"DEPOSIT_REFUND","DEMO-NORM-"+id+"-REFUND",at);
     }
    }
    if(scalar(c,"SELECT COUNT(*) FROM motorbikes")!=bikes||scalar(c,"SELECT COUNT(*) FROM rental_orders")!=orders)throw new IllegalStateException("Unexpected row-count change");
    if(scalar(c,"SELECT COUNT(*) FROM rental_orders o WHERE (SELECT COALESCE(SUM(received_amount),0) FROM payments p WHERE p.order_id=o.order_id) <> CASE WHEN o.status='COMPLETED' THEN o.total_price ELSE 0 END")!=0)throw new IllegalStateException("Demo bookkeeping failed reconciliation");
    c.commit();System.out.println("NORMALIZATION_COMMITTED database="+target+" bikes="+bikes+" orders="+orders+" samplePayments="+scalar(c,"SELECT COUNT(*) FROM payments WHERE transaction_code LIKE 'DEMO-NORM-%'"));
   }catch(Exception ex){c.rollback();throw ex;}
  }
 }
}
