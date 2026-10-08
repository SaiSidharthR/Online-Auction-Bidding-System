package com.sat.auction.repository;

import com.sat.auction.model.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
    List<TransactionHistory> findByReferenceIdOrderByEventAtDesc(String referenceId);
    List<TransactionHistory> findAllByOrderByEventAtDesc();
}
