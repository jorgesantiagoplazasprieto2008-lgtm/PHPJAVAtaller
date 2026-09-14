<?php
require_once __DIR__ . '/../includes/auth_check.php';
requireRole('ADMIN');
require_once __DIR__ . '/../services/ApiService.php';

$mensaje = null;
$error = null;

// Manejo de creación de encuesta
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'crear_encuesta') {
    $titulo = trim($_POST['titulo'] ?? '');
    $descripcion = trim($_POST['descripcion'] ?? '');
    $opciones = $_POST['opciones'] ?? [];

    $opcionesFiltradas = array_filter(array_map('trim', $opciones));

    if (empty($titulo) || count($opcionesFiltradas) < 2) {
        $error = 'Debe indicar un título y al menos dos opciones para la encuesta.';
    } else {
        $res = ApiService::crearEncuesta($titulo, $descripcion, array_values($opcionesFiltradas));
        if ($res['success']) {
            $mensaje = 'Encuesta institucional creada exitosamente en estado BORRADOR.';
        } else {
            $error = $res['data']['error'] ?? $res['error'] ?? 'Error al crear la encuesta.';
        }
    }
}

// Manejo de cambio de estado (Abrir / Cerrar)
if (isset($_GET['cambiar_estado'], $_GET['id'])) {
    $id = (int)$_GET['id'];
    $nuevoEstado = strtoupper($_GET['cambiar_estado']);
    if (in_array($nuevoEstado, ['ACTIVA', 'CERRADA', 'BORRADOR'])) {
        $res = ApiService::cambiarEstadoEncuesta($id, $nuevoEstado);
        if ($res['success']) {
            $mensaje = "Estado de la encuesta #$id actualizado a $nuevoEstado.";
        } else {
            $error = $res['data']['error'] ?? 'No se pudo cambiar el estado.';
        }
    }
}

// Manejo de disparo de generación masiva de OTPs
if (isset($_GET['generar_otps'], $_GET['id'])) {
    $id = (int)$_GET['id'];
    $res = ApiService::generarTokens($id, 180);
    if ($res['success']) {
        $total = $res['data']['totalGenerados'] ?? 0;
        $mensaje = "Padrón generado exitosamente: $total nuevos tokens OTP asignados a los aprendices.";
    } else {
        $error = $res['data']['error'] ?? 'Error al generar padrón de tokens.';
    }
}

// Consultar todas las encuestas
$respEncuestas = ApiService::getTodasEncuestas();
$encuestas = $respEncuestas['success'] ? ($respEncuestas['data'] ?? []) : [];

// Métricas globales
$totalEncuestas = count($encuestas);
$totalActivas = 0;
$totalVotosGlobal = 0;

foreach ($encuestas as $enc) {
    if (($enc['estado'] ?? '') === 'ACTIVA') $totalActivas++;
    if (!empty($enc['opciones'])) {
        foreach ($enc['opciones'] as $op) {
            $totalVotosGlobal += ($op['votos'] ?? 0);
        }
    }
}

$pageTitle = "Panel de Gestión Electoral - " . APP_NAME;
include __DIR__ . '/../includes/header.php';
?>

