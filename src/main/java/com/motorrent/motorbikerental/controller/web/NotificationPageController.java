package com.motorrent.motorbikerental.controller.web;

@org.springframework.stereotype.Controller
@lombok.RequiredArgsConstructor
public class NotificationPageController {
    private final com.motorrent.motorbikerental.service.NotificationService service;
    @org.springframework.web.bind.annotation.GetMapping("/customer/notifications")
    public String page(org.springframework.ui.Model model) {
        model.addAttribute("notifications", service.mine());
        return "customer/notifications";
    }
    @org.springframework.web.bind.annotation.PostMapping("/customer/notifications/{id}/read")
    public String read(@org.springframework.web.bind.annotation.PathVariable Long id) {
        service.read(id);
        return "redirect:/customer/notifications";
    }
}
