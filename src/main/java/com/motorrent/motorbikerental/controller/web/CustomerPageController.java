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
    private final RentalOrderService rentalOrderService;
    private final UserRepository userRepository;

    @GetMapping({"/", "/customer/home"})
    public String home(Model model) {
        model.addAttribute("motorbikes", motorbikeService.getAllMotorbikes());
        model.addAttribute("activeNav", "home");
        return "customer/home";
    }

    @GetMapping("/customer/motorbike-list")
    public String motorbikeList(Model model,
                                @RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "all") String category,
                                @RequestParam(defaultValue = "price_asc") String sort) {
        model.addAttribute("motorbikes", motorbikeService.getAllMotorbikes());
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("sort", sort);
        model.addAttribute("activeNav", "motorbike-list");
        return "customer/motorbike-list";
    }

    @GetMapping("/customer/motorbike-detail/{id}")
    public String motorbikeDetail(@PathVariable Long id, Model model) {
        model.addAttribute("motorbike", motorbikeService.getMotorbikeDetail(id));
        model.addAttribute("activeNav", "motorbike-list");
        return "customer/motorbike-detail";
    }

    @GetMapping("/customer/rental-contract/{id}")
    public String rentalContract(@PathVariable Long id, Model model) {
        model.addAttribute("motorbike", motorbikeService.getMotorbikeDetail(id));
        model.addAttribute("activeNav", "motorbike-list");
        return "customer/rental-contract";
    }

    @GetMapping("/customer/my-rentals")
    public String myRentals(Model model) {
        User customer = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No customer found"));
        var allOrders = rentalOrderService.getCustomerOrders(customer.getId());
        var activeOrders = allOrders.stream()
                .filter(o -> !List.of("COMPLETED", "CANCELLED", "REJECTED").contains(o.getStatus()))
                .collect(Collectors.toList());
        model.addAttribute("rentals", activeOrders);
        model.addAttribute("activeNav", "my-rentals");
        return "customer/my-rentals";
    }

    @GetMapping("/customer/rental-detail/{id}")
    public String rentalDetail(@PathVariable Long id, Model model) {
        model.addAttribute("rental", rentalOrderService.getOrderDetail(id));
        model.addAttribute("activeNav", "my-rentals");
        return "customer/rental-detail";
    }

    @GetMapping("/customer/rental-extension/{id}")
    public String rentalExtension(@PathVariable Long id, Model model) {
        return "customer/rental-extension";
    }

    @GetMapping("/customer/rental-history")
    public String rentalHistory(Model model) {
        User customer = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No customer found"));
        var allOrders = rentalOrderService.getCustomerOrders(customer.getId());
        var historyOrders = allOrders.stream()
                .filter(o -> List.of("COMPLETED", "CANCELLED", "REJECTED").contains(o.getStatus()))
                .collect(Collectors.toList());
        model.addAttribute("rentals", historyOrders);
        model.addAttribute("activeNav", "history");
        return "customer/rental-history";
    }

    @GetMapping("/customer/profile")
    public String profile(Model model) {
        model.addAttribute("activeNav", "profile");
        return "customer/profile";
    }

    @GetMapping("/customer/edit-profile")
    public String editProfile(Model model) {
        return "customer/edit-profile";
    }

    @GetMapping("/customer/change-password")
    public String changePassword() {
        return "customer/change-password";
    }

    @GetMapping("/customer/payment/{id}")
    public String payment(@PathVariable Long id, Model model) {
        return "customer/payment";
    }

    @GetMapping("/customer/payment-result")
    public String paymentResult(@RequestParam(defaultValue = "success") String status, Model model) {
        model.addAttribute("status", status);
        return "customer/payment-result";
    }

    @GetMapping("/customer/review/{id}")
    public String review(@PathVariable Long id, Model model) {
        return "customer/review";
    }
}