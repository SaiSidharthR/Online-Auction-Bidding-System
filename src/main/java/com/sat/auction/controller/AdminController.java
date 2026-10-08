package com.sat.auction.controller;

import com.sat.auction.model.Role;
import com.sat.auction.model.Status;
import com.sat.auction.model.Transaction;
import com.sat.auction.model.User;
import com.sat.auction.repository.UserRepository;
import com.sat.auction.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public AdminController(UserRepository userRepository, TransactionService transactionService) {
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    private User currentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalStateException("User is not logged in.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found."));
    }

    private void enforceAdmin(User user) {
        if (user.getRole() != Role.ADMINISTRATOR) {
            throw new IllegalStateException("Access denied.");
        }
    }

    @GetMapping("/transactions")
    public String transactions(HttpSession session, Model model) {
        try {
            User user = currentUser(session);
            enforceAdmin(user);
            model.addAttribute("user", user);
            model.addAttribute("transactions", transactionService.getAllTransactions());
            return "admin-transactions";
        } catch (IllegalStateException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/transactions/{id}/authorise")
    public String authoriseTransaction(@PathVariable Long id,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            User user = currentUser(session);
            enforceAdmin(user);
            transactionService.updateStatus(id, user, Status.AUTHORISED, null);
            redirectAttributes.addFlashAttribute("success", "Transaction authorised.");
            return "redirect:/admin/transactions";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/transactions";
        }
    }

    @PostMapping("/transactions/{id}/success")
    public String completeTransaction(@PathVariable Long id,
                                      @RequestParam(required = false) String pin,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            User user = currentUser(session);
            enforceAdmin(user);
            if (pin == null || !pin.matches("\\d{4}")) {
                throw new IllegalArgumentException("Administrator PIN must be exactly 4 digits.");
            }
            transactionService.updateStatus(id, user, Status.SUCCESS, pin);
            redirectAttributes.addFlashAttribute("success", "Transaction marked as SUCCESS.");
            return "redirect:/admin/transactions";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/transactions";
        }
    }

    @GetMapping("/audit")
    public String auditHistory(HttpSession session, Model model) {
        try {
            User user = currentUser(session);
            enforceAdmin(user);
            model.addAttribute("user", user);
            model.addAttribute("auditHistory", transactionService.getAllAuditHistory());
            return "admin-audit";
        } catch (IllegalStateException ex) {
            return "redirect:/access-denied";
        }
    }

    @GetMapping("/transactions/{id}")
    public String transactionDetail(@PathVariable Long id,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = currentUser(session);
            enforceAdmin(user);
            Transaction tx = transactionService.getAllTransactions().stream()
                    .filter(t -> t.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));
            model.addAttribute("transaction", tx);
            model.addAttribute("history", transactionService.getHistoryForTransaction(tx.getReference()));
            return "transaction-detail";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/transactions";
        }
    }
}
