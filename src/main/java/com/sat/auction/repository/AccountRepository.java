package com.sat.auction.repository;

import com.sat.auction.model.Account;
import com.sat.auction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByAccountId(String accountId);
    List<Account> findByOwner(User owner);
}
