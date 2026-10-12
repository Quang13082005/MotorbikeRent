import java.sql.*; import java.nio.file.*; import java.io.*; import java.util.*;
class BackupDatabase {
 public static void main(String[] args) throws Exception {
  var p=new Properties(); try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
  try(var c=DriverManager.getConnection(p.getProperty("spring.datasource.url"),p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
   c.setReadOnly(true); c.setAutoCommit(false);
   var names=new ArrayList<String>(); try(var s=c.createStatement(); var r=s.executeQuery("SHOW TABLES")){while(r.next())names.add(r.getString(1));}
   Path backup=Path.of("docs/implementation-20260925/"+(args.length==0?"database-before.sql":args[0]));
   if(args.length>0 && !args[0].matches("database-[a-zA-Z0-9-]+\\.sql"))throw new IllegalArgumentException("Invalid backup filename");
   try(var w=Files.newBufferedWriter(backup,java.nio.charset.StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
    w.write("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n");
    for(String n:names){
     try(var s=c.createStatement();var r=s.executeQuery("SHOW CREATE TABLE `"+n+"`")){r.next();w.write(r.getString(2)+";\n");}
     try(var s=c.createStatement();var r=s.executeQuery("SELECT * FROM `"+n+"`")){var m=r.getMetaData(); while(r.next()){
      w.write("INSERT INTO `"+n+"` VALUES (");
      for(int i=1;i<=m.getColumnCount();i++){if(i>1)w.write(","); byte[] b=r.getBytes(i); w.write(b==null?"NULL":"X'"+HexFormat.of().formatHex(b)+"'");} w.write(");\n");
     }}
    } w.write("SET FOREIGN_KEY_CHECKS=1;\n");
   } c.rollback(); System.out.println("BACKUP_COMPLETE tables="+names.size());
  }
 }
}
