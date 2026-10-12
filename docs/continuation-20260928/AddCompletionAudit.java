import java.sql.*;import java.nio.file.*;import java.util.*;
class AddCompletionAudit {
 public static void main(String[] args)throws Exception{
 var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
 for(String db:List.of("motorent_verify_20260926","motorent_db")){
 String url=p.getProperty("spring.datasource.url").replaceAll("(?<=3306/)[^?]+",db);
 try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
 var names=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("SHOW TABLES")){while(r.next())names.add(r.getString(1));}
 Path backup=Path.of("docs/continuation-20260928/database-before-simple-staff-"+db+".sql");
 c.setAutoCommit(false);
 try(var w=Files.newBufferedWriter(backup,java.nio.charset.StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
 w.write("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n");
 for(String n:names){try(var s=c.createStatement();var r=s.executeQuery("SHOW CREATE TABLE `"+n+"`")){r.next();w.write(r.getString(2)+";\n");}
 try(var s=c.createStatement();var r=s.executeQuery("SELECT * FROM `"+n+"`")){var m=r.getMetaData();while(r.next()){w.write("INSERT INTO `"+n+"` VALUES (");for(int i=1;i<=m.getColumnCount();i++){if(i>1)w.write(",");byte[] b=r.getBytes(i);w.write(b==null?"NULL":"X'"+HexFormat.of().formatHex(b)+"'");}w.write(");\n");}}}
 w.write("SET FOREIGN_KEY_CHECKS=1;\n");}c.commit();
 System.out.println(db+" BACKUP_COMPLETE "+Files.size(backup));
 try(var s=c.createStatement()){
 for(String column:List.of("completed_at","completion_staff_id")){
 boolean exists;try(var r=c.getMetaData().getColumns(c.getCatalog(),null,"rental_contracts",column)){exists=r.next();}
 if(!exists)s.executeUpdate("ALTER TABLE rental_contracts ADD COLUMN "+column+(column.equals("completed_at")?" datetime(6) NULL":" varchar(255) NULL"));
 }
 System.out.println(db+" COMPLETION_AUDIT_READY");
 }
 }
 }
 }
}
