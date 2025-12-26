<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title><c:choose><c:when test="${empty product.id}">Add Product</c:when><c:otherwise>Edit Product</c:otherwise></c:choose></title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; max-width: 600px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; font-weight: bold; margin-bottom: 5px; }
        input[type="text"], input[type="number"], textarea { width: 100%; padding: 8px; box-sizing: border-box; }
        textarea { height: 100px; }
        .btn { padding: 10px 20px; margin-right: 10px; cursor: pointer; border: none; }
        .btn-primary { background-color: #4CAF50; color: white; }
        .btn-secondary { background-color: #808080; color: white; text-decoration: none; display: inline-block; }
        .error { color: red; padding: 10px; background-color: #ffe6e6; border: 1px solid red; margin-bottom: 15px; }
        .info { color: #555; font-size: 0.9em; margin-top: 5px; }
    </style>
</head>
<body>
<h1><c:choose><c:when test="${empty product.id}">Add New Product</c:when><c:otherwise>Edit Product #${product.id}</c:otherwise></c:choose></h1>

<c:if test="${not empty error}">
    <div class="error">${error}</div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/products">
    <input type="hidden" name="action" value="save" />
    <c:if test="${not empty product.id}">
        <input type="hidden" name="id" value="${product.id}" />
        <input type="hidden" name="version" value="${product.version}" />
    </c:if>
    
    <div class="form-group">
        <label for="name">Product Name *</label>
        <input type="text" id="name" name="name" value="${product.name}" required />
    </div>
    
    <div class="form-group">
        <label for="price">Price *</label>
        <input type="number" step="0.01" id="price" name="price" value="${product.price}" required />
        <div class="info">Enter price in decimal format (e.g., 19.99)</div>
    </div>
    
    <div class="form-group">
        <label for="description">Description</label>
        <textarea id="description" name="description">${product.description}</textarea>
    </div>
    
    <c:if test="${not empty product.version}">
        <div class="info">Version: ${product.version} (optimistic locking enabled)</div>
    </c:if>
    
    <div style="margin-top: 20px;">
        <button type="submit" class="btn btn-primary">Save Product</button>
        <a href="${pageContext.request.contextPath}/products?action=list" class="btn btn-secondary">Cancel</a>
    </div>
</form>
</body>
</html>
