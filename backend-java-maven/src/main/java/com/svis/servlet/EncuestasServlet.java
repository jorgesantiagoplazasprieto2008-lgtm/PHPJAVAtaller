package com.svis.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.svis.dao.EncuestaDAO;
import com.svis.model.Encuesta;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/encuestas/*")
public class EncuestasServlet extends HttpServlet {

    private final EncuestaDAO encuestaDAO = new EncuestaDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String pathInfo = req.getPathInfo(); // e.g. /activas, /todas, /{id}/resultados, /resultados

        try {
            if (pathInfo == null || "/".equals(pathInfo) || "/activas".equalsIgnoreCase(pathInfo)) {
                // GET /api/encuestas/activas
                List<Encuesta> activas = encuestaDAO.listarActivas();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(activas));

            } else if ("/todas".equalsIgnoreCase(pathInfo)) {
                // GET /api/encuestas/todas
                List<Encuesta> todas = encuestaDAO.listarTodas();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(todas));

            } else if (pathInfo.endsWith("/resultados") || "/resultados".equalsIgnoreCase(pathInfo)) {
                // GET /api/encuestas/{id}/resultados o GET /api/encuestas/resultados?id={id}
                int encuestaId = -1;
                String idParam = req.getParameter("id");

                if (idParam != null && !idParam.isEmpty()) {
                    encuestaId = Integer.parseInt(idParam);
                } else {
                    String[] parts = pathInfo.split("/");
                    if (parts.length >= 2 && !parts[1].isEmpty() && !"resultados".equalsIgnoreCase(parts[1])) {
                        encuestaId = Integer.parseInt(parts[1]);
                    }
                }

                if (encuestaId <= 0) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\": \"Identificador de encuesta inválido\"}");
                    return;
                }

                Map<String, Object> resultados = encuestaDAO.obtenerResultadosConsolidados(encuestaId);
                if (resultados == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\": \"Encuesta no encontrada\"}");
                    return;
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(resultados));

            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Ruta no encontrada\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Error interno al procesar encuestas: " + e.getMessage());
            resp.getWriter().write(gson.toJson(err));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String pathInfo = req.getPathInfo();

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

            if (pathInfo != null && pathInfo.contains("/estado")) {
                // POST /api/encuestas/estado
                int id = json.get("id").getAsInt();
                String nuevoEstado = json.get("estado").getAsString().toUpperCase();

                boolean ok = encuestaDAO.cambiarEstado(id, nuevoEstado);
                if (ok) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{\"success\": true, \"mensaje\": \"Estado actualizado a " + nuevoEstado + "\"}");
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\": \"No se pudo actualizar el estado de la encuesta.\"}");
                }
                return;
            }

            // POST /api/encuestas -> Crear encuesta con sus opciones
            if (json == null || !json.has("titulo") || !json.has("opciones")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Debe proporcionar título, descripción y opciones.\"}");
                return;
            }

            String titulo = json.get("titulo").getAsString();
            String descripcion = json.has("descripcion") ? json.get("descripcion").getAsString() : "";
            JsonArray opcionesArray = json.getAsJsonArray("opciones");

            List<String> opciones = new ArrayList<>();
            for (int i = 0; i < opcionesArray.size(); i++) {
                String op = opcionesArray.get(i).getAsString();
                if (!op.trim().isEmpty()) {
                    opciones.add(op.trim());
                }
            }

            if (opciones.size() < 2) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Una encuesta institucional debe tener al menos dos opciones de sufragio.\"}");
                return;
            }

            int nuevaId = encuestaDAO.crearEncuesta(titulo, descripcion, opciones);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            Map<String, Object> respMap = new HashMap<>();
            respMap.put("success", true);
            respMap.put("id", nuevaId);
            respMap.put("mensaje", "Encuesta y opciones registradas exitosamente");
            resp.getWriter().write(gson.toJson(respMap));

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Error al crear la encuesta: " + e.getMessage());
            resp.getWriter().write(gson.toJson(err));
        }
    }
}
