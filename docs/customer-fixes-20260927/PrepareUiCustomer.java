import java.sql.*;import java.nio.file.*;import java.util.*;
class PrepareUiCustomer {
 public static void main(String[] args)throws Exception{
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
  String url=p.getProperty("spring.datasource.url").replace("/motorent_db?","/motorent_verify_20260926?");
  if(!url.startsWith("jdbc:mysql://localhost:")||!url.contains("/motorent_verify_20260926?"))throw new IllegalStateException("Clone only");
  try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"));var s=c.createStatement()){
   int n=s.executeUpdate("INSERT INTO users(user_id,username,email,password,role,status,first_name,last_name,created_at,updated_at) SELECT UUID(),'customer_ui_verify','customer_ui_verify@example.test',password,'CUSTOMER','ACTIVE','UI','Verification',NOW(),NOW() FROM users WHERE username='customer_demo' AND NOT EXISTS (SELECT 1 FROM users WHERE username='customer_ui_verify')");
   System.out.println("CLONE_UI_CUSTOMER_CREATED="+n);
  }
 }
}
