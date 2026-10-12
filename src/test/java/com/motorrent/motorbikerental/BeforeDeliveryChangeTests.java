package com.motorrent.motorbikerental;

import com.motorrent.motorbikerental.dto.request.VehicleChangeRequest;
import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.enums.*;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Transactional
class BeforeDeliveryChangeTests {
 @Autowired UserRepository users;@Autowired MotorbikeRepository bikes;@Autowired RentalOrderRepository orders;@Autowired PaymentRepository payments;@Autowired RentalVehicleChangeRepository changes;
 @Autowired VehicleChangeService service;@Autowired PaymentService paymentService;@Autowired AvailabilityService availability;@Autowired RentalOrderService rentalService;
 User customer,staff;Motorbike oldBike,newBike;RentalOrder order;Payment booking;
 void login(User u){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.getUsername(),"test",List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole().name()))));}
 @BeforeEach void fixture(){
  String key=UUID.randomUUID().toString();customer=users.save(User.builder().username("bc-"+key).email(key+"@example.invalid").password("test").role(UserRole.CUSTOMER).build());staff=users.save(User.builder().username("bs-"+key).email("s"+key+"@example.invalid").password("test").role(UserRole.STAFF).build());
  oldBike=bikes.save(Motorbike.builder().name("Old bike").pricePerDay(100000).depositAmount(1000000).build());newBike=bikes.save(Motorbike.builder().name("New bike").pricePerDay(100000).depositAmount(2000000).build());
  var start=AvailabilityService.now().plusDays(7).withNano(0);
  order=orders.saveAndFlush(RentalOrder.builder().orderCode("BC-"+key).customer(customer).motorbike(oldBike).pickupDate(start).returnDate(start.plusDays(2)).days(2).dailyRate(100000).rentalFee(200000).deliveryFee(0).pickupFee(0).returnFee(0).totalPrice(200000).depositDue(100000).depositPaid(100000).balancePaid(0).securityDeposit(1000000).pickupType("STORE").returnType("STORE").status(RentalStatus.PENDING).build());
  booking=payments.saveAndFlush(Payment.builder().rentalOrder(order).amount(100000).receivedAmount(100000).purpose("DEPOSIT").paymentMethod("PAYOS").status("PAID").transactionCode("BC-"+key).build());login(staff);
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 VehicleChangeRequest request(){return new VehicleChangeRequest(newBike.getId(),"Xe cũ cần bảo dưỡng",null,null,null,null,null,null,null,null);}
 Long propose(){return (Long)service.propose(order.getId(),request()).get("id");}
 Payment topup(Long id){login(customer);var p=service.preparePayment(id);login(staff);return p;}
 void rate(int rate){newBike.setPricePerDay(rate);bikes.saveAndFlush(newBike);}
 @Test void samePriceKeepsOrderAndOriginalPayment(){
  Long orderId=order.getId();Long change=propose();
  assertThat(order.getId()).isEqualTo(orderId);assertThat(order.getMotorbike().getId()).isEqualTo(newBike.getId());assertThat(order.getTotalPrice()).isEqualTo(200000);
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(orderId)).singleElement().satisfies(p->assertThat(p.getId()).isEqualTo(booking.getId()));
  var c=changes.findById(change).orElseThrow();assertThat(c.getStatus()).isEqualTo("APPLIED");assertThat(c.getOldVehicle().getId()).isEqualTo(oldBike.getId());assertThat(c.getProcessedByStaffId()).isEqualTo(staff.getId());
 }
 @Test void cheaperBikeReducesBalanceWithoutRefundingAutomatically(){rate(80000);propose();assertThat(order.getRemainingRentalAmount()).isEqualTo(60000);assertThat(order.getRentalCreditAmount()).isZero();assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).hasSize(1);}
 @Test void excessMoneyCanBeRefundedOnceWithAudit(){
  rate(20000);propose();assertThat(order.getRemainingRentalAmount()).isZero();assertThat(order.getRentalCreditAmount()).isEqualTo(60000);
  var refund=new VehicleChangeService.Refund(60000,"BANK_TRANSFER","bc-credit","Xe thay thế rẻ hơn",null);service.refund(order.getId(),refund,false);
  assertThat(order.getRentalCreditAmount()).isZero();assertThatThrownBy(()->service.refund(order.getId(),refund,false)).isInstanceOf(IllegalArgumentException.class);
  assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).anySatisfy(p->{assertThat(p.getTransactionType()).isEqualTo("BOOKING_ADJUSTMENT_REFUND");assertThat(p.getDirection()).isEqualTo("OUT");assertThat(p.getProcessedByStaffId()).isEqualTo(staff.getId());});
 }
 @Test void dearerBikeOnlyChangesAfterPaymentAndCallbackIsIdempotent(){
  rate(150000);Long id=propose();var p=topup(id);assertThat(p.getAmount()).isEqualTo(50000);assertThat(order.getMotorbike().getId()).isEqualTo(oldBike.getId());assertThat(order.getTotalPrice()).isEqualTo(200000);
  paymentService.apply(p.getProviderCode(),50000,"bc-link","bc-ref");paymentService.apply(p.getProviderCode(),50000,"bc-link","bc-ref");
  assertThat(order.getMotorbike().getId()).isEqualTo(newBike.getId());assertThat(order.getTotalPrice()).isEqualTo(300000);assertThat(order.getDepositPaid()).isEqualTo(150000);assertThat(order.getRemainingRentalAmount()).isEqualTo(150000);
  assertThat(booking.getAmount()).isEqualTo(100000);assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).hasSize(2);
 }
 @Test void wrongAmountRequiresReviewWithoutChangingOrder(){rate(150000);Long id=propose();var p=topup(id);paymentService.apply(p.getProviderCode(),51000,"bc-link","bc-wrong");assertThat(p.getStatus()).isEqualTo("REVIEW_REQUIRED");assertThat(order.getMotorbike().getId()).isEqualTo(oldBike.getId());}
 @Test void holdBlocksOtherBookingsAndExpiryReleasesIt(){rate(150000);Long id=propose();assertThat(availability.available(newBike.getId(),null,order.getPickupDate(),order.getReturnDate())).isFalse();var c=changes.findById(id).orElseThrow();c.setExpiresAt(AvailabilityService.now().minusSeconds(1));changes.saveAndFlush(c);assertThat(availability.available(newBike.getId(),null,order.getPickupDate(),order.getReturnDate())).isTrue();}
 @Test void lateTopupIsRecordedForReview(){rate(150000);Long id=propose();var p=topup(id);var c=changes.findById(id).orElseThrow();c.setExpiresAt(AvailabilityService.now().minusSeconds(1));changes.saveAndFlush(c);paymentService.apply(p.getProviderCode(),50000,"bc-link","bc-late");assertThat(p.getStatus()).isEqualTo("REVIEW_REQUIRED");assertThat(order.getDepositPaid()).isEqualTo(100000);}
 @Test void conflictIsRecheckedWhenStaffSubmits(){
  assertThat(service.options(order.getId())).anyMatch(m->newBike.getId().equals(m.get("newVehicleId")));
  orders.saveAndFlush(RentalOrder.builder().orderCode("CONFLICT-"+UUID.randomUUID()).customer(customer).motorbike(newBike).pickupDate(order.getPickupDate()).returnDate(order.getReturnDate()).days(2).totalPrice(200000).status(RentalStatus.CONFIRMED).build());
  assertThat(service.options(order.getId())).noneMatch(m->newBike.getId().equals(m.get("newVehicleId")));assertThatThrownBy(this::propose).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void paidOrderCannotBeRejectedBeforeRecordingRefund(){assertThatThrownBy(()->rentalService.rejectOrder(order.getId(),staff.getId(),"Không còn xe")).isInstanceOf(IllegalArgumentException.class);assertThat(order.getStatus()).isEqualTo(RentalStatus.PENDING);}
 @Test void refundAndRejectionAreOneFinalAction(){service.refund(order.getId(),new VehicleChangeService.Refund(100000,"CASH","bc-full","Khách không đổi xe",null),true);assertThat(order.getStatus()).isEqualTo(RentalStatus.REJECTED);assertThat(order.getDepositPaid()).isZero();assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId()).stream().mapToLong(Financials::cash).sum()).isZero();}
 @Test void cancellingProposalPreservesOriginalVehicleAndStopsPayment(){rate(150000);Long id=propose();var p=topup(id);service.cancel(id);assertThat(availability.available(newBike.getId(),null,order.getPickupDate(),order.getReturnDate())).isTrue();paymentService.apply(p.getProviderCode(),50000,"bc-link","bc-cancel");assertThat(p.getStatus()).isEqualTo("REVIEW_REQUIRED");assertThat(order.getMotorbike().getId()).isEqualTo(oldBike.getId());}
 @Test void deliveryFeeChangesUseServerPricing(){
  var r=new VehicleChangeRequest(newBike.getId(),"Giao tại cửa hàng", "DELIVERY","Cửa hàng",16.0526,108.1718,"DELIVERY","Cửa hàng",16.0526,108.1718);
  var q=service.preview(order.getId(),r);assertThat(q.get("newDeliveryFee")).isEqualTo(30000);assertThat(q.get("additionalPayment")).isEqualTo(30000);
 }
 @Test void twoDocumentsKeepSecurityDepositZero(){order.setIdentitySnapshot("[{},{}]");order.setSecurityDeposit(0);orders.saveAndFlush(order);propose();assertThat(order.getSecurityDeposit()).isZero();}
 @Test void customerCannotChooseReplacementOnBehalfOfStaff(){login(customer);assertThatThrownBy(this::propose).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);}
 @Test void missedScheduleStillAllowsRefundOptionsToLoad(){order.setReturnDate(AvailabilityService.now().minusDays(1));orders.saveAndFlush(order);assertThat(service.options(order.getId())).isEmpty();service.refund(order.getId(),new VehicleChangeService.Refund(100000,"CASH","missed","Missed schedule",null),true);assertThat(order.getStatus()).isEqualTo(RentalStatus.REJECTED);}
 @Test void extraDeliveryChargesAreAllocatedToBothLegs(){
  var r=new VehicleChangeRequest(newBike.getId(),"New delivery", "DELIVERY","Shop",16.0526,108.1718,"DELIVERY","Shop",16.0526,108.1718);
  var id=(Long)service.propose(order.getId(),r).get("id");var p=topup(id);assertThat(p.getAmount()).isEqualTo(30000);assertThat(p.getComponentsJson()).contains("DELIVERY_FEE", "RETURN_FEE", "15000");
 }
 @Test void customerCancellationReleasesReplacementHold(){rate(150000);propose();rentalService.cancelOrder(order.getId());assertThat(availability.available(newBike.getId(),null,order.getPickupDate(),order.getReturnDate())).isTrue();assertThat(service.history(order.getId()).get(0).get("status")).isEqualTo("CANCELLED");}
}
