package com.nextgenbank.servlet;

import com.nextgenbank.dao.AccountDAO;
import com.nextgenbank.model.Customer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

public class TransferServlet extends HttpServlet {

    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Customer customer = (session != null) ? (Customer) session.getAttribute("customer") : null;
        if (customer == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        // CSRF check: the token in the form must match the one issued to this session at login
        String sessionToken = (String) session.getAttribute("csrfToken");
        String submittedToken = req.getParameter("csrfToken");
        if (sessionToken == null || !sessionToken.equals(submittedToken)) {
            resp.sendRedirect(req.getContextPath() + "/dashboard?transfer=invalid_request");
            return;
        }

        try {
            long fromAccountId = Long.parseLong(req.getParameter("fromAccountId"));
            long toAccountId = Long.parseLong(req.getParameter("toAccountId"));
            BigDecimal amount = new BigDecimal(req.getParameter("amount"));

            // Ownership check: the source account MUST belong to the logged-in customer.
            // Without this, any authenticated user could drain any account by ID -
            // this was Phase 1's most important gap.
            if (!accountDAO.isOwnedBy(fromAccountId, customer.getCustomerId())) {
                log("SECURITY: customer " + customer.getCustomerId() +
                        " attempted transfer from account " + fromAccountId + " which they do not own");
                resp.sendRedirect(req.getContextPath() + "/dashboard?transfer=not_authorized");
                return;
            }

            accountDAO.transfer(fromAccountId, toAccountId, amount);
            resp.sendRedirect(req.getContextPath() + "/dashboard?transfer=success");
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/dashboard?transfer=badinput");
        } catch (IllegalArgumentException e) {
            // e.g. zero/negative amount
            resp.sendRedirect(req.getContextPath() + "/dashboard?transfer=" + urlSafe(e.getMessage()));
        } catch (IllegalStateException e) {
            // e.g. insufficient funds
            resp.sendRedirect(req.getContextPath() + "/dashboard?transfer=" + urlSafe(e.getMessage()));
        } catch (SQLException e) {
            log("Transfer failed", e);
            resp.sendRedirect(req.getContextPath() + "/dashboard?transfer=error");
        }
    }

    private String urlSafe(String msg) {
        return msg == null ? "error" : msg.replaceAll("[^a-zA-Z0-9]", "_");
    }
}
