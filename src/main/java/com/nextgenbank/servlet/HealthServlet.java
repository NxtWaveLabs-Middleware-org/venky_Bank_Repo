package com.nextgenbank.servlet;

import com.nextgenbank.util.DataSourceProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;

/**
 * Minimal health endpoint: confirms the app is up AND can reach PostgreSQL
 * through the WAS-managed pool. Wire this into your wsadmin deploy script
 * (Session 28) and your CI/CD pipeline (Session 43) as the post-deploy check.
 */
public class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        try (Connection conn = DataSourceProvider.get().getConnection()) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"status\":\"UP\",\"db\":\"UP\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.getWriter().write("{\"status\":\"DOWN\",\"db\":\"DOWN\"}");
        }
    }
}