<main class="container">

    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem;">
        <div>
            <h2>Panel de Gestión Electoral</h2>
            <p style="color: var(--text-muted); font-size: 0.9rem;">
                Administración de procesos democráticos, padrón de tokens OTP y monitoreo de escrutinio
            </p>
        </div>
        <a href="#modal-crear" class="btn btn-primary" onclick="document.getElementById('form-crear-card').scrollIntoView({behavior: 'smooth'})">
            + Nueva Encuesta
        </a>
    </div>

    <?php if ($mensaje): ?>
        <div class="alert alert-success">
            <span>✓</span>
            <div><?= htmlspecialchars($mensaje) ?></div>
        </div>
    <?php endif; ?>

    <?php if ($error): ?>
        <div class="alert alert-danger">
            <span>⚠️</span>
            <div><?= htmlspecialchars($error) ?></div>
        </div>
    <?php endif; ?>

    <!-- Tarjetas de Estadísticas Globales -->
    <div class="stat-grid">
        <div class="stat-card">
            <div class="stat-icon primary">📋</div>
            <div>
                <div class="stat-value"><?= $totalEncuestas ?></div>
                <div class="stat-label">Encuestas Creadas</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon success">🟢</div>
            <div>
                <div class="stat-value"><?= $totalActivas ?></div>
                <div class="stat-label">Votaciones Activas</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon warning">🗳️</div>
            <div>
                <div class="stat-value"><?= $totalVotosGlobal ?></div>
                <div class="stat-label">Total Votos Emitidos</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon info">🛡️</div>
            <div>
                <div class="stat-value">ACID</div>
                <div class="stat-label">Integridad de Sufragio</div>
            </div>
        </div>
    </div>

    <!-- Lista de Encuestas Registradas -->
    <div class="card" style="margin-bottom: 2.5rem;">
        <div class="card-header">
            <h3>Procesos Electorales Registrados</h3>
            <span style="font-size: 0.85rem; color: var(--text-muted);">Gestionar Estados y Padrón</span>
        </div>

        <?php if (empty($encuestas)): ?>
            <div style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                <p style="font-size: 2.5rem; margin-bottom: 0.5rem;">📭</p>
                <p>No se han registrado encuestas todavía. Utilice el formulario inferior para crear la primera.</p>
            </div>
        <?php else: ?>
            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Título de la Encuesta</th>
                            <th>Opciones</th>
                            <th>Estado</th>
                            <th>Votos Totales</th>
                            <th style="text-align: right;">Acciones Electorales</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach ($encuestas as $enc): 
                            $votosEncuesta = 0;
                            if (!empty($enc['opciones'])) {
                                foreach ($enc['opciones'] as $op) $votosEncuesta += ($op['votos'] ?? 0);
                            }
                            $estado = $enc['estado'] ?? 'BORRADOR';
                        ?>
                            <tr>
                                <td class="font-mono">#<?= $enc['id'] ?></td>
                                <td>
                                    <strong><?= htmlspecialchars($enc['titulo']) ?></strong>
                                    <div style="font-size: 0.8rem; color: var(--text-muted); max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                                        <?= htmlspecialchars($enc['descripcion']) ?>
                                    </div>
                                </td>
                                <td><?= count($enc['opciones'] ?? []) ?> opciones</td>
                                <td>
                                    <span class="badge badge-<?= strtolower($estado) ?>">
                                        ● <?= $estado ?>
                                    </span>
                                </td>
                                <td class="font-mono" style="font-weight: 700; color: var(--info);">
                                    <?= $votosEncuesta ?>
                                </td>
                                <td style="text-align: right;">
                                    <div style="display: inline-flex; gap: 0.4rem; flex-wrap: wrap; justify-content: flex-end;">
                                        <!-- Control de Apertura y Cierre (Regla de Negocio 2.1) -->
                                        <?php if ($estado !== 'ACTIVA'): ?>
                                            <a href="/admin/dashboard.php?cambiar_estado=ACTIVA&id=<?= $enc['id'] ?>" class="btn btn-success btn-sm" title="Abrir votación para que admita votos">
                                                ▶ Abrir
                                            </a>
                                        <?php endif; ?>

                                        <?php if ($estado === 'ACTIVA'): ?>
                                            <a href="/admin/dashboard.php?cambiar_estado=CERRADA&id=<?= $enc['id'] ?>" class="btn btn-danger btn-sm" title="Cerrar votación">
                                                ⏹ Cerrar
                                            </a>
                                        <?php endif; ?>

                                        <!-- Generación del Padrón y Tokens OTP -->
                                        <a href="/admin/dashboard.php?generar_otps=1&id=<?= $enc['id'] ?>" class="btn btn-primary btn-sm" title="Generar masivamente tokens OTP para cada estudiante">
                                            🔑 Generar OTPs
                                        </a>

                                        <!-- Padrón de Votantes -->
                                        <a href="/admin/padron.php?id=<?= $enc['id'] ?>" class="btn btn-outline btn-sm" title="Ver lista de tokens generados">
                                            👥 Padrón
                                        </a>

                                        <!-- Monitoreo de Resultados en Tiempo Real -->
                                        <a href="/admin/resultados.php?id=<?= $enc['id'] ?>" class="btn btn-outline btn-sm" style="border-color: var(--primary); color: var(--primary);" title="Visualizar escrutinio y porcentajes">
                                            📊 Resultados
                                        </a>
                                    </div>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
        <?php endif; ?>
    </div>

    <!-- Formulario para Crear Encuesta Dinámica -->
    <div class="card" id="form-crear-card">
        <div class="card-header">
            <h3>Parametrizar Nueva Encuesta / Votación</h3>
            <span style="font-size: 0.85rem; color: var(--text-muted);">Definir título, descripción y opciones</span>
        </div>

        <form method="POST" action="/admin/dashboard.php">
            <input type="hidden" name="action" value="crear_encuesta">

            <div class="form-group">
                <label for="titulo">Título de la Consulta Democrática *</label>
                <input type="text" id="titulo" name="titulo" class="form-control" placeholder="Ej: Elección del Vocero Estudiantil ADSO Ficha 271234" required>
            </div>

            <div class="form-group">
                <label for="descripcion">Descripción Institucional</label>
                <textarea id="descripcion" name="descripcion" class="form-control" placeholder="Explique el propósito de la votación, las fechas límite y reglas..."></textarea>
            </div>

            <div style="margin-top: 1.5rem; margin-bottom: 1rem; display: flex; justify-content: space-between; align-items: center;">
                <label style="margin-bottom: 0;">Opciones de Sufragio * (Mínimo 2 opciones)</label>
                <button type="button" id="btn-add-option" class="btn btn-outline btn-sm">+ Agregar Otra Opción</button>
            </div>

            <div id="options-container">
                <div class="form-group option-input-row" style="display: flex; gap: 0.5rem;">
                    <input type="text" name="opciones[]" class="form-control" placeholder="Opción 1: Candidato o Postura A" required>
                    <button type="button" class="btn btn-danger btn-sm btn-remove-option" title="Eliminar opción">✕</button>
                </div>
                <div class="form-group option-input-row" style="display: flex; gap: 0.5rem;">
                    <input type="text" name="opciones[]" class="form-control" placeholder="Opción 2: Candidato o Postura B" required>
                    <button type="button" class="btn btn-danger btn-sm btn-remove-option" title="Eliminar opción">✕</button>
                </div>
                <div class="form-group option-input-row" style="display: flex; gap: 0.5rem;">
                    <input type="text" name="opciones[]" class="form-control" placeholder="Opción 3: Voto en Blanco" required>
                    <button type="button" class="btn btn-danger btn-sm btn-remove-option" title="Eliminar opción">✕</button>
                </div>
            </div>

            <button type="submit" class="btn btn-primary" style="margin-top: 1rem;">
                🚀 Registrar y Publicar Parámetros de Encuesta
            </button>
        </form>
    </div>

</main>

<?php include __DIR__ . '/../includes/footer.php'; ?>
