package com.example.store.controller;

import com.example.store.dao.ProductDAO;
import com.example.store.model.Product;
import com.example.store.util.DBConnectionManager;
import com.example.store.util.CacheManager;
import com.example.store.util.OptimisticLockException;
import com.example.store.util.DataIntegrityException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
@jakarta.servlet.annotation.MultipartConfig(maxFileSize = 5 * 1024 * 1024)
public class ProductServlet extends HttpServlet {
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp"};
    private java.nio.file.Path uploadDir;
    private ProductDAO productDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        // Initialize DBConnectionManager and CacheManager here (config from context params)
        DBConnectionManager db = new DBConnectionManager("jdbc:mysql://localhost:3306/homework_ds","root","");
        CacheManager cache = new CacheManager();
        productDAO = new ProductDAO(db, cache);
        String basePath = System.getProperty("user.home") + "/product-uploads";
        uploadDir = java.nio.file.Paths.get(basePath);
        try {
            java.nio.file.Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new ServletException("Unable to initialize upload directory", e);
        }
        getServletContext().setAttribute("uploadDir", uploadDir.toString());
    }

    private boolean isAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        Object role = session.getAttribute("currentUserRole");
        return role != null && "ADMIN".equals(role.toString());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // route actions: list, new, edit, delete
        String action = req.getParameter("action");
        if (action == null || action.equals("list")) {
            handleList(req, resp);
        } else if (action.equals("new")) {
            if (!isAdmin(req)) { resp.sendRedirect(req.getContextPath() + "/products?action=list"); return; }
            handleNew(req, resp);
        } else if (action.equals("edit")) {
            if (!isAdmin(req)) { resp.sendRedirect(req.getContextPath() + "/products?action=list"); return; }
            handleEdit(req, resp);
        } else if (action.equals("delete")) {
            if (!isAdmin(req)) { resp.sendRedirect(req.getContextPath() + "/products?action=list"); return; }
            handleDelete(req, resp);
        } else {
            handleList(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // handle save
        if (!isAdmin(req)) { resp.sendRedirect(req.getContextPath() + "/products?action=list"); return; }
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
        try {
            if (idParam != null) {
                int id = Integer.parseInt(idParam);
                productDAO.delete(id);
            }
            resp.sendRedirect(req.getContextPath() + "/products?action=list");
        } catch (DataIntegrityException die) {
            // Show friendly message on list view
            req.setAttribute("error", die.getMessage());
            handleList(req, resp);
        }
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
        String imageUrl = req.getParameter("imageUrl");
        String versionParam = req.getParameter("version");
        jakarta.servlet.http.Part imagePart = req.getPart("imageFile");

        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(priceParam));
        product.setDescription(description);

        try {
            if (idParam == null || idParam.isEmpty()) {
                if (isValidImagePart(imagePart)) {
                    product.setImageUrl(storeImage(imagePart));
                } else {
                    product.setImageUrl(imageUrl);
                }
                productDAO.create(product);
            } else {
                int id = Integer.parseInt(idParam);
                int expectedVersion = Integer.parseInt(versionParam);
                Optional<Product> existing = productDAO.findById(id);
                existing.ifPresent(p -> product.setImageUrl(p.getImageUrl()));
                product.setId(id);
                if (isValidImagePart(imagePart)) {
                    product.setImageUrl(storeImage(imagePart));
                } else if (imageUrl != null && !imageUrl.isBlank()) {
                    product.setImageUrl(imageUrl);
                }
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

    private boolean isValidImagePart(jakarta.servlet.http.Part part) {
        if (part == null || part.getSize() == 0) return false;
        String type = part.getContentType();
        for (String allowed : ALLOWED_TYPES) {
            if (allowed.equalsIgnoreCase(type)) return true;
        }
        return false;
    }

    private String storeImage(jakarta.servlet.http.Part part) throws IOException {
        String submitted = part.getSubmittedFileName();
        String safeName = java.nio.file.Paths.get(submitted == null ? "" : submitted).getFileName().toString();
        String ext = "";
        int dot = safeName.lastIndexOf('.');
        if (dot >= 0) ext = safeName.substring(dot);
        String fileName = java.util.UUID.randomUUID() + ext;
        java.nio.file.Path target = uploadDir.resolve(fileName);
        try (java.io.InputStream in = part.getInputStream()) {
            java.nio.file.Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return reqPath("/images/" + fileName);
    }

    private String reqPath(String path) {
        return path;
    }
}
