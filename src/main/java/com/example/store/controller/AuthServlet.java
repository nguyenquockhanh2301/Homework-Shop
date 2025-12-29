package com.example.store.controller;

import com.example.store.dao.UserDAO;
import com.example.store.model.User;
import com.example.store.util.DBConnectionManager;
import com.example.store.util.PasswordHasher;
import com.example.store.util.SchemaInitializer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

@WebServlet(name = "AuthServlet", urlPatterns = {"/auth"})
public class AuthServlet extends HttpServlet {
    private UserDAO userDAO;
    private PasswordHasher hasher;

    @Override
    public void init() throws ServletException {
        super.init();
        DBConnectionManager db = new DBConnectionManager("jdbc:mysql://localhost:3306/homework_ds","root","");
        // Ensure minimal schema exists in dev envs so registration/login doesn't 500
        SchemaInitializer.ensureUsersTable(db);
        userDAO = new UserDAO(db);
        hasher = new PasswordHasher();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("register".equals(action)) {
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
        } else if ("logout".equals(action)) {
            handleLogout(req, resp);
        } else {
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("register".equals(action)) {
            handleRegister(req, resp);
        } else if ("logout".equals(action)) {
            handleLogout(req, resp);
        } else {
            handleLogin(req, resp);
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (isBlank(username) || isBlank(email) || isBlank(password)) {
            req.setAttribute("error", "All fields are required.");
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        if (userDAO.findByUsername(username).isPresent()) {
            req.setAttribute("error", "Username already exists.");
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }
        if (userDAO.findByEmail(email).isPresent()) {
            req.setAttribute("error", "Email already registered.");
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        byte[] salt = hasher.generateSalt();
        byte[] hash = hasher.hash(password.toCharArray(), salt);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordSalt(salt);
        user.setPasswordHash(hash);
        user.setRole("USER");

        userDAO.create(user);

        // auto login after register
        HttpSession session = req.getSession();
        session.setAttribute("currentUserId", user.getId());
        session.setAttribute("currentUsername", user.getUsername());
        session.setAttribute("currentUserRole", user.getRole());

        resp.sendRedirect(req.getContextPath() + "/products?action=list");
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (isBlank(username) || isBlank(password)) {
            req.setAttribute("error", "Username and password required.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        Optional<User> opt = userDAO.findByUsername(username);
        if (!opt.isPresent()) {
            req.setAttribute("error", "Invalid credentials.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }
        User user = opt.get();
        boolean ok = hasher.verify(password.toCharArray(), user.getPasswordSalt(), user.getPasswordHash());
        if (!ok) {
            req.setAttribute("error", "Invalid credentials.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        HttpSession session = req.getSession();
        session.setAttribute("currentUserId", user.getId());
        session.setAttribute("currentUsername", user.getUsername());
        session.setAttribute("currentUserRole", user.getRole());

        resp.sendRedirect(req.getContextPath() + "/products?action=list");
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/auth?action=login");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
