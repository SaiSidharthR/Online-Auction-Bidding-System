package com.sat.auction.service;

import com.sat.auction.model.*;
import com.sat.auction.repository.AccountRepository;
import com.sat.auction.repository.TransactionHistoryRepository;
import com.sat.auction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public TransactionService(AccountRepository accountRepository,
                             TransactionRepository transactionRepository,
                             TransactionHistoryRepository transactionHistoryRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    public BigDecimal validateAmount(String amountText) {
        if (amountText == null || amountText.isBlank()) {
            throw new IllegalArgumentException("Amount is required.");
        }

        String trim = amountText.trim();
        if (!trim.matches("\\d+")) {
            throw new IllegalArgumentException("Amount must be a whole rupee value from Rs.100 to Rs.20,000.");
        }

        BigDecimal amount = new BigDecimal(trim);
        if (amount.compareTo(new BigDecimal("100")) < 0 || amount.compareTo(new BigDecimal("20000")) > 0) {
            throw new IllegalArgumentException("Amount must be from Rs.100 to Rs.20,000.");
        }
        return amount;
    }

    public BigDecimal calculateCharge(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required.");
        }
        return amount.compareTo(new BigDecimal("1000")) < 0 ? new BigDecimal("10") : BigDecimal.ZERO;
    }

    public void checkEligibility(Account account, BigDecimal amount) {
        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active.");
        }
        BigDecimal totalDebit = amount.add(calculateCharge(amount));
        if (account.getBalance().compareTo(totalDebit) < 0) {
            throw new IllegalArgumentException("Insufficient balance for the total debit.");
        }
    }

    @Transactional
    public Transaction authorise(User actor, String accountId, String auctionItemRef, BigDecimal amount, String requestRef) {
        if (actor == null) {
            throw new IllegalArgumentException("User must be provided.");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be blank.");
        }
        if (auctionItemRef == null || auctionItemRef.isBlank()) {
            throw new IllegalArgumentException("Auction item reference is required.");
        }
        if (requestRef == null || requestRef.isBlank()) {
            throw new IllegalArgumentException("Request reference is required.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        if (!account.getOwner().getId().equals(actor.getId())) {
            throw new IllegalArgumentException("You cannot authorise for another customer's account.");
        }

        BigDecimal validAmount = validateAmount(amount.toPlainString());
        checkEligibility(account, validAmount);

        if (transactionRepository.findByRequestRef(requestRef).isPresent()) {
            throw new IllegalArgumentException("Duplicate request reference: this bid has already been submitted.");
        }

        Transaction transaction = new Transaction();
        transaction.setReference("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        transaction.setRequestRef(requestRef);
        transaction.setOwnerUser(actor);
        transaction.setAccountId(accountId);
        transaction.setAuctionItemRef(auctionItemRef);
        transaction.setAmount(validAmount);
        transaction.setCharge(calculateCharge(validAmount));
        transaction.setTotalDebit(validAmount.add(transaction.getCharge()));
        transaction.setStatus(Status.INITIATED);
        transaction.setCreatedAt(LocalDateTime.now());

        Transaction saved = transactionRepository.save(transaction);
        recordHistory(saved, null, Status.INITIATED, actor);
        return saved;
    }

    @Transactional
    public Transaction createTransaction(User actor, String accountId, String auctionItemRef, BigDecimal amount,
                                         String requestRef, String pin) {
        if (actor == null) {
            throw new IllegalArgumentException("User must be provided.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));

        if (!actor.getId().equals(account.getOwner().getId())) {
            throw new IllegalArgumentException("Customer does not own this account.");
        }

        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }
        if (!actor.getPin().equals(pin)) {
            throw new IllegalArgumentException("Invalid PIN.");
        }

        BigDecimal validAmount = validateAmount(amount.toPlainString());
        checkEligibility(account, validAmount);

        if (transactionRepository.findByRequestRef(requestRef).isPresent()) {
            throw new IllegalArgumentException("Duplicate request reference: this bid has already been submitted.");
        }

        BigDecimal charge = calculateCharge(validAmount);
        BigDecimal totalDebit = validAmount.add(charge);

        Transaction transaction = new Transaction();
        transaction.setReference("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        transaction.setRequestRef(requestRef);
        transaction.setOwnerUser(actor);
        transaction.setAccountId(accountId);
        transaction.setAuctionItemRef(auctionItemRef);
        transaction.setAmount(validAmount);
        transaction.setCharge(charge);
        transaction.setTotalDebit(totalDebit);
        transaction.setStatus(Status.SUCCESS);
        transaction.setCreatedAt(LocalDateTime.now());

        Transaction savedTransaction = transactionRepository.save(transaction);
        account.setBalance(account.getBalance().subtract(totalDebit));
        accountRepository.save(account);
        recordHistory(savedTransaction, null, Status.SUCCESS, actor);
        return savedTransaction;
    }

    @Transactional
    public Transaction updateStatus(Long transactionId, User actor, Status targetStatus, String pin) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

        if (actor == null) {
            throw new IllegalArgumentException("Actor is required.");
        }
        if (targetStatus == null) {
            throw new IllegalArgumentException("Status is required.");
        }

        Status currentStatus = transaction.getStatus();
        if (targetStatus == Status.AUTHORISED) {
            if (currentStatus != Status.INITIATED) {
                throw new IllegalArgumentException("Only an INITIATED transaction can be authorised.");
            }
            if (actor.getRole() == Role.CUSTOMER) {
                if (pin == null || !pin.matches("\\d{4}")) {
                    throw new IllegalArgumentException("PIN must be exactly 4 digits.");
                }
                if (!actor.getPin().equals(pin)) {
                    throw new IllegalArgumentException("Invalid PIN.");
                }
            }
            Status oldStatus = transaction.getStatus();
            transaction.setStatus(Status.AUTHORISED);
            transactionRepository.save(transaction);
            recordHistory(transaction, oldStatus, Status.AUTHORISED, actor);
            return transaction;
        }

        if (targetStatus == Status.SUCCESS) {
            if (currentStatus != Status.AUTHORISED) {
                throw new IllegalArgumentException("Only an AUTHORISED transaction can be completed to SUCCESS.");
            }
            if (pin == null || !pin.matches("\\d{4}")) {
                throw new IllegalArgumentException("PIN must be exactly 4 digits.");
            }
            if (!actor.getPin().equals(pin)) {
                throw new IllegalArgumentException("Invalid PIN.");
            }
            if (actor.getRole() == Role.CUSTOMER && !actor.getId().equals(transaction.getOwnerUser().getId())) {
                throw new IllegalArgumentException("You cannot complete another user's transaction.");
            }

            Account account = accountRepository.findById(transaction.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found."));
            checkEligibility(account, transaction.getAmount());
            if (account.getBalance().compareTo(transaction.getTotalDebit()) < 0) {
                throw new IllegalArgumentException("Insufficient balance for the total debit.");
            }

            account.setBalance(account.getBalance().subtract(transaction.getTotalDebit()));
            accountRepository.save(account);

            transaction.setStatus(Status.SUCCESS);
            transactionRepository.save(transaction);
            recordHistory(transaction, Status.AUTHORISED, Status.SUCCESS, actor);
            return transaction;
        }

        if (targetStatus == Status.REVERSED) {
            if (transaction.getStatus() == Status.REVERSED) {
                throw new IllegalArgumentException("This transaction has already been reversed.");
            }
            if (currentStatus != Status.SUCCESS) {
                throw new IllegalArgumentException("Only a SUCCESS transaction can be reversed.");
            }
            if (!actor.getId().equals(transaction.getOwnerUser().getId())) {
                throw new IllegalArgumentException("Access denied: you cannot reverse another user's transaction.");
            }

            Account account = accountRepository.findById(transaction.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found."));
            account.setBalance(account.getBalance().add(transaction.getTotalDebit()));
            accountRepository.save(account);

            transaction.setStatus(Status.REVERSED);
            transactionRepository.save(transaction);
            recordHistory(transaction, Status.SUCCESS, Status.REVERSED, actor);
            return transaction;
        }

        throw new IllegalArgumentException("Invalid status action.");
    }

    @Transactional
    public Transaction reverseTransaction(Long transactionId, User actor) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

        if (transaction.getStatus() == Status.REVERSED) {
            throw new IllegalArgumentException("This transaction has already been reversed.");
        }
        if (transaction.getStatus() != Status.SUCCESS) {
            throw new IllegalArgumentException("Only a SUCCESS transaction can be reversed.");
        }
        if (!actor.getId().equals(transaction.getOwnerUser().getId())) {
            throw new IllegalArgumentException("Access denied: you cannot reverse another user's transaction.");
        }

        Account account = accountRepository.findById(transaction.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        account.setBalance(account.getBalance().add(transaction.getTotalDebit()));
        accountRepository.save(account);

        transaction.setStatus(Status.REVERSED);
        transactionRepository.save(transaction);
        recordHistory(transaction, Status.SUCCESS, Status.REVERSED, actor);
        return transaction;
    }

    public List<Transaction> getUserTransactions(User customer) {
        return transactionRepository.findByOwnerUser(customer);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<TransactionHistory> getHistoryForTransaction(String reference) {
        return transactionHistoryRepository.findByReferenceIdOrderByEventAtDesc(reference);
    }

    public List<TransactionHistory> getAllAuditHistory() {
        return transactionHistoryRepository.findAllByOrderByEventAtDesc();
    }

    private void recordHistory(Transaction transaction, Status oldStatus, Status newStatus, User actor) {
        if (transaction == null || newStatus == null) {
            return;
        }
        TransactionHistory history = new TransactionHistory(
                transaction.getReference(),
                actor == null ? 0L : actor.getId(),
                oldStatus,
                newStatus,
                LocalDateTime.now()
        );
        transactionHistoryRepository.save(history);
    }
}
