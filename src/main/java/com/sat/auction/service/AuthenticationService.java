package com.sat.auction.service;

import com.sat.auction.model.Role;
import com.sat.auction.model.User;
import com.sat.auction.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthenticationService {

    private static final String INVALID_PIN_ATTEMPTS = "invalidPinAttempts";

    private final UserRepository userRepository;

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User validateUser(String customerId, Role role) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be blank.");
        }
        return userRepository.findByCustomerIdAndRole(customerId.trim(), role)
                .orElseThrow(() -> new IllegalArgumentException("Invalid customer or role."));
    }

    public boolean validatePIN(User user, String pin) {
        if (user == null) {
            return false;
        }
        if (pin == null || !pin.matches("\\d{4}")) {
            return false;
        }
        return Objects.equals(user.getPin(), pin);
    }

    public void blockSession(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    public boolean checkRole(HttpSession session, Role expectedRole) {
        if (session == null) {
            return false;
        }
        Object userRole = session.getAttribute("userRole");
        return expectedRole.name().equals(String.valueOf(userRole));
    }

    public void registerInvalidAttempt(HttpSession session) {
        if (session == null) {
            return;
        }
        Integer attempts = (Integer) session.getAttribute(INVALID_PIN_ATTEMPTS);
        attempts = attempts == null ? 1 : attempts + 1;
        session.setAttribute(INVALID_PIN_ATTEMPTS, attempts);
        if (attempts >= 3) {
            session.setAttribute("loginBlocked", true);
            session.setAttribute("loginMessage", "Too many invalid PIN attempts. Your session has been blocked.");
            session.invalidate();
        }
    }

    public boolean isSessionBlocked(HttpSession session) {
        if (session == null) {
            return true;
        }
        Object blocked = session.getAttribute("loginBlocked");
        return blocked != null && Boolean.TRUE.equals(blocked);
    }
}
