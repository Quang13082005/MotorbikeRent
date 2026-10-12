package com.motorrent.motorbikerental;
import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.enums.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDateTime;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:customer_security;MODE=MySQL;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect","spring.ai.openai.api-key=test","spring.ai.openai.chat.enabled=false","spring.ai.openai.embedding.enabled=false","app.seed-demo=false","logging.file.name=target/customer-tests.log"})
@Transactional
class CustomerSecurityIntegrationTests {
 @Autowired WebApplicationContext context; @Autowired UserRepository users; @Autowired MotorbikeRepository bikes; @Autowired RentalOrderRepository orders;
 @org.springframework.test.context.bean.override.mockito.MockitoBean vn.payos.PayOS payOS;
 @Autowired PaymentRepository payments; @Autowired com.motorrent.motorbikerental.service.PaymentService paymentService;
 MockMvc mvc; User customer,other,staff; RentalOrder order; Motorbike bike;
 @BeforeEach void setup(){
  mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  customer=makeUser("alice",UserRole.CUSTOMER);other=makeUser("bob",UserRole.CUSTOMER);staff=makeUser("operator",UserRole.STAFF);
  bike=bikes.save(Motorbike.builder().name("Xe kiểm thử").pricePerDay(120000).depositAmount(1000000).year("2024").build());
  order=orders.saveAndFlush(RentalOrder.builder().orderCode("TEST-OWNER").customer(customer).motorbike(bike).pickupDate(LocalDateTime.now().plusDays(1)).returnDate(LocalDateTime.now().plusDays(2)).days(1).totalPrice(120000).depositDue(60000).depositPaid(0).paymentExpiresAt(LocalDateTime.now().plusMinutes(3)).status(RentalStatus.PENDING_PAYMENT).build());
 }
 User makeUser(String name,UserRole role){return users.save(User.builder().username(name).email(name+"@example.test").password("unused").firstName(name).lastName("Test").role(role).build());}
  @Test void customerCannotInitiateOrCancelVehicleChangeEvenForOwnRental() throws Exception {
   var changes=context.getBean(RentalVehicleChangeRepository.class);
   long before=changes.count();
   String route="/api/operations/orders/"+order.getId();
   String body="{\"motorbikeId\":"+bike.getId()+",\"reason\":\"Customer attempted change\",\"staffId\":\""+staff.getId()+"\"}";
   for(var state:java.util.List.of(RentalStatus.PENDING,RentalStatus.HANDED_OVER)) {
    order.setStatus(state);orders.saveAndFlush(order);
    mvc.perform(get(route+"/replacement-options").with(user("alice").roles("CUSTOMER"))).andExpect(status().isForbidden());
    for(String suffix:java.util.List.of("/vehicle-changes/quote","/vehicle-changes"))
     mvc.perform(post(route+suffix).with(user("alice").roles("CUSTOMER")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
    mvc.perform(delete("/api/operations/vehicle-changes/123").with(user("alice").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
    assertThat(orders.findById(order.getId()).orElseThrow().getMotorbike().getId()).isEqualTo(bike.getId());
    assertThat(order.getStatus()).isEqualTo(state);
   }
   assertThat(changes.count()).isEqualTo(before);
   assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).isEmpty();
  }
  @Test void vehicleChangeServiceUsesStoredStaffRoleNotSubmittedActorOrForgedAuthority() throws Exception {
   String route="/api/operations/orders/"+order.getId();
   // Pass the route-level role check, then require the real persisted account role.
   for(String suffix:java.util.List.of("/vehicle-changes/quote","/vehicle-changes"))
    mvc.perform(post(route+suffix).with(user("alice").roles("STAFF")).with(csrf()).contentType("application/json")
     .content("{\"motorbikeId\":"+bike.getId()+",\"reason\":\"Forged actor\",\"staffId\":\""+staff.getId()+"\"}"))
     .andExpect(status().isForbidden());
   mvc.perform(post(route+"/vehicle-changes").with(user("operator").roles("STAFF")).contentType("application/json").content("{}"))
    .andExpect(status().isForbidden());
  }

 @Test void duplicatePassportIgnoringCaseIsRejected() {
  var request=com.motorrent.motorbikerental.dto.request.RentalOrderRequest.builder().motorbikeId(bike.getId()).pickupDate(LocalDateTime.now().plusDays(4)).returnDate(LocalDateTime.now().plusDays(5)).securityOption(2).termsAccepted(true)
   .people(java.util.List.of(new com.motorrent.motorbikerental.dto.request.RentalOrderRequest.Person("Test A","0900000001","AB123456","1990-01-01","Da Nang"),new com.motorrent.motorbikerental.dto.request.RentalOrderRequest.Person("Test B","0900000002","ab123456","1991-01-01","Da Nang"))).build();
  assertThatThrownBy(()->context.getBean(com.motorrent.motorbikerental.service.RentalOrderService.class).createOrder(request,customer.getId())).isInstanceOf(IllegalArgumentException.class).hasMessage("Hai giấy tờ phải thuộc hai người khác nhau");
 }
 @Test void distinctIdentityDocumentsCreateOrderWithoutSecurityDeposit() {
  var request=com.motorrent.motorbikerental.dto.request.RentalOrderRequest.builder().motorbikeId(bike.getId()).pickupDate(LocalDateTime.now().plusDays(4)).returnDate(LocalDateTime.now().plusDays(5)).securityOption(2).termsAccepted(true)
   .people(java.util.List.of(new com.motorrent.motorbikerental.dto.request.RentalOrderRequest.Person("Test A","0900000001","AB123456","1990-01-01","Da Nang"),new com.motorrent.motorbikerental.dto.request.RentalOrderRequest.Person("Test B","0900000002","CD123456","1991-01-01","Da Nang"))).build();
  var created=context.getBean(com.motorrent.motorbikerental.service.RentalOrderService.class).createOrder(request,customer.getId());
  assertThat(orders.findById(created.getId()).orElseThrow().getSecurityDeposit()).isZero();
 }
 @Test void pastRentalSearchAndQuoteAreRejected() throws Exception {
  var start=java.time.LocalDateTime.now().minusDays(1);var end=start.plusDays(2);
  var result=mvc.perform(get("/customer/motorbike-list").param("pickup",start.toString()).param("returnAt",end.toString())).andExpect(status().isOk()).andReturn();
  assertThat(result.getModelAndView().getModel().get("searchError")).isEqualTo("Ngày nhận xe không được ở quá khứ");
  assertThat((java.util.List<?>)result.getModelAndView().getModel().get("motorbikes")).isEmpty();
  var request=com.motorrent.motorbikerental.dto.request.RentalOrderRequest.builder().motorbikeId(bike.getId()).pickupDate(start).returnDate(end).build();
  assertThatThrownBy(()->context.getBean(com.motorrent.motorbikerental.service.PricingService.class).quote(request,bike)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void securityDepositChangesWithCategoryAndGuaranteeOption() {
  var request=com.motorrent.motorbikerental.dto.request.RentalOrderRequest.builder().pickupDate(LocalDateTime.now().plusDays(1)).returnDate(LocalDateTime.now().plusDays(2)).securityOption(1).build();
  var pricing=context.getBean(com.motorrent.motorbikerental.service.PricingService.class);
  for(int amount:new int[]{1000000,2000000,1500000}){
   bike.setCategory(MotorbikeCategory.builder().name("Loại xe").securityDeposit(amount).build());bike.setDepositAmount(0);request.setSecurityOption(1);
   var quote=pricing.quote(request,bike);assertThat(quote.get("securityDeposit")).isEqualTo(amount);var due=quote.get("depositDue");
   request.setSecurityOption(2);quote=pricing.quote(request,bike);assertThat(quote.get("securityDeposit")).isEqualTo(0);assertThat(quote.get("depositDue")).isEqualTo(due);
  }
  bike.setCategory(null);
 }
 @Test void paymentLinkRetryUsesPersistedIntentAndDoesNotMarkPaid() throws Exception {
  var checkout=org.mockito.Mockito.mock(vn.payos.type.CheckoutResponseData.class);
  org.mockito.Mockito.when(checkout.getCheckoutUrl()).thenReturn("https://pay.payos.vn/test-checkout");
  org.mockito.Mockito.when(checkout.getPaymentLinkId()).thenReturn("test-link");
  org.mockito.Mockito.when(payOS.createPaymentLink(org.mockito.ArgumentMatchers.any(vn.payos.type.PaymentData.class)))
    .thenThrow(new java.io.IOException("simulated timeout"))
    .thenReturn(checkout);
  var url="/api/payment/create/"+order.getId();
  mvc.perform(post(url).with(user("alice").roles("CUSTOMER")).with(csrf())).andExpect(status().isBadGateway());
  var intent=payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId()).get(0);long code=intent.getProviderCode();
  mvc.perform(post(url).with(user("alice").roles("CUSTOMER")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.checkoutUrl").value("https://pay.payos.vn/test-checkout"));
  mvc.perform(post(url).with(user("alice").roles("CUSTOMER")).with(csrf())).andExpect(status().isOk());
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).hasSize(1);
  assertThat(intent.getProviderCode()).isEqualTo(code);assertThat(order.getDepositPaid()).isZero();assertThat(order.getStatus()).isEqualTo(RentalStatus.PENDING_PAYMENT);
  org.mockito.Mockito.verify(payOS,org.mockito.Mockito.times(2)).createPaymentLink(org.mockito.ArgumentMatchers.any(vn.payos.type.PaymentData.class));
 }
 @Test void travelPlacesAreStoredAndOnlyPublishedEntriesArePublic() throws Exception {
  makeUser("administrator",UserRole.ADMIN);
  String body="{\"name\":\"Điểm thử Sơn Trà\",\"address\":\"Đà Nẵng\",\"published\":\"false\",\"mapsUrl\":\"https://maps.google.com/?q=Da+Nang\"}";
  String endpoint="/api/operations/data/travel-places";
  mvc.perform(post(endpoint).with(user("operator").roles("STAFF")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post(endpoint).with(user("administrator").roles("ADMIN")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  var repository=context.getBean(TravelPlaceRepository.class);var p=repository.findAll().get(0);
  assertThat(p.getName()).isEqualTo("Điểm thử Sơn Trà");assertThat(p.isPublished()).isFalse();
  String html=mvc.perform(get("/customer/travel-places")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertThat(html).doesNotContain("Điểm thử Sơn Trà");
  mvc.perform(put(endpoint+"/"+p.getId()).with(user("administrator").roles("ADMIN")).with(csrf()).contentType("application/json").content(body.replace("\"false\"","\"true\""))).andExpect(status().isOk());
  html=mvc.perform(get("/customer/travel-places").param("query","son tra")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertThat(html).contains("Điểm thử Sơn Trà");
  mvc.perform(get("/api/operations/data/travel-places").with(user("operator").roles("STAFF"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/travel-places").with(user("administrator").roles("ADMIN"))).andExpect(status().isOk());
 }
 @Test void largePlaceCatalogIsPagedAndMapSearchIsBounded() throws Exception {
  var repo=context.getBean(TravelPlaceRepository.class);
  for(int i=0;i<30;i++){var p=new TravelPlace();p.setName("Sơn Trà "+String.format("%02d",i));p.setAddress("Đà Nẵng");p.setPublished(true);p.setLat(16.0);p.setLng(108.0);repo.save(p);}
  repo.flush();
  var service=context.getBean(com.motorrent.motorbikerental.service.TravelPlaceService.class);
  assertThat(service.published("son tra",0,24).getTotalElements()).isEqualTo(30);
  assertThat(service.published("son tra",1,24).getContent()).hasSize(6);
  assertThat(service.mapSearch("son tra")).hasSize(12);
  assertThat(service.published("%",0,24).getTotalElements()).isZero();
  mvc.perform(get("/customer/travel-places").param("page","1")).andExpect(status().isOk());
  makeUser("administrator",UserRole.ADMIN);
  mvc.perform(get("/api/operations/data/travel-places").param("query","son tra").with(user("administrator").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(30));
 }
 @Test void homeRecommendationsRespectCategoryAndSixBikeLimit() throws Exception {
  var categories=context.getBean(MotorbikeCategoryRepository.class);
  var category=categories.save(MotorbikeCategory.builder().name("Xe tay ga").build());
  for(int i=0;i<8;i++)bikes.save(Motorbike.builder().name("Xe ga đề cử "+i).category(category).pricePerDay(100000).year("2024").rating((double)i/2).build());
  var result=mvc.perform(get("/").param("recommended","xe-ga")).andExpect(status().isOk()).andReturn();
  var items=(java.util.List<com.motorrent.motorbikerental.dto.response.MotorbikeResponse>)result.getModelAndView().getModel().get("motorbikes");
  assertThat(items).hasSize(6).allMatch(m->"Xe tay ga".equals(m.getCategory()));
  assertThat(items.get(0).getRating()).isEqualTo(3.5);
  assertThat(result.getResponse().getContentAsString()).doesNotContain("Tìm xe theo nhu cầu");
 }
 @Test void promotionPageAndCheckoutControlsAreRemoved() throws Exception {
  makeUser("administrator",UserRole.ADMIN);
  mvc.perform(get("/admin/promotions").with(user("administrator").roles("ADMIN"))).andExpect(status().isNotFound());
  var html=mvc.perform(get("/customer/rental-contract/"+bike.getId()).with(user("alice").roles("CUSTOMER"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertThat(html).doesNotContain("promotionCode","promotionSaving","Mã khuyến mãi");
 }
 @Test void travelPlaceRejectsExecutableLinks() throws Exception {
  makeUser("administrator",UserRole.ADMIN);
  mvc.perform(post("/api/operations/data/travel-places").with(user("administrator").roles("ADMIN")).with(csrf()).contentType("application/json")
   .content("{\"name\":\"Địa điểm\",\"address\":\"Đà Nẵng\",\"published\":\"true\",\"mapsUrl\":\"javascript:alert(1)\"}"))
   .andExpect(status().isBadRequest());
 }
 @Test void customerCannotAccessAnotherOrderOrOperateStaffActions() throws Exception {
  mvc.perform(get("/api/rental-orders/"+order.getId()).with(user("bob").roles("CUSTOMER"))).andExpect(status().isForbidden());
  mvc.perform(post("/api/rental-orders/"+order.getId()+"/cancel").with(user("bob").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/api/rental-orders/"+order.getId()+"/confirm").with(user("alice").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/api/payment/create/"+order.getId()).with(user("bob").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
 }
 @Test void forgedPaymentReturnDoesNotMarkOrderPaid() throws Exception {
  mvc.perform(get("/customer/payment-result").param("orderId",order.getId().toString()).param("code","00").param("cancel","false").with(user("alice").roles("CUSTOMER"))).andExpect(status().isOk());
  assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.PENDING_PAYMENT);
  assertThat(order.getDepositPaid()).isZero();
 }
 @Test void customerPagesRenderWithoutDemoIdentity() throws Exception {
  for(String path:java.util.List.of("/customer/profile","/customer/my-rentals","/customer/rental-history","/customer/rental-detail/"+order.getId(),"/customer/rental-extension/"+order.getId(),"/customer/rental-contract/"+bike.getId(),"/customer/motorbike-detail/"+bike.getId(),"/customer/motorbike-list","/")) {
   String html=mvc.perform(get(path).with(user("alice").roles("CUSTOMER"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   assertThat(html).as(path).doesNotContain("customer_demo@motorent.vn","Trần Thị Bích Ngọc","Ã¡", "á»");
  }
 }
 @Test void csrfAndAuthenticationRequired() throws Exception {
  mvc.perform(post("/api/rental-orders/"+order.getId()+"/cancel").with(user("alice").roles("CUSTOMER"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
 }
 @Test void paymentAmountAndIdempotencyAreEnforced() {
  var p=payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(60000).providerCode(9999L).paymentMethod("PAYOS").purpose("DEPOSIT").expiresAt(LocalDateTime.now().plusMinutes(3)).build());
  paymentService.apply(9999L,60000,"test-link","test-ref");
  paymentService.apply(9999L,60000,"test-link","test-ref");
  assertThat(order.getDepositPaid()).isEqualTo(60000);assertThat(order.getStatus()).isEqualTo(RentalStatus.PENDING);
  assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("PAID");
 }
 @Test void mismatchedOrLateMoneyRequiresReconciliation() {
  var p=payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(60000).providerCode(9998L).paymentMethod("PAYOS").purpose("DEPOSIT").expiresAt(LocalDateTime.now().minusMinutes(1)).build());
  paymentService.apply(9998L,1000,"late-link","late-ref");
  assertThat(order.getDepositPaid()).isZero();assertThat(order.getStatus()).isEqualTo(RentalStatus.PENDING_PAYMENT);
  assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("REVIEW_REQUIRED");
 }
 @Test void profilePersistsAndQuoteIgnoresClientAmount() throws Exception {
  mvc.perform(put("/api/account").with(user("alice").roles("CUSTOMER")).with(csrf()).contentType("application/json")
   .content("{\"firstName\":\"An\",\"lastName\":\"Nguyễn\",\"email\":\"alice@example.test\",\"phone\":\"0905123456\",\"address\":\"Đà Nẵng\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("firstName").value("An"));
  assertThat(users.findById(customer.getId()).orElseThrow().getAddress()).isEqualTo("Đà Nẵng");
  String body="{\"motorbikeId\":"+bike.getId()+",\"pickupDate\":\""+LocalDateTime.now().plusDays(1)+"\",\"returnDate\":\""+LocalDateTime.now().plusDays(2).plusMinutes(1)+"\",\"pickupType\":\"STORE\",\"returnType\":\"STORE\",\"depositAmount\":1}";
  mvc.perform(post("/api/rental-orders/quote").with(user("alice").roles("CUSTOMER")).with(csrf()).contentType("application/json").content(body))
   .andExpect(status().isOk()).andExpect(jsonPath("days").value(2)).andExpect(jsonPath("depositDue").value(120000));
 }

 @Test void authPagesProvideCsrfTokenAndLoginAcceptsIt() throws Exception {
  var encoder=context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class);
  customer.setPassword(encoder.encode("testPassword123"));users.saveAndFlush(customer);
  for(String page:java.util.List.of("login","register","forgot-password","reset-password")) {
   var html=mvc.perform(get("/auth/"+page)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   assertThat(html).contains("name=\"csrf-token\"","/js/request-security.js");
  }
  mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"username\":\"alice@example.test\",\"password\":\"testPassword123\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("result.token").isNotEmpty());
 }
 @Test void staffCannotEditAdminDataAndAdminPagesRender() throws Exception {
  makeUser("administrator",UserRole.ADMIN);
  mvc.perform(get("/api/operations/data/customers").with(user("operator").roles("STAFF"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/reports").with(user("operator").roles("STAFF"))).andExpect(status().isForbidden());
  for(String page:java.util.List.of("dashboard","motorbikes","customers","staffs","rental-orders","maintenance","payments","extensions","reviews","settings","reports")) {
   mvc.perform(get("/admin/"+page).with(user("administrator").roles("ADMIN"))).andExpect(status().isOk());
  }
 }
 @Test void balanceReceiptCannotBeRecordedTwice() throws Exception {
  order.setStatus(RentalStatus.HANDED_OVER);order.setDepositPaid(60000);order.setBalancePaid(0);orders.saveAndFlush(order);
  String route="/api/operations/orders/"+order.getId()+"/cash";
  String body="{\"purpose\":\"BALANCE\",\"reference\":\"TEST-CASH-1\"}";
  mvc.perform(post(route).with(user("operator").roles("STAFF")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(post(route).with(user("operator").roles("STAFF")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
  assertThat(order.getBalancePaid()).isEqualTo(60000);
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).hasSize(1);
 }

 @Test void completeRentalLifecycleKeepsCustomerStaffAndAdminAmountsConsistent() throws Exception {
  order.setSecurityDeposit(1000000);order.setDailyRate(120000);orders.saveAndFlush(order);
  payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(60000).providerCode(887700L).paymentMethod("PAYOS").purpose("DEPOSIT").expiresAt(LocalDateTime.now().plusMinutes(3)).build());
  paymentService.apply(887700L,60000,"lifecycle-link","lifecycle-ref");
  String url="/api/rental-orders/"+order.getId();
  mvc.perform(post(url+"/confirm").with(user("operator").roles("STAFF")).with(csrf())).andExpect(status().isOk());
  // Advance the fixture to pickup time without changing the real clock or customer's data.
  order.setPickupDate(LocalDateTime.now().minusMinutes(1));orders.saveAndFlush(order);
  mvc.perform(post(url+"/hand-over").with(user("operator").roles("STAFF")).with(csrf()))
   .andExpect(status().isOk());
  mvc.perform(post(url+"/extensions").with(user("alice").roles("CUSTOMER")).with(csrf()).contentType("application/json").content("{\"newReturnDate\":\""+order.getReturnDate().plusDays(1)+"\",\"reason\":\"Thêm một ngày\"}"))
   .andExpect(status().isOk());
  var extension=context.getBean(RentalExtensionRepository.class).findByRentalOrderIdOrderByCreatedAtDesc(order.getId()).get(0);
  mvc.perform(post("/api/operations/extensions/"+extension.getId()).param("approve","true").with(user("operator").roles("STAFF")).with(csrf())).andExpect(status().isOk());
  // Phase 4 supersedes immediate extension on approval: payment must succeed first.
  assertThat(order.getTotalPrice()).isEqualTo(120000);assertThat(order.getStatus()).isEqualTo(RentalStatus.HANDED_OVER);
  assertThat(extension.getStatus()).isEqualTo("WAITING_FOR_PAYMENT");
  var extensionUrl="/api/payment/extension/"+extension.getId();
  mvc.perform(post(extensionUrl).with(user("bob").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post(extensionUrl).with(user("operator").roles("STAFF")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post(extensionUrl).with(user("alice").roles("CUSTOMER"))).andExpect(status().isForbidden());
  var checkout=org.mockito.Mockito.mock(vn.payos.type.CheckoutResponseData.class);
  org.mockito.Mockito.when(checkout.getCheckoutUrl()).thenReturn("https://pay.payos.vn/test-extension");
  org.mockito.Mockito.when(checkout.getPaymentLinkId()).thenReturn("extension-checkout");
  org.mockito.Mockito.when(payOS.createPaymentLink(org.mockito.ArgumentMatchers.any(vn.payos.type.PaymentData.class))).thenThrow(new java.io.IOException("extension timeout")).thenReturn(checkout);
  mvc.perform(post(extensionUrl).with(user("alice").roles("CUSTOMER")).with(csrf())).andExpect(status().isBadGateway());
  assertThat(order.getTotalPrice()).isEqualTo(120000);
  mvc.perform(post(extensionUrl).with(user("alice").roles("CUSTOMER")).with(csrf())).andExpect(status().isOk());
  var extensionPayment=payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId()).stream().filter(p->"EXTENSION".equals(p.getSourceType())).findFirst().orElseThrow();
  paymentService.apply(extensionPayment.getProviderCode(),120000,"extension-checkout","extension-lifecycle-ref");
  assertThat(order.getTotalPrice()).isEqualTo(240000);assertThat(order.getStatus()).isEqualTo(RentalStatus.EXTENDED);
  mvc.perform(get("/staff/rental-orders").with(user("operator").roles("STAFF"))).andExpect(content().string(org.hamcrest.Matchers.containsString("data-action=\"complete\"")));
  mvc.perform(post(url+"/complete").with(user("operator").roles("STAFF")).with(csrf())).andExpect(status().isOk());
  assertThat(order.getStatus()).isEqualTo(RentalStatus.COMPLETED);assertThat(order.getActualReturnDate()).isNotNull();assertThat(bike.getStatus()).isEqualTo(MotorbikeStatus.AVAILABLE);
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).hasSize(5);
  assertThat(order.getBalancePaid()).isEqualTo(180000);
  mvc.perform(get("/customer/rental-history").with(user("alice").roles("CUSTOMER"))).andExpect(content().string(org.hamcrest.Matchers.containsString("TEST-OWNER")));
  String review="{\"orderId\":"+order.getId()+",\"rating\":5,\"comment\":\"Xe tốt\"}";
  mvc.perform(post("/api/reviews").with(user("alice").roles("CUSTOMER")).with(csrf()).contentType("application/json").content(review)).andExpect(status().isOk());
  mvc.perform(post("/api/reviews").with(user("alice").roles("CUSTOMER")).with(csrf()).contentType("application/json").content(review)).andExpect(status().isBadRequest());
  assertThat(bike.getRating()).isEqualTo(5.0);assertThat(bike.getReviewCount()).isEqualTo(1);
  makeUser("administrator",UserRole.ADMIN);
  mvc.perform(get("/api/admin/reports").with(user("administrator").roles("ADMIN")))
   .andExpect(status().isOk()).andExpect(jsonPath("revenue").value(240000)).andExpect(jsonPath("collected").value(240000)).andExpect(jsonPath("activeRentals").value(0));
 }
 @Test void cancelledPaidOrderRefundIsRecordedOnce() throws Exception {
  order.setStatus(RentalStatus.PENDING);order.setDepositPaid(60000);orders.saveAndFlush(order);
  String url="/api/rental-orders/"+order.getId();
  mvc.perform(post(url+"/cancel").with(user("alice").roles("CUSTOMER")).with(csrf())).andExpect(status().isOk());
  String receipt="{\"purpose\":\"DEPOSIT_REFUND\",\"reference\":\"cancel-refund\"}";
  String cash="/api/operations/orders/"+order.getId()+"/cash";
  mvc.perform(post(cash).with(user("operator").roles("STAFF")).with(csrf()).contentType("application/json").content(receipt)).andExpect(status().isOk());
  mvc.perform(post(cash).with(user("operator").roles("STAFF")).with(csrf()).contentType("application/json").content(receipt)).andExpect(status().isBadRequest());
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).singleElement().satisfies(p->{assertThat(p.getAmount()).isEqualTo(60000);assertThat(p.getDirection()).isEqualTo("OUT");});
 }
 @Test void customerCannotConfirmHandoverOrRecordRefund() throws Exception {
  mvc.perform(post("/api/rental-orders/"+order.getId()+"/hand-over").with(user("alice").roles("CUSTOMER")).with(csrf()))
   .andExpect(status().isForbidden());
  mvc.perform(post("/api/operations/orders/"+order.getId()+"/cash").with(user("alice").roles("CUSTOMER")).with(csrf()).contentType("application/json").content("{\"purpose\":\"SECURITY_REFUND\",\"reference\":\"forged\"}"))
   .andExpect(status().isForbidden());
 }

 @Test void notificationsBelongToCurrentCustomer() throws Exception {
  var repository=context.getBean(NotificationRepository.class);
  var notice=repository.saveAndFlush(Notification.builder().user(customer).title("Đơn đã xác nhận").message("Thông báo kiểm thử").link("/customer/rental-detail/"+order.getId()).build());
  mvc.perform(get("/customer/notifications").with(user("alice").roles("CUSTOMER"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Thông báo kiểm thử")));
  mvc.perform(post("/customer/notifications/"+notice.getId()+"/read").with(user("bob").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
  assertThat(notice.getIsRead()).isFalse();
  mvc.perform(post("/customer/notifications/"+notice.getId()+"/read").with(user("alice").roles("CUSTOMER")).with(csrf())).andExpect(status().is3xxRedirection());
  assertThat(notice.getIsRead()).isTrue();
 }

 @Test void staffBankReceiptHasImmutableActorAndComponents() throws Exception {
  order.setStatus(RentalStatus.CONFIRMED);order.setDepositPaid(60000);orders.saveAndFlush(order);
  mvc.perform(post("/api/operations/orders/"+order.getId()+"/cash").with(user("operator").roles("STAFF")).with(csrf()).contentType("application/json")
   .content("{\"purpose\":\"BALANCE\",\"reference\":\"bank-p1\",\"method\":\"BANK_TRANSFER\",\"note\":\"Đã nhận tại cửa hàng\"}"))
   .andExpect(status().isOk());
  var p=payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId()).get(0);
  assertThat(p.getTransactionType()).isEqualTo("RENTAL_REMAINING");assertThat(p.getDirection()).isEqualTo("IN");
  assertThat(p.getProcessedByStaffId()).isEqualTo(staff.getId());assertThat(p.getProcessedAt()).isNotNull();
  assertThat(p.getPaymentMethod()).isEqualTo("BANK_TRANSFER");assertThat(p.getComponentsJson()).contains("60000");
  order.setStaff(other);orders.saveAndFlush(order);assertThat(p.getProcessedByStaffId()).isEqualTo(staff.getId());
 }
 @Test void pendingAssetReceiptCannotBeRefunded() throws Exception {
  order.setStatus(RentalStatus.COMPLETED);orders.saveAndFlush(order);
  payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(1000000).purpose("SECURITY").status("PENDING").build());
  mvc.perform(post("/api/operations/orders/"+order.getId()+"/cash").with(user("operator").roles("STAFF")).with(csrf()).contentType("application/json")
   .content("{\"purpose\":\"SECURITY_REFUND\",\"reference\":\"not-received\"}")).andExpect(status().isBadRequest());
 }
 @Test void summarySeparatesAssetRefundAndMoneyRequiringReview() throws Exception {
  payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(60000).receivedAmount(60000).purpose("DEPOSIT").status("PAID").build());
  payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(1000000).purpose("SECURITY").status("PAID").build());
  payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(-1000000).purpose("SECURITY_REFUND").status("PAID").build());
  payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(-60000).purpose("DEPOSIT_REFUND").status("PAID").build());
  payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(60000).receivedAmount(65000).purpose("DEPOSIT").status("REVIEW_REQUIRED").build());
  mvc.perform(get("/api/payment/order/"+order.getId()+"/summary").with(user("alice").roles("CUSTOMER")))
   .andExpect(status().isOk()).andExpect(jsonPath("rentalCashNet").value(0)).andExpect(jsonPath("assetDepositHeld").value(0)).andExpect(jsonPath("unreconciledAmount").value(65000));
  mvc.perform(get("/api/payment/order/"+order.getId()+"/summary").with(user("bob").roles("CUSTOMER"))).andExpect(status().isForbidden());
 }
 @Test void bookingPaymentComponentsMatchFourHundredThousand() throws Exception {
  order.setDepositDue(400000);order.setPickupFee(60000);order.setReturnFee(40000);orders.saveAndFlush(order);
  org.mockito.Mockito.when(payOS.createPaymentLink(org.mockito.ArgumentMatchers.any())).thenThrow(new RuntimeException("offline test"));
  mvc.perform(post("/api/payment/create/"+order.getId()).with(user("alice").roles("CUSTOMER")).with(csrf())).andExpect(status().isBadGateway());
  var p=payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId()).get(0);
  var parts=new com.fasterxml.jackson.databind.ObjectMapper().readTree(p.getComponentsJson());
  assertThat(p.getAmount()).isEqualTo(400000);assertThat(parts.get("BOOKING_PAYMENT").asInt()).isEqualTo(300000);
  assertThat(parts.get("DELIVERY_FEE").asInt()).isEqualTo(60000);assertThat(parts.get("RETURN_FEE").asInt()).isEqualTo(40000);
  assertThat(p.getTransactionType()).isEqualTo("BOOKING_PAYMENT");assertThat(p.getDirection()).isEqualTo("IN");
  assertThat(p.getProcessedByStaffId()).isNull();
 }
 @Test void overduePhysicalVehicleBlocksFutureBookingsUntilReturned() {
  order.setPickupDate(LocalDateTime.now().minusDays(2));order.setReturnDate(LocalDateTime.now().minusDays(1));order.setStatus(RentalStatus.HANDED_OVER);orders.saveAndFlush(order);
  var availability=context.getBean(com.motorrent.motorbikerental.service.AvailabilityService.class);
  assertThat(availability.available(bike.getId(),null,LocalDateTime.now().plusDays(1),LocalDateTime.now().plusDays(2))).isFalse();
  order.setActualReturnDate(LocalDateTime.now());order.setStatus(RentalStatus.COMPLETED);orders.saveAndFlush(order);
  assertThat(availability.available(bike.getId(),null,LocalDateTime.now().plusDays(1),LocalDateTime.now().plusDays(2))).isTrue();
 }
 @Test void expiredUnpaidHoldIsNotAvailableToALateCallback() {
  var end=order.getReturnDate();order.setPaymentExpiresAt(LocalDateTime.now().minusMinutes(1));orders.saveAndFlush(order);
  var availability=context.getBean(com.motorrent.motorbikerental.service.AvailabilityService.class);
  assertThat(availability.available(bike.getId(),null,order.getPickupDate(),end)).isTrue();
  payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(60000).purpose("DEPOSIT").providerCode(999111L).expiresAt(order.getPaymentExpiresAt()).build());
  paymentService.apply(999111L,60000,"expired-link","expired-ref");
  assertThat(order.getStatus()).isEqualTo(RentalStatus.PENDING_PAYMENT);
  assertThat(payments.findByProviderCode(999111L).orElseThrow().getStatus()).isEqualTo("REVIEW_REQUIRED");
 }
 @Test void adjacentBookingDoesNotConflictButOneMinuteOverlapDoes() {
  order.setReturnDate(order.getReturnDate().withNano(0));orders.saveAndFlush(order);
  var availability=context.getBean(com.motorrent.motorbikerental.service.AvailabilityService.class);
  assertThat(availability.available(bike.getId(),null,order.getReturnDate(),order.getReturnDate().plusDays(1))).isTrue();
  assertThat(availability.available(bike.getId(),null,order.getReturnDate().minusMinutes(1),order.getReturnDate().plusDays(1))).isFalse();
 }

 @Test void quoteChargesHalfRentalAndAllDeliveryWithoutAssetDeposit() {
  var settingsRepo=context.getBean(ShopSettingsRepository.class);var shop=settingsRepo.findById(1L).orElseThrow();
  shop.setDepositPercent(50);shop.setDeliveryBaseFee(50000);settingsRepo.saveAndFlush(shop);
  bike.setPricePerDay(600000);bikes.saveAndFlush(bike);
  var start=LocalDateTime.now().plusDays(3).withNano(0);
  var r=com.motorrent.motorbikerental.dto.request.RentalOrderRequest.builder().motorbikeId(bike.getId()).pickupDate(start).returnDate(start.plusDays(1))
   .pickupType("DELIVERY").returnType("DELIVERY").pickupAddress("Test pickup").returnAddress("Test return")
   .pickupLat(shop.getLat()).pickupLng(shop.getLng()).returnLat(shop.getLat()).returnLng(shop.getLng()).securityOption(1).build();
  var quote=context.getBean(com.motorrent.motorbikerental.service.PricingService.class).quote(r,bike);
  assertThat(quote.get("depositDue")).isEqualTo(400000);assertThat(quote.get("securityDeposit")).isEqualTo(1000000);
  assertThat(quote.get("pickupFee")).isEqualTo(50000);assertThat(quote.get("returnFee")).isEqualTo(50000);
 }
}
