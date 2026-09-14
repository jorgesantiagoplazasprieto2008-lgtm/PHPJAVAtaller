<?php
require_once __DIR__ . '/../config/config.php';
$isLoggedIn = isset($_SESSION['rol']);
$rol = $_SESSION['rol'] ?? null;
$nombre = $_SESSION['nombre'] ?? '';
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= htmlspecialchars($pageTitle ?? APP_NAME) ?></title>
    <link rel="stylesheet" href="/assets/css/style.css">
    <link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🗳️</text></svg>">
</head>
<body>

<header class="navbar">
    <div class="navbar-container">
        <a href="<?= $rol === 'ADMIN' ? '/admin/dashboard.php' : ($rol === 'ESTUDIANTE' ? '/votar.php' : '/login.php') ?>" class="brand">
            <div class="brand-icon">🗳️</div>
            <div class="brand-text">
                <h1><?= APP_NAME ?></h1>
                <p><?= APP_SUBTITLE ?></p>
            </div>
        </a>

        <div class="nav-links">
            <?php if ($isLoggedIn): ?>
                <?php if ($rol === 'ADMIN'): ?>
                    <a href="/admin/dashboard.php" class="btn btn-outline btn-sm">Panel Control</a>
                <?php else: ?>
                    <a href="/votar.php" class="btn btn-outline btn-sm">Mis Encuestas</a>
                <?php endif; ?>

                <div class="nav-user">
                    <span class="user-badge <?= strtolower($rol) ?>"><?= $rol ?></span>
                    <span style="font-size: 0.85rem; font-weight: 600;"><?= htmlspecialchars($nombre) ?></span>
                </div>

                <a href="/logout.php" class="btn btn-danger btn-sm" title="Cerrar sesión">Salir 🚪</a>
            <?php else: ?>
                <a href="/login.php" class="btn btn-primary btn-sm">Iniciar Sesión</a>
            <?php endif; ?>
        </div>
    </div>
</header>
