package com.nextgenbank.servlet;

import com.nextgenbank.dao.AccountDAO;
import com.nextgenbank.model.Account;
import com.nextgenbank.model.Customer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DashboardServlet extends HttpServlet {

    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Customer customer = (session != null) ? (Customer) session.getAttribute("customer") : null;
        if (customer == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        try {
            List<Account> accounts = accountDAO.findByCustomer(customer.getCustomerId());
            req.setAttribute("accounts", accounts);
            req.setAttribute("transactions", accountDAO.recentTransactions(customer.getCustomerId(), 10));
            req.setAttribute("csrfToken", session.getAttribute("csrfToken"));
            req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, resp);
        } catch (SQLException e) {
            log("Failed to load accounts for customer " + customer.getCustomerId(), e);
            req.setAttribute("error", "Unable to load your accounts right now.");
            req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, resp);
        }
    }
}
