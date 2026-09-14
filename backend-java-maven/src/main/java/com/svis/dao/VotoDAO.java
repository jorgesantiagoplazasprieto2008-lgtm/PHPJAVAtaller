package com.svis.dao;

import com.svis.config.DatabaseConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class VotoDAO {

    public static class VotoResult {
        public enum Status {
            SUCCESS,
            TOKEN_NOT_FOUND,
            TOKEN_ALREADY_USED,
            TOKEN_EXPIRED,
            SURVEY_NOT_ACTIVE,
            OPTION_NOT_FOUND,
            DATABASE_ERROR
        }

        private final Status status;
        private final String receiptHash;
        private final String message;

        public VotoResult(Status status, String receiptHash, String message) {
            this.status = status;
            this.receiptHash = receiptHash;
            this.message = message;
        }

        public Status getStatus() { return status; }
        public String getReceiptHash() { return receiptHash; }
        public String getMessage() { return message; }
    }

    /**
     * Procesa la emisión de un voto aplicando:
     * - REGLA 2: Quema atómica del OTP en la misma transacción ACID.
     * - REGLA 3: Aislamiento contra condiciones de carrera con SELECT ... FOR UPDATE (bloqueo pesimista).
     * - REGLA 4: Secreto absoluto del sufragio (el voto no se vincula a usuario ni token).
     */
    public VotoResult emitirVoto(int encuestaId, int opcionId, String tokenString) {
        Connection conn = null;
        PreparedStatement stmtCheckToken = null;
        PreparedStatement stmtCheckEncuesta = null;
        PreparedStatement stmtBurnToken = null;
        PreparedStatement stmtIncrementVote = null;
        PreparedStatement stmtReceipt = null;
        ResultSet rsToken = null;
        ResultSet rsEncuesta = null;

        try {
            conn = DatabaseConfig.getConnection();
            // Iniciar transacción explícita
            conn.setAutoCommit(false);

            // 1. BLOQUEO PESIMISTA DEL TOKEN (SELECT ... FOR UPDATE)
            // Si dos peticiones simultáneas compiten por el mismo token, una obtiene el bloqueo primero.
            // La segunda espera en el FOR UPDATE y cuando lo lee, su estado ya es 'USADO', retornando conflicto 409.
            String sqlToken = "SELECT id, estado, (expira_en < NOW()) AS expirado FROM tokens_otp WHERE token = ? AND encuesta_id = ? FOR UPDATE";
            stmtCheckToken = conn.prepareStatement(sqlToken);
            stmtCheckToken.setString(1, tokenString.trim());
            stmtCheckToken.setInt(2, encuestaId);
            rsToken = stmtCheckToken.executeQuery();

            if (!rsToken.next()) {
                conn.rollback();
                return new VotoResult(VotoResult.Status.TOKEN_NOT_FOUND, null, "El token proporcionado no existe o no corresponde a esta encuesta.");
            }

            int tokenId = rsToken.getInt("id");
            String estadoToken = rsToken.getString("estado");
            boolean expirado = rsToken.getInt("expirado") == 1;

            // REGLA 2 & 3: Si ya está usado, abortar inmediatamente con Rollback
            if ("USADO".equalsIgnoreCase(estadoToken)) {
                conn.rollback();
                return new VotoResult(VotoResult.Status.TOKEN_ALREADY_USED, null, "Conflicto: El token OTP ya fue utilizado.");
            }

            // Validar TTL de expiración
            if (expirado) {
                conn.rollback();
                return new VotoResult(VotoResult.Status.TOKEN_EXPIRED, null, "El token OTP ha expirado.");
            }

            // 2. Verificar que la encuesta esté en estado ACTIVA
            String sqlEncuesta = "SELECT estado FROM encuestas WHERE id = ? FOR UPDATE";
            stmtCheckEncuesta = conn.prepareStatement(sqlEncuesta);
            stmtCheckEncuesta.setInt(1, encuestaId);
            rsEncuesta = stmtCheckEncuesta.executeQuery();

            if (!rsEncuesta.next() || !"ACTIVA".equalsIgnoreCase(rsEncuesta.getString("estado"))) {
                conn.rollback();
                return new VotoResult(VotoResult.Status.SURVEY_NOT_ACTIVE, null, "La encuesta no está en estado ACTIVA para admitir votos.");
            }

            // 3. REGLA 2: Quema Atómica del Token (DISPONIBLE -> USADO)
            String sqlBurn = "UPDATE tokens_otp SET estado = 'USADO', usado_en = NOW() WHERE id = ? AND estado = 'DISPONIBLE'";
            stmtBurnToken = conn.prepareStatement(sqlBurn);
            stmtBurnToken.setInt(1, tokenId);
            int rowsBurned = stmtBurnToken.executeUpdate();

            if (rowsBurned == 0) {
                // Si otra transacción concurrente modificó la fila en el ínterin
                conn.rollback();
                return new VotoResult(VotoResult.Status.TOKEN_ALREADY_USED, null, "Conflicto: El token OTP ya fue quemado concurrentemente.");
            }

            // 4. REGLA 4: Incremento numérico anónimo en la opción (+1)
            // Se garantiza que NUNCA se asocia usuario_id ni token_id con la opción.
            String sqlIncrement = "UPDATE opciones SET votos = votos + 1 WHERE id = ? AND encuesta_id = ?";
            stmtIncrementVote = conn.prepareStatement(sqlIncrement);
            stmtIncrementVote.setInt(1, opcionId);
            stmtIncrementVote.setInt(2, encuestaId);
            int rowsIncremented = stmtIncrementVote.executeUpdate();

            if (rowsIncremented == 0) {
                conn.rollback();
                return new VotoResult(VotoResult.Status.OPTION_NOT_FOUND, null, "La opción de votación seleccionada no pertenece a la encuesta.");
            }

            // 5. Generación de Comprobante Digital Anónimo (Hash SHA-256)
            String rawReceipt = UUID.randomUUID().toString() + "-" + encuestaId + "-" + System.currentTimeMillis();
            String receiptHash = generarHashSHA256(rawReceipt);

            String sqlReceipt = "INSERT INTO comprobantes_voto (encuesta_id, recibo_hash, fecha_emision) VALUES (?, ?, NOW())";
            stmtReceipt = conn.prepareStatement(sqlReceipt);
            stmtReceipt.setInt(1, encuestaId);
            stmtReceipt.setString(2, receiptHash);
            stmtReceipt.executeUpdate();

            // 6. Confirmación definitiva de la transacción ACID
            conn.commit();

            return new VotoResult(VotoResult.Status.SUCCESS, receiptHash, "Sufragio procesado exitosamente con garantía de anonimato.");

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return new VotoResult(VotoResult.Status.DATABASE_ERROR, null, "Error transaccional en base de datos: " + e.getMessage());
        } finally {
            closeQuietly(rsToken);
            closeQuietly(rsEncuesta);
            closeQuietly(stmtCheckToken);
            closeQuietly(stmtCheckEncuesta);
            closeQuietly(stmtBurnToken);
            closeQuietly(stmtIncrementVote);
            closeQuietly(stmtReceipt);
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    private static String generarHashSHA256(String input) {
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
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private static void closeQuietly(AutoCloseable res) {
        if (res != null) {
            try { res.close(); } catch (Exception ignored) {}
        }
    }
}
