# Product & Cart MVC2 Webapp

MVC2 (Servlet + JSP + JavaBean + DAO) web application for product and shopping cart management using JDK 17, Tomcat 11, and MySQL (XAMPP).

## Features

- ✅ Complete Product CRUD with optimistic locking (version column)
- ✅ ConcurrentHashMap-based product cache for O(1) lookups
- ✅ Session-based shopping cart with DB persistence
- ✅ LinkedHashMap for O(1) cart operations
- ✅ JSTL + EL in JSPs (no scriptlets)
- ✅ MVC2 architecture with clear separation of concerns

## Quick Start

### 1. Database Setup
Start XAMPP and ensure MySQL is running:
```powershell
# Import schema
mysql -u root -p < schema.sql

# Load sample data
mysql -u root -p < sample-data.sql
```

### 2. Build
```powershell
mvn clean package
```

### 3. Deploy
Copy `target/product-cart-mvc2.war` to Tomcat 11's `webapps` folder and start Tomcat.

### 4. Access
Open browser: `http://localhost:8080/product-cart-mvc2/products?action=list`

## Project Structure

```
src/main/java/com/example/store/
├── controller/
│   ├── ProductServlet.java    # Product CRUD controller
│   └── CartServlet.java        # Cart operations controller
├── dao/
│   ├── ProductDAO.java         # Product CRUD + cache
│   └── CartDAO.java            # Cart persistence
├── model/
│   ├── Product.java            # Product JavaBean
│   ├── Cart.java               # Cart container
│   └── CartItem.java           # Cart item
└── util/
    ├── DBConnectionManager.java
    ├── CacheManager.java
    └── OptimisticLockException.java

src/main/webapp/WEB-INF/views/
├── product-list.jsp            # Product listing
├── product-form.jsp            # Create/edit form
└── cart.jsp                    # Shopping cart view
```

## Data Structure Choices

### 1. ConcurrentHashMap<Integer, Product> (Product Cache)
- **Why**: Thread-safe O(1) lookups for concurrent servlet requests
- **Why not HashMap**: Not thread-safe for multi-threaded environment
- **Big-O**: get/put/remove all O(1)

### 2. LinkedHashMap<Integer, CartItem> (Shopping Cart)
- **Why**: O(1) lookups + preserves insertion order for display
- **Why not List**: Would require O(n) search to find items
- **Big-O**: add/update/remove O(1), totalPrice/totalQty O(n)

### 3. ArrayList<Product> (DAO Results)
- **Why**: Indexed access O(1), preserves DB ordering
- **Big-O**: get(index) O(1), iteration O(n)

## Cache Strategy

- **Read by ID**: Check cache first (O(1)), on miss load from DB and populate
- **Read all**: Load from DB to ensure completeness, refresh cache entries
- **Create**: Insert DB → cache.put()
- **Update**: Optimistic-lock UPDATE DB → reload → cache.put()
- **Delete**: Delete DB → cache.remove()
- **External changes**: Use refreshCache() or scheduled refresh

## Database Schema

**products**
- id (PK, AUTO_INCREMENT)
- name, price, description
- version (optimistic locking)
- created_at, updated_at

**carts**
- id (PK), session_id, user_id
- status, version
- created_at, updated_at

**cart_items**
- id (PK), cart_id (FK), product_id (FK)
- quantity, price_snapshot

## Configuration

DB credentials in servlet init() methods:
- URL: `jdbc:mysql://localhost:3306/homework_ds`
- User: `root`
- Password: (empty for XAMPP default)

## Testing

See [TESTS.md](TESTS.md) for test scenarios.
See [CACHE.md](CACHE.md) for cache behavior details.

## Assignment Requirements Met

1. ✅ Products table (id, name, price, description)
2. ✅ Product JavaBean + ProductDAO with full CRUD
3. ✅ ConcurrentHashMap cache with clear read/write rules
4. ✅ ProductServlet (list/new/edit/delete/save)
5. ✅ JSP views using JSTL + EL only
6. ✅ Cart with Map<Integer, CartItem> + CartServlet
7. ✅ Analysis comments (ConcurrentHashMap, LinkedHashMap, ArrayList, Big-O)

## Notes

- Optimistic locking prevents concurrent update conflicts
- Cart persists across server restarts via DB
- Session binding allows cart recovery
- All DAOs use transactions for data integrity
