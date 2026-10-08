package com.sat.auction.controller;

import com.sat.auction.model.*;
import com.sat.auction.repository.AccountRepository;
import com.sat.auction.repository.UserRepository;
import com.sat.auction.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    public CustomerController(UserRepository userRepository,
                             AccountRepository accountRepository,
                             TransactionService transactionService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
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

    private void enforceCustomer(User user) {
        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalStateException("Access denied.");
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        try {
            User user = currentUser(session);
            enforceCustomer(user);
            model.addAttribute("user", user);
            return "customer-dashboard";
        } catch (IllegalStateException ex) {
            return "redirect:/access-denied";
        }
    }

    @GetMapping("/bid")
    public String bidForm(HttpSession session, Model model) {
        try {
            User user = currentUser(session);
            enforceCustomer(user);
            model.addAttribute("user", user);
            model.addAttribute("requestRef", generateRequestRef());
            return "bid-form";
        } catch (IllegalStateException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/bid/preview")
    public String previewBid(@RequestParam String customerId,
                            @RequestParam String auctionItemRef,
                            @RequestParam String amount,
                            @RequestParam String requestRef,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        try {
            User user = currentUser(session);
            enforceCustomer(user);

            if (customerId == null || customerId.isBlank()) {
                throw new IllegalArgumentException("Customer ID cannot be blank.");
            }
            if (!user.getCustomerId().equalsIgnoreCase(customerId.trim())) {
                throw new IllegalArgumentException("Access denied: you can only bid using your own account.");
            }

            Account account = accountRepository.findById(customerId.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found."));
            if (!account.getOwner().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Access denied: account ownership mismatch.");
            }

            BigDecimal bidAmount = transactionService.validateAmount(amount);
            transactionService.checkEligibility(account, bidAmount);

            BigDecimal fee = transactionService.calculateCharge(bidAmount);
            BigDecimal totalDebit = bidAmount.add(fee);

            Map<String, Object> bid = new HashMap<>();
            bid.put("customerId", customerId.trim());
            bid.put("auctionItemRef", auctionItemRef);
            bid.put("amount", bidAmount);
            bid.put("charge", fee);
            bid.put("totalDebit", totalDebit);
            bid.put("requestRef", requestRef);
            session.setAttribute("pendingBid", bid);

            return "redirect:/customer/bid/review";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/customer/bid";
        }
    }

    @GetMapping("/bid/review")
    public String reviewBid(HttpSession session, Model model) {
        try {
            User user = currentUser(session);
            enforceCustomer(user);
            Map<String, Object> bid = (Map<String, Object>) session.getAttribute("pendingBid");
            if (bid == null) {
                model.addAttribute("error", "No pending bid found.");
                return "bid-review";
            }
            model.addAttribute("user", user);
            model.addAttribute("bid", bid);
            return "bid-review";
        } catch (IllegalStateException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/bid/authorise")
    public String authoriseBid(@RequestParam String pin,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = currentUser(session);
            enforceCustomer(user);

            Map<String, Object> bid = (Map<String, Object>) session.getAttribute("pendingBid");
            if (bid == null) {
                throw new IllegalArgumentException("No bid found to authorise.");
            }
            if (pin == null || !pin.matches("\\d{4}")) {
                throw new IllegalArgumentException("PIN must be exactly 4 digits.");
            }
            if (!user.getPin().equals(pin)) {
                throw new IllegalArgumentException("Invalid PIN.");
            }

            BigDecimal bidAmount = (BigDecimal) bid.get("amount");
            String customerId = (String) bid.get("customerId");
            String auctionItemRef = (String) bid.get("auctionItemRef");
            String requestRef = (String) bid.get("requestRef");

            Account account = accountRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found."));
            transactionService.checkEligibility(account, bidAmount);

            Transaction tx = transactionService.authorise(user, customerId, auctionItemRef, bidAmount, requestRef);
            session.setAttribute("pendingTransactionId", tx.getId());
            redirectAttributes.addFlashAttribute("success", "Bid authorised. Please complete the transaction.");
            return "redirect:/customer/bid/complete";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/customer/bid/review";
        }
    }

    @GetMapping("/bid/complete")
    public String completeBidPage(HttpSession session, Model model) {
        try {
            User user = currentUser(session);
            enforceCustomer(user);
            Long transactionId = (Long) session.getAttribute("pendingTransactionId");
            if (transactionId == null) {
                return "redirect:/customer/bid";
            }
            Transaction tx = transactionService.getAllTransactions().stream()
                    .filter(t -> t.getId().equals(transactionId))
                    .findFirst()
                    .orElse(null);
            if (tx == null) {
                return "redirect:/customer/bid";
            }
            model.addAttribute("transaction", tx);
            return "bid-complete";
        } catch (IllegalStateException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/bid/complete")
    public String completeBid(@RequestParam String pin,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = currentUser(session);
            enforceCustomer(user);
            Long transactionId = (Long) session.getAttribute("pendingTransactionId");
            if (transactionId == null) {
                throw new IllegalArgumentException("No pending transaction found.");
            }
            if (pin == null || !pin.matches("\\d{4}")) {
                throw new IllegalArgumentException("PIN must be exactly 4 digits.");
            }

            Transaction tx = transactionService.getAllTransactions().stream()
                    .filter(t -> t.getId().equals(transactionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

            if (!tx.getOwnerUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Access denied.");
            }

            transactionService.updateStatus(tx.getId(), user, Status.SUCCESS, pin);

            session.removeAttribute("pendingBid");
            session.removeAttribute("pendingTransactionId");
            redirectAttributes.addFlashAttribute("success", "Transaction completed successfully.");
            return "redirect:/customer/transactions";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/customer/bid/review";
        }
    }

    @GetMapping("/transactions")
    public String transactionHistory(HttpSession session, Model model) {
        try {
            User user = currentUser(session);
            enforceCustomer(user);
            model.addAttribute("user", user);
            model.addAttribute("transactions", transactionService.getUserTransactions(user));
            return "transaction-history";
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
            enforceCustomer(user);
            Transaction tx = transactionService.getAllTransactions().stream()
                    .filter(t -> t.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));
            if (!tx.getOwnerUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Access denied: you can only view your own transactions.");
            }
            model.addAttribute("transaction", tx);
            model.addAttribute("history", transactionService.getHistoryForTransaction(tx.getReference()));
            return "transaction-detail";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/customer/transactions";
        }
    }

    @PostMapping("/transactions/{id}/reverse")
    public String reverseTransaction(@PathVariable Long id,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = currentUser(session);
            enforceCustomer(user);
            transactionService.reverseTransaction(id, user);
            redirectAttributes.addFlashAttribute("success", "Transaction reversed successfully.");
            return "redirect:/customer/transactions";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/customer/transactions";
        }
    }

    private String generateRequestRef() {
        SecureRandom random = new SecureRandom();
        return "REQ-" + random.nextInt(1000000);
    }
}
