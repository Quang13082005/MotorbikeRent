package com.motorrent.motorbikerental.controller.web;

import com.motorrent.motorbikerental.service.PreviewMockDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Preview Controller - trang trung tâm xem giao diện.
 * Truy cập: http://localhost:8080/preview
 */
@Controller
@RequestMapping("/preview")
@RequiredArgsConstructor
public class PreviewController {

    private final PreviewMockDataService mockDataService;

    /**
     * Trang chủ Preview - hiển thị 3 role: Customer, Staff, Admin
     */
    @GetMapping({"", "/"})
    public String previewIndex(Model model) {
        model.addAttribute("pageTitle", "Preview - MotoRent Đà Nẵng");
        return "preview/index";
    }

    /**
     * Preview Customer role - landing page
     */
    @GetMapping("/customer")
    public String previewCustomer() {
        return "redirect:/customer/home";
    }

    /**
     * Preview Staff role - dashboard
     */
    @GetMapping("/staff")
    public String previewStaff() {
        return "redirect:/staff/dashboard";
    }

    /**
     * Preview Admin role - dashboard
     */
    @GetMapping("/admin")
    public String previewAdmin() {
        return "redirect:/admin/dashboard";
    }
}
