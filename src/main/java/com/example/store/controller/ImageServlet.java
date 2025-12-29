package com.example.store.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@WebServlet(name = "ImageServlet", urlPatterns = {"/images/*"})
public class ImageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uploadDir = (String) getServletContext().getAttribute("uploadDir");
        if (uploadDir == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String requested = req.getPathInfo();
        if (requested == null || requested.contains("..")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Path file = Paths.get(uploadDir).resolve(requested.substring(1));
        if (!Files.exists(file) || !Files.isReadable(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String mime = Files.probeContentType(file);
        if (mime == null) mime = "application/octet-stream";
        resp.setContentType(mime);
        resp.setHeader("Cache-Control", "public, max-age=604800");
        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(file, out);
        }
    }
}

