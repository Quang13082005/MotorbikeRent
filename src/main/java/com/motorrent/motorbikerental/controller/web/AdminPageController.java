package com.motorrent.motorbikerental.controller.web;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activeNav", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/motorbikes")
    public String motorbikes(Model model) {
        model.addAttribute("activeNav", "motorbikes");
        return "admin/motorbikes";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("activeNav", "customers");
        return "admin/customers";
    }

    @GetMapping("/staffs")
    public String staffs(Model model) {
        model.addAttribute("activeNav", "staffs");
        return "admin/staffs";
    }

    @GetMapping("/promotions")
    public String promotions(Model model) {
        model.addAttribute("activeNav", "promotions");
        return "admin/promotions";
    }

    @GetMapping("/stations")
    public String stations(Model model) {
        model.addAttribute("activeNav", "stations");
        return "admin/stations";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("activeNav", "reports");
        return "admin/reports";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("activeNav", "settings");
        return "admin/settings";
    }
}