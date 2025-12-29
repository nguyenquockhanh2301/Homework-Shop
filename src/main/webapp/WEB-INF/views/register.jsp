<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title>Register</title>
</head>
<body>
<h1>Register</h1>
<c:if test="${not empty error}">
    <p style="color:red">${error}</p>
</c:if>
<form method="post" action="${pageContext.request.contextPath}/auth">
    <input type="hidden" name="action" value="register" />
    <div>
        <label>Username: <input type="text" name="username" /></label>
    </div>
    <div>
        <label>Email: <input type="email" name="email" /></label>
    </div>
    <div>
        <label>Password: <input type="password" name="password" /></label>
    </div>
    <div>
        <button type="submit">Register</button>
    </div>
</form>
<p>Already have an account? <a href="${pageContext.request.contextPath}/auth?action=login">Login</a></p>
</body>
</html>

