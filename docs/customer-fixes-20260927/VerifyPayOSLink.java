import java.nio.file.*;import java.util.*;import vn.payos.PayOS;import vn.payos.type.*;
class VerifyPayOSLink {
 public static void main(String[] args)throws Exception{
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(r);}
  var sdk=new PayOS(p.getProperty("payos.client-id"),p.getProperty("payos.api-key"),p.getProperty("payos.checksum-key"));
  long code=1+new java.security.SecureRandom().nextLong(4_503_599_627_370_495L);
  Files.writeString(Path.of("docs/customer-fixes-20260927/payos-test-code.txt"),Long.toString(code),StandardOpenOption.CREATE_NEW);
  try{
   var response=sdk.createPaymentLink(PaymentData.builder().orderCode(code).amount(2000).description("TEST CODE").returnUrl("http://localhost:8080/customer/payment-result").cancelUrl("http://localhost:8080/customer/payment-result").expiredAt(java.time.Instant.now().plusSeconds(300).getEpochSecond()).item(ItemData.builder().name("Kiem thu ky thuat - khong thanh toan").price(2000).quantity(1).build()).build());
   System.out.println("PAYOS_CREATE_LINK="+(response.getCheckoutUrl()!=null && response.getCheckoutUrl().startsWith("https://pay.payos.vn/")));
  }catch(vn.payos.exception.PayOSException e){System.out.println("PAYOS_CREATE_ERROR_CODE="+e.getCode());}
  finally{
   try{var result=sdk.cancelPaymentLink(code,"Kiem thu ky thuat da ket thuc - khong thu tien");System.out.println("PAYOS_TEST_LINK_STATUS="+result.getStatus()+" AMOUNT_PAID="+result.getAmountPaid());}
   catch(vn.payos.exception.PayOSException e){System.out.println("PAYOS_CANCEL_RESPONSE_CODE="+e.getCode());}
  }
 }
}
