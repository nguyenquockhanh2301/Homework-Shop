# Product & Cart MVC2 Webapp

MVC2 (Servlet + JSP + JavaBean + DAO) web application for product and shopping cart management using JDK 17, Tomcat 11, and MySQL (XAMPP).

## Features

- ✅ Complete Product CRUD with optimistic locking (version column)
- ✅ ConcurrentHashMap-based product cache for O(1) lookups
- ✅ Session-based shopping cart with DB persistence
- ✅ LinkedHashMap for O(1) cart operations
- ✅ JSTL + EL in JSPs (no scriptlets)
- ✅ MVC2 architecture with clear separation of concerns
- ✅ Authentication: register, login, logout with salted+hashed passwords
- ✅ Role-based access: ADMIN only for product create/edit/delete
- ✅ Image upload per product (jpg/png/webp) with stored URL and fallback

## Quick Start

### 1. Database Setup
Start XAMPP and ensure MySQL is running:
```powershell
# Import schema
mysql -u root -p < schema.sql

# Load sample data
mysql -u root -p < sample-data.sql
```

> If you need an admin user, insert a row into `users` with `role='ADMIN'` or update an existing user role via SQL.

### 2. Build
```powershell
mvn clean package
```

### 3. Deploy
Copy `target/product-cart-mvc2.war` to Tomcat 11's `webapps` folder and start Tomcat.

### 4. Access
Open browser: `http://localhost:8080/product-cart-mvc2/products?action=list`

### 5. Auth Flow
- Register: `GET /auth?action=register` → submit form
- Login: `GET /auth?action=login` → submit form
- Logout: `GET /auth?action=logout`
- Session keys: `currentUserId`, `currentUsername`, `currentUserRole`

## Project Structure

```
src/main/java/com/example/store/
├── controller/
│   ├── ProductServlet.java    # Product CRUD controller + image upload + admin gate
│   ├── CartServlet.java       # Cart operations controller (session-based)
│   └── AuthServlet.java       # Register/Login/Logout
├── dao/
│   ├── ProductDAO.java        # Product CRUD + cache + optimistic locking
│   ├── CartDAO.java           # Cart persistence
│   └── UserDAO.java           # User lookup/create
├── model/                     # Product, Cart, CartItem, User beans
└── util/                      # DBConnectionManager, CacheManager, PasswordHasher, etc.

src/main/webapp/WEB-INF/views/
├── product-list.jsp           # Product listing (shows image placeholder if none)
├── product-form.jsp           # Create/edit form with image upload field
├── cart.jsp                   # Shopping cart view
├── login.jsp                  # Login form
└── register.jsp               # Registration form
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
- name, price, description, image_url
- version (optimistic locking)
- created_at, updated_at

**users**
- id (PK, AUTO_INCREMENT)
- username, email, password_hash, password_salt, role (USER/ADMIN)
- version, created_at, updated_at

**carts**
- id (PK), session_id, user_id
- status, version
- created_at, updated_at

**cart_items**
- id (PK), cart_id (FK), product_id (FK)
- quantity, price_snapshot

## Image Handling

- Multipart uploads enabled on `ProductServlet` (`@MultipartConfig`).
- Allowed types: jpg, png, webp; max size 5 MB.
- Files saved to `~/product-uploads` (created on startup); path stored as imageUrl.
- `ImageServlet` serves files from upload directory; `product-list.jsp` shows a placeholder if no image.

## Access Control

- Product create/edit/delete routes require `currentUserRole == "ADMIN"` (checked in `ProductServlet`).
- All users (even anonymous) can view product list and add to cart; cart is session-based.

## Configuration

DB credentials in servlet init() methods:
- URL: `jdbc:mysql://localhost:3306/homework_ds`
- User: `root`
- Password: (empty for XAMPP default)

To change upload directory: set `user.home` or update `ProductServlet` init; path is also stored in servlet context attribute `uploadDir`.

## Verification Flow

1) Import schema + sample data; ensure MySQL running.
2) `mvn clean package`; deploy WAR to Tomcat 11.
3) Register a user (`/auth?action=register`), then login.
4) Promote a user to ADMIN via SQL and verify product create/edit/delete is allowed only for ADMIN.
5) Add products with and without images; verify list renders images/placeholder and optimistic locking errors surface on concurrent edits.
6) Add items to cart, update quantities, clear cart; ensure cart persists across session and DB (see `TESTS.md`).

## Testing

See [TESTS.md](TESTS.md) for test scenarios.
See [CACHE.md](CACHE.md) for cache behavior details.

## Assignment Requirements Met

1. ✅ Products table (id, name, price, description, image_url)
2. ✅ Product JavaBean + ProductDAO with full CRUD
3. ✅ ConcurrentHashMap cache with clear read/write rules
4. ✅ ProductServlet (list/new/edit/delete/save) with admin gate + image upload
5. ✅ JSP views using JSTL + EL only
6. ✅ Cart with Map<Integer, CartItem> + CartServlet using session
7. ✅ Authentication + role-based authorization
8. ✅ Analysis comments (ConcurrentHashMap, LinkedHashMap, ArrayList, Big-O)

## Notes

- Optimistic locking prevents concurrent update conflicts
- Cart persists across server restarts via DB
- Session binding allows cart recovery
- All DAOs use transactions for data integrity
- Auto-increment IDs may not reuse deleted IDs by design; gaps are acceptable for auditability
