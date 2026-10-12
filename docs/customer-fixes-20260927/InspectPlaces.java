import java.sql.*;import java.nio.file.*;import java.util.*;
class InspectPlaces {
 public static void main(String[] args)throws Exception{
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
  try(var c=DriverManager.getConnection(p.getProperty("spring.datasource.url"),p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"));var s=c.createStatement()){
   c.setReadOnly(true);
   try(var r=s.executeQuery("SELECT name,image_url,image_credit,published FROM travel_places WHERE external_id IN ('ChIJWewCLXwOQjERRkh9adZkWHY','ChIJyUQ4aw72QTERCXl5YV-4U1w')")){while(r.next())System.out.println(r.getString(1)+" | "+r.getString(2)+" | "+r.getString(3)+" | "+r.getBoolean(4));}
   try(var r=s.executeQuery("SELECT COUNT(*) FROM travel_places WHERE image_url IS NOT NULL AND image_url<>''")){r.next();System.out.println("WITH_IMAGE="+r.getInt(1));}
  }
 }
}
