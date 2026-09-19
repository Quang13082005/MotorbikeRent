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
        model.addAttribute("pendingOrders", 5);
        model.addAttribute("handoverToday", 12);
        model.addAttribute("returnToday", 8);
        model.addAttribute("maintenance", 3);
        model.addAttribute("totalRevenue", 12500000);
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

    @GetMapping("/handover")
    public String handover(Model model) {
        model.addAttribute("activeNav", "handover");
        return "staff/handover";
    }

    @GetMapping("/return")
    public String returnMotorbike(Model model) {
        model.addAttribute("activeNav", "return");
        return "staff/return";
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
}