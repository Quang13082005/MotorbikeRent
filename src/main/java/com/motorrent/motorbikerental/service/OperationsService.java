package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.enums.*;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class OperationsService {
 private final CurrentUser current;
 private final MotorbikeRepository bikes; private final MotorbikeBrandRepository brands; private final MotorbikeCategoryRepository categories;
 private final MotorbikeImageRepository images; private final UserRepository users;
 private final RentalOrderRepository orders; private final PaymentRepository payments; private final MaintenanceRecordRepository maintenance;
 private final ShopSettingsRepository settings; private final RentalExtensionRepository extensions; private final ExtensionService extensionService;
 private final RentalContractRepository contracts; private final TokenAuthenticationRepository tokens; private final PasswordEncoder encoder;
 private final ReviewRepository reviews;
 public static Map<String,Object> row(Object... pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put(pairs[i].toString(),pairs[i+1]);return m;}
 public void admin(){if(current.get().getRole()!=UserRole.ADMIN)throw new AccessDeniedException("Chỉ quản trị viên được thực hiện");}
 private String text(Map<String,Object> data,String key,int max,boolean required){String s=Objects.toString(data.get(key),"").trim();if((required && s.isBlank()) || s.length()>max)throw new IllegalArgumentException("Trường "+key+" không hợp lệ");return s;}
 private int number(Map<String,Object> data,String key,int min,int max){try{int n=Integer.parseInt(Objects.toString(data.get(key),"0"));if(n<min || n>max)throw new Exception();return n;}catch(Exception e){throw new IllegalArgumentException("Giá trị "+key+" ngoài giới hạn");}}
 private long id(Map<String,Object> data,String key){return Long.parseLong(text(data,key,20,true));}
 private LocalDateTime date(Map<String,Object> data,String key){try{return LocalDateTime.parse(text(data,key,40,true));}catch(Exception e){throw new IllegalArgumentException("Thời gian "+key+" không hợp lệ");}}
 private String choice(Map<String,Object> data,String key,String... choices){String s=text(data,key,60,true);if(!List.of(choices).contains(s))throw new IllegalArgumentException("Trạng thái không hợp lệ");return s;}
 private String name(User u){return u==null?"":Objects.toString(u.getLastName(),"")+" "+Objects.toString(u.getFirstName(),"");}
 @Transactional(readOnly=true) public List<Map<String,Object>> list(String module){
  current.operator();
  return switch(module){
   case "motorbikes" -> {admin();yield bikes.findAll().stream().map(b->row("id",b.getId(),"name",b.getName(),"licensePlate",b.getLicensePlate(),"brandId",b.getBrand()==null?null:b.getBrand().getId(),"categoryId",b.getCategory()==null?null:b.getCategory().getId(),"year",b.getYear(),"cc",b.getCc(),"pricePerDay",b.getPricePerDay(),"depositAmount",b.getDepositAmount(),"status",b.getStatus().name(),"odoKm",b.getOdoKm(),"description",b.getDescription(),"imageUrl",b.getImagePrimary())).toList();}
   case "customers","staffs" -> {admin();UserRole role=module.equals("staffs")?UserRole.STAFF:UserRole.CUSTOMER;yield users.findAll().stream().filter(u->u.getRole()==role).map(u->row("id",u.getId(),"username",u.getUsername(),"firstName",u.getFirstName(),"lastName",u.getLastName(),"email",u.getEmail(),"phone",u.getPhone(),"address",u.getAddress(),"status",u.getStatus())).toList();}
   case "rental-orders" -> orders.findAll().stream().sorted(Comparator.comparing(RentalOrder::getCreatedAt).reversed()).map(o->row("id",o.getId(),"orderCode",o.getOrderCode(),"customer",name(o.getCustomer()),"phone",o.getCustomer().getPhone(),"motorbike",o.getMotorbike().getName(),"pickupDate",o.getPickupDate(),"returnDate",o.getReturnDate(),"totalPrice",o.getTotalPrice(),"paid",Objects.requireNonNullElse(o.getDepositPaid(),0)+Objects.requireNonNullElse(o.getBalancePaid(),0),"status",o.getStatus().name(),"staff",name(o.getStaff()))).toList();
   case "maintenance" -> maintenance.findAll().stream().map(m->row("id",m.getId(),"motorbikeId",m.getMotorbike().getId(),"motorbike",m.getMotorbike().getName(),"type",m.getType(),"description",m.getDescription(),"cost",m.getCost(),"status",m.getStatus(),"reportedBy",name(m.getReportedBy()),"startedAt",m.getStartedAt(),"completedAt",m.getCompletedAt())).toList();
   case "payments" -> payments.findAll().stream().sorted(Comparator.comparing(Payment::getCreatedAt).reversed()).map(p->row("id",p.getId(),"orderId",p.getRentalOrder().getId(),"orderCode",p.getRentalOrder().getOrderCode(),"amount",Financials.signedAmount(p),"receivedAmount",Financials.signedReceived(p),"absoluteAmount",p.getAmount(),"transactionType",Financials.type(p),"direction",p.getDirection(),"transactionStatus",Financials.transactionStatus(p),"processedByStaffId",p.getProcessedByStaffId(),"processedBy",p.getProcessedByStaffId()==null?("PAYOS".equals(p.getPaymentMethod())?"PayOS":"Chưa ghi nhận"):users.findById(p.getProcessedByStaffId()).map(this::name).orElse(p.getProcessedByStaffId()),"processedAt",p.getProcessedAt(),"note",p.getNote(),"components",p.getComponentsJson(),"purpose",p.getPurpose(),"method",p.getPaymentMethod(),"status",p.getStatus(),"paidAt",p.getPaidAt(),"reference",p.getTransactionCode())).toList();
   case "extensions" -> extensions.findAll().stream().map(e->row("id",e.getId(),"orderCode",e.getRentalOrder().getOrderCode(),"customer",name(e.getRequestedBy()),"newReturnDate",e.getNewReturnDate(),"extraAmount",e.getExtraAmount(),"reason",e.getReason(),"status",extensionService.effectiveStatus(e),"expiresAt",e.getExpiresAt(),"approvedAt",e.getApprovedAt())).toList();
   case "reviews" -> reviews.findAll().stream().map(r->row("id",r.getId(),"motorbike",r.getMotorbike().getName(),"customer",name(r.getCustomer()),"rating",r.getRating(),"comment",r.getComment(),"staffReply",r.getStaffReply(),"createdAt",r.getCreatedAt())).toList();
   default -> throw new IllegalArgumentException("Danh mục không tồn tại");
  };
 }
 @Transactional(readOnly=true) public Map<String,Object> lookups(){current.operator();return row("bikes",bikes.findAll().stream().map(b->row("id",b.getId(),"name",b.getName()+" · "+Objects.toString(b.getLicensePlate(),"Chưa có biển số"))).toList(),"brands",brands.findAll().stream().map(b->row("id",b.getId(),"name",b.getName())).toList(),"categories",categories.findAll().stream().map(c->row("id",c.getId(),"name",c.getName(),"securityDeposit",c.getSecurityDeposit())).toList());}
 @Transactional public void save(String module,String recordId,Map<String,Object> d){
  var actor=current.operator();boolean create=recordId==null;
  switch(module){
   case "motorbikes" -> {
    admin();Motorbike b=create?new Motorbike():bikes.lockById(Long.valueOf(recordId)).orElseThrow();
    b.setName(text(d,"name",150,true));b.setLicensePlate(text(d,"licensePlate",40,true));b.setYear(Integer.toString(number(d,"year",1980,Year.now().getValue()+1)));b.setCc(number(d,"cc",40,2000));b.setPricePerDay(number(d,"pricePerDay",1000,10000000));b.setDepositAmount(number(d,"depositAmount",0,100000000));b.setOdoKm(number(d,"odoKm",0,2000000));b.setDescription(text(d,"description",4000,false));
    b.setBrand(brands.findById(id(d,"brandId")).orElseThrow());b.setCategory(categories.findById(id(d,"categoryId")).orElseThrow());if(b.getCategory().getSecurityDeposit()!=null)b.setDepositAmount(b.getCategory().getSecurityDeposit());
    var status=MotorbikeStatus.valueOf(choice(d,"status","AVAILABLE","UNAVAILABLE"));
    if(!create && (b.getStatus()==MotorbikeStatus.RENTED || b.getStatus()==MotorbikeStatus.MAINTENANCE))throw new IllegalArgumentException("Xe đang thuê hoặc bảo dưỡng; hoàn tất quy trình trước khi chỉnh sửa");
    if(status==MotorbikeStatus.UNAVAILABLE && !create && orders.findAll().stream().anyMatch(o->o.getMotorbike().getId().equals(b.getId()) && !List.of(RentalStatus.COMPLETED,RentalStatus.CANCELLED,RentalStatus.REJECTED).contains(o.getStatus())))throw new IllegalArgumentException("Xe đang có lịch đặt, chưa thể ngừng hoạt động");
    b.setStatus(status);if(create){b.setRating(0.0);b.setReviewCount(0);}bikes.saveAndFlush(b);
    String url=text(d,"imageUrl",1500,false);if(!url.isBlank()){
     if(!(url.startsWith("https://") || url.startsWith("/images/") || url.startsWith("/uploads/")))throw new IllegalArgumentException("Ảnh phải dùng HTTPS hoặc đường dẫn ảnh của cửa hàng");
     var image=images.findByMotorbikeIdOrderBySortOrderAsc(b.getId()).stream().filter(i->Boolean.TRUE.equals(i.getIsPrimary())).findFirst().orElseGet(()->MotorbikeImage.builder().motorbike(b).isPrimary(true).build());image.setImageUrl(url);images.save(image);
    }
   }
   case "customers","staffs" -> {
    admin();UserRole role=module.equals("staffs")?UserRole.STAFF:UserRole.CUSTOMER;
    var u=create?new User():users.findById(recordId).orElseThrow();if(!create && u.getRole()!=role)throw new IllegalArgumentException("Sai loại tài khoản");
    String username=text(d,"username",100,true),email=text(d,"email",255,true),phone=text(d,"phone",20,true);
    if(!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || !phone.matches("[+0-9 ()-]{9,20}"))throw new IllegalArgumentException("Email hoặc số điện thoại không hợp lệ");
    if(users.findByEmail(email).filter(x->!Objects.equals(x.getId(),u.getId())).isPresent() || users.findByPhone(phone).filter(x->!Objects.equals(x.getId(),u.getId())).isPresent() || users.findByUsername(username).filter(x->!Objects.equals(x.getId(),u.getId())).isPresent())throw new IllegalArgumentException("Tên đăng nhập, email hoặc số điện thoại đã tồn tại");
    if(!create && !u.getUsername().equals(username))throw new IllegalArgumentException("Không đổi tên đăng nhập của tài khoản đã tạo");
    u.setUsername(username);u.setEmail(email);u.setPhone(phone);u.setFirstName(text(d,"firstName",100,true));u.setLastName(text(d,"lastName",100,true));u.setAddress(text(d,"address",255,false));u.setRole(role);u.setStatus(choice(d,"status","ACTIVE","BLOCKED"));
    String password=text(d,"password",72,false);if(create && password.isBlank())throw new IllegalArgumentException("Cần mật khẩu cho tài khoản mới");
    if(!password.isBlank()){if(password.length()<8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("Mật khẩu tối thiểu 8 ký tự, tối đa 72 byte");u.setPassword(encoder.encode(password));}
    users.saveAndFlush(u);if(!create && (!password.isBlank() || "BLOCKED".equals(u.getStatus())))tokens.revokeAll(u.getId());
   }
   case "maintenance" -> {
    MaintenanceRecord m=create?new MaintenanceRecord():maintenance.findById(Long.valueOf(recordId)).orElseThrow();
    Motorbike bike=bikes.lockById(create?id(d,"motorbikeId"):m.getMotorbike().getId()).orElseThrow();
    if(!create && "COMPLETED".equals(m.getStatus()))throw new IllegalArgumentException("Phiếu đã hoàn tất không thể chỉnh sửa");
    if(create && (bike.getStatus()==MotorbikeStatus.RENTED || bike.getStatus()==MotorbikeStatus.MAINTENANCE || orders.findAll().stream().anyMatch(o->o.getMotorbike().getId().equals(bike.getId()) && !List.of(RentalStatus.CANCELLED,RentalStatus.REJECTED,RentalStatus.COMPLETED).contains(o.getStatus()))))throw new IllegalArgumentException("Cần xử lý các đơn thuê hoặc phiếu bảo dưỡng hiện tại trước");
    m.setMotorbike(bike);if(create){m.setReportedBy(actor);m.setStartedAt(LocalDateTime.now());}
    m.setType(text(d,"type",100,true));m.setDescription(text(d,"description",4000,true));m.setCost(number(d,"cost",0,100000000));m.setStatus(choice(d,"status","IN_PROGRESS","COMPLETED"));
    bike.setStatus("COMPLETED".equals(m.getStatus())?MotorbikeStatus.AVAILABLE:MotorbikeStatus.MAINTENANCE);if("COMPLETED".equals(m.getStatus()))m.setCompletedAt(LocalDateTime.now());maintenance.save(m);bikes.save(bike);
   }
   case "reviews" -> {if(create)throw new IllegalArgumentException("Không tạo đánh giá thay khách");var r=reviews.findById(Long.valueOf(recordId)).orElseThrow();r.setStaffReply(text(d,"staffReply",2000,true));reviews.save(r);}
   default -> throw new IllegalArgumentException("Danh mục không hỗ trợ lưu");
  }
 }
 @Transactional(readOnly=true) public Map<String,Object> reports(LocalDate from,LocalDate to){
  admin();if(from==null)from=LocalDate.now().withDayOfMonth(1);if(to==null)to=LocalDate.now();if(to.isBefore(from) || from.plusYears(5).isBefore(to))throw new IllegalArgumentException("Khoảng báo cáo không hợp lệ");
  var start=from.atStartOfDay();var end=to.plusDays(1).atStartOfDay();

  var allPaymentsList = payments.findAll();
  var periodPayments = allPaymentsList.stream()
      .filter(p -> p.getPaidAt() != null && !p.getPaidAt().isBefore(start) && p.getPaidAt().isBefore(end))
      .toList();

  long payosIn = periodPayments.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "IN".equals(p.getDirection()) && "PAYOS".equals(p.getPaymentMethod()))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long cashIn = periodPayments.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "IN".equals(p.getDirection()) && "CASH".equals(p.getPaymentMethod()))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long bankTransferIn = periodPayments.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "IN".equals(p.getDirection()) && "BANK_TRANSFER".equals(p.getPaymentMethod()))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long rentalCollected = periodPayments.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "IN".equals(p.getDirection()) &&
              List.of("BOOKING_PAYMENT", "RENTAL_REMAINING", "EXTENSION_PAYMENT").contains(Financials.type(p)))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long rentalRefunded = periodPayments.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "OUT".equals(p.getDirection()) &&
              List.of("BOOKING_REFUND", "BOOKING_ADJUSTMENT_REFUND", "EARLY_RETURN_REFUND").contains(Financials.type(p)))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long lateFeeCollected = periodPayments.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "IN".equals(p.getDirection()) &&
              "LATE_FEE".equals(Financials.type(p)))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long rentalNetRevenue = rentalCollected - rentalRefunded;

  long assetDepositIn = periodPayments.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "IN".equals(p.getDirection()) &&
              List.of("ASSET_DEPOSIT", "ASSET_DEPOSIT_ADDITIONAL").contains(Financials.type(p)))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long assetDepositRefunded = periodPayments.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "OUT".equals(p.getDirection()) &&
              "ASSET_DEPOSIT_REFUND".equals(Financials.type(p)))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long assetDepositDeducted = periodPayments.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "OUT".equals(p.getDirection()) &&
              "DEPOSIT_DEDUCTION".equals(Financials.type(p)))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long systemTotalDepositIn = allPaymentsList.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "IN".equals(p.getDirection()) &&
              List.of("ASSET_DEPOSIT", "ASSET_DEPOSIT_ADDITIONAL").contains(Financials.type(p)))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long systemTotalDepositOut = allPaymentsList.stream()
      .filter(p -> "PAID".equals(p.getStatus()) && "OUT".equals(p.getDirection()) &&
              List.of("ASSET_DEPOSIT_REFUND", "DEPOSIT_DEDUCTION").contains(Financials.type(p)))
      .mapToLong(p -> Math.abs(p.getAmount())).sum();

  long totalAssetDepositHeld = Math.max(0, systemTotalDepositIn - systemTotalDepositOut);

  long cost = maintenance.findAll().stream()
      .filter(m -> "COMPLETED".equals(m.getStatus()) && m.getCompletedAt() != null &&
              !m.getCompletedAt().isBefore(start) && m.getCompletedAt().isBefore(end))
      .mapToLong(m -> Objects.requireNonNullElse(m.getCost(), 0)).sum();

  long operatingProfit = rentalNetRevenue + lateFeeCollected - cost;

  var completed = orders.findAll().stream()
      .filter(o -> o.getStatus() == RentalStatus.COMPLETED && o.getActualReturnDate() != null &&
              !o.getActualReturnDate().isBefore(start) && o.getActualReturnDate().isBefore(end))
      .toList();
  long completedRevenue = completed.stream().mapToLong(RentalOrder::getTotalPrice).sum();

  long revenue = completedRevenue > 0 ? completedRevenue : (rentalNetRevenue + lateFeeCollected);
  long collected = periodPayments.stream()
      .filter(p -> List.of("PAID", "REVIEW_REQUIRED", "REFUNDED").contains(Objects.toString(p.getStatus(), "")) &&
              !Financials.asset(p))
      .mapToLong(p -> Objects.requireNonNullElse(Financials.signedReceived(p), Financials.signedAmount(p))).sum();

  var daily = new TreeMap<String, Long>();
  for (var o : completed) {
      daily.merge(o.getActualReturnDate().toLocalDate().toString(), (long) o.getTotalPrice(), Long::sum);
  }

  return row(
      "from", from, "to", to,
      "revenue", revenue,
      "collected", collected,
      "rentalCollected", rentalCollected,
      "rentalRefunded", rentalRefunded,
      "rentalNetRevenue", rentalNetRevenue,
      "lateFeeCollected", lateFeeCollected,
      "payosIn", payosIn,
      "cashIn", cashIn,
      "bankTransferIn", bankTransferIn,
      "assetDepositIn", assetDepositIn,
      "assetDepositRefunded", assetDepositRefunded,
      "assetDepositDeducted", assetDepositDeducted,
      "totalAssetDepositHeld", totalAssetDepositHeld,
      "maintenanceCost", cost,
      "operatingProfit", operatingProfit,
      "completedOrders", completed.size(),
      "totalBikes", bikes.count(),
      "activeRentals", orders.findAll().stream().filter(o -> List.of(RentalStatus.HANDED_OVER, RentalStatus.EXTENDED, RentalStatus.OVERDUE).contains(o.getStatus())).count(),
      "paymentsToReview", allPaymentsList.stream().filter(p -> "REVIEW_REQUIRED".equals(p.getStatus())).count(),
      "daily", daily
  );
 }
 @Transactional public void settings(Map<String,Object> d){
  admin();var s=settings.findById(1L).orElseThrow();s.setName(text(d,"name",150,true));s.setAddress(text(d,"address",255,true));s.setPhone(text(d,"phone",20,true));s.setEmail(text(d,"email",255,true));s.setRepresentative(text(d,"representative",150,true));s.setHours(text(d,"hours",255,true));s.setTerms(text(d,"terms",4000,false));s.setZalo(text(d,"zalo",255,false));
  try {double lat=Double.parseDouble(text(d,"lat",40,true)),lng=Double.parseDouble(text(d,"lng",40,true));if(!Double.isFinite(lat)||!Double.isFinite(lng)||Math.abs(lat)>90||Math.abs(lng)>180)throw new Exception();s.setLat(lat);s.setLng(lng);}catch(Exception e){throw new IllegalArgumentException("Tọa độ không hợp lệ");}
  s.setDeliveryBaseFee(number(d,"deliveryBaseFee",0,1000000));s.setDeliveryPerKm(number(d,"deliveryPerKm",0,100000));s.setDeliveryBaseKm(number(d,"deliveryBaseKm",0,100));s.setDeliveryMaxKm(number(d,"deliveryMaxKm",1,200));s.setHoldMinutes(number(d,"holdMinutes",1,1440));s.setDepositPercent(number(d,"depositPercent",1,100));settings.save(s);
 }
}
