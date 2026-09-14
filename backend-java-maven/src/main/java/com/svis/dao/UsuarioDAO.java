package com.svis.dao;

import com.svis.config.DatabaseConfig;
import com.svis.model.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public Usuario autenticar(String documento, String passwordPlana) throws SQLException {
        String hashInput = hashSHA256(passwordPlana);
        String sql = "SELECT id, documento, nombre_completo, correo, rol FROM usuarios " +
                     "WHERE documento = ? AND password_hash = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, documento.trim());
            stmt.setString(2, hashInput);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                        rs.getInt("id"),
                        rs.getString("documento"),
                        rs.getString("nombre_completo"),
                        rs.getString("correo"),
                        rs.getString("rol")
                    );
                }
            }
        }
        return null;
    }

    public List<Usuario> listarEstudiantes() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id, documento, nombre_completo, correo, rol FROM usuarios WHERE rol = 'ESTUDIANTE' ORDER BY nombre_completo ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Usuario(
                    rs.getInt("id"),
                    rs.getString("documento"),
                    rs.getString("nombre_completo"),
                    rs.getString("correo"),
                    rs.getString("rol")
                ));
            }
        }
        return lista;
    }

    private static String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando hash de credencial", e);
        }
    }
}
