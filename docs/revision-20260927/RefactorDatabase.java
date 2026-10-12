import java.sql.*;import java.nio.file.*;import java.util.*;
class RefactorDatabase {
 static long count(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static boolean table(Connection c,String name)throws Exception{return count(c,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='"+name+"'")>0;}
 static void run(Connection c,String sql)throws Exception{try(var s=c.createStatement()){s.execute(sql);}}
 public static void main(String[]args)throws Exception{
  if(args.length!=2||!Set.of("motorent_db","motorent_verify_20260926").contains(args[0])||!Set.of("--check","--apply").contains(args[1]))throw new IllegalArgumentException("Known schema and --check/--apply required");
  var backup=Path.of("docs/implementation-20260925/database-before-stations-promotions-20260927.sql");
  if(!Files.readString(backup).endsWith("SET FOREIGN_KEY_CHECKS=1;\n"))throw new IllegalStateException("Backup incomplete");
  var props=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){props.load(r);}
  var url=props.getProperty("spring.datasource.url");if(!url.startsWith("jdbc:mysql://localhost:")&&!url.startsWith("jdbc:mysql://127.0.0.1:"))throw new IllegalStateException("Local database required");
  url=url.replaceFirst("/motorent_db(?=[?]|$)","/"+args[0]);
  try(var c=DriverManager.getConnection(url,props.getProperty("spring.datasource.username"),props.getProperty("spring.datasource.password"))){
   if(!args[0].equals(c.getCatalog()))throw new IllegalStateException("Wrong database");
   var dropKeys=new ArrayList<String>();
   try(var s=c.createStatement();var rs=s.executeQuery("SELECT table_name,column_name,constraint_name,referenced_table_name FROM information_schema.key_column_usage WHERE referenced_table_schema=DATABASE() AND referenced_table_name IN ('stations','promotions')")){
    while(rs.next()){
     System.out.println("REFERENCE "+rs.getString(1)+"."+rs.getString(2)+" -> "+rs.getString(4));
     if(!rs.getString(1).equals("motorbikes")||!rs.getString(2).equals("station_id")||!rs.getString(4).equals("stations"))throw new IllegalStateException("Unexpected dependency; no changes applied");
     dropKeys.add(rs.getString(3));
    }
   }
   for(String t:List.of("stations","promotions")){
    if(table(c,t))System.out.println("TABLE "+t+" rows="+count(c,"SELECT COUNT(*) FROM `"+t+"`"));
    for(String sql:List.of("SELECT COUNT(*) FROM information_schema.views WHERE table_schema=DATABASE() AND (view_definition IS NULL OR view_definition REGEXP '\\b"+t+"\\b')","SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema=DATABASE() AND (event_object_table='"+t+"' OR action_statement LIKE '%"+t+"%')","SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=DATABASE() AND (routine_definition IS NULL OR routine_definition LIKE '%"+t+"%')","SELECT COUNT(*) FROM information_schema.events WHERE event_schema=DATABASE() AND (event_definition IS NULL OR event_definition LIKE '%"+t+"%')"))if(count(c,sql)>0)throw new IllegalStateException("Database object dependency found");
   }
   System.out.println("CHECK_OK "+args[0]);if(args[1].equals("--check"))return;
   long bikes=count(c,"SELECT COUNT(*) FROM motorbikes"),orders=count(c,"SELECT COUNT(*) FROM rental_orders"),payments=count(c,"SELECT COUNT(*) FROM payments");
   for(String key:dropKeys)run(c,"ALTER TABLE motorbikes DROP FOREIGN KEY `"+key.replace("`","``")+"`");
   if(count(c,"SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='motorbikes' AND column_name='station_id'")>0)run(c,"ALTER TABLE motorbikes DROP COLUMN station_id");
   for(String t:List.of("stations","promotions"))if(table(c,t))run(c,"DROP TABLE `"+t+"`");
   if(count(c,"SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='travel_places' AND column_name='search_text'")==0)run(c,"ALTER TABLE travel_places ADD COLUMN search_text VARCHAR(1600)");
   c.setAutoCommit(false);
   try {
    // Fill search keys for existing curated records without overwriting their content/publication choices.
    try(var s=c.createStatement();var rs=s.executeQuery("SELECT id,name,area,address FROM travel_places WHERE search_text IS NULL");var update=c.prepareStatement("UPDATE travel_places SET search_text=? WHERE id=?")){
     while(rs.next()){update.setString(1,norm(rs.getString(2))+" | "+norm(rs.getString(3))+" | "+norm(rs.getString(4)));update.setLong(2,rs.getLong(1));update.addBatch();}update.executeBatch();
    }
    var existing=new HashSet<String>();try(var s=c.createStatement();var rs=s.executeQuery("SELECT external_id FROM travel_places")){while(rs.next())existing.add(rs.getString(1));}
    int added=0,skipped=0;
    try(var insert=c.prepareStatement("INSERT INTO travel_places (external_id,name,address,area,category,phone,maps_url,lat,lng,source_status,reviewed_at,search_text,published) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")){
     for(String line:Files.readAllLines(Path.of("docs/revision-20260927/places-import.tsv"))){
      var f=Arrays.stream(line.split("\t",-1)).map(v->new String(Base64.getDecoder().decode(v),java.nio.charset.StandardCharsets.UTF_8)).toArray(String[]::new);
      if(!existing.add(f[0])){skipped++;continue;}
      for(int i=0;i<12;i++){if(i==7||i==8){if(f[i].isBlank())insert.setNull(i+1,Types.DOUBLE);else insert.setDouble(i+1,Double.parseDouble(f[i]));}else insert.setString(i+1,f[i]);}
      insert.setBoolean(13,"OPERATIONAL".equals(f[9])&&!f[1].isBlank()&&!f[2].isBlank());insert.addBatch();added++;if(added%500==0)insert.executeBatch();
     }insert.executeBatch();
    }
    if(bikes!=count(c,"SELECT COUNT(*) FROM motorbikes")||orders!=count(c,"SELECT COUNT(*) FROM rental_orders")||payments!=count(c,"SELECT COUNT(*) FROM payments"))throw new IllegalStateException("Core counts changed");
    c.commit();System.out.println("IMPORTED="+added+" EXISTING="+skipped+" PLACES="+count(c,"SELECT COUNT(*) FROM travel_places")+" PUBLISHED="+count(c,"SELECT COUNT(*) FROM travel_places WHERE published=1"));
    System.out.println("PRESERVED bikes="+bikes+" orders="+orders+" payments="+payments);
   }catch(Exception e){c.rollback();throw e;}
  }
 }
 static String norm(String s){return java.text.Normalizer.normalize(Objects.toString(s,""),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT);}
}
