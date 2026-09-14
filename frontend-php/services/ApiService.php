<?php
require_once __DIR__ . '/../config/config.php';

class ApiService {

    private static function request($method, $endpoint, $data = null) {
        $url = rtrim(API_BASE_URL, '/') . '/' . ltrim($endpoint, '/');
        $ch = curl_init();

        $headers = [
            'Accept: application/json',
            'Content-Type: application/json; charset=utf-8'
        ];

        if ($method === 'GET' && !empty($data)) {
            $url .= '?' . http_build_query($data);
        }

        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

        if ($method === 'POST') {
            $payload = is_array($data) ? json_encode($data, JSON_UNESCAPED_UNICODE) : $data;
            curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
        }

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        curl_close($ch);

        if ($response === false) {
            return [
                'status' => 0,
                'success' => false,
                'error' => 'Error de conexión con el Backend Java REST: ' . $curlError,
                'data' => null
            ];
        }

        $decoded = json_decode($response, true);

        return [
            'status' => $httpCode,
            'success' => ($httpCode >= 200 && $httpCode < 300),
            'data' => $decoded,
            'raw' => $response
        ];
    }

    public static function get($endpoint, $params = []) {
        return self::request('GET', $endpoint, $params);
    }

    public static function post($endpoint, $data = []) {
        return self::request('POST', $endpoint, $data);
    }

    // Métodos de dominio
    public static function login($documento, $password) {
        return self::post('auth/login', [
            'documento' => $documento,
            'password' => $password
        ]);
    }

    public static function getEncuestasActivas() {
        return self::get('encuestas/activas');
    }

    public static function getTodasEncuestas() {
        return self::get('encuestas/todas');
    }

    public static function crearEncuesta($titulo, $descripcion, $opciones) {
        return self::post('encuestas', [
            'titulo' => $titulo,
            'descripcion' => $descripcion,
            'opciones' => $opciones
        ]);
    }

    public static function cambiarEstadoEncuesta($id, $estado) {
        return self::post('encuestas/estado', [
            'id' => (int)$id,
            'estado' => $estado
        ]);
    }

    public static function generarTokens($encuestaId, $ttlMinutos = 180) {
        return self::post('tokens/generar', [
            'encuesta_id' => (int)$encuestaId,
            'ttl_minutos' => (int)$ttlMinutos
        ]);
    }

    public static function getTokensEncuesta($encuestaId) {
        return self::get('tokens', ['encuestaId' => $encuestaId]);
    }

    public static function getTokenUsuario($encuestaId, $usuarioId) {
        return self::get('tokens/usuario', [
            'encuestaId' => $encuestaId,
            'usuarioId' => $usuarioId
        ]);
    }

    public static function emitirVoto($encuestaId, $opcionId, $token) {
        return self::post('votos/emitir', [
            'encuesta_id' => (int)$encuestaId,
            'opcion_id' => (int)$opcionId,
            'token' => trim($token)
        ]);
    }

    public static function getResultados($encuestaId) {
        return self::get('encuestas/' . (int)$encuestaId . '/resultados');
    }
}
