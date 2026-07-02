package com.nextgenbank.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Account implements Serializable {
    private long accountId;
    private long customerId;
    private String accountNumber;
    private String accountType;   // CHECKING, SAVINGS
    private BigDecimal balance;

    public Account() {}

    public Account(long accountId, long customerId, String accountNumber, String accountType, BigDecimal balance) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
    }

    public long getAccountId() { return accountId; }
    public void setAccountId(long accountId) { this.accountId = accountId; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
