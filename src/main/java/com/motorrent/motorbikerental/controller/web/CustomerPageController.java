package com.motorrent.motorbikerental.controller.web;
import com.motorrent.motorbikerental.service.MotorbikeService;
import com.motorrent.motorbikerental.service.RentalOrderService;
import com.motorrent.motorbikerental.repository.UserRepository;
import com.motorrent.motorbikerental.entity.User;
import com.motorrent.motorbikerental.enums.UserRole;
import com.motorrent.motorbikerental.enums.RentalStatus;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class CustomerPageController {

    private final MotorbikeService motorbikeService;
    private final com.motorrent.motorbikerental.service.AvailabilityService availability;
    private final com.motorrent.motorbikerental.service.TravelPlaceService travelPlaces;
    private final RentalOrderService rentalOrderService;
    private final com.motorrent.motorbikerental.security.CurrentUser current;
    private final com.motorrent.motorbikerental.service.PaymentService payments;
    private final com.motorrent.motorbikerental.service.ExtensionService extensions;
    private final com.motorrent.motorbikerental.repository.ReviewRepository reviews;
    private final com.motorrent.motorbikerental.repository.RentalContractRepository contracts;
    private final com.motorrent.motorbikerental.repository.RentalOrderRepository rentalOrderRepository;

    @Value("${google.maps.api-key:}")
    private String googleMapsApiKey;

    @org.springframework.web.bind.annotation.ModelAttribute("googleMapsApiKey")
    public String addGoogleMapsApiKey() {
        return googleMapsApiKey;
    }

    @GetMapping({"/", "/customer/home"})
    public String home(Model model, @RequestParam(defaultValue="xe-ga") String recommended) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            boolean isStaff = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) return "redirect:/admin/dashboard";
            if (isStaff) return "redirect:/staff/dashboard";
        }

        if(!List.of("xe-ga","xe-so","xe-con-tay").contains(recommended)) recommended="xe-ga";
        model.addAttribute("recommended",recommended);
        model.addAttribute("motorbikes",motorbikeService.filterMotorbikes("",recommended,null,null,null,"popular").stream().filter(m->!List.of("MAINTENANCE","UNAVAILABLE").contains(m.getStatus())).limit(6).toList());
        model.addAttribute("travelPlaces",travelPlaces.featured());
        model.addAttribute("recentReviews",reviews.findAll().stream().sorted(java.util.Comparator.comparing(com.motorrent.motorbikerental.entity.Review::getCreatedAt).reversed()).limit(6).toList());
        model.addAttribute("activeNav", "home");
        return "customer/home";
    }

    @GetMapping("/customer/shop-information")
    public String shopInformation() {
        return "customer/shop-information";
    }

    @GetMapping("/customer/travel-places")
    public String travelPlaces(@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="0") int page,Model model){
        var results=travelPlaces.published(query,page,24);model.addAttribute("places",results.getContent());model.addAttribute("placePage",results);model.addAttribute("query",query);
        return "customer/travel-places";
    }

    @GetMapping("/customer/motorbike-list")
    public String motorbikeList(Model model,
                                @RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "all") String category,
                                @RequestParam(required = false) String priceFilter,
                                @RequestParam(required = false) String ccFilter,
                                @RequestParam(required = false) String statusFilter,
                                @RequestParam(defaultValue = "popular") String sortBy,
                                @RequestParam(defaultValue = "store") String location,
                                @RequestParam(required = false) String deliveryAddress,
                                @RequestParam(required = false) String deliveryLat,
                                @RequestParam(required = false) String deliveryLng,
                                @RequestParam(required=false) java.time.LocalDateTime pickup, @RequestParam(required=false) java.time.LocalDateTime returnAt) {
        
        List<com.motorrent.motorbikerental.dto.response.MotorbikeResponse> motorbikes = 
            motorbikeService.filterMotorbikes(keyword, category, priceFilter, ccFilter, pickup!=null && returnAt!=null ? null : statusFilter, sortBy);
        
        try {
            if(pickup!=null || returnAt!=null){
                com.motorrent.motorbikerental.service.PricingService.validateStart(pickup);
                com.motorrent.motorbikerental.service.PricingService.days(pickup,returnAt);
                motorbikes=motorbikes.stream().filter(m->availability.available(m.getId(),null,pickup,returnAt)).toList();
            }
        }catch(IllegalArgumentException e){model.addAttribute("searchError",e.getMessage());motorbikes=List.of();}
        model.addAttribute("pickup",pickup);model.addAttribute("returnAt",returnAt);
        model.addAttribute("motorbikes", motorbikes);
        model.addAttribute("ccOptions",motorbikeService.getAllMotorbikes().stream().map(com.motorrent.motorbikerental.dto.response.MotorbikeResponse::getCc).filter(java.util.Objects::nonNull).distinct().sorted().toList());
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("priceFilter", priceFilter);
        model.addAttribute("ccFilter", ccFilter);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("location", location);
        model.addAttribute("deliveryAddress", deliveryAddress);
        model.addAttribute("deliveryLat", deliveryLat);
        model.addAttribute("deliveryLng", deliveryLng);
        model.addAttribute("activeNav", "motorbike-list");
        return "customer/motorbike-list";
    }

    @GetMapping("/customer/motorbike-detail/{id}")
    public String motorbikeDetail(@PathVariable Long id, Model model) {
        model.addAttribute("motorbike", motorbikeService.getMotorbikeDetail(id));
        model.addAttribute("bikeReviews",reviews.findByMotorbikeId(id));
        model.addAttribute("activeNav", "motorbike-list");
        return "customer/motorbike-detail";
    }

    @GetMapping("/customer/rental-contract/{id}")
    public String rentalContract(@PathVariable Long id, Model model) {
        model.addAttribute("customer",current.customer());
        model.addAttribute("motorbike", motorbikeService.getMotorbikeDetail(id));
        model.addAttribute("activeNav", "motorbike-list");
        return "customer/rental-contract";
    }

    @GetMapping("/customer/my-rentals")
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
        rentalOrderService.requireOwned(orderId);
        try {
            payments.reconcile(orderId);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(CustomerPageController.class).warn("Auto reconcile failed for order {}: {}", orderId, e.getMessage());
        }
        var order=rentalOrderService.requireOwned(orderId);
        model.addAttribute("rental",order);
        model.addAttribute("payments",payments.list(orderId));
        return "customer/payment-result";
    }
}
