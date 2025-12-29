<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title>Products</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        table { border-collapse: collapse; width: 100%; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        tr:hover { background-color: #f5f5f5; }
        .actions a, .actions button { margin-right: 5px; }
        .btn { padding: 5px 10px; text-decoration: none; border: none; cursor: pointer; }
        .btn-primary { background-color: #4CAF50; color: white; }
        .btn-danger { background-color: #f44336; color: white; }
        .btn-warning { background-color: #ff9800; color: white; }
        .header { display: flex; justify-content: space-between; align-items: center; }
        .auth { font-size: 0.9em; }
        .error { color: #b30000; background: #ffe6e6; padding: 10px; border: 1px solid #b30000; margin-top: 10px; }
    </style>
</head>
<body>
<div class="header">
    <h1>Product Management</h1>
    <div>
        <a href="${pageContext.request.contextPath}/cart?action=view" class="btn btn-warning">View Cart</a>
        <c:if test="${sessionScope.currentUserRole eq 'ADMIN'}">
            <a href="${pageContext.request.contextPath}/products?action=list" class="btn btn-primary">Admin: Products</a>
            <a href="${pageContext.request.contextPath}/products?action=new" class="btn btn-primary">Add New Product</a>
        </c:if>
    </div>
</div>
<div class="auth">
    <c:choose>
        <c:when test="${not empty sessionScope.currentUsername}">
            Hello, <strong>${sessionScope.currentUsername}</strong> (<c:out value="${sessionScope.currentUserRole}" />) |
            <a href="${pageContext.request.contextPath}/auth?action=logout">Logout</a>
        </c:when>
        <c:otherwise>
            <a href="${pageContext.request.contextPath}/auth?action=login">Login</a> |
            <a href="${pageContext.request.contextPath}/auth?action=register">Register</a>
        </c:otherwise>
    </c:choose>
</div>
<c:if test="${not empty error}">
    <div class="error">${error}</div>
</c:if>

<c:if test="${empty products}">
    <p>No products available. <a href="${pageContext.request.contextPath}/products?action=new">Add your first product</a></p>
</c:if>

<c:if test="${not empty products}">
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Price</th>
                <th>Description</th>
                <th>Version</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
        <c:forEach var="p" items="${products}">
            <tr>
                <td>${p.id}</td>
                <td>${p.name}</td>
                <td><fmt:formatNumber value="${p.price}" type="currency" /></td>
                <td>${p.description}</td>
                <td>${p.version}</td>
                <td class="actions">
                    <c:if test="${sessionScope.currentUserRole eq 'ADMIN'}">
                        <a href="${pageContext.request.contextPath}/products?action=edit&id=${p.id}" class="btn btn-primary">Edit</a>
                        <a href="${pageContext.request.contextPath}/products?action=delete&id=${p.id}"
                           onclick="return confirm('This will delete the product. If the product is in any cart, deletion will fail. Continue?');" class="btn btn-danger">Delete</a>
                    </c:if>
                    <form method="post" action="${pageContext.request.contextPath}/cart" style="display:inline">
                        <input type="hidden" name="action" value="add" />
                        <input type="hidden" name="id" value="${p.id}" />
                        <input type="hidden" name="qty" value="1" />
                        <button type="submit" class="btn btn-warning">Add to Cart</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>
</body>
</html>
