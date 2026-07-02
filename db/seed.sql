-- Seed data. Both users' password is: Passw0rd!
-- (SHA-256 hash below matches com.nextgenbank.util.PasswordUtil)

INSERT INTO customers (username, password_hash, full_name, email) VALUES
('jsmith', 'e66860546f18cdbbcd86b35e18b525bffc67f772c650cedfe3ff7a0026fa1dee', 'John Smith', 'jsmith@example.com'),
('amiller', 'e66860546f18cdbbcd86b35e18b525bffc67f772c650cedfe3ff7a0026fa1dee', 'Alice Miller', 'amiller@example.com');

INSERT INTO accounts (customer_id, account_number, account_type, balance) VALUES
(1, '1000100010', 'CHECKING', 5230.55),
(1, '1000100011', 'SAVINGS',  18500.00),
(2, '1000200020', 'CHECKING', 940.10);
