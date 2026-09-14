package com.svis.dao;

import com.svis.config.DatabaseConfig;
import com.svis.model.Encuesta;
import com.svis.model.Opcion;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EncuestaDAO {

    public int crearEncuesta(String titulo, String descripcion, List<String> opcionesTexto) throws SQLException {
        String sqlEncuesta = "INSERT INTO encuestas (titulo, descripcion, estado) VALUES (?, ?, 'BORRADOR')";
        String sqlOpcion = "INSERT INTO opciones (encuesta_id, texto_opcion, votos) VALUES (?, ?, 0)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            int encuestaId = -1;

            try (PreparedStatement stmtEnc = conn.prepareStatement(sqlEncuesta, Statement.RETURN_GENERATED_KEYS)) {
                stmtEnc.setString(1, titulo);
                stmtEnc.setString(2, descripcion);
                stmtEnc.executeUpdate();

                try (ResultSet rsKeys = stmtEnc.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        encuestaId = rsKeys.getInt(1);
                    }
                }
            }

            if (encuestaId <= 0) {
                conn.rollback();
                throw new SQLException("No se pudo obtener el identificador de la encuesta generada.");
            }

            try (PreparedStatement stmtOp = conn.prepareStatement(sqlOpcion)) {
                for (String texto : opcionesTexto) {
                    if (texto != null && !texto.trim().isEmpty()) {
                        stmtOp.setInt(1, encuestaId);
                        stmtOp.setString(2, texto.trim());
                        stmtOp.addBatch();
                    }
                }
                stmtOp.executeBatch();
            }

            conn.commit();
            return encuestaId;
        } catch (SQLException e) {
            throw e;
        }
    }

    public List<Encuesta> listarActivas() throws SQLException {
        List<Encuesta> lista = new ArrayList<>();
        String sql = "SELECT id, titulo, descripcion, estado, creado_en FROM encuestas WHERE estado = 'ACTIVA' ORDER BY id DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Encuesta enc = new Encuesta(
                    rs.getInt("id"),
                    rs.getString("titulo"),
                    rs.getString("descripcion"),
                    rs.getString("estado")
                );
                enc.setCreadoEn(rs.getTimestamp("creado_en"));
                enc.setOpciones(obtenerOpciones(conn, enc.getId(), false)); // false: no revelar votos
                lista.add(enc);
            }
        }
        return lista;
    }

    public List<Encuesta> listarTodas() throws SQLException {
        List<Encuesta> lista = new ArrayList<>();
        String sql = "SELECT id, titulo, descripcion, estado, creado_en, cerrado_en FROM encuestas ORDER BY id DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Encuesta enc = new Encuesta(
                    rs.getInt("id"),
                    rs.getString("titulo"),
                    rs.getString("descripcion"),
                    rs.getString("estado")
                );
                enc.setCreadoEn(rs.getTimestamp("creado_en"));
                enc.setCerradoEn(rs.getTimestamp("cerrado_en"));
                enc.setOpciones(obtenerOpciones(conn, enc.getId(), true)); // true: incluir conteo para admin
                lista.add(enc);
            }
        }
        return lista;
    }

    public boolean cambiarEstado(int encuestaId, String nuevoEstado) throws SQLException {
        String sql = "UPDATE encuestas SET estado = ?, cerrado_en = (CASE WHEN ? = 'CERRADA' THEN NOW() ELSE NULL END) WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado);
            stmt.setString(2, nuevoEstado);
            stmt.setInt(3, encuestaId);
            return stmt.executeUpdate() > 0;
        }
    }

    public Encuesta obtenerPorId(int encuestaId) throws SQLException {
        String sql = "SELECT id, titulo, descripcion, estado, creado_en, cerrado_en FROM encuestas WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, encuestaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Encuesta enc = new Encuesta(
                        rs.getInt("id"),
                        rs.getString("titulo"),
                        rs.getString("descripcion"),
                        rs.getString("estado")
                    );
                    enc.setCreadoEn(rs.getTimestamp("creado_en"));
                    enc.setCerradoEn(rs.getTimestamp("cerrado_en"));
                    enc.setOpciones(obtenerOpciones(conn, enc.getId(), true));
                    return enc;
                }
            }
        }
        return null;
    }

    private List<Opcion> obtenerOpciones(Connection conn, int encuestaId, boolean incluirVotos) throws SQLException {
        List<Opcion> opciones = new ArrayList<>();
        String sql = "SELECT id, encuesta_id, texto_opcion, votos FROM opciones WHERE encuesta_id = ? ORDER BY id ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, encuestaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int votos = incluirVotos ? rs.getInt("votos") : 0;
                    opciones.add(new Opcion(
                        rs.getInt("id"),
                        rs.getInt("encuesta_id"),
                        rs.getString("texto_opcion"),
                        votos
                    ));
                }
            }
        }
        return opciones;
    }

    /**
     * Consolida los resultados electorales calculando porcentajes y métricas de participación
     */
    public Map<String, Object> obtenerResultadosConsolidados(int encuestaId) throws SQLException {
        Encuesta enc = obtenerPorId(encuestaId);
        if (enc == null) return null;

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("id", enc.getId());
        resultado.put("titulo", enc.getTitulo());
        resultado.put("descripcion", enc.getDescripcion());
        resultado.put("estado", enc.getEstado());
        resultado.put("creadoEn", enc.getCreadoEn());
        resultado.put("cerradoEn", enc.getCerradoEn());

        int totalVotos = 0;
        for (Opcion op : enc.getOpciones()) {
            totalVotos += op.getVotos();
        }
        resultado.put("totalVotos", totalVotos);

        List<Map<String, Object>> opcionesConPorcentaje = new ArrayList<>();
        for (Opcion op : enc.getOpciones()) {
            Map<String, Object> mapOp = new HashMap<>();
            mapOp.put("id", op.getId());
            mapOp.put("textoOpcion", op.getTextoOpcion());
            mapOp.put("votos", op.getVotos());
            double porcentaje = totalVotos > 0 ? ((double) op.getVotos() / totalVotos) * 100.0 : 0.0;
            mapOp.put("porcentaje", Math.round(porcentaje * 100.0) / 100.0);
            opcionesConPorcentaje.add(mapOp);
        }
        resultado.put("opciones", opcionesConPorcentaje);

        // Métricas de tokens generados vs usados (Participación)
        String sqlTokens = "SELECT " +
                           "COUNT(*) AS total_tokens, " +
                           "SUM(CASE WHEN estado = 'USADO' THEN 1 ELSE 0 END) AS tokens_usados, " +
                           "SUM(CASE WHEN estado = 'DISPONIBLE' THEN 1 ELSE 0 END) AS tokens_disponibles " +
                           "FROM tokens_otp WHERE encuesta_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlTokens)) {
            stmt.setInt(1, encuestaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalTokens = rs.getInt("total_tokens");
                    int tokensUsados = rs.getInt("tokens_usados");
                    int tokensDisponibles = rs.getInt("tokens_disponibles");
                    resultado.put("totalTokens", totalTokens);
                    resultado.put("tokensUsados", tokensUsados);
                    resultado.put("tokensDisponibles", tokensDisponibles);
                    double participacion = totalTokens > 0 ? ((double) tokensUsados / totalTokens) * 100.0 : 0.0;
                    resultado.put("tasaParticipacion", Math.round(participacion * 100.0) / 100.0);
                }
            }
        }

        return resultado;
    }
}
