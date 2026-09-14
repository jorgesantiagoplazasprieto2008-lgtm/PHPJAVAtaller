package com.svis.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.svis.dao.TokenDAO;
import com.svis.model.TokenOTP;

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

@WebServlet("/api/tokens/*")
public class TokensServlet extends HttpServlet {

    private final TokenDAO tokenDAO = new TokenDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String pathInfo = req.getPathInfo();

        try {
            if ("/usuario".equalsIgnoreCase(pathInfo)) {
                // GET /api/tokens/usuario?encuestaId=1&usuarioId=2
                String encuestaIdStr = req.getParameter("encuestaId");
                String usuarioIdStr = req.getParameter("usuarioId");

                if (encuestaIdStr == null || usuarioIdStr == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\": \"Parámetros encuestaId y usuarioId requeridos\"}");
                    return;
                }

                int encuestaId = Integer.parseInt(encuestaIdStr);
                int usuarioId = Integer.parseInt(usuarioIdStr);

                TokenOTP token = tokenDAO.obtenerPorUsuarioYEncuesta(encuestaId, usuarioId);
                if (token == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"asignado\": false, \"mensaje\": \"El estudiante no tiene token asignado para esta encuesta.\"}");
                    return;
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(token));
                return;
            }

            // GET /api/tokens?encuestaId=1
            String encuestaIdStr = req.getParameter("encuestaId");
            if (encuestaIdStr == null || encuestaIdStr.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Parámetro encuestaId requerido\"}");
                return;
            }

            int encuestaId = Integer.parseInt(encuestaIdStr);
            List<TokenOTP> tokens = tokenDAO.listarPorEncuesta(encuestaId);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(tokens));

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Error al consultar tokens: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

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

            if (json == null || !json.has("encuesta_id")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"El campo 'encuesta_id' es obligatorio.\"}");
                return;
            }

            int encuestaId = json.get("encuesta_id").getAsInt();
            int ttlMinutos = json.has("ttl_minutos") ? json.get("ttl_minutos").getAsInt() : 120; // Default 2 horas

            List<TokenOTP> generados = tokenDAO.generarPadron(encuestaId, ttlMinutos);

            Map<String, Object> respMap = new HashMap<>();
            respMap.put("success", true);
            respMap.put("encuestaId", encuestaId);
            respMap.put("totalGenerados", generados.size());
            respMap.put("tokens", generados);
            respMap.put("mensaje", "Padrón electoral y tokens OTP generados satisfactoriamente");

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(respMap));

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Error en la generación del padrón de tokens: " + e.getMessage() + "\"}");
        }
    }
}
