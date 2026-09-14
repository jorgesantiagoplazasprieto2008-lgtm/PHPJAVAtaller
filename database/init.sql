-- ========================================================================
-- SISTEMA DE VOTACIONES Y ENCUESTAS INSTITUCIONALES SEGURAS (SVIS)
-- Esquema de Base de Datos DDL - Integridad Transaccional y Secreto de Sufragio
-- Instructor: Osman Alonso Aranguren Escobar
-- ========================================================================

CREATE DATABASE IF NOT EXISTS svis_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE svis_db;

-- ------------------------------------------------------------------------
-- 1. TABLA: usuarios
-- Almacena el padrón institucional (Administradores y Estudiantes/Aprendices)
-- ------------------------------------------------------------------------
DROP TABLE IF EXISTS comprobantes_voto;
DROP TABLE IF EXISTS tokens_otp;
DROP TABLE IF EXISTS opciones;
DROP TABLE IF EXISTS encuestas;
DROP TABLE IF EXISTS usuarios;

CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    documento VARCHAR(20) NOT NULL UNIQUE,
    nombre_completo VARCHAR(120) NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE,
    rol ENUM('ADMIN', 'ESTUDIANTE') NOT NULL DEFAULT 'ESTUDIANTE',
    password_hash VARCHAR(255) NOT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------
-- 2. TABLA: encuestas
-- Almacena las consultas democráticas institucionales y su estado
-- ------------------------------------------------------------------------
CREATE TABLE encuestas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT NOT NULL,
    estado ENUM('BORRADOR', 'ACTIVA', 'CERRADA') NOT NULL DEFAULT 'BORRADOR',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cerrado_en TIMESTAMP NULL DEFAULT NULL
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------
-- 3. TABLA: opciones
-- REGLA 4: SECRETO ABSOLUTO DEL VOTO
-- ESTRICTAMENTE PROHIBIDO almacenar usuario_id o token_id aquí.
-- El contador 'votos' solo se incrementa numéricamente (+1).
-- ------------------------------------------------------------------------
CREATE TABLE opciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    encuesta_id INT NOT NULL,
    texto_opcion VARCHAR(200) NOT NULL,
    votos INT UNSIGNED NOT NULL DEFAULT 0,
    CONSTRAINT fk_opciones_encuesta 
        FOREIGN KEY (encuesta_id) REFERENCES encuestas(id) 
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------
-- 4. TABLA: tokens_otp
-- REGLA 1: UNICIDAD EN BASE DE DATOS -> UNIQUE(encuesta_id, usuario_id)
-- REGLA 2: QUEMA ATÓMICA -> estado DISPONIBLE -> USADO
-- ------------------------------------------------------------------------
CREATE TABLE tokens_otp (
    id INT AUTO_INCREMENT PRIMARY KEY,
    encuesta_id INT NOT NULL,
    usuario_id INT NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    estado ENUM('DISPONIBLE', 'USADO') NOT NULL DEFAULT 'DISPONIBLE',
    expira_en DATETIME NOT NULL,
    usado_en DATETIME NULL DEFAULT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tokens_encuesta 
        FOREIGN KEY (encuesta_id) REFERENCES encuestas(id) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tokens_usuario 
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_encuesta_usuario UNIQUE (encuesta_id, usuario_id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------
-- 5. TABLA: comprobantes_voto
-- REGLA 4: COMPROBANTE DIGITAL ANÓNIMO
-- Registra la confirmación del voto sin revelar al votante ni a la opción.
-- Es un resumen criptográfico SHA-256 para validación de auditoría electoral.
-- ------------------------------------------------------------------------
CREATE TABLE comprobantes_voto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    encuesta_id INT NOT NULL,
    recibo_hash CHAR(64) NOT NULL UNIQUE,
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comprobantes_encuesta 
        FOREIGN KEY (encuesta_id) REFERENCES encuestas(id) 
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Índices estratégicos para optimizar el SELECT ... FOR UPDATE en concurrencia masiva
CREATE INDEX idx_tokens_lookup ON tokens_otp(token, encuesta_id, estado);
CREATE INDEX idx_encuestas_estado ON encuestas(estado);
