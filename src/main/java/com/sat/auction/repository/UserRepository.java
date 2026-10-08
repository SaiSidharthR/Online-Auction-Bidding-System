package com.sat.auction.repository;

import com.sat.auction.model.Role;
import com.sat.auction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByCustomerIdAndRole(String customerId, Role role);
    Optional<User> findByCustomerId(String customerId);
}
