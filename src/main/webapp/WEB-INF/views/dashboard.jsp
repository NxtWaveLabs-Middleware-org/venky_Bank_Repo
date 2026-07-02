<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nextgenbank.model.Customer, com.nextgenbank.model.Account, com.nextgenbank.model.Transaction, java.util.List" %>
<%
  Customer customer = (Customer) session.getAttribute("customer");
  if (customer == null) {
      response.sendRedirect(request.getContextPath() + "/login.jsp");
      return;
  }
  List<Account> accounts = (List<Account>) request.getAttribute("accounts");
  List<Transaction> transactions = (List<Transaction>) request.getAttribute("transactions");
  String csrfToken = (String) request.getAttribute("csrfToken");
  String transferResult = request.getParameter("transfer");
%>
<!DOCTYPE html>
<html>
<head>
  <title>NextGenBank | Dashboard</title>
  <style>
    body { font-family: Arial, sans-serif; background:#0f1b2b; color:#e6edf3; margin:0; }
    header { display:flex; justify-content:space-between; align-items:center; padding:16px 32px; background:#16263c; }
    header a { color:#8fa3bd; text-decoration:none; font-size:13px; }
    main { max-width:720px; margin:32px auto; padding:0 16px; }
    h2 { font-size:16px; color:#8fa3bd; text-transform:uppercase; letter-spacing:1px; margin:24px 0 12px; }
    .account { background:#16263c; border-radius:8px; padding:16px 20px; margin-bottom:10px; display:flex; justify-content:space-between; align-items:center; }
    .account .num { font-size:13px; color:#8fa3bd; }
    .account .bal { font-size:20px; font-weight:600; }
    .type { font-size:11px; color:#1f6feb; text-transform:uppercase; }
    form.transfer { background:#16263c; border-radius:8px; padding:20px; margin-top:24px; }
    form.transfer label { display:block; font-size:12px; color:#8fa3bd; margin:12px 0 4px; }
    form.transfer select, form.transfer input { width:100%; padding:9px; border-radius:4px; border:1px solid #2a3f5a; background:#0f1b2b; color:#e6edf3; box-sizing:border-box; }
    form.transfer button { margin-top:16px; padding:10px 18px; background:#1f6feb; border:none; border-radius:4px; color:#fff; font-weight:600; cursor:pointer; }
    .banner { padding:10px 14px; border-radius:6px; font-size:13px; margin-bottom:16px; }
    .banner.ok { background:#123a24; color:#5fd98a; }
    .banner.bad { background:#3a1414; color:#ff8080; }
  </style>
</head>
<body>
  <header>
    <strong>NextGenBank</strong>
    <div>
      <span style="margin-right:16px; font-size:13px;">Welcome, <%= customer != null ? customer.getFullName() : "" %></span>
      <a href="${pageContext.request.contextPath}/logout">Sign out</a>
    </div>
  </header>

  <main>
    <% if ("success".equals(transferResult)) { %>
      <div class="banner ok">Transfer completed successfully.</div>
    <% } else if (transferResult != null) { %>
      <div class="banner bad">Transfer failed: <%= transferResult.replace("_", " ") %></div>
    <% } %>

    <h2>Your Accounts</h2>
    <% if (accounts != null) { for (Account a : accounts) { %>
      <div class="account">
        <div>
          <div class="type"><%= a.getAccountType() %></div>
          <div class="num">Acct #<%= a.getAccountNumber() %> (id <%= a.getAccountId() %>)</div>
        </div>
        <div class="bal">$<%= a.getBalance() %></div>
      </div>
    <% } } %>

    <h2>Transfer Funds</h2>
    <form class="transfer" method="post" action="${pageContext.request.contextPath}/transfer">
      <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
      <label>From Account ID</label>
      <input type="number" name="fromAccountId" required />
      <label>To Account ID</label>
      <input type="number" name="toAccountId" required />
      <label>Amount</label>
      <input type="number" step="0.01" name="amount" required />
      <button type="submit">Send Transfer</button>
    </form>

    <h2>Recent Transactions</h2>
    <% if (transactions != null && !transactions.isEmpty()) { %>
      <table style="width:100%; border-collapse:collapse; margin-top:8px;">
        <thead>
          <tr style="text-align:left; font-size:11px; color:#8fa3bd; text-transform:uppercase;">
            <th style="padding:8px 4px;">Date</th>
            <th style="padding:8px 4px;">From</th>
            <th style="padding:8px 4px;">To</th>
            <th style="padding:8px 4px;">Amount</th>
            <th style="padding:8px 4px;">Status</th>
          </tr>
        </thead>
        <tbody>
          <% for (Transaction t : transactions) { %>
            <tr style="border-top:1px solid #22344c; font-size:13px;">
              <td style="padding:8px 4px;"><%= t.getCreatedAt() %></td>
              <td style="padding:8px 4px;"><%= t.getFromAccountId() != null ? t.getFromAccountId() : "-" %></td>
              <td style="padding:8px 4px;"><%= t.getToAccountId() != null ? t.getToAccountId() : "-" %></td>
              <td style="padding:8px 4px;">$<%= t.getAmount() %></td>
              <td style="padding:8px 4px;"><%= t.getStatus() %></td>
            </tr>
          <% } %>
        </tbody>
      </table>
    <% } else { %>
      <p style="color:#8fa3bd; font-size:13px;">No transactions yet.</p>
    <% } %>
  </main>
</body>
</html>
