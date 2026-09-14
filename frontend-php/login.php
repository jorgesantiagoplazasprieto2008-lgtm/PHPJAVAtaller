<?php
require_once __DIR__ . '/config/config.php';
require_once __DIR__ . '/services/ApiService.php';

// Si ya está logueado, redirigir
if (isset($_SESSION['rol'])) {
    if ($_SESSION['rol'] === 'ADMIN') {
        header('Location: /admin/dashboard.php');
    } else {
        header('Location: /votar.php');
    }
    exit;
}

$error = null;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $documento = trim($_POST['documento'] ?? '');
    $password = trim($_POST['password'] ?? '');

    if (empty($documento) || empty($password)) {
        $error = 'Por favor ingrese su número de documento y contraseña.';
    } else {
        $resp = ApiService::login($documento, $password);
        if ($resp['success'] && isset($resp['data']['id'])) {
            $_SESSION['usuario_id'] = $resp['data']['id'];
            $_SESSION['documento'] = $resp['data']['documento'];
            $_SESSION['nombre'] = $resp['data']['nombre'];
            $_SESSION['rol'] = $resp['data']['rol'];
            $_SESSION['correo'] = $resp['data']['correo'];

            if ($_SESSION['rol'] === 'ADMIN') {
                header('Location: /admin/dashboard.php');
            } else {
                header('Location: /votar.php');
            }
            exit;
        } else {
            $error = $resp['data']['error'] ?? $resp['error'] ?? 'Credenciales no válidas. Verifique sus datos.';
        }
    }
}

$pageTitle = "Iniciar Sesión - " . APP_NAME;
include __DIR__ . '/includes/header.php';
?>

<main class="container" style="max-width: 520px; margin-top: 3.5rem;">
    <div class="card">
        <div style="text-align: center; margin-bottom: 2rem;">
            <div style="font-size: 3rem; margin-bottom: 0.5rem;">🔐</div>
            <h2>Acceso al Sistema Electoral</h2>
            <p style="color: var(--text-muted); font-size: 0.9rem; margin-top: 0.25rem;">
                Ingrese con sus credenciales institucionales registradas en el padrón
            </p>
        </div>

        <?php if ($error): ?>
            <div class="alert alert-danger">
                <span>⚠️</span>
                <div><?= htmlspecialchars($error) ?></div>
            </div>
        <?php endif; ?>

        <form method="POST" action="/login.php">
            <div class="form-group">
                <label for="documento">Documento de Identidad</label>
                <input type="text" id="documento" name="documento" class="form-control" placeholder="Ej: 1001" required autofocus value="<?= htmlspecialchars($_POST['documento'] ?? '') ?>">
            </div>

            <div class="form-group">
                <label for="password">Contraseña Institucional</label>
                <input type="password" id="password" name="password" class="form-control" placeholder="••••••••" required>
            </div>

            <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 1rem; padding: 0.85rem;">
                Ingresar a la Plataforma ➔
            </button>
        </form>

        <div style="margin-top: 2rem; padding-top: 1.5rem; border-top: 1px solid var(--border-subtle);">
            <p style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.75rem; text-align: center; font-weight: 600;">
                Acceso Rápido para Pruebas del Taller:
            </p>
            <div style="display: flex; gap: 0.5rem; flex-wrap: wrap; justify-content: center;">
                <button type="button" class="btn btn-outline btn-sm" onclick="setDemo('1001', 'admin123')">
                    👑 Admin (1001)
                </button>
                <button type="button" class="btn btn-outline btn-sm" onclick="setDemo('1002', 'aprendiz123')">
                    🎓 Aprendiz Carlos (1002)
                </button>
                <button type="button" class="btn btn-outline btn-sm" onclick="setDemo('1003', 'aprendiz123')">
                    🎓 Aprendiz Valentina (1003)
                </button>
            </div>
        </div>
    </div>
</main>

<script>
function setDemo(doc, pass) {
    document.getElementById('documento').value = doc;
    document.getElementById('password').value = pass;
}
</script>

<?php include __DIR__ . '/includes/footer.php'; ?>
