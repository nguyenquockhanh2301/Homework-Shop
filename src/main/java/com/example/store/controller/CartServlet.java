package com.example.store.controller;

import com.example.store.dao.CartDAO;
import com.example.store.dao.ProductDAO;
import com.example.store.model.Cart;
import com.example.store.model.Product;
import com.example.store.util.DBConnectionManager;
import com.example.store.util.CacheManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

/**
 * CartServlet: MVC2 controller for shopping cart operations.
 * Routes: /cart?action=add|update|remove|clear|view
 * 
 * Data Structure Analysis:
 * 
 * WHY LinkedHashMap<Integer, CartItem> for cart:
 * - O(1) lookup by product ID - critical for update/remove operations
 * - Preserves insertion order for predictable display to users
 * - Prevents duplicate products (quantities are merged instead)
 * - Map interface allows direct key-based access vs List requiring O(n) search
 * 
 * WHY NOT List<CartItem>:
 * - List would require O(n) linear search to find item by product ID
 * - No built-in duplicate prevention - would need manual checks
 * - Update/remove operations would be O(n) instead of O(1)
 * 
 * Big-O Time Complexity:
 * - findById from cache: O(1) - ConcurrentHashMap direct lookup
 * - addProduct to cart: O(1) - LinkedHashMap.compute() operation
 * - updateQuantity in cart: O(1) - LinkedHashMap.get() + put()
 * - removeProduct from cart: O(1) - LinkedHashMap.remove()
 * - totalQuantity: O(n) where n = number of distinct items
 * - totalPrice: O(n) where n = number of distinct items
 */
@WebServlet(name = "CartServlet", urlPatterns = {"/cart"})
public class CartServlet extends HttpServlet {
    private CartDAO cartDAO;
    private ProductDAO productDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        DBConnectionManager db = new DBConnectionManager("jdbc:mysql://localhost:3306/homework_ds","root","");
        cartDAO = new CartDAO(db);
        productDAO = new ProductDAO(db, new CacheManager());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null || action.equals("view")) {
            viewCart(req, resp);
        } else {
            viewCart(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("add".equals(action)) {
            addToCart(req, resp);
        } else if ("update".equals(action)) {
            updateQuantity(req, resp);
        } else if ("remove".equals(action)) {
            removeFromCart(req, resp);
        } else if ("clear".equals(action)) {
            clearCart(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
        }
    }

    /**
     * Get or create cart for current session.
     * Loads from DB if exists, otherwise creates new cart.
     * Cart is bound to session_id and optionally user_id.
     */
    private Cart getOrCreateCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        Long cartId = (Long) session.getAttribute("cartId");
        
        if (cart != null && cartId != null) {
            return cart;
        }
        
        // Try loading from DB by session ID
        String sessionId = session.getId();
        cart = cartDAO.loadCartBySessionId(sessionId);
        
        if (cart == null) {
            // Create new cart
            cart = new Cart();
            cart.setSessionId(sessionId);
            // TODO: Set user_id here if authentication is implemented
            long newCartId = cartDAO.createCart(cart);
            cart.setId(newCartId);
        }
        
        session.setAttribute("cart", cart);
        session.setAttribute("cartId", cart.getId());
        return cart;
    }

    /**
     * Add product to cart. Uses Map.compute for O(1) atomic add/merge operation.
     */
    private void addToCart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String idParam = req.getParameter("id");
            String qtyParam = req.getParameter("qty");
            
            if (idParam == null) {
                resp.sendRedirect(req.getContextPath() + "/products?action=list");
                return;
            }
            
            int productId = Integer.parseInt(idParam);
            int quantity = (qtyParam != null) ? Integer.parseInt(qtyParam) : 1;
            
            // Load product from cache (O(1) lookup)
            Optional<Product> opt = productDAO.findById(productId);
            if (!opt.isPresent()) {
                resp.sendRedirect(req.getContextPath() + "/products?action=list");
                return;
            }
            
            HttpSession session = req.getSession();
            Cart cart = getOrCreateCart(session);
            
            // Add to cart - O(1) operation using LinkedHashMap
            cart.addProduct(opt.get(), quantity);
            
            // Persist to DB
            cartDAO.saveCart(cart);
            
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/products?action=list");
        }
    }

    /**
     * Update quantity in cart. O(1) operation via direct Map access.
     */
    private void updateQuantity(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String idParam = req.getParameter("id");
            String qtyParam = req.getParameter("qty");
            
            if (idParam == null || qtyParam == null) {
                resp.sendRedirect(req.getContextPath() + "/cart?action=view");
                return;
            }
            
            int productId = Integer.parseInt(idParam);
            int quantity = Integer.parseInt(qtyParam);
            
            HttpSession session = req.getSession();
            Cart cart = getOrCreateCart(session);
            
            // Update quantity - O(1) via LinkedHashMap.get() + set
            cart.updateQuantity(productId, quantity);
            
            // Persist to DB
            cartDAO.saveCart(cart);
            
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
        }
    }

    /**
     * Remove product from cart. O(1) operation via Map.remove().
     */
    private void removeFromCart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String idParam = req.getParameter("id");
            if (idParam == null) {
                resp.sendRedirect(req.getContextPath() + "/cart?action=view");
                return;
            }
            
            int productId = Integer.parseInt(idParam);
            
            HttpSession session = req.getSession();
            Cart cart = getOrCreateCart(session);
            
            // Remove from cart - O(1) via LinkedHashMap.remove()
            cart.removeProduct(productId);
            
            // Persist to DB
            cartDAO.saveCart(cart);
            
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
        }
    }

    /**
     * Clear entire cart. Removes all items and marks cart as cleared in DB.
     */
    private void clearCart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            HttpSession session = req.getSession();
            Cart cart = getOrCreateCart(session);
            
            cart.clear();
            cartDAO.clearCart(cart.getId());
            
            // Remove from session
            session.removeAttribute("cart");
            session.removeAttribute("cartId");
            
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
        }
    }

    /**
     * View cart contents. Loads cart from session/DB and forwards to JSP.
     */
    private void viewCart(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        Cart cart = getOrCreateCart(session);
        req.setAttribute("cart", cart);
        req.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(req, resp);
    }
}
