<?php
require_once __DIR__ . '/../config/config.php';

function requireRole($requiredRole) {
    if (!isset($_SESSION['rol'])) {
        header('Location: /login.php');
        exit;
    }

    if ($_SESSION['rol'] !== $requiredRole) {
        if ($_SESSION['rol'] === 'ADMIN') {
            header('Location: /admin/dashboard.php');
        } else {
            header('Location: /votar.php');
        }
        exit;
    }
}

function requireAuth() {
    if (!isset($_SESSION['rol'])) {
        header('Location: /login.php');
        exit;
    }
}
