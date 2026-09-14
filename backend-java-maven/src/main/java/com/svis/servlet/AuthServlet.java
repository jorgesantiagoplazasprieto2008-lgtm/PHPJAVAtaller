package com.svis.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.svis.dao.UsuarioDAO;
import com.svis.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String pathInfo = req.getPathInfo();

        try {
            if ("/estudiantes".equalsIgnoreCase(pathInfo)) {
                List<Usuario> estudiantes = usuarioDAO.listarEstudiantes();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(estudiantes));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Ruta no disponible\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && !pathInfo.equals("/") && !pathInfo.equalsIgnoreCase("/login")) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Endpoint de autenticación no encontrado\"}");
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (java.io.InputStream is = req.getInputStream();
             BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        try {
            JsonObject json = gson.fromJson(sb.toString(), JsonObject.class);
            if (json == null || !json.has("documento") || !json.has("password")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Debe ingresar documento y contraseña.\"}");
                return;
            }

            String documento = json.get("documento").getAsString();
            String password = json.get("password").getAsString();

            Usuario u = usuarioDAO.autenticar(documento, password);
            if (u == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"Credenciales inválidas. Verifique documento y clave.\"}");
                return;
            }

            Map<String, Object> respMap = new HashMap<>();
            respMap.put("success", true);
            respMap.put("id", u.getId());
            respMap.put("documento", u.getDocumento());
            respMap.put("nombre", u.getNombreCompleto());
            respMap.put("correo", u.getCorreo());
            respMap.put("rol", u.getRol());

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(respMap));

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Error al autenticar: " + e.getMessage() + "\"}");
        }
    }
}
