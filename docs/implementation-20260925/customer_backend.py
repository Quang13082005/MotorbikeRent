from pathlib import Path
R=Path('src/main/java/com/motorrent/motorbikerental')
def put(p,s): (R/p).write_text(s,encoding='utf-8')
def edit(p,a,b):
 f=R/p;s=f.read_text('utf-8');assert a in s,(p,a);f.write_text(s.replace(a,b),encoding='utf-8')
f=R/'controller/web/CustomerPageController.java';t=f.read_text('utf-8');a=t.index('    @GetMapping("/customer/my-rentals")');t=t[:a]+'''    @GetMapping("/customer/my-rentals")
    public String myRentals(Model model) {
        var all=rentalOrderService.getCustomerOrders(current.customer().getId());
        model.addAttribute("rentals",all.stream().filter(o->!List.of("COMPLETED","CANCELLED","REJECTED").contains(o.getStatus())).toList());
        model.addAttribute("activeNav","my-rentals"); return "customer/my-rentals";
    }
    @GetMapping("/customer/rental-history")
    public String rentalHistory(Model model,@RequestParam(defaultValue="ALL") String status,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="0") int months) {
        var all=rentalOrderService.getCustomerOrders(current.customer().getId()).stream().filter(o->List.of("COMPLETED","CANCELLED","REJECTED").contains(o.getStatus())).toList();
        model.addAttribute("totalHistoryCount",all.size());model.addAttribute("completedCount",all.stream().filter(o->"COMPLETED".equals(o.getStatus())).count());model.addAttribute("cancelledCount",all.stream().filter(o->!"COMPLETED".equals(o.getStatus())).count());
        var filtered=all.stream().filter(o->"ALL".equals(status) || status.equals(o.getStatus()) || "CANCELLED".equals(status) && "REJECTED".equals(o.getStatus()))
            .filter(o->q.isBlank() || (o.getOrderCode()+" "+o.getMotorbikeName()).toLowerCase(java.util.Locale.ROOT).contains(q.toLowerCase(java.util.Locale.ROOT)))
            .filter(o->months<=0 || o.getPickupDate().isAfter(java.time.LocalDateTime.now().minusMonths(Math.min(months,120)))).toList();
        int pages=Math.max(1,(filtered.size()+7)/8);int index=Math.max(0,Math.min(page,pages-1));
        model.addAttribute("rentals",filtered.stream().skip(index*8L).limit(8).toList());model.addAttribute("total",filtered.size());model.addAttribute("page",index);model.addAttribute("pages",pages);model.addAttribute("q",q);model.addAttribute("status",status);model.addAttribute("months",months);model.addAttribute("activeNav","history");
        return "customer/rental-history";
    }
    @GetMapping("/customer/profile") public String profile(Model model) { model.addAttribute("customer",current.get());return "customer/profile"; }
    @GetMapping("/customer/edit-profile") public String editProfile() { return "redirect:/customer/profile"; }
    @GetMapping("/customer/rental-detail/{id}") public String rentalDetail(@PathVariable Long id,Model model) {
        var order=rentalOrderService.requireReadable(id);model.addAttribute("rental",order);
        model.addAttribute("payments",payments.list(id));model.addAttribute("extensions",extensions.forOrder(id));
        model.addAttribute("reviewed",reviews.existsByRentalOrderId(id));
        model.addAttribute("contract",contracts.findByRentalOrderId(id).orElse(null));return "customer/rental-detail";
    }
    @GetMapping("/customer/rental-extension/{id}") public String rentalExtension(@PathVariable Long id,Model model) {model.addAttribute("rental",rentalOrderService.requireOwned(id));return "customer/rental-extension";}
    @GetMapping("/customer/review/{id}") public String review(@PathVariable Long id,Model model) {
        var order=rentalOrderService.requireOwned(id);if(order.getStatus()!=RentalStatus.COMPLETED || reviews.existsByRentalOrderId(id)) return "redirect:/customer/rental-detail/"+id;
        model.addAttribute("rental",order);return "customer/review";
    }
    @GetMapping("/customer/payment/{id}") public String payment(@PathVariable Long id) {rentalOrderService.requireOwned(id);return "redirect:/customer/payment-result?orderId="+id;}
    @GetMapping("/customer/payment-result") public String paymentResult(Model model,@RequestParam Long orderId) {
        var order=rentalOrderService.requireOwned(orderId);model.addAttribute("rental",order);model.addAttribute("payments",payments.list(orderId));return "customer/payment-result";
    }
}
'''
t=t.replace('private final UserRepository userRepository;','''private final com.motorrent.motorbikerental.security.CurrentUser current;
    private final com.motorrent.motorbikerental.service.PaymentService payments;
    private final com.motorrent.motorbikerental.service.ExtensionService extensions;
    private final com.motorrent.motorbikerental.repository.ReviewRepository reviews;
    private final com.motorrent.motorbikerental.repository.RentalContractRepository contracts;''')
