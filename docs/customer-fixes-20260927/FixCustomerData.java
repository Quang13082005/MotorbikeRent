import java.sql.*;import java.nio.file.*;import java.util.*;
class FixCustomerData {
 static long count(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void run(Connection c,String sql)throws Exception{try(var s=c.createStatement()){s.execute(sql);}}
 static void column(Connection c,String table,String name,String type)throws Exception{if(count(c,"SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='"+table+"' AND column_name='"+name+"'")==0)run(c,"ALTER TABLE "+table+" ADD COLUMN "+name+" "+type);}
 static String norm(String s){return java.text.Normalizer.normalize(Objects.toString(s,""),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT);}
 public static void main(String[] args)throws Exception{
  if(args.length!=1||!Set.of("motorent_db","motorent_verify_20260926").contains(args[0]))throw new IllegalArgumentException("Known database required");
  if(!Files.readString(Path.of("docs/implementation-20260925/database-before-customer-fixes-20260927.sql")).endsWith("SET FOREIGN_KEY_CHECKS=1;\n"))throw new IllegalStateException("Complete backup required");
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
  String url=p.getProperty("spring.datasource.url");if(!url.startsWith("jdbc:mysql://localhost:")&&!url.startsWith("jdbc:mysql://127.0.0.1:"))throw new IllegalStateException("Local database required");url=url.replaceFirst("/motorent_db(?=[?]|$)","/"+args[0]);
  try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
   if(!args[0].equals(c.getCatalog()))throw new IllegalStateException("Wrong database");
   if(count(c,"SELECT COUNT(*) FROM information_schema.key_column_usage WHERE referenced_table_schema=DATABASE() AND referenced_table_name='travel_places'")>0)throw new IllegalStateException("Place references require manual review");
   column(c,"motorbike_categories","security_deposit","INT");for(String col:List.of("image_credit","image_source_url","image_license_url")){column(c,"travel_places",col,"TEXT");run(c,"ALTER TABLE travel_places MODIFY COLUMN "+col+" TEXT");}
   c.setAutoCommit(false);
   try{
    try(var s=c.createStatement();var rs=s.executeQuery("SELECT category_id,name FROM motorbike_categories");var q=c.prepareStatement("UPDATE motorbike_categories SET security_deposit=? WHERE category_id=?")){
     while(rs.next()){String name=norm(rs.getString(2)).replace(' ','-');Integer amount=switch(name){case "xe-so"->1000000;case "xe-ga","xe-tay-ga"->2000000;case "xe-con-tay"->1500000;default->null;};if(amount!=null){q.setInt(1,amount);q.setLong(2,rs.getLong(1));q.addBatch();}}q.executeBatch();
    }
    run(c,"UPDATE motorbikes b JOIN motorbike_categories cat ON cat.category_id=b.category_id SET b.deposit_amount=cat.security_deposit WHERE cat.security_deposit IS NOT NULL");
    int deleted=0,hidden=0;
    try(var get=c.prepareStatement("SELECT address FROM travel_places WHERE external_id=?");var del=c.prepareStatement("DELETE FROM travel_places WHERE external_id=?");var hide=c.prepareStatement("UPDATE travel_places SET published=0,source_status='REVIEW_ADDRESS_REQUIRED' WHERE external_id=?")){
     for(String line:Files.readAllLines(Path.of("docs/customer-fixes-20260927/place-actions.tsv"))){var f=line.split("\t");get.setString(1,f[1]);String address;try(var rs=get.executeQuery()){if(!rs.next())continue;address=norm(rs.getString(1));}
      if(address.contains("da nang")||address.contains("quang nam"))continue;
      if(f[0].equals("DELETE")&&List.of("quang ngai","hue","kon tum","ho chi minh").stream().anyMatch(address::contains)){del.setString(1,f[1]);deleted+=del.executeUpdate();}else{hide.setString(1,f[1]);hidden+=hide.executeUpdate();}
     }
    }
    try(var q=c.prepareStatement("UPDATE travel_places SET image_url=?,image_credit=?,image_source_url=?,image_license_url=? WHERE external_id=? AND (image_url IS NULL OR image_url='' OR image_url LIKE '%wikimedia.org%')")){
     String[][] photos={
      {"https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e3/2024-12-20_Japanese_Bridge_in_Hoi_An.jpg/960px-2024-12-20_Japanese_Bridge_in_Hoi_An.jpg","Chùa Cầu · Alexey Komarov · CC BY 4.0 · ảnh hiển thị thu nhỏ, không chỉnh sửa","https://commons.wikimedia.org/wiki/File:2024-12-20_Japanese_Bridge_in_Hoi_An.jpg","https://creativecommons.org/licenses/by/4.0/","ChIJWewCLXwOQjERRkh9adZkWHY"},
      {"https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0c/Golden_Bridge_at_Ba_Na_Hills_20250718.jpg/960px-Golden_Bridge_at_Ba_Na_Hills_20250718.jpg","Cầu Vàng trong khu Bà Nà Hills · DvTor8303 · CC0","https://commons.wikimedia.org/wiki/File:Golden_Bridge_at_Ba_Na_Hills_20250718.jpg","https://creativecommons.org/publicdomain/zero/1.0/","ChIJyUQ4aw72QTERCXl5YV-4U1w"}
     };for(var values:photos){for(int i=0;i<5;i++)q.setString(i+1,values[i]);q.addBatch();}q.executeBatch();
    }
    c.commit();System.out.println("DATABASE="+args[0]+" DELETED_OUTSIDE="+deleted+" HIDDEN_UNCLEAR="+hidden+" PLACES="+count(c,"SELECT COUNT(*) FROM travel_places")+" PUBLISHED="+count(c,"SELECT COUNT(*) FROM travel_places WHERE published=1"));
    System.out.println("RATES_READY; historical order/payment amounts unchanged");
   }catch(Exception e){c.rollback();throw e;}
  }
 }
}
