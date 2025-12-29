<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title>Login</title>
</head>
<body>
<h1>Login</h1>
<c:if test="${not empty error}">
    <p style="color:red">${error}</p>
</c:if>
<form method="post" action="${pageContext.request.contextPath}/auth">
    <input type="hidden" name="action" value="login" />
    <div>
        <label>Username: <input type="text" name="username" /></label>
    </div>
    <div>
        <label>Password: <input type="password" name="password" /></label>
    </div>
    <div>
        <button type="submit">Login</button>
    </div>
</form>
<p>No account? <a href="${pageContext.request.contextPath}/auth?action=register">Register</a></p>
</body>
</html>
