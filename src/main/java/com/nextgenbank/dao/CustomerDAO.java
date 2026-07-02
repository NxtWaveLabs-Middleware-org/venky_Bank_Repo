package com.nextgenbank.dao;

import com.nextgenbank.model.Customer;
import com.nextgenbank.util.DataSourceProvider;
import com.nextgenbank.util.PasswordUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerDAO {

    /**
     * Returns the authenticated Customer, or null if credentials are invalid.
     * Uses a try-with-resources block so the Connection always returns to the
     * WAS pool - forget this and you reproduce the Session 31 connection-leak lab.
     */
    public Customer authenticate(String username, String password) throws SQLException {
        String sql = "SELECT customer_id, username, full_name, email, password_hash " +
                     "FROM customers WHERE username = ?";
        DataSource ds = getDataSource();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String storedHash = rs.getString("password_hash");
                if (!PasswordUtil.matches(password, storedHash)) {
                    return null;
                }
                return new Customer(
                        rs.getLong("customer_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("email")
                );
            }
        } catch (Exception e) {
            throw new SQLException("Datasource lookup or query failed", e);
        }
    }

    private DataSource getDataSource() throws SQLException {
        try {
            return DataSourceProvider.get();
        } catch (Exception e) {
            throw new SQLException("Could not resolve jdbc/nextgenbankDS via JNDI", e);
        }
    }
}
