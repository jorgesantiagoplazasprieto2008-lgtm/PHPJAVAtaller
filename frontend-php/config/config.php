<?php
/**
 * Configuración global del Frontend PHP Desacoplado
 * SVIS - Sistema de Votaciones y Encuestas Institucionales Seguras
 * 
 * NOTA DE ARQUITECTURA:
 * Este cliente web NO contiene credenciales de base de datos MySQL.
 * Toda la comunicación se realiza a través del Backend Java REST vía HTTP cURL.
 */

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

define('API_BASE_URL', 'http://localhost:8080/svis/api');
define('APP_NAME', 'SVIS Institucional');
define('APP_SUBTITLE', 'Sistema de Votaciones y Encuestas Seguras');
