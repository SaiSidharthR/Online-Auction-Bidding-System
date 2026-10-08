package com.sat.auction.controller;

import com.sat.auction.model.Role;
import com.sat.auction.model.User;
import com.sat.auction.service.AuthenticationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) {
            Object role = session.getAttribute("userRole");
            if (Role.CUSTOMER.name().equals(String.valueOf(role))) {
                return "redirect:/customer/dashboard";
            }
            if (Role.ADMINISTRATOR.name().equals(String.valueOf(role))) {
                return "redirect:/admin/transactions";
            }
        }
        if (!model.containsAttribute("error")) {
            model.addAttribute("error", null);
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String customerId,
                        @RequestParam String pin,
                        @RequestParam String role,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            if (customerId == null || customerId.isBlank()) {
                throw new IllegalArgumentException("Customer ID cannot be blank.");
            }
            Role selectedRole = Role.valueOf(role.trim().toUpperCase());
            User user = authenticationService.validateUser(customerId, selectedRole);

            if (!authenticationService.validatePIN(user, pin)) {
                authenticationService.registerInvalidAttempt(session);
                if (session.isNew() || session.getAttribute("loginBlocked") != null) {
                    redirectAttributes.addFlashAttribute("error", "Too many invalid PIN attempts. Your session has been blocked.");
                    return "redirect:/login";
                }
                redirectAttributes.addFlashAttribute("error", "Invalid PIN. Please try again.");
                return "redirect:/login";
            }

            session.setAttribute("userId", user.getId());
            session.setAttribute("userRole", user.getRole().name());
            session.setAttribute("userName", user.getName());
            session.setAttribute("customerId", user.getCustomerId());
            session.removeAttribute("invalidPinAttempts");
            session.removeAttribute("loginBlocked");

            redirectAttributes.addFlashAttribute("success", "Login successful.");
            return selectedRole == Role.CUSTOMER ? "redirect:/customer/dashboard" : "redirect:/admin/transactions";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "You have been logged out.");
        return "redirect:/login";
    }
}
