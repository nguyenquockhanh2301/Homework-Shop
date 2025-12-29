<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title>Products</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f7fb; }
        .grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(240px,1fr)); gap: 16px; margin-top: 20px; }
        .card { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.04); display: flex; flex-direction: column; gap: 10px; }
        .card img { width: 100%; height: 160px; object-fit: cover; border-radius: 8px; background: #f9fafb; border: 1px solid #e5e7eb; }
        .card h3 { margin: 0; font-size: 1.05rem; }
        .card .price { font-weight: bold; color: #0f766e; }
        .card .desc { color: #555; min-height: 48px; }
        .toolbar { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
        .btn { padding: 6px 10px; text-decoration: none; border: none; cursor: pointer; border-radius: 6px; font-size: 0.9rem; }
        .btn-primary { background-color: #2563eb; color: white; }
        .btn-danger { background-color: #dc2626; color: white; }
        .btn-warning { background-color: #d97706; color: white; }
        .btn-ghost { background: transparent; color: #2563eb; border: 1px solid #2563eb; }
        .header { display: flex; justify-content: space-between; align-items: center; }
        .auth { font-size: 0.9em; }
        .error { color: #b30000; background: #ffe6e6; padding: 10px; border: 1px solid #b30000; margin-top: 10px; }
        .empty { padding: 20px; background: #fff; border: 1px dashed #cbd5e1; border-radius: 8px; }
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
    <div class="grid">
        <c:forEach var="p" items="${products}">
            <div class="card">
                <img src="${empty p.imageUrl ? 'https://via.placeholder.com/300x160?text=No+Image' : p.imageUrl}" alt="${p.name}" onerror="this.src='https://via.placeholder.com/300x160?text=No+Image'" />
                <h3>${p.name}</h3>
                <div class="price"><fmt:formatNumber value="${p.price}" type="currency" /></div>
                <div class="desc">${p.description}</div>
                <div class="toolbar">
                    <form method="post" action="${pageContext.request.contextPath}/cart" style="display:inline">
                        <input type="hidden" name="action" value="add" />
                        <input type="hidden" name="id" value="${p.id}" />
                        <input type="hidden" name="qty" value="1" />
                        <button type="submit" class="btn btn-warning">Add to Cart</button>
                    </form>
                    <c:if test="${sessionScope.currentUserRole eq 'ADMIN'}">
                        <a href="${pageContext.request.contextPath}/products?action=edit&id=${p.id}" class="btn btn-primary">Edit</a>
                        <a href="${pageContext.request.contextPath}/products?action=delete&id=${p.id}" onclick="return confirm('This will delete the product. If the product is in any cart, deletion will fail. Continue?');" class="btn btn-danger">Delete</a>
                    </c:if>
                </div>
            </div>
        </c:forEach>
    </div>
</c:if>
</body>
</html>
