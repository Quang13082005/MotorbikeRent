import java.sql.*;import java.nio.file.*;import java.util.*;
class ReadinessCheck {
 public static void main(String[] args)throws Exception{
 var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
 for(String key:List.of("spring.mail.host","spring.mail.username","spring.mail.password","google.cloud.vision.api-key","payos.client-id","payos.api-key","payos.checksum-key"))System.out.println(key+" configured="+(!p.getProperty(key,"").isBlank()));
 var base=p.getProperty("app.base-url","http://localhost:8080");System.out.println("BASE_URL_LOCAL="+(base.contains("localhost")||base.contains("127.0.0.1")));
 try(var c=DriverManager.getConnection(p.getProperty("spring.datasource.url"),p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"));var s=c.createStatement()){
 c.setReadOnly(true);
 try(var r=s.executeQuery("SELECT terms,representative,lat,lng FROM shop_settings WHERE id=1")){if(r.next())System.out.println("SHOP_TERMS_PRESENT="+(r.getString(1)!=null&&!r.getString(1).isBlank())+" REPRESENTATIVE_PRESENT="+(r.getString(2)!=null&&!r.getString(2).isBlank())+" COORDINATES_PRESENT="+(r.getObject(3)!=null&&r.getObject(4)!=null));}
 for(String q:List.of("SELECT COUNT(*) FROM motorbikes","SELECT COUNT(*) FROM motorbike_images WHERE image_url IS NOT NULL AND image_url<>''","SELECT COUNT(*) FROM travel_places WHERE published=1 AND image_url IS NOT NULL AND image_url<>''"))try(var r=s.executeQuery(q)){r.next();System.out.println(q+" => "+r.getInt(1));}
 }
 }
}
