import java.nio.file.*;
import java.io.*;
import java.sql.*;
import java.util.*;
class DatabaseAudit {
 public static void main(String[] args) throws Exception {
  Properties p = new Properties();
  try (Reader r = Files.newBufferedReader(Path.of("src/main/resources/application.properties"))) { p.load(r); }
  try (Connection c = DriverManager.getConnection(p.getProperty("spring.datasource.url"), p.getProperty("spring.datasource.username"), p.getProperty("spring.datasource.password"))) {
   c.setReadOnly(true);
   try (Statement s = c.createStatement()) {
    try (ResultSet r = s.executeQuery("SELECT DATABASE(), @@character_set_database, @@collation_database")) { while(r.next()) System.out.println("CONNECTED " + r.getString(1)+" "+r.getString(2)+" "+r.getString(3)); }
    List<String> names = new ArrayList<>();
    try (ResultSet r = s.executeQuery("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME")) { while(r.next()) names.add(r.getString(1)); }
    for(String n : names) { try(ResultSet r = s.executeQuery("SELECT COUNT(*) FROM `"+n.replace("`","``")+"`")) {r.next(); System.out.println(n+" rows="+r.getLong(1));} }
    try(ResultSet r = s.executeQuery("SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL")) {while(r.next()) System.out.println("FK "+r.getString(1)+"."+r.getString(2)+" -> "+r.getString(3));}
   }
  } catch(SQLException e) { System.out.println("CONNECTION_FAILED SQLState="+e.getSQLState()+" code="+e.getErrorCode()); System.exit(1); }
 }
}
