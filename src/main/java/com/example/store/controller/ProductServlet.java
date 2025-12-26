package com.example.store.controller;

import com.example.store.dao.ProductDAO;
import com.example.store.model.Product;
import com.example.store.util.DBConnectionManager;
import com.example.store.util.CacheManager;
import com.example.store.util.OptimisticLockException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * ProductServlet: MVC2 controller for product CRUD operations.
 * Routes: /products?action=list|new|edit|delete|save
 * 
 * Data structure notes:
 * - Uses ArrayList for findAll() result: provides indexed access O(1) and preserves DB ordering.
 * - DAO uses ConcurrentHashMap for thread-safe product cache: O(1) lookup by ID.
 */
@WebServlet(name = "ProductServlet", urlPatterns = {"/products"})
public class ProductServlet extends HttpServlet {
    private ProductDAO productDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        // Initialize DBConnectionManager and CacheManager here (config from context params)
        DBConnectionManager db = new DBConnectionManager("jdbc:mysql://localhost:3306/homework_ds","root","");
        CacheManager cache = new CacheManager();
        productDAO = new ProductDAO(db, cache);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // route actions: list, new, edit, delete
        String action = req.getParameter("action");
        if (action == null || action.equals("list")) {
            handleList(req, resp);
        } else if (action.equals("new")) {
            handleNew(req, resp);
        } else if (action.equals("edit")) {
            handleEdit(req, resp);
        } else if (action.equals("delete")) {
            handleDelete(req, resp);
        } else {
            handleList(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // handle save
        handleSave(req, resp);
    }

    /**
     * List all products.
     * Loads from DAO (uses ArrayList for result) and forwards to JSP.
     */
    private void handleList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Product> products = productDAO.findAll();
        req.setAttribute("products", products);
        req.getRequestDispatcher("/WEB-INF/views/product-list.jsp").forward(req, resp);
    }

    /**
     * Show empty product form for creation.
     */
    private void handleNew(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Product product = new Product();
        req.setAttribute("product", product);
        req.getRequestDispatcher("/WEB-INF/views/product-form.jsp").forward(req, resp);
    }

    /**
     * Load product by ID and show edit form.
     * Uses cache-first lookup (O(1) if cached).
     */
    private void handleEdit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (idParam == null) {
            resp.sendRedirect(req.getContextPath() + "/products?action=list");
            return;
        }
        int id = Integer.parseInt(idParam);
        Optional<Product> opt = productDAO.findById(id);
        if (opt.isPresent()) {
            req.setAttribute("product", opt.get());
            req.getRequestDispatcher("/WEB-INF/views/product-form.jsp").forward(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/products?action=list");
        }
    }

    /**
     * Delete product by ID.
     * Removes from DB and cache.
     */
    private void handleDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (idParam != null) {
            int id = Integer.parseInt(idParam);
            productDAO.delete(id);
        }
        resp.sendRedirect(req.getContextPath() + "/products?action=list");
    }

    /**
     * Save product (create or update with optimistic locking).
     * On version conflict, show error message.
     */
    private void handleSave(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        String name = req.getParameter("name");
        String priceParam = req.getParameter("price");
        String description = req.getParameter("description");
        String versionParam = req.getParameter("version");

        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(priceParam));
        product.setDescription(description);

        try {
            if (idParam == null || idParam.isEmpty()) {
                // Create new product
                productDAO.create(product);
            } else {
                // Update existing product with optimistic locking
                int id = Integer.parseInt(idParam);
                int expectedVersion = Integer.parseInt(versionParam);
                product.setId(id);
                productDAO.update(product, expectedVersion);
            }
            resp.sendRedirect(req.getContextPath() + "/products?action=list");
        } catch (OptimisticLockException e) {
            // Version conflict - reload product and show error
            req.setAttribute("error", "Product was updated by another user. Please reload and try again.");
            req.setAttribute("product", product);
            req.getRequestDispatcher("/WEB-INF/views/product-form.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error saving product: " + e.getMessage());
            req.setAttribute("product", product);
            req.getRequestDispatcher("/WEB-INF/views/product-form.jsp").forward(req, resp);
        }
    }
}
