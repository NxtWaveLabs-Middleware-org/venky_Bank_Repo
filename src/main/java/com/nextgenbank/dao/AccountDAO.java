package com.nextgenbank.dao;

import com.nextgenbank.model.Account;
import com.nextgenbank.util.DataSourceProvider;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {

    public List<Account> findByCustomer(long customerId) throws SQLException {
        String sql = "SELECT account_id, customer_id, account_number, account_type, balance " +
                     "FROM accounts WHERE customer_id = ? ORDER BY account_id";
        List<Account> results = new ArrayList<>();
        try (Connection conn = dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new Account(
                            rs.getLong("account_id"),
                            rs.getLong("customer_id"),
                            rs.getString("account_number"),
                            rs.getString("account_type"),
                            rs.getBigDecimal("balance")
                    ));
                }
            }
        } catch (Exception e) {
            throw new SQLException("Could not load accounts for customer " + customerId, e);
        }
        return results;
    }

    /** Returns true only if accountId exists and belongs to customerId. */
    public boolean isOwnedBy(long accountId, long customerId) throws SQLException {
        String sql = "SELECT 1 FROM accounts WHERE account_id = ? AND customer_id = ?";
        try (Connection conn = dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setLong(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new SQLException("Ownership check failed for account " + accountId, e);
        }
    }

    /** Last N transactions touching any account owned by this customer. */
    public List<com.nextgenbank.model.Transaction> recentTransactions(long customerId, int limit) throws SQLException {
        String sql = "SELECT t.transaction_id, t.from_account_id, t.to_account_id, t.amount, t.status, t.created_at " +
                     "FROM transactions t " +
                     "WHERE t.from_account_id IN (SELECT account_id FROM accounts WHERE customer_id = ?) " +
                     "   OR t.to_account_id   IN (SELECT account_id FROM accounts WHERE customer_id = ?) " +
                     "ORDER BY t.created_at DESC LIMIT ?";
        List<com.nextgenbank.model.Transaction> results = new ArrayList<>();
        try (Connection conn = dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            ps.setLong(2, customerId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.nextgenbank.model.Transaction t = new com.nextgenbank.model.Transaction();
                    t.setTransactionId(rs.getLong("transaction_id"));
                    long fromId = rs.getLong("from_account_id");
                    t.setFromAccountId(rs.wasNull() ? null : fromId);
                    long toId = rs.getLong("to_account_id");
                    t.setToAccountId(rs.wasNull() ? null : toId);
                    t.setAmount(rs.getBigDecimal("amount"));
                    t.setStatus(rs.getString("status"));
                    t.setCreatedAt(rs.getTimestamp("created_at"));
                    results.add(t);
                }
            }
        } catch (Exception e) {
            throw new SQLException("Could not load transaction history for customer " + customerId, e);
        }
        return results;
    }

    /**
     * Local-transaction transfer: debit + credit + audit row, all-or-nothing.
     *
     * IMPORTANT (this is intentional, and becomes a lab itself): as written this
     * uses a single JDBC Connection's local transaction, so it's atomic *only*
     * within this one PostgreSQL connection. In Month 2 (Session 22, 24) you'll
     * re-implement this as a container-managed EJB method so it participates in
     * a real global/XA transaction alongside the JMS audit message - compare the
     * two implementations, that comparison is a strong interview story.
     */
    public void transfer(long fromAccountId, long toAccountId, BigDecimal amount) throws SQLException {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (fromAccountId == toAccountId) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Connection conn = null;
        try {
            conn = dataSource().getConnection();
            conn.setAutoCommit(false);

            BigDecimal fromBalance = lockAndGetBalance(conn, fromAccountId);
            if (fromBalance.compareTo(amount) < 0) {
                conn.rollback();
                throw new IllegalStateException("Insufficient funds in account " + fromAccountId);
            }

            updateBalance(conn, fromAccountId, fromBalance.subtract(amount));
            BigDecimal toBalance = lockAndGetBalance(conn, toAccountId);
            updateBalance(conn, toAccountId, toBalance.add(amount));

            long transactionId = insertTransactionRow(conn, fromAccountId, toAccountId, amount, "COMPLETED");
            insertAuditRow(conn, transactionId, "Transfer of " + amount + " from account " + fromAccountId +
                    " to account " + toAccountId + " - COMPLETED");

            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignore) { }
            }
            throw new SQLException("Transfer failed, rolled back", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignore) { }
            }
        }
    }

    private BigDecimal lockAndGetBalance(Connection conn, long accountId) throws SQLException {
        // SELECT ... FOR UPDATE prevents a lost-update race between two concurrent transfers
        String sql = "SELECT balance FROM accounts WHERE account_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Account not found: " + accountId);
                }
                return rs.getBigDecimal("balance");
            }
        }
    }

    private void updateBalance(Connection conn, long accountId, BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, accountId);
            ps.executeUpdate();
        }
    }

    private long insertTransactionRow(Connection conn, long fromId, long toId, BigDecimal amount, String status) throws SQLException {
        String sql = "INSERT INTO transactions (from_account_id, to_account_id, amount, status, created_at) " +
                     "VALUES (?, ?, ?, ?, now())";
        try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, fromId);
            ps.setLong(2, toId);
            ps.setBigDecimal(3, amount);
            ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert did not return a generated transaction_id");
            }
        }
    }

    private void insertAuditRow(Connection conn, long transactionId, String detail) throws SQLException {
        String sql = "INSERT INTO audit_log (transaction_id, event_detail, logged_at) VALUES (?, ?, now())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionId);
            ps.setString(2, detail);
            ps.executeUpdate();
        }
    }

    private DataSource dataSource() throws SQLException {
        try {
            return DataSourceProvider.get();
        } catch (Exception e) {
            throw new SQLException("Could not resolve jdbc/nextgenbankDS via JNDI", e);
        }
    }
}
