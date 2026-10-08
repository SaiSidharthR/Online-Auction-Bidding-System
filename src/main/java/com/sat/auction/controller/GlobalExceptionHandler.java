package com.sat.auction.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException ex, RedirectAttributes redirectAttributes, HttpSession session) {
        session.removeAttribute("pendingBid");
        session.removeAttribute("pendingTransactionId");
        redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "Access denied." : ex.getMessage());
        return "redirect:/access-denied";
    }
}
