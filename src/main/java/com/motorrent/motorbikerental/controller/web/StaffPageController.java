package com.motorrent.motorbikerental.controller.web;
import com.motorrent.motorbikerental.service.RentalOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffPageController {

    private final RentalOrderService rentalOrderService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activeNav", "dashboard");
        var orders = rentalOrderService.getAllOrders();
        var pending = orders.stream().filter(o -> "PENDING".equals(o.getStatus()))
                .sorted(java.util.Comparator.comparing(
                        com.motorrent.motorbikerental.dto.response.RentalOrderResponse::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
        model.addAttribute("pendingCount", pending.size());
        model.addAttribute("preparingCount", orders.stream().filter(o ->
                "CONFIRMED".equals(o.getStatus()) || "READY_FOR_HANDOVER".equals(o.getStatus())).count());
        model.addAttribute("rentingCount", orders.stream().filter(o ->
                "HANDED_OVER".equals(o.getStatus()) || "EXTENDED".equals(o.getStatus())).count());
        model.addAttribute("overdueCount", orders.stream().filter(o -> "OVERDUE".equals(o.getStatus())).count());
        model.addAttribute("pendingRentals", pending.stream().limit(5).toList());
        return "staff/dashboard";
    }

    @GetMapping("/rental-orders")
    public String rentalOrders(Model model) {
        var activeOrders = rentalOrderService.getAllOrders().stream()
                .filter(o -> !java.util.List.of("COMPLETED", "CANCELLED", "REJECTED").contains(o.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("rentals", activeOrders);
        model.addAttribute("activeNav", "rental-orders");
        return "staff/rental-orders";
    }




    
    @GetMapping("/maintenance")
    public String maintenance(Model model) {
        model.addAttribute("activeNav", "maintenance");
        return "staff/maintenance";
    }
    
    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("activeNav", "profile");
        return "staff/profile";
    }
    @GetMapping({"/payments","/extensions","/reviews"})
    public String tools(jakarta.servlet.http.HttpServletRequest request,Model model){String page=request.getRequestURI().substring("/staff/".length());model.addAttribute("activeNav",page);return "staff/"+page;}
}
