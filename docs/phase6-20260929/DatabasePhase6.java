import java.sql.*;
import java.nio.file.*;
import java.util.*;

/** Offline migration utility for Phase 6: never logs credentials or customer records. */
class DatabasePhase6 {
 static Properties settings() throws Exception {
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}return p;
 }
 public static void main(String[] args)throws Exception {
  if(args.length!=2 || !List.of("backup","migrate","inspect").contains(args[0]) || !List.of("motorent_db","motorent_verify_20260926").contains(args[1]))throw new IllegalArgumentException("mode and allowed database required");
  var p=settings();String db=args[1];String url=p.getProperty("spring.datasource.url").replaceAll("(?<=3306/)[^?]+",db);
  try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
   Path backup=Path.of("docs/phase6-20260929/before-"+db+".sql");
   if(args[0].equals("backup")){
    c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
    var names=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("SHOW TABLES")){while(r.next())names.add(r.getString(1));}
    try(var w=Files.newBufferedWriter(backup,java.nio.charset.StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
     w.write("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n");
     for(String n:names){try(var s=c.createStatement();var r=s.executeQuery("SHOW CREATE TABLE `"+n+"`")){r.next();w.write(r.getString(2)+";\n");}
      try(var s=c.createStatement();var r=s.executeQuery("SELECT * FROM `"+n+"`")){var m=r.getMetaData();while(r.next()){w.write("INSERT INTO `"+n+"` VALUES (");for(int i=1;i<=m.getColumnCount();i++){if(i>1)w.write(",");byte[] b=r.getBytes(i);w.write(b==null?"NULL":"X'"+HexFormat.of().formatHex(b)+"'");}w.write(");\n");}}
     }w.write("SET FOREIGN_KEY_CHECKS=1;\n");
    }c.commit();System.out.println(db+" backup bytes="+Files.size(backup));
   }
   if(args[0].equals("migrate")){
    if(!Files.exists(backup)||Files.size(backup)==0)throw new IllegalStateException("Backup required");
    String sql=Files.readString(Path.of("docs/phase6-20260929/V006__early_return_refund.sql"));
    long before;
    try(var s=c.createStatement();var r=s.executeQuery("SELECT COALESCE(SUM(CASE WHEN direction='OUT' THEN -ABS(amount) ELSE amount END),0) FROM payments")){r.next();before=r.getLong(1);}
    try(var s=c.createStatement()){for(String part:sql.split(";")){
     part=part.trim();if(part.isBlank())continue;
     if(part.startsWith("ALTER TABLE")){var words=part.split("\\s+");try(var r=c.getMetaData().getColumns(c.getCatalog(),null,words[2],words[5])){if(r.next())continue;}}
     s.execute(part);
    }}
    try(var s=c.createStatement();var r=s.executeQuery("SELECT COALESCE(SUM(CASE WHEN direction='OUT' THEN -amount ELSE amount END),0) FROM payments")){r.next();if(r.getLong(1)!=before)throw new IllegalStateException("Signed total changed");System.out.println("Signed total preserved="+before);}
    System.out.println(db+" migrated");
   }
   try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*),COALESCE(SUM(amount),0),COALESCE(SUM(received_amount),0) FROM payments")){r.next();System.out.println(db+" payment count="+r.getLong(1)+" amounts="+r.getLong(2)+" received="+r.getLong(3));}
   try(var s=c.createStatement();var r=s.executeQuery("SELECT purpose,status,COUNT(*) FROM payments GROUP BY purpose,status")){while(r.next())System.out.println("payment group="+r.getString(1)+"/"+r.getString(2)+" count="+r.getLong(3));}
   try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM rental_orders")){r.next();System.out.println(db+" orders="+r.getLong(1));}
  }
 }
}
