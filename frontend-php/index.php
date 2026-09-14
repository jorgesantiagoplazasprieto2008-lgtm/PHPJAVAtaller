<?php
require_once __DIR__ . '/config/config.php';
if (isset($_SESSION['rol'])) {
    if ($_SESSION['rol'] === 'ADMIN') {
        header('Location: /admin/dashboard.php');
    } else {
        header('Location: /votar.php');
    }
    exit;
}
header('Location: /login.php');
exit;
