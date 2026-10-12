import java.sql.*;import java.nio.file.*;import java.util.*;
/** Removes only the empty, unreferenced legacy table. Owner authorized cleanup; backup required. */
class RemoveUnusedDepositTable {
 static long count(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 public static void main(String[] args)throws Exception{
  if(args.length!=2 || !Set.of("motorent_db","motorent_verify_20260926").contains(args[0]) || !Set.of("--check","--apply").contains(args[1]))throw new IllegalArgumentException("Specify known database and --check or --apply");
  var backup=Path.of("docs/implementation-20260925/database-before-cleanup-20260927.sql");
  if(!Files.exists(backup)||!Files.readString(backup).endsWith("SET FOREIGN_KEY_CHECKS=1;\n"))throw new IllegalStateException("Complete backup required");
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
  String url=p.getProperty("spring.datasource.url");if(!url.startsWith("jdbc:mysql://localhost:")&&!url.startsWith("jdbc:mysql://127.0.0.1:"))throw new IllegalStateException("Local database only");
  url=url.replaceFirst("/motorent_db(?=[?]|$)","/"+args[0]);
  try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
   if(!args[0].equals(c.getCatalog()))throw new IllegalStateException("Wrong database");
   if(count(c,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='deposit_attempts'")==0){System.out.println("ALREADY_ABSENT "+args[0]);return;}
   if(count(c,"SELECT COUNT(*) FROM deposit_attempts")!=0)throw new IllegalStateException("Table has data; refusing drop");
   if(count(c,"SELECT COUNT(*) FROM information_schema.key_column_usage WHERE (referenced_table_schema=DATABASE() AND referenced_table_name='deposit_attempts') OR (table_schema=DATABASE() AND table_name='deposit_attempts' AND referenced_table_name IS NOT NULL)")!=0)throw new IllegalStateException("Foreign key dependency found");
   for(String sql:List.of(
    "SELECT COUNT(*) FROM information_schema.views WHERE table_schema=DATABASE() AND (view_definition IS NULL OR view_definition LIKE '%deposit_attempts%')",
    "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema=DATABASE() AND (event_object_table='deposit_attempts' OR action_statement LIKE '%deposit_attempts%')",
    "SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=DATABASE() AND (routine_definition IS NULL OR routine_definition LIKE '%deposit_attempts%')",
    "SELECT COUNT(*) FROM information_schema.events WHERE event_schema=DATABASE() AND (event_definition IS NULL OR event_definition LIKE '%deposit_attempts%')")){
    if(count(c,sql)!=0)throw new IllegalStateException("Database object dependency or unreadable definition found");
   }
   System.out.println("CHECK_OK database="+args[0]+" table=deposit_attempts rows=0 dependencies=0");
   if(args[1].equals("--apply")){try(var s=c.createStatement()){s.executeUpdate("DROP TABLE deposit_attempts");}System.out.println("REMOVED deposit_attempts");}
  }
 }
}
