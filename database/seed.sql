-- ========================================================================
-- SISTEMA DE VOTACIONES Y ENCUESTAS INSTITUCIONALES SEGURAS (SVIS)
-- Datos Semilla (Padrón Electoral Inicial)
-- ========================================================================

USE svis_db;

-- Limpieza de usuarios previos
DELETE FROM usuarios;

-- Inserción de Administrador Institucional y Estudiantes / Aprendices de Prueba
-- Las contraseñas se almacenan como hashes SHA-256 para compatibilidad limpia entre capas
-- 'admin123' -> SHA256: 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
-- 'aprendiz123' -> SHA256: b2d31670e3fed631a4bf9e11db220b13aafb34b4ad450772c5bd6bfb5d99b5cf
INSERT INTO usuarios (id, documento, nombre_completo, correo, rol, password_hash) VALUES
(1, '1001', 'Administrador Electoral ADSO', 'admin@institucion.edu.co', 'ADMIN', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'),
(2, '1002', 'Carlos Mario Restrepo', 'carlos.restrepo@institucion.edu.co', 'ESTUDIANTE', 'b2d31670e3fed631a4bf9e11db220b13aafb34b4ad450772c5bd6bfb5d99b5cf'),
(3, '1003', 'Valentina Gómez Peña', 'valentina.gomez@institucion.edu.co', 'ESTUDIANTE', 'b2d31670e3fed631a4bf9e11db220b13aafb34b4ad450772c5bd6bfb5d99b5cf'),
(4, '1004', 'Andrés Felipe Morales', 'andres.morales@institucion.edu.co', 'ESTUDIANTE', 'b2d31670e3fed631a4bf9e11db220b13aafb34b4ad450772c5bd6bfb5d99b5cf'),
(5, '1005', 'Daniela Sofía Vargas', 'daniela.vargas@institucion.edu.co', 'ESTUDIANTE', 'b2d31670e3fed631a4bf9e11db220b13aafb34b4ad450772c5bd6bfb5d99b5cf'),
(6, '1006', 'Juan Camilo Quintero', 'juan.quintero@institucion.edu.co', 'ESTUDIANTE', 'b2d31670e3fed631a4bf9e11db220b13aafb34b4ad450772c5bd6bfb5d99b5cf');
