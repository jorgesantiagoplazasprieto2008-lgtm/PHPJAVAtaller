package com.svis.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.svis.dao.VotoDAO;
import com.svis.dao.VotoDAO.VotoResult;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/votos/*")
public class VotosServlet extends HttpServlet {

    private final VotoDAO votoDAO = new VotoDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String pathInfo = req.getPathInfo();

        // Acepta tanto POST /api/votos/emitir como POST /api/votos
        if (pathInfo != null && !pathInfo.equals("/") && !pathInfo.equalsIgnoreCase("/emitir")) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Endpoint de votación no encontrado\"}");
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

            if (json == null || !json.has("encuesta_id") || !json.has("opcion_id") || !json.has("token")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Campos requeridos: encuesta_id, opcion_id, token\"}");
                return;
            }

            int encuestaId = json.get("encuesta_id").getAsInt();
            int opcionId = json.get("opcion_id").getAsInt();
            String token = json.get("token").getAsString();

            // Ejecución transaccional con aislamiento y quema atómica
            VotoResult result = votoDAO.emitirVoto(encuestaId, opcionId, token);

            Map<String, Object> respMap = new HashMap<>();

            switch (result.getStatus()) {
                case SUCCESS:
                    // 200 OK: Voto procesado y comprobante generado
                    resp.setStatus(HttpServletResponse.SC_OK);
                    respMap.put("success", true);
                    respMap.put("comprobante", result.getReceiptHash());
                    respMap.put("mensaje", result.getMessage());
                    break;

                case TOKEN_ALREADY_USED:
                    // 409 Conflict: REGLA 3 (Aislamiento contra colisiones/doble voto concurrente)
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    respMap.put("success", false);
                    respMap.put("error", "Conflicto: El token OTP ya ha sido utilizado previamente.");
                    respMap.put("codigo", 409);
                    break;

                case TOKEN_EXPIRED:
                    // 410 Gone: El token caducó según su TTL
                    resp.setStatus(HttpServletResponse.SC_GONE);
                    respMap.put("success", false);
                    respMap.put("error", result.getMessage());
                    respMap.put("codigo", 410);
                    break;

                case SURVEY_NOT_ACTIVE:
                    // 403 Forbidden: La encuesta está CERRADA o en BORRADOR
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    respMap.put("success", false);
                    respMap.put("error", result.getMessage());
                    respMap.put("codigo", 403);
                    break;

                case TOKEN_NOT_FOUND:
                case OPTION_NOT_FOUND:
                    // 404 Not Found: Credencial o candidato inválido
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    respMap.put("success", false);
                    respMap.put("error", result.getMessage());
                    respMap.put("codigo", 404);
                    break;

                case DATABASE_ERROR:
                default:
                    // 500 Internal Server Error
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    respMap.put("success", false);
                    respMap.put("error", result.getMessage());
                    respMap.put("codigo", 500);
                    break;
            }

            resp.getWriter().write(gson.toJson(respMap));

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Error inesperado al emitir voto: " + e.getMessage());
            resp.getWriter().write(gson.toJson(err));
        }
    }
}
