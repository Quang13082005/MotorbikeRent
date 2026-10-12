package com.motorrent.motorbikerental;

import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.enums.*;
import com.motorrent.motorbikerental.dto.request.RentalOrderRequest;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Runs on H2 normally; -Dphase1.mysql=true runs only against the allowlisted clone. */
@SpringBootTest
class Phase1ConcurrencyTests {
 @DynamicPropertySource static void database(DynamicPropertyRegistry r)throws Exception {
  if(!Boolean.getBoolean("phase1.mysql"))return;
  var p=new Properties();
  var localCfg=Path.of("application-local.properties");
  if(Files.exists(localCfg)){try(var in=Files.newBufferedReader(localCfg)){p.load(in);}}
  else{try(var in=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){p.load(in);}}
  String url=p.getProperty("spring.datasource.url").replaceAll("(?<=3306/)[^?]+","motorent_verify_20260926");
  if(!url.contains("/motorent_verify_20260926?"))throw new IllegalStateException("Clone only");
  r.add("spring.datasource.url",()->url);r.add("spring.datasource.username",()->p.getProperty("spring.datasource.username"));r.add("spring.datasource.password",()->p.getProperty("spring.datasource.password"));
  r.add("spring.datasource.driver-class-name",()->"com.mysql.cj.jdbc.Driver");r.add("spring.jpa.hibernate.ddl-auto",()->"validate");
  r.add("spring.jpa.properties.hibernate.dialect",()->"org.hibernate.dialect.MySQLDialect");
 }
 @Autowired UserRepository users;@Autowired MotorbikeRepository bikes;@Autowired RentalOrderRepository orders;
 @Autowired PaymentRepository payments;@Autowired NotificationRepository notifications;
 @Autowired RentalOrderService rental;@Autowired SettlementService settlement;@Autowired TransactionTemplate tx;
 @Autowired PaymentService paymentService;
 @Autowired VehicleChangeService changesService;@Autowired RentalVehicleChangeRepository changes;
 @Autowired RentalContractRepository contracts;
 @Autowired ExtensionService extensionService; @Autowired RentalExtensionRepository extensions;
 @Autowired AvailabilityService availability;
 @Autowired DuringRentalChangeService during;
 @Autowired DuringRentalQuoteService duringQuotes;
 RentalOrder duringOrder(){
  var o=paidOrder(bike);o.setPickupDate(AvailabilityService.now().minusDays(1).withNano(0));o.setReturnDate(AvailabilityService.now().plusHours(1).withNano(0));o.setBalancePaid(60000);o.setSecurityDeposit(1000000);o.setStatus(RentalStatus.HANDED_OVER);orders.saveAndFlush(o);
  bike.setStatus(MotorbikeStatus.RENTED);bikes.saveAndFlush(bike);
  payments.saveAndFlush(Payment.builder().rentalOrder(o).amount(1000000).receivedAmount(1000000).purpose("SECURITY").direction("IN").status("PAID").build());return o;
 }
 com.motorrent.motorbikerental.dto.request.DuringRentalQuoteRequest duringRequest(RentalOrder o,Motorbike target){return new com.motorrent.motorbikerental.dto.request.DuringRentalQuoteRequest(target.getId(),o.getReturnDate(),o.getReturnDate().plusDays(2),100000,"Tại cửa hàng","Tiếp tục thuê theo phương án đã thống nhất");}
 @Test void duringPaymentPreservesOldCustodyAndCollectsOnlyAssetDifference() {
  var o=duringOrder();var target=extra(180000);target.setDepositAmount(2000000);bikes.saveAndFlush(target);as(staff);
  Long id=during.propose(o.getId(),duringRequest(o,target));as(customer);var p=during.preparePayment(id);
  assertThat(p.getAmount()).isEqualTo(460000);assertThat(p.getComponentsJson()).contains("360000","100000");
  assertThat(orders.findById(o.getId()).orElseThrow().getReturnDate()).isEqualTo(o.getReturnDate());
  paymentService.apply(p.getProviderCode(),460000,"during-link","during-ref");paymentService.apply(p.getProviderCode(),460000,"during-link","during-ref");
  var saved=orders.findById(o.getId()).orElseThrow();assertThat(saved.getMotorbike().getId()).isEqualTo(bike.getId());assertThat(saved.getReturnDate()).isEqualTo(o.getReturnDate().plusDays(2));assertThat(saved.getTotalPrice()).isEqualTo(580000);
  assertThat(bikes.findById(bike.getId()).orElseThrow().getStatus()).isEqualTo(MotorbikeStatus.RENTED);
  assertThat(availability.available(target.getId(),null,o.getReturnDate(),o.getReturnDate().plusDays(2))).isFalse();
  as(staff);assertThatThrownBy(()->during.confirm(id,new DuringRentalChangeService.Confirmation(1000000,"CASH","receipt"))).hasMessageContaining("Chưa đến giờ");
  try(var clock=org.mockito.Mockito.mockStatic(AvailabilityService.class)){
   clock.when(AvailabilityService::now).thenReturn(o.getReturnDate().plusSeconds(1));
   assertThatThrownBy(()->during.confirm(id,new DuringRentalChangeService.Confirmation(0,"CASH","receipt"))).hasMessageContaining("Chưa thu đủ");
   assertThat(orders.findById(o.getId()).orElseThrow().getMotorbike().getId()).isEqualTo(bike.getId());
   during.confirm(id,new DuringRentalChangeService.Confirmation(1000000,"CASH","receipt-"+id));
   during.confirm(id,new DuringRentalChangeService.Confirmation(1000000,"CASH","receipt-"+id));
  }
  assertThat(orders.findById(o.getId()).orElseThrow().getMotorbike().getId()).isEqualTo(target.getId());
  assertThat(bikes.findById(bike.getId()).orElseThrow().getStatus()).isEqualTo(MotorbikeStatus.AVAILABLE);
  assertThat(changes.findById(id).orElseThrow().getChangedByStaffId()).isEqualTo(staff.getId());
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(o.getId()).stream().filter(x->"ASSET_DEPOSIT_ADDITIONAL".equals(Financials.type(x)))).hasSize(1);
  var summary=new FinancialSummaryService(payments,orders,new com.motorrent.motorbikerental.security.CurrentUser(users)).forOrder(o.getId());
  assertThat(summary.get("assetDepositHeld")).isEqualTo(2000000L);assertThat(summary.get("rentalCashNet")).isEqualTo(460000L);
 }
 @Test void duringWrongLateClosedAndUnavailablePaymentsCannotChangeSchedule(){
  for(String scenario:List.of("wrong","late","closed","unavailable")){
   var o=duringOrder();var target=extra(180000);as(staff);Long id=during.propose(o.getId(),duringRequest(o,target));as(customer);var p=during.preparePayment(id);
   if(scenario.equals("late"))tx.executeWithoutResult(s->{var c=changes.findById(id).orElseThrow();c.setExpiresAt(AvailabilityService.now().minusSeconds(1));});
   if(scenario.equals("closed"))tx.executeWithoutResult(s->{orders.findById(o.getId()).orElseThrow().setStatus(RentalStatus.COMPLETED);});
   if(scenario.equals("unavailable")){target.setStatus(MotorbikeStatus.MAINTENANCE);bikes.saveAndFlush(target);}
   paymentService.apply(p.getProviderCode(),scenario.equals("wrong")?1:460000,"link-"+id,"ref-"+id);
   assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("REVIEW_REQUIRED");
   assertThat(orders.findById(o.getId()).orElseThrow().getReturnDate()).isEqualTo(o.getReturnDate());
  }
 }
 @Test void duringOnlyStaffCanProposeAndNoMidSegmentPolicyIsInvented(){
  var o=duringOrder();var target=extra(180000);as(customer);
  assertThatThrownBy(()->during.propose(o.getId(),duringRequest(o,target))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  as(staff);var r=duringRequest(o,target);
  assertThatThrownBy(()->duringQuotes.quote(o.getId(),new com.motorrent.motorbikerental.dto.request.DuringRentalQuoteRequest(target.getId(),o.getReturnDate().minusMinutes(10),r.newReturnTime(),0,"Store","Test"))).hasMessageContaining("cuối chặng");
  Long id=during.propose(o.getId(),r);as(customer);assertThatThrownBy(()->extensionService.request(o.getId(),o.getReturnDate().plusDays(3),"Test")).hasMessageContaining("phương án đổi xe");
  as(staff);during.cancel(id);as(customer);assertThatThrownBy(()->during.preparePayment(id)).hasMessageContaining("hết hiệu lực");
 }
 @Test void duringConcurrentCallbacksPostOnce()throws Exception{
  var o=duringOrder();var target=extra(180000);as(staff);Long id=during.propose(o.getId(),duringRequest(o,target));as(customer);var p=during.preparePayment(id);SecurityContextHolder.clearContext();
  assertThat(race(()->{paymentService.apply(p.getProviderCode(),460000,"parallel-link","parallel-ref-"+id);return true;})).containsExactly(true,true);
  assertThat(orders.findById(o.getId()).orElseThrow().getTotalPrice()).isEqualTo(580000);
  assertThat(changes.findById(id).orElseThrow().getStatus()).isEqualTo("READY_FOR_EXCHANGE");
 }
 @Test void duringTwoStaffConfirmOnlyOneAssetMovement()throws Exception{
  var o=duringOrder();var target=extra(180000);target.setDepositAmount(2000000);bikes.saveAndFlush(target);as(staff);Long id=during.propose(o.getId(),duringRequest(o,target));as(customer);var p=during.preparePayment(id);paymentService.apply(p.getProviderCode(),460000,"confirm-link","confirm-ref-"+id);SecurityContextHolder.clearContext();
  assertThat(race(()->{as(staff);try(var clock=org.mockito.Mockito.mockStatic(AvailabilityService.class)){clock.when(AvailabilityService::now).thenReturn(o.getReturnDate().plusSeconds(1));during.confirm(id,new DuringRentalChangeService.Confirmation(1000000,"CASH","parallel-asset-"+id));return true;}finally{SecurityContextHolder.clearContext();}})).containsExactly(true,true);
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(o.getId()).stream().filter(x->"ASSET_DEPOSIT_ADDITIONAL".equals(Financials.type(x)))).hasSize(1);
 }
 List<Motorbike> extras=new ArrayList<>();
 User customer,staff;Motorbike bike;
 @BeforeEach void fixture(){
  String key=UUID.randomUUID().toString();
  customer=users.save(User.builder().username("p1c-"+key).email("p1c-"+key+"@example.invalid").password("test-only").firstName("Phase1").lastName("Test").role(UserRole.CUSTOMER).build());
  staff=users.save(User.builder().username("p1s-"+key).email("p1s-"+key+"@example.invalid").password("test-only").firstName("Phase1").lastName("Staff").role(UserRole.STAFF).build());
  bike=bikes.save(Motorbike.builder().name("PHASE1-CONCURRENCY-"+key).pricePerDay(120000).depositAmount(1000000).year("2024").cc(110).build());
 }
 @AfterEach void cleanup(){
  SecurityContextHolder.clearContext();
  tx.executeWithoutResult(s->{
   if(bike!=null)for(var o:orders.findAll().stream().filter(o->o.getCustomer().getId().equals(customer.getId())).toList()){
    extensions.deleteAll(extensions.findByRentalOrderIdOrderByCreatedAtDesc(o.getId()));extensions.flush();
    changes.deleteAll(changes.findByRentalOrderIdOrderByIdDesc(o.getId()));changes.flush();
    payments.deleteAll(payments.findByRentalOrderIdOrderByCreatedAtDesc(o.getId()));payments.flush();
    contracts.findByRentalOrderId(o.getId()).ifPresent(contracts::delete);contracts.flush();
    orders.delete(o);
   }
   orders.flush();
   if(customer!=null)notifications.deleteAll(notifications.findTop100ByUserIdOrderByCreatedAtDesc(customer.getId()));
   notifications.flush();if(bike!=null)bikes.deleteById(bike.getId());for(var extra:extras)bikes.deleteById(extra.getId());extras.clear();
   if(customer!=null)users.deleteById(customer.getId());if(staff!=null)users.deleteById(staff.getId());
  });
 }
 List<Boolean> race(Callable<Boolean> work)throws Exception {
  var gate=new CountDownLatch(1);var ready=new CountDownLatch(2);var pool=Executors.newFixedThreadPool(2);
  try{
   Callable<Boolean> task=()->{ready.countDown();if(!gate.await(10,TimeUnit.SECONDS))throw new AssertionError("Gate timeout");return work.call();};
   var one=pool.submit(task);var two=pool.submit(task);assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();gate.countDown();
   return List.of(one.get(30,TimeUnit.SECONDS),two.get(30,TimeUnit.SECONDS));
  }finally{pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
 }
 @Test void simultaneousBookingsOnlyOneCommits()throws Exception{
  var start=AvailabilityService.now().plusDays(30).withSecond(0).withNano(0);
  var request=RentalOrderRequest.builder().motorbikeId(bike.getId()).pickupDate(start).returnDate(start.plusDays(1)).pickupType("STORE").returnType("STORE").securityOption(1).termsAccepted(true)
   .people(List.of(new RentalOrderRequest.Person("Phase One","0901234567","123456789012","1990-01-01","Test address"))).build();
  var results=race(()->{try{rental.createOrder(request,customer.getId());return true;}catch(IllegalArgumentException ex){assertThat(ex.getMessage()).contains("không còn trống");return false;}});
  assertThat(results).containsExactlyInAnyOrder(true,false);
  assertThat(orders.findAll().stream().filter(o->o.getCustomer().getId().equals(customer.getId())).count()).isEqualTo(1);
 }
 @Test void simultaneousCallbacksRemainPaidWithoutDuplicateOrReview()throws Exception{
  var now=AvailabilityService.now();
  var o=orders.saveAndFlush(RentalOrder.builder().orderCode("P1-"+UUID.randomUUID()).customer(customer).motorbike(bike).pickupDate(now.plusDays(30)).returnDate(now.plusDays(31)).days(1).totalPrice(120000).depositDue(60000).depositPaid(0).paymentExpiresAt(now.plusMinutes(3)).status(RentalStatus.PENDING_PAYMENT).build());
  long code=1+new java.security.SecureRandom().nextLong(4_000_000_000_000L);
  payments.saveAndFlush(Payment.builder().rentalOrder(o).amount(60000).purpose("DEPOSIT").paymentMethod("PAYOS").providerCode(code).expiresAt(o.getPaymentExpiresAt()).build());
  assertThat(race(()->{paymentService.apply(code,60000,"p1-link-"+code,"p1-ref-"+code);return true;})).containsExactly(true,true);
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(o.getId())).singleElement().satisfies(p->assertThat(p.getStatus()).isEqualTo("PAID"));
  var saved=orders.findById(o.getId()).orElseThrow();assertThat(saved.getStatus()).isEqualTo(RentalStatus.PENDING);assertThat(saved.getDepositPaid()).isEqualTo(60000);
 }
 @Test void simultaneousRefundsOnlyOnePostsAndAudits()throws Exception{
  var o=orders.saveAndFlush(RentalOrder.builder().orderCode("P1-"+UUID.randomUUID()).customer(customer).motorbike(bike).pickupDate(AvailabilityService.now()).returnDate(AvailabilityService.now().plusDays(1)).days(1).totalPrice(120000).depositPaid(60000).status(RentalStatus.CANCELLED).build());
  var results=race(()->{
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(staff.getUsername(),"test",List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));
   try{settlement.cash(o.getId(),"DEPOSIT_REFUND",UUID.randomUUID().toString(),"BANK_TRANSFER","Concurrency test");return true;}
   catch(IllegalArgumentException ex){assertThat(ex.getMessage()).contains("Không còn khoản cọc");return false;}
   finally{SecurityContextHolder.clearContext();}
  });
  assertThat(results).containsExactlyInAnyOrder(true,false);
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(o.getId())).singleElement().satisfies(p->{
   assertThat(p.getAmount()).isEqualTo(60000);assertThat(p.getDirection()).isEqualTo("OUT");assertThat(p.getTransactionType()).isEqualTo("BOOKING_REFUND");
   assertThat(p.getProcessedByStaffId()).isEqualTo(staff.getId());assertThat(p.getProcessedAt()).isNotNull();
  });
 }

 void as(User u){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.getUsername(),"test",List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole().name()))));}
 Motorbike extra(int rate){var b=bikes.saveAndFlush(Motorbike.builder().name("P2-"+UUID.randomUUID()).pricePerDay(rate).depositAmount(1000000).build());extras.add(b);return b;}
 RentalOrder paidOrder(Motorbike b){var start=AvailabilityService.now().plusDays(40).withSecond(0).withNano(0);return orders.saveAndFlush(RentalOrder.builder().orderCode("P2-"+UUID.randomUUID()).customer(customer).motorbike(b).pickupDate(start).returnDate(start.plusDays(1)).days(1).dailyRate(120000).rentalFee(120000).deliveryFee(0).pickupFee(0).returnFee(0).pickupType("STORE").returnType("STORE").totalPrice(120000).depositDue(60000).depositPaid(60000).status(RentalStatus.PENDING).build());}
 com.motorrent.motorbikerental.dto.request.VehicleChangeRequest changeRequest(Motorbike b){return new com.motorrent.motorbikerental.dto.request.VehicleChangeRequest(b.getId(),"Phase2 concurrency",null,null,null,null,null,null,null,null);}
 @Test void twoOrdersCannotHoldTheSameReplacement()throws Exception{
  var target=extra(180000);var first=paidOrder(bike);var second=paidOrder(extra(120000));var next=new java.util.concurrent.atomic.AtomicInteger();
  var result=race(()->{as(staff);try{changesService.propose(next.getAndIncrement()==0?first.getId():second.getId(),changeRequest(target));return true;}catch(IllegalArgumentException ex){assertThat(ex.getMessage()).contains("không còn trống");return false;}finally{SecurityContextHolder.clearContext();}});
  assertThat(result).containsExactlyInAnyOrder(true,false);
 }
 @Test void simultaneousReplacementCallbacksApplyExactlyOnce()throws Exception{
  var target=extra(180000);var order=paidOrder(bike);as(staff);Long id=(Long)changesService.propose(order.getId(),changeRequest(target)).get("id");as(customer);var p=changesService.preparePayment(id);SecurityContextHolder.clearContext();
  assertThat(race(()->{paymentService.apply(p.getProviderCode(),30000,"p2-link","p2-ref");return true;})).containsExactly(true,true);
  var saved=orders.findById(order.getId()).orElseThrow();assertThat(saved.getMotorbike().getId()).isEqualTo(target.getId());assertThat(saved.getDepositPaid()).isEqualTo(90000);assertThat(changes.findById(id).orElseThrow().getStatus()).isEqualTo("APPLIED");assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("PAID");
 }
 @Test void twoStaffRefundActionsOnlyPostOnce()throws Exception{
  var order=paidOrder(bike);
  var result=race(()->{as(staff);try{changesService.refund(order.getId(),new VehicleChangeService.Refund(60000,"BANK_TRANSFER",UUID.randomUUID().toString(),"No replacement",null),true);return true;}catch(IllegalArgumentException ex){assertThat(ex.getMessage()).contains("trước bàn giao");return false;}finally{SecurityContextHolder.clearContext();}});
  assertThat(result).containsExactlyInAnyOrder(true,false);assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).hasSize(1);assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.REJECTED);
 }
 RentalOrder extendingOrder(){var o=paidOrder(bike);o.setStatus(RentalStatus.HANDED_OVER);o.setBalancePaid(60000);orders.saveAndFlush(o);return o;}
 Long requestExtension(RentalOrder o){as(customer);extensionService.request(o.getId(),o.getReturnDate().plusDays(2),"Phase4 test");return extensions.findByRentalOrderIdOrderByCreatedAtDesc(o.getId()).get(0).getId();}
 Payment approvedExtension(RentalOrder o){Long id=requestExtension(o);as(staff);extensionService.decide(id,true);as(customer);return extensionService.preparePayment(id);}
 @Test void extensionApprovalHoldsButOnlyDuplicateSafePaymentAdvancesSchedule()throws Exception{
  var o=extendingOrder();var p=approvedExtension(o);
  var pending=orders.findById(o.getId()).orElseThrow();assertThat(pending.getReturnDate()).isEqualTo(o.getReturnDate());assertThat(pending.getTotalPrice()).isEqualTo(120000);
  assertThat(availability.available(bike.getId(),null,o.getReturnDate(),o.getReturnDate().plusDays(2))).isFalse();
  as(customer);assertThat(extensionService.preparePayment(p.getSourceId()).getId()).isEqualTo(p.getId());SecurityContextHolder.clearContext();
  assertThat(race(()->{paymentService.apply(p.getProviderCode(),240000,"extension-link","extension-ref");return true;})).containsExactly(true,true);
  var saved=orders.findById(o.getId()).orElseThrow();assertThat(saved.getReturnDate()).isEqualTo(o.getReturnDate().plusDays(2));assertThat(saved.getTotalPrice()).isEqualTo(360000);assertThat(saved.getRentalFee()).isEqualTo(360000);assertThat(saved.getBalancePaid()).isEqualTo(300000);assertThat(saved.getDepositPaid()).isEqualTo(60000);assertThat(saved.getStatus()).isEqualTo(RentalStatus.EXTENDED);
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(o.getId())).singleElement().satisfies(v->{assertThat(v.getTransactionType()).isEqualTo("EXTENSION_PAYMENT");assertThat(v.getStatus()).isEqualTo("PAID");assertThat(v.getDirection()).isEqualTo("IN");assertThat(v.getProcessedAt()).isNotNull();});
  var e=extensions.findById(p.getSourceId()).orElseThrow();assertThat(e.getStatus()).isEqualTo("PAID");assertThat(e.getApprovedBy().getId()).isEqualTo(staff.getId());assertThat(e.getApprovedAt()).isNotNull();assertThat(e.getProcessedAt()).isNotNull();
 }
 @Test void expiredExtensionReleasesHoldAndLatePaymentRequiresReview(){
  var o=extendingOrder();var p=approvedExtension(o);var e=extensions.findById(p.getSourceId()).orElseThrow();e.setExpiresAt(AvailabilityService.now().minusSeconds(1));extensions.saveAndFlush(e);
  assertThat(availability.available(bike.getId(),null,o.getReturnDate(),e.getNewReturnDate())).isTrue();
  as(customer);assertThatThrownBy(()->extensionService.preparePayment(e.getId())).isInstanceOf(IllegalArgumentException.class);
  paymentService.apply(p.getProviderCode(),240000,"late-link","late-ref");
  assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("REVIEW_REQUIRED");assertThat(orders.findById(o.getId()).orElseThrow().getReturnDate()).isEqualTo(o.getReturnDate());
 }
 @Test void wrongExtensionAmountAndWrongLinkCannotAdvanceSchedule(){
  var o=extendingOrder();var p=approvedExtension(o);p.setProviderLinkId("right-link");payments.saveAndFlush(p);
  assertThatThrownBy(()->paymentService.apply(p.getProviderCode(),240000,"wrong-link","wrong-ref")).isInstanceOf(IllegalArgumentException.class);
  assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
  paymentService.apply(p.getProviderCode(),240001,"right-link","amount-ref");paymentService.apply(p.getProviderCode(),240001,"right-link","amount-ref");
  assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("REVIEW_REQUIRED");assertThat(orders.findById(o.getId()).orElseThrow().getTotalPrice()).isEqualTo(120000);
 }
 @Test void extensionChecksConflictAtRequestApprovalAndPayment(){
  var o=extendingOrder();Long id=requestExtension(o);
  var conflicting=paidOrder(bike);conflicting.setPickupDate(o.getReturnDate());conflicting.setReturnDate(o.getReturnDate().plusDays(1));orders.saveAndFlush(conflicting);
  as(staff);assertThatThrownBy(()->extensionService.decide(id,true)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hotline");
  conflicting.setStatus(RentalStatus.CANCELLED);orders.saveAndFlush(conflicting);extensionService.decide(id,true);as(customer);var p=extensionService.preparePayment(id);
  // Simulate a stale external schedule change; callback must recheck, even after approval.
  conflicting.setStatus(RentalStatus.CONFIRMED);orders.saveAndFlush(conflicting);
  paymentService.apply(p.getProviderCode(),240000,"conflict-link","conflict-ref");assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("REVIEW_REQUIRED");
  assertThat(orders.findById(o.getId()).orElseThrow().getReturnDate()).isEqualTo(o.getReturnDate());
  assertThatThrownBy(()->extensionService.request(o.getId(),o.getReturnDate().plusDays(2),"Again")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hotline");
 }
 @Test void extensionClosedOrderAndChangedVehicleCannotBeApplied(){
  var o=extendingOrder();var p=approvedExtension(o);o.setStatus(RentalStatus.COMPLETED);orders.saveAndFlush(o);
  assertThat(availability.available(bike.getId(),null,o.getReturnDate(),o.getReturnDate().plusDays(2))).isTrue();
  paymentService.apply(p.getProviderCode(),240000,"closed-link","closed-ref");assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("REVIEW_REQUIRED");assertThat(orders.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.COMPLETED);
  o.setStatus(RentalStatus.HANDED_OVER);orders.saveAndFlush(o);var next=approvedExtension(o);o.setMotorbike(extra(120000));orders.saveAndFlush(o);
  paymentService.apply(next.getProviderCode(),240000,"changed-link","changed-ref");assertThat(payments.findById(next.getId()).orElseThrow().getStatus()).isEqualTo("REVIEW_REQUIRED");
 }
 @Test void simultaneousExtensionRequestsAndLinkRetriesOnlyCreateOneIntent()throws Exception{
  var o=extendingOrder();assertThat(race(()->{as(customer);try{extensionService.request(o.getId(),o.getReturnDate().plusDays(2),"race");return true;}catch(IllegalArgumentException ex){assertThat(ex.getMessage()).contains("đang xử lý");return false;}finally{SecurityContextHolder.clearContext();}})).containsExactlyInAnyOrder(true,false);
  Long id=extensions.findByRentalOrderIdOrderByCreatedAtDesc(o.getId()).get(0).getId();as(staff);extensionService.decide(id,true);
  assertThat(race(()->{as(customer);try{extensionService.preparePayment(id);return true;}finally{SecurityContextHolder.clearContext();}})).containsExactly(true,true);
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(o.getId())).hasSize(1);
 }
 @Test void extensionAuthorizationAndDatesAreEnforced(){
  var o=extendingOrder();as(customer);
  assertThatThrownBy(()->extensionService.request(o.getId(),o.getReturnDate(),"invalid")).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->extensionService.request(o.getId(),null,"invalid")).isInstanceOf(IllegalArgumentException.class);
  Long id=requestExtension(o);assertThatThrownBy(()->extensionService.decide(id,true)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  as(staff);extensionService.decide(id,false);as(customer);assertThatThrownBy(()->extensionService.preparePayment(id)).isInstanceOf(IllegalArgumentException.class);
  assertThat(orders.findById(o.getId()).orElseThrow().getReturnDate()).isEqualTo(o.getReturnDate());
 }
 @Test void extensionApprovalRacingNewBookingCannotReserveBoth()throws Exception{
  var o=extendingOrder();Long id=requestExtension(o);var next=new java.util.concurrent.atomic.AtomicInteger();
  var results=race(()->{as(staff);try{
   if(next.getAndIncrement()==0)extensionService.decide(id,true);
   else tx.executeWithoutResult(s->{var locked=bikes.lockById(bike.getId()).orElseThrow();availability.requireAvailable(locked,null,o.getReturnDate(),o.getReturnDate().plusDays(2));orders.saveAndFlush(RentalOrder.builder().orderCode("P4-RACE-"+UUID.randomUUID()).customer(customer).motorbike(locked).pickupDate(o.getReturnDate()).returnDate(o.getReturnDate().plusDays(2)).days(2).dailyRate(120000).totalPrice(240000).status(RentalStatus.CONFIRMED).build());});
   return true;
  }catch(IllegalArgumentException ex){assertThat(ex.getMessage()).containsAnyOf("không còn trống","hotline");return false;}finally{SecurityContextHolder.clearContext();}});
  assertThat(results).containsExactlyInAnyOrder(true,false);
 }
 @Test void twoStaffHandoverActionsOnlyPostOnce()throws Exception{
  var order=paidOrder(bike);
  order.setSecurityDeposit(1000000);
  order.setStatus(RentalStatus.CONFIRMED);
  orders.saveAndFlush(order);
  var req=com.motorrent.motorbikerental.dto.request.HandoverRequest.builder()
      .remainingAmount(60000)
      .securityDeposit(1000000)
      .paymentMethod("CASH")
      .build();
  var result=race(()->{
   as(staff);
   try{
    rental.handOverOrder(order.getId(),staff.getId(),req);
    return true;
   }catch(Exception ex){
    return false;
   }finally{
    SecurityContextHolder.clearContext();
   }
  });
  assertThat(result).containsExactlyInAnyOrder(true,false);
  var saved=orders.findById(order.getId()).orElseThrow();
  assertThat(saved.getStatus()).isEqualTo(RentalStatus.HANDED_OVER);
  assertThat(saved.getBalancePaid()).isEqualTo(60000);
  assertThat(bikes.findById(bike.getId()).orElseThrow().getStatus()).isEqualTo(MotorbikeStatus.RENTED);
  var pList=payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId());
  assertThat(pList.stream().filter(p->"RENTAL_REMAINING".equals(p.getTransactionType()))).hasSize(1);
  assertThat(pList.stream().filter(p->"ASSET_DEPOSIT".equals(p.getTransactionType()))).hasSize(1);
 }
}