t=t.replace('public String rentalContract(@PathVariable Long id, Model model) {','public String rentalContract(@PathVariable Long id, Model model) {\n        model.addAttribute("customer",current.customer());')
t=t.replace('@RequestParam(required = false) String deliveryLng) {','@RequestParam(required = false) String deliveryLng,\n                                @RequestParam(required=false) java.time.LocalDateTime pickup, @RequestParam(required=false) java.time.LocalDateTime returnAt) {')
t=t.replace('model.addAttribute("motorbikes", motorbikes);','''if(pickup!=null && returnAt!=null) { com.motorrent.motorbikerental.service.PricingService.days(pickup,returnAt);motorbikes=motorbikes.stream().filter(m->!"MAINTENANCE".equals(m.getStatus()) && !"UNAVAILABLE".equals(m.getStatus()) && !rentalOrderRepository.existsOverlappingOrder(m.getId(),pickup,returnAt)).toList(); }
        model.addAttribute("pickup",pickup);model.addAttribute("returnAt",returnAt);
        model.addAttribute("motorbikes", motorbikes);''')
f.write_text(t,encoding='utf-8')
edit('repository/RentalExtensionRepository.java','public interface RentalExtensionRepository extends JpaRepository<RentalExtension, Long> {','public interface RentalExtensionRepository extends JpaRepository<RentalExtension, Long> {\n java.util.List<RentalExtension> findByRentalOrderIdOrderByCreatedAtDesc(Long id);\n boolean existsByRentalOrderIdAndStatus(Long id,String status);')
put('service/ExtensionService.java','''package com.motorrent.motorbikerental.service;
import com.motorrent.motorbikerental.entity.*; import com.motorrent.motorbikerental.repository.*; import com.motorrent.motorbikerental.enums.RentalStatus; import com.motorrent.motorbikerental.security.CurrentUser;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.*; import java.util.*;
@Service @RequiredArgsConstructor
public class ExtensionService {
 private final RentalOrderRepository orders;private final RentalExtensionRepository extensions;private final MotorbikeRepository bikes;private final CurrentUser current;
 public List<Map<String,Object>> forOrder(Long id) {current.canRead(orders.findById(id).orElseThrow());return extensions.findByRentalOrderIdOrderByCreatedAtDesc(id).stream().map(e->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.getId());m.put("newReturnDate",e.getNewReturnDate());m.put("extraAmount",e.getExtraAmount());m.put("status",e.getStatus());m.put("reason",e.getReason());return m;}).toList();}
 @Transactional public void request(Long id,LocalDateTime end,String reason) {
  var order=orders.lockById(id).orElseThrow();current.owns(order);
  if(!List.of(RentalStatus.HANDED_OVER,RentalStatus.EXTENDED).contains(order.getStatus()))throw new IllegalArgumentException("Chỉ được gia hạn khi đang thuê xe");
  if(extensions.existsByRentalOrderIdAndStatus(id,"PENDING"))throw new IllegalArgumentException("Bạn đã có yêu cầu gia hạn chờ xử lý");
  int days=PricingService.days(order.getReturnDate(),end);
  if(orders.overlapping(order.getMotorbike().getId(),id,order.getReturnDate(),end)>0)throw new IllegalArgumentException("Xe đã có lịch đặt trong thời gian gia hạn");
  int rate=Objects.requireNonNullElse(order.getDailyRate(),order.getMotorbike().getPricePerDay());
  extensions.save(RentalExtension.builder().rentalOrder(order).requestedBy(current.get()).originalReturnDate(order.getReturnDate()).newReturnDate(end).extraDays(days).extraAmount(Math.multiplyExact(days,rate)).reason(reason).build());
 }
 @Transactional public void decide(Long id,boolean approve) {
  var actor=current.operator();var e=extensions.findById(id).orElseThrow();
  bikes.lockById(e.getRentalOrder().getMotorbike().getId()).orElseThrow();var order=orders.lockById(e.getRentalOrder().getId()).orElseThrow();
  if(!"PENDING".equals(e.getStatus()))throw new IllegalArgumentException("Yêu cầu đã được xử lý");
  if(approve) {
   if(!List.of(RentalStatus.HANDED_OVER,RentalStatus.EXTENDED,RentalStatus.OVERDUE).contains(order.getStatus()) || !order.getReturnDate().equals(e.getOriginalReturnDate()))throw new IllegalArgumentException("Đơn thuê đã thay đổi, cần tạo yêu cầu mới");
   if(orders.overlapping(order.getMotorbike().getId(),order.getId(),order.getReturnDate(),e.getNewReturnDate())>0)throw new IllegalArgumentException("Xe đã có đơn trùng lịch");
   order.setReturnDate(e.getNewReturnDate());order.setDays(Math.addExact(order.getDays(),e.getExtraDays()));order.setTotalPrice(Math.addExact(order.getTotalPrice(),e.getExtraAmount()));order.setRentalFee(Math.addExact(Objects.requireNonNullElse(order.getRentalFee(),order.getTotalPrice()-e.getExtraAmount()),e.getExtraAmount()));order.setStatus(RentalStatus.EXTENDED);orders.save(order);
  }e.setApprovedBy(actor);e.setStatus(approve?"APPROVED":"REJECTED");extensions.save(e);
 }
}
''')
put('controller/api/ExtensionController.java','''package com.motorrent.motorbikerental.controller.api;
@org.springframework.web.bind.annotation.RestController @lombok.RequiredArgsConstructor
public class ExtensionController {
 private final com.motorrent.motorbikerental.service.ExtensionService service;
 public record Request(java.time.LocalDateTime newReturnDate,String reason) {}
 @org.springframework.web.bind.annotation.PostMapping("/api/rental-orders/{id}/extensions")
 public Object request(@org.springframework.web.bind.annotation.PathVariable Long id,@org.springframework.web.bind.annotation.RequestBody Request request) {service.request(id,request.newReturnDate(),request.reason());return java.util.Map.of("message","Đã gửi yêu cầu gia hạn");}
 @org.springframework.web.bind.annotation.PostMapping("/api/operations/extensions/{id}")
 public Object decide(@org.springframework.web.bind.annotation.PathVariable Long id,@org.springframework.web.bind.annotation.RequestParam boolean approve) {service.decide(id,approve);return java.util.Map.of("message","Đã xử lý yêu cầu");}
}
''')
edit('service/ReviewService.java','rentalOrderRepository.findById(request.getOrderId())','rentalOrderRepository.lockById(request.getOrderId())')
edit('service/ReviewService.java','        reviewRepository.save(review);','''        if(request.getRating()==null || !Double.isFinite(request.getRating()) || request.getRating()<1 || request.getRating()>5 || request.getRating()%1!=0) throw new IllegalArgumentException("Chọn số sao từ 1 đến 5");
        if(request.getComment()!=null && request.getComment().length()>2000) throw new IllegalArgumentException("Đánh giá tối đa 2000 ký tự");
        reviewRepository.saveAndFlush(review);
        var bike=order.getMotorbike();var list=reviewRepository.findByMotorbikeId(bike.getId());bike.setReviewCount(list.size());bike.setRating(list.stream().mapToDouble(Review::getRating).average().orElse(0));''')
edit('repository/ReviewRepository.java','boolean existsByRentalOrderId(Long orderId);','boolean existsByRentalOrderId(Long orderId);\n java.util.List<Review> findByMotorbikeId(Long id);')
edit('service/AuthService.java','new Random().nextInt(999999)','new java.security.SecureRandom().nextInt(1000000)')
edit('service/AuthService.java','// Xóa OTP sau khi dùng thành công','tokenRepo.revokeAll(user.getId());\n        // Xóa OTP sau khi dùng thành công')
