package com.sat.auction.repository;

import com.sat.auction.model.Transaction;
import com.sat.auction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByReference(String reference);
    Optional<Transaction> findByRequestRef(String requestRef);
    List<Transaction> findByOwnerUser(User ownerUser);
    List<Transaction> findByAccountId(String accountId);
    List<Transaction> findAllByOrderByCreatedAtDesc();
}
