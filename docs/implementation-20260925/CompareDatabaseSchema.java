import java.sql.*;
import java.nio.file.*;
import java.util.*;

/** Read-only comparison of the original schema and the tested clone. Prints no user data. */
class CompareDatabaseSchema {
 public static void main(String[] args) throws Exception {
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
  try(var c=DriverManager.getConnection(p.getProperty("spring.datasource.url"),p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
   c.setReadOnly(true);String original=c.getCatalog(),clone="motorent_verify_20260926";
   var schemas=new LinkedHashMap<String,Map<String,String>>();
   for(String schema:List.of(original,clone)){
    var columns=new TreeMap<String,String>();
    try(var q=c.prepareStatement("SELECT table_name,column_name,column_type,is_nullable,column_default,extra FROM information_schema.columns WHERE table_schema=? ORDER BY table_name,ordinal_position")){
     q.setString(1,schema);try(var r=q.executeQuery()){while(r.next())columns.put(r.getString(1)+"."+r.getString(2),r.getString(3)+" | null="+r.getString(4)+" | default="+r.getString(5)+" | "+r.getString(6));}
    }schemas.put(schema,columns);
   }
   var old=schemas.get(original);var tested=schemas.get(clone);
   for(var e:tested.entrySet()){
    if(!old.containsKey(e.getKey()))System.out.println("ADD "+e.getKey()+" : "+e.getValue());
    else if(!old.get(e.getKey()).equals(e.getValue()))System.out.println("CHANGE "+e.getKey()+" : "+old.get(e.getKey())+" -> "+e.getValue());
   }
   for(String key:old.keySet())if(!tested.containsKey(key))System.out.println("MISSING IN CLONE "+key);
   System.out.println("Original schema inspected read-only: "+original);
   for(String sql:List.of(
    "SELECT COUNT(*) FROM motorbikes WHERE license_plate IS NULL OR TRIM(license_plate)=''",
    "SELECT COUNT(*) FROM motorbikes b WHERE status='RENTED' AND NOT EXISTS (SELECT 1 FROM rental_orders o WHERE o.motorbike_id=b.motorbike_id AND o.status IN ('HANDED_OVER','EXTENDED','OVERDUE','RETURN_REQUESTED','RETURNED'))",
    "SELECT COUNT(*) FROM rental_orders WHERE status='COMPLETED' AND actual_return_date IS NULL",
    "SELECT COUNT(*) FROM payments",
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='deposit_attempts'")){
     try(var q=c.createStatement();var r=q.executeQuery(sql)){r.next();System.out.println(sql+" => "+r.getLong(1));}
   }
  }
 }
}
