package com.svis.dao;

import com.svis.config.DatabaseConfig;
import com.svis.model.TokenOTP;

import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TokenDAO {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Genera un token alfanumérico seguro de 8 caracteres en formato XXXX-XXXX
     */
    public static String generarCodigoOTP() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 4; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        sb.append("-");
        for (int i = 0; i < 4; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * Genera masivamente tokens OTP únicos para todos los estudiantes registrados
     * REGLA 1: La restricción UNIQUE(encuesta_id, usuario_id) protege contra duplicados
     */
    public List<TokenOTP> generarPadron(int encuestaId, int ttlMinutos) throws SQLException {
        List<TokenOTP> generados = new ArrayList<>();

        String sqlEstudiantes = "SELECT id, documento, nombre_completo FROM usuarios WHERE rol = 'ESTUDIANTE'";
        String sqlInsert = "INSERT INTO tokens_otp (encuesta_id, usuario_id, token, estado, expira_en) " +
                           "VALUES (?, ?, ?, 'DISPONIBLE', DATE_ADD(NOW(), INTERVAL ? MINUTE))";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmtEstudiantes = conn.prepareStatement(sqlEstudiantes);
             ResultSet rsEstudiantes = stmtEstudiantes.executeQuery()) {

            conn.setAutoCommit(false);

            try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                while (rsEstudiantes.next()) {
                    int usuarioId = rsEstudiantes.getInt("id");
                    String documento = rsEstudiantes.getString("documento");
                    String nombre = rsEstudiantes.getString("nombre_completo");

                    // Generar token único
                    String token = generarCodigoOTP();

                    stmtInsert.setInt(1, encuestaId);
                    stmtInsert.setInt(2, usuarioId);
                    stmtInsert.setString(3, token);
                    stmtInsert.setInt(4, ttlMinutos);

                    try {
                        stmtInsert.executeUpdate();

                        TokenOTP t = new TokenOTP();
                        t.setEncuestaId(encuestaId);
                        t.setUsuarioId(usuarioId);
                        t.setToken(token);
                        t.setEstado("DISPONIBLE");
                        t.setUsuarioDocumento(documento);
                        t.setUsuarioNombre(nombre);
                        generados.add(t);
                    } catch (SQLIntegrityConstraintViolationException ex) {
                        // REGLA 1: Si ya existe un token para este estudiante en esta encuesta, se omite (idempotencia)
                        System.out.println("Usuario " + usuarioId + " ya cuenta con token para encuesta " + encuestaId);
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        return generados;
    }

    /**
     * Retorna todos los tokens generados para una encuesta con los datos del aprendiz
     */
    public List<TokenOTP> listarPorEncuesta(int encuestaId) throws SQLException {
        List<TokenOTP> lista = new ArrayList<>();
        String sql = "SELECT t.id, t.encuesta_id, t.usuario_id, t.token, t.estado, t.expira_en, t.usado_en, t.creado_en, " +
                     "u.nombre_completo, u.documento " +
                     "FROM tokens_otp t " +
                     "JOIN usuarios u ON t.usuario_id = u.id " +
                     "WHERE t.encuesta_id = ? " +
                     "ORDER BY u.nombre_completo ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, encuestaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TokenOTP t = new TokenOTP();
                    t.setId(rs.getInt("id"));
                    t.setEncuestaId(rs.getInt("encuesta_id"));
                    t.setUsuarioId(rs.getInt("usuario_id"));
                    t.setToken(rs.getString("token"));
                    t.setEstado(rs.getString("estado"));
                    t.setExpiraEn(rs.getTimestamp("expira_en"));
                    t.setUsadoEn(rs.getTimestamp("usado_en"));
                    t.setCreadoEn(rs.getTimestamp("creado_en"));
                    t.setUsuarioNombre(rs.getString("nombre_completo"));
                    t.setUsuarioDocumento(rs.getString("documento"));
                    lista.add(t);
                }
            }
        }
        return lista;
    }

    /**
     * Obtiene el token de un usuario particular para una encuesta específica
     */
    public TokenOTP obtenerPorUsuarioYEncuesta(int encuestaId, int usuarioId) throws SQLException {
        String sql = "SELECT id, encuesta_id, usuario_id, token, estado, expira_en, usado_en, creado_en " +
                     "FROM tokens_otp WHERE encuesta_id = ? AND usuario_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, encuestaId);
            stmt.setInt(2, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    TokenOTP t = new TokenOTP();
                    t.setId(rs.getInt("id"));
                    t.setEncuestaId(rs.getInt("encuesta_id"));
                    t.setUsuarioId(rs.getInt("usuario_id"));
                    t.setToken(rs.getString("token"));
                    t.setEstado(rs.getString("estado"));
                    t.setExpiraEn(rs.getTimestamp("expira_en"));
                    t.setUsadoEn(rs.getTimestamp("usado_en"));
                    t.setCreadoEn(rs.getTimestamp("creado_en"));
                    return t;
                }
            }
        }
        return null;
    }
}
