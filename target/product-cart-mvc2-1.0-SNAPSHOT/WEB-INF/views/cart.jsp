<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title>Shopping Cart</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        table { border-collapse: collapse; width: 100%; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #ff9800; color: white; }
        tr:hover { background-color: #f5f5f5; }
        .total-row { font-weight: bold; background-color: #f9f9f9; }
        .btn { padding: 5px 10px; text-decoration: none; border: none; cursor: pointer; margin-right: 5px; }
        .btn-primary { background-color: #4CAF50; color: white; }
        .btn-danger { background-color: #f44336; color: white; }
        .btn-warning { background-color: #ff9800; color: white; }
        .qty-input { width: 60px; padding: 5px; }
        .empty-cart { text-align: center; padding: 40px; background-color: #f5f5f5; margin: 20px 0; }
        .cart-actions { margin-top: 20px; text-align: right; }
        .header { display: flex; justify-content: space-between; align-items: center; }
    </style>
</head>
<body>
<div class="header">
    <h1>Shopping Cart</h1>
    <a href="${pageContext.request.contextPath}/products?action=list" class="btn btn-primary">Continue Shopping</a>
</div>

<c:choose>
    <c:when test="${empty cart or empty cart.items}">
        <div class="empty-cart">
            <h2>Your cart is empty</h2>
            <p>Add some products to get started!</p>
            <a href="${pageContext.request.contextPath}/products?action=list" class="btn btn-primary">Browse Products</a>
        </div>
    </c:when>
    <c:otherwise>
        <table>
            <thead>
                <tr>
                    <th>Product</th>
                    <th>Description</th>
                    <th>Unit Price</th>
                    <th>Quantity</th>
                    <th>Subtotal</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="item" items="${cart.items}">
                <tr>
                    <td>${item.product.name}</td>
                    <td>${item.product.description}</td>
                    <td><fmt:formatNumber value="${item.priceSnapshot}" type="currency" /></td>
                    <td>
                        <form method="post" action="${pageContext.request.contextPath}/cart" style="display:inline">
                            <input type="hidden" name="action" value="update" />
                            <input type="hidden" name="id" value="${item.product.id}" />
                            <input type="number" name="qty" value="${item.quantity}" min="1" class="qty-input" />
                            <button type="submit" class="btn btn-primary">Update</button>
                        </form>
                    </td>
                    <td><fmt:formatNumber value="${item.totalPrice}" type="currency" /></td>
                    <td>
                        <form method="post" action="${pageContext.request.contextPath}/cart" style="display:inline">
                            <input type="hidden" name="action" value="remove" />
                            <input type="hidden" name="id" value="${item.product.id}" />
                            <button type="submit" class="btn btn-danger" onclick="return confirm('Remove this item from cart?');">Remove</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <tr class="total-row">
                <td colspan="3">Total</td>
                <td>${cart.totalQuantity()} items</td>
                <td><fmt:formatNumber value="${cart.totalPrice()}" type="currency" /></td>
                <td></td>
            </tr>
            </tbody>
        </table>
        
        <div class="cart-actions">
            <form method="post" action="${pageContext.request.contextPath}/cart" style="display:inline">
                <input type="hidden" name="action" value="clear" />
                <button type="submit" class="btn btn-danger" onclick="return confirm('Clear entire cart?');">Clear Cart</button>
            </form>
            <button class="btn btn-warning" onclick="alert('Checkout not implemented yet');">Proceed to Checkout</button>
        </div>
    </c:otherwise>
</c:choose>

<!-- Data Structure Analysis Comments -->
<!--
Data Structure Choices:

1. LinkedHashMap<Integer, CartItem> for cart items:
   - WHY: O(1) lookup by product ID for update/remove operations
   - WHY: Preserves insertion order for predictable display
   - WHY: Map prevents duplicate products (merge quantities instead)
   - Alternative List would require O(n) search to find product

2. Time Complexity Analysis:
   - addProduct(product, qty): O(1) - HashMap compute operation
   - updateQuantity(productId, qty): O(1) - direct Map access by key
   - removeProduct(productId): O(1) - direct Map remove by key
   - totalQuantity(): O(n) - must iterate all items
   - totalPrice(): O(n) - must iterate and sum all items
-->
</body>
</html>
