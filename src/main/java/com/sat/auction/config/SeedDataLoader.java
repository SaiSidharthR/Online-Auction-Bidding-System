package com.sat.auction.config;

import com.sat.auction.model.Account;
import com.sat.auction.model.Role;
import com.sat.auction.model.User;
import com.sat.auction.repository.AccountRepository;
import com.sat.auction.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class SeedDataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public SeedDataLoader(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        User customer1 = new User("A101", "Sample Customer 1", Role.CUSTOMER, "ACTIVE", "1111");
        User customer2 = new User("A102", "Sample Customer 2", Role.CUSTOMER, "ACTIVE", "2222");
        User blockedCustomer = new User("A103", "Blocked Sample", Role.CUSTOMER, "INACTIVE", "3333");
        User admin = new User("ADMIN01", "System Administrator", Role.ADMINISTRATOR, "ACTIVE", "9999");

        userRepository.save(customer1);
        userRepository.save(customer2);
        userRepository.save(blockedCustomer);
        userRepository.save(admin);

        accountRepository.save(new Account("A101", customer1, new BigDecimal("25000.00"), "ACTIVE", LocalDateTime.now()));
        accountRepository.save(new Account("A102", customer2, new BigDecimal("8000.00"), "ACTIVE", LocalDateTime.now()));
        accountRepository.save(new Account("A103", blockedCustomer, new BigDecimal("12000.00"), "INACTIVE", LocalDateTime.now()));
    }
}
