<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
  <title>NextGenBank | Sign In</title>
  <style>
    body { font-family: Arial, sans-serif; background:#0f1b2b; color:#e6edf3; display:flex; align-items:center; justify-content:center; height:100vh; margin:0; }
    .card { background:#16263c; padding:32px 36px; border-radius:8px; width:320px; box-shadow:0 4px 20px rgba(0,0,0,.4); }
    h1 { font-size:20px; margin:0 0 4px; }
    p.tag { color:#8fa3bd; font-size:12px; margin:0 0 20px; }
    label { display:block; font-size:12px; color:#8fa3bd; margin:14px 0 4px; }
    input { width:100%; padding:10px; border-radius:4px; border:1px solid #2a3f5a; background:#0f1b2b; color:#e6edf3; box-sizing:border-box; }
    button { width:100%; margin-top:20px; padding:10px; background:#1f6feb; border:none; border-radius:4px; color:#fff; font-weight:600; cursor:pointer; }
    .error { background:#3a1414; color:#ff8080; padding:8px 10px; border-radius:4px; font-size:12px; margin-top:14px; }
  </style>
</head>
<body>
  <div class="card">
    <h1>NextGenBank</h1>
    <p class="tag">Practice banking app · WebSphere Application Server</p>
    <form method="post" action="${pageContext.request.contextPath}/login">
      <label>Username</label>
      <input type="text" name="username" required autofocus />
      <label>Password</label>
      <input type="password" name="password" required />
      <button type="submit">Sign In</button>
    </form>
    <% if (request.getAttribute("error") != null) { %>
      <div class="error"><%= request.getAttribute("error") %></div>
    <% } %>
  </div>
</body>
</html>
