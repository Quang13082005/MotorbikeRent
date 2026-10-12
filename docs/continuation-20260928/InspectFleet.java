import java.sql.*;import java.nio.file.*;import java.util.*;
class InspectFleet {
 public static void main(String[] args)throws Exception{
 var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
 for(String db:List.of("motorent_db","motorent_verify_20260926")){
 String url=p.getProperty("spring.datasource.url");url=url.replaceAll("(?<=3306/)[^?]+",db);
 try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"));var s=c.createStatement()){
 System.out.println("DATABASE="+c.getCatalog());
 for(String q:List.of("select count(*) as electric_orders from rental_orders where motorbike_id=16", "select count(*) as electric_reviews from reviews where motorbike_id=16", "select count(*) as electric_maintenance from maintenance_records where motorbike_id=16","select motorbike_id,name,category_id,cc from motorbikes","select TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME from information_schema.KEY_COLUMN_USAGE where TABLE_SCHEMA=DATABASE() and REFERENCED_TABLE_NAME in ('motorbikes','rental_orders','motorbike_categories','motorbike_brands')"))try(var r=s.executeQuery(q)){while(r.next()){for(int i=1;i<=r.getMetaData().getColumnCount();i++)System.out.print(r.getString(i)+" | ");System.out.println();}}
 }
 }
 }
}
