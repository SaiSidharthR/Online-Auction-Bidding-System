CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    pin VARCHAR(4) NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(32) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance DECIMAL(19,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference VARCHAR(64) NOT NULL UNIQUE,
    owner_user_id BIGINT NOT NULL,
    account_id VARCHAR(32) NOT NULL,
    auction_item_ref VARCHAR(64) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    charge DECIMAL(19,2) NOT NULL,
    total_debit DECIMAL(19,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_transactions_user FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

CREATE TABLE IF NOT EXISTS transaction_history (
    history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_id VARCHAR(64) NOT NULL,
    actor_id BIGINT NOT NULL,
    old_status VARCHAR(32) NOT NULL,
    new_status VARCHAR(32) NOT NULL,
    event_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_history_reference FOREIGN KEY (reference_id) REFERENCES transactions(reference),
    CONSTRAINT fk_history_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);
