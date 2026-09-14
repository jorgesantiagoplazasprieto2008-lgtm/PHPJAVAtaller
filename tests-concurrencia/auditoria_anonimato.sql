-- ========================================================================
-- SISTEMA DE VOTACIONES Y ENCUESTAS INSTITUCIONALES SEGURAS (SVIS)
-- Evidencia 5.2: Prueba de Anonimato en Base de Datos (Auditoría Electoral)
-- Instructor: Osman Alonso Aranguren Escobar
-- ========================================================================

USE svis_db;

-- ------------------------------------------------------------------------
-- PRUEBA 1: INSPECCIÓN DE COLUMNAS EN LA TABLA DE OPCIONES DE VOTO
-- Demuestra que la tabla 'opciones' NO posee columnas para usuario_id ni token_id.
-- Solo existe el acumulador numérico 'votos'.
-- ------------------------------------------------------------------------
SELECT 
    COLUMN_NAME AS Columna, 
    DATA_TYPE AS TipoDato, 
    IS_NULLABLE AS PermiteNulo,
    COLUMN_COMMENT AS Comentario
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'svis_db' AND TABLE_NAME = 'opciones';

-- ------------------------------------------------------------------------
-- PRUEBA 2: INSPECCIÓN DE LA TABLA DE TOKENS OTP
-- Demuestra que la tabla 'tokens_otp' NO posee ninguna columna que relacione
-- al votante con la opción de candidato por la cual sufragó.
-- ------------------------------------------------------------------------
SELECT 
    COLUMN_NAME AS Columna, 
    DATA_TYPE AS TipoDato
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'svis_db' AND TABLE_NAME = 'tokens_otp';

-- ------------------------------------------------------------------------
-- PRUEBA 3: INSPECCIÓN DE LA TABLA DE COMPROBANTES DE VOTO
-- Demuestra que los comprobantes digitales solo registran un resumen hash anónimo
-- SHA-256 y fecha de emisión, sin llaves foráneas a usuarios ni a opciones.
-- ------------------------------------------------------------------------
SELECT 
    COLUMN_NAME AS Columna, 
    DATA_TYPE AS TipoDato
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'svis_db' AND TABLE_NAME = 'comprobantes_voto';

-- ------------------------------------------------------------------------
-- PRUEBA 4: BALANCE Y CONCILIACIÓN MATEMÁTICA ELECTORAL
-- Verifica la inalterabilidad:
-- Total de tokens en estado 'USADO' == Sumatoria de votos en opciones == Total comprobantes
-- ------------------------------------------------------------------------
SELECT 
    e.id AS EncuestaId,
    e.titulo AS TituloEncuesta,
    (SELECT COUNT(*) FROM tokens_otp t WHERE t.encuesta_id = e.id AND t.estado = 'USADO') AS TokensQuemados_Usados,
    (SELECT SUM(o.votos) FROM opciones o WHERE o.encuesta_id = e.id) AS TotalVotosSumadosEnOpciones,
    (SELECT COUNT(*) FROM comprobantes_voto c WHERE c.encuesta_id = e.id) AS TotalComprobantesEmitidos,
    CASE 
        WHEN (SELECT COUNT(*) FROM tokens_otp t WHERE t.encuesta_id = e.id AND t.estado = 'USADO') =
             (SELECT SUM(o.votos) FROM opciones o WHERE o.encuesta_id = e.id) AND
             (SELECT SUM(o.votos) FROM opciones o WHERE o.encuesta_id = e.id) =
             (SELECT COUNT(*) FROM comprobantes_voto c WHERE c.encuesta_id = e.id)
        THEN '✓ CONCILIACIÓN MATEMÁTICA PERFECTA (100% AUDITABLE Y ANÓNIMO)'
        ELSE '✗ DISCREPANCIA DETECTADA'
    END AS DiagnosticoIntegridad
FROM encuestas e
GROUP BY e.id;

-- ------------------------------------------------------------------------
-- PRUEBA 5: REPORTE DE OPCIONES Y CONTEO ACUMULADO NUMÉRICO
-- ------------------------------------------------------------------------
SELECT 
    o.id AS OpcionId,
    o.encuesta_id AS EncuestaId,
    o.texto_opcion AS Candidato_Opcion,
    o.votos AS VotosAcumulados
FROM opciones o
ORDER BY o.encuesta_id, o.id;
