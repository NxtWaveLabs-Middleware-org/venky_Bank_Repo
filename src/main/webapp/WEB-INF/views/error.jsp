<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<!DOCTYPE html>
<html>
<head>
  <title>NextGenBank | Something went wrong</title>
  <style>
    body { font-family: Arial, sans-serif; background:#0f1b2b; color:#e6edf3; display:flex; align-items:center; justify-content:center; height:100vh; margin:0; }
    .card { background:#16263c; padding:32px 36px; border-radius:8px; width:360px; text-align:center; }
    a { color:#1f6feb; }
  </style>
</head>
<body>
  <div class="card">
    <h2>Something went wrong</h2>
    <p style="color:#8fa3bd; font-size:13px;">
      We hit an unexpected error. Nothing you can do here except try again -
      the real detail is in SystemOut.log / FFDC, which is exactly where a
      WAS admin would go look next.
    </p>
    <p><a href="${pageContext.request.contextPath}/login.jsp">Back to sign in</a></p>
  </div>
</body>
</html>
