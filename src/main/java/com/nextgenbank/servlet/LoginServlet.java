package com.nextgenbank.servlet;

import com.nextgenbank.dao.CustomerDAO;
import com.nextgenbank.model.Customer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

public class LoginServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            Customer customer = customerDAO.authenticate(username, password);
            if (customer == null) {
                req.setAttribute("error", "Invalid username or password.");
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
                return;
            }

            // Invalidate any pre-auth session and start a fresh one - prevents session fixation,
            // where an attacker could plant a known session ID before the victim logs in.
            HttpSession existing = req.getSession(false);
            if (existing != null) {
                existing.invalidate();
            }
            HttpSession session = req.getSession(true);
            session.setAttribute("customer", customer);
            session.setAttribute("csrfToken", java.util.UUID.randomUUID().toString());
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            // Surface a clean message to the user, but log the real cause -
            // this is exactly the kind of stack trace you'll be reading in
            // SystemOut.log / FFDC during the Session 33 lab.
            log("Login failed due to a datasource/query error", e);
            req.setAttribute("error", "The bank service is temporarily unavailable. Please try again shortly.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
