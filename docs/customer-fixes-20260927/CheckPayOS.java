import java.nio.file.*;import java.util.*;import vn.payos.PayOS;
class CheckPayOS {
 public static void main(String[] args)throws Exception{
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
  var sdk=new PayOS(p.getProperty("payos.client-id"),p.getProperty("payos.api-key"),p.getProperty("payos.checksum-key"));
  try{sdk.getPaymentLinkInformation(4503599627370494L);System.out.println("PAYOS_READ_OK");}
  catch(vn.payos.exception.PayOSException e){System.out.println("PAYOS_READ_RESPONSE_CODE="+Objects.toString(e.getCode(),"unknown").replaceAll("[^A-Za-z0-9_-]",""));String message=Objects.toString(e.getMessage(),"");for(String key:List.of("payos.client-id","payos.api-key","payos.checksum-key"))message=message.replace(p.getProperty(key),"[REDACTED]");System.out.println("PAYOS_DESCRIPTION="+message.replaceAll("[A-Za-z0-9_-]{24,}","[ID]").substring(0,Math.min(250,message.replaceAll("[A-Za-z0-9_-]{24,}","[ID]").length())));}
  catch(Exception e){System.out.println("PAYOS_READ_ERROR_TYPE="+e.getClass().getSimpleName());}
 }
}
