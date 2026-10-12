package com.motorrent.motorbikerental.controller.web;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;import java.util.Set;
@Controller @RequestMapping("/admin")
public class AdminPageController {
 @GetMapping("/{page}") public String page(@PathVariable String page,Model model){
  if(page.equals("stations"))return "redirect:/admin/settings";
  if(!Set.of("dashboard","reports","settings","motorbikes","customers","staffs","rental-orders","maintenance","payments","extensions","reviews","travel-places").contains(page))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
  model.addAttribute("activeNav",page);return "admin/"+page;
 }
}
