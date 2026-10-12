import java.sql.*;import java.nio.file.*;import java.util.*;
class UpdateFleetAndHandover {
 public static void main(String[] args)throws Exception{
 var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
 for(String db:List.of("motorent_verify_20260926","motorent_db")){
 String url=p.getProperty("spring.datasource.url").replaceAll("(?<=3306/)[^?]+",db);
 try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
 var names=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("SHOW TABLES")){while(r.next())names.add(r.getString(1));}
 Path backup=Path.of("docs/continuation-20260928/database-before-handover-"+db+".sql");
 c.setAutoCommit(false);
 try(var w=Files.newBufferedWriter(backup,java.nio.charset.StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
 w.write("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n");
 for(String n:names){try(var s=c.createStatement();var r=s.executeQuery("SHOW CREATE TABLE `"+n+"`")){r.next();w.write(r.getString(2)+";\n");}
 try(var s=c.createStatement();var r=s.executeQuery("SELECT * FROM `"+n+"`")){var m=r.getMetaData();while(r.next()){w.write("INSERT INTO `"+n+"` VALUES (");for(int i=1;i<=m.getColumnCount();i++){if(i>1)w.write(",");byte[] b=r.getBytes(i);w.write(b==null?"NULL":"X'"+HexFormat.of().formatHex(b)+"'");}w.write(");\n");}}}
 w.write("SET FOREIGN_KEY_CHECKS=1;\n");}c.commit();
 System.out.println(db+" BACKUP_COMPLETE "+Files.size(backup));
 try(var s=c.createStatement()){
 try(var r=s.executeQuery("SELECT name,cc,category_id,brand_id FROM motorbikes WHERE motorbike_id=16 FOR UPDATE")){if(!r.next()||!"VinFast Feliz S".equals(r.getString(1))||r.getInt(2)!=0||r.getInt(3)!=4)throw new IllegalStateException("Unexpected fleet data; no deletion");}
 for(String t:List.of("rental_orders","reviews","maintenance_records"))try(var r=s.executeQuery("SELECT COUNT(*) FROM "+t+" WHERE motorbike_id=16")){r.next();if(r.getInt(1)>0)throw new IllegalStateException("Linked history; no deletion");}
 s.executeUpdate("DELETE FROM motorbike_images WHERE motorbike_id=16");
 s.executeUpdate("DELETE FROM motorbikes WHERE motorbike_id=16 AND cc=0 AND category_id=4");
 s.executeUpdate("DELETE FROM motorbike_categories WHERE category_id=4 AND NOT EXISTS (SELECT 1 FROM motorbikes WHERE category_id=4)");
 s.executeUpdate("DELETE FROM motorbike_brands WHERE name='VinFast' AND brand_id NOT IN (SELECT brand_id FROM motorbikes WHERE brand_id IS NOT NULL)");c.commit();
 s.executeUpdate("ALTER TABLE rental_contracts ADD COLUMN identity_checked bit NULL, ADD COLUMN payment_received bit NULL, ADD COLUMN handed_over_at datetime(6) NULL, ADD COLUMN handover_staff_id varchar(255) NULL");
 try(var r=s.executeQuery("SELECT COUNT(*) FROM motorbikes WHERE cc=0 OR category_id=4")){r.next();if(r.getInt(1)!=0)throw new IllegalStateException("Electric fleet remains");}
 System.out.println(db+" MIGRATION_COMPLETE electric=0 audit_columns=4");
 }
 }
 }
 }
}
