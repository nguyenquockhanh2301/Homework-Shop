<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title><c:choose><c:when test="${product.id <= 0}">Add Product</c:when><c:otherwise>Edit Product</c:otherwise></c:choose></title>
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
        .img-preview { width: 140px; height: 140px; object-fit: cover; border: 1px solid #ccc; border-radius: 6px; background: #f9f9f9; }
        .img-row { display: flex; align-items: center; gap: 12px; }
    </style>
</head>
<body>
<c:if test="${sessionScope.currentUserRole ne 'ADMIN'}">
    <p class="error">Only admins can manage products.</p>
    <a href="${pageContext.request.contextPath}/products?action=list" class="btn btn-secondary">Back</a>
</c:if>
<c:if test="${sessionScope.currentUserRole eq 'ADMIN'}">
<h1><c:choose><c:when test="${product.id <= 0}">Add New Product</c:when><c:otherwise>Edit Product #${product.id}</c:otherwise></c:choose></h1>

<c:if test="${not empty error}">
    <div class="error">${error}</div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/products" enctype="multipart/form-data">
    <input type="hidden" name="action" value="save" />
    <c:if test="${product.id > 0}">
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

    <div class="form-group">
        <label for="imageUrl">Image URL</label>
        <input type="text" id="imageUrl" name="imageUrl" value="${product.imageUrl}" placeholder="https://example.com/image.jpg" />
        <div class="info">Paste a direct image URL (jpg/png/webp) or upload below.</div>
    </div>

    <div class="form-group">
        <label for="imageFile">Upload Image</label>
        <input type="file" id="imageFile" name="imageFile" accept="image/jpeg,image/png,image/webp" />
        <div class="info">Max 5 MB. If both upload and URL are provided, the upload is used.</div>
    </div>

    <div class="form-group img-row">
        <div>
            <div class="info">Preview</div>
            <img src="${empty product.imageUrl ? 'https://via.placeholder.com/140?text=No+Image' : product.imageUrl}" alt="Preview" class="img-preview" onerror="this.src='https://via.placeholder.com/140?text=No+Image'" />
        </div>
    </div>

    <c:if test="${product.id > 0}">
        <div class="info">Version: ${product.version} (optimistic locking enabled)</div>
    </c:if>
    
    <div style="margin-top: 20px;">
        <button type="submit" class="btn btn-primary">Save Product</button>
        <a href="${pageContext.request.contextPath}/products?action=list" class="btn btn-secondary">Cancel</a>
    </div>
</form>
</c:if>
</body>
</html>
