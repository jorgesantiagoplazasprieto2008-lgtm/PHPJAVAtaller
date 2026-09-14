<?php
require_once __DIR__ . '/../includes/auth_check.php';
requireRole('ADMIN');
require_once __DIR__ . '/../services/ApiService.php';

$encuestaId = (int)($_GET['id'] ?? 0);
if ($encuestaId <= 0) {
    header('Location: /admin/dashboard.php');
    exit;
}

$resp = ApiService::getResultados($encuestaId);
$data = $resp['success'] ? ($resp['data'] ?? null) : null;

$pageTitle = "Escrutinio y Resultados - " . APP_NAME;
include __DIR__ . '/../includes/header.php';
?>

<main class="container">

    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem;">
        <div>
            <h2>Resultados del Escrutinio Electoral</h2>
            <p style="color: var(--text-muted); font-size: 0.9rem;">
                Consolidado oficial en tiempo real &bull; Encuesta #<?= $encuestaId ?>: <?= htmlspecialchars($data['titulo'] ?? '') ?>
            </p>
        </div>
        <div style="display: flex; gap: 0.5rem;">
            <button onclick="window.location.reload();" class="btn btn-outline btn-sm">🔄 Actualizar Escrutinio</button>
            <a href="/admin/dashboard.php" class="btn btn-primary btn-sm">⬅ Volver al Panel</a>
        </div>
    </div>

    <?php if (!$data): ?>
        <div class="alert alert-danger">
            No se pudo obtener los resultados de la encuesta solicitada.
        </div>
    <?php else: 
        $totalVotos = $data['totalVotos'] ?? 0;
        $totalTokens = $data['totalTokens'] ?? 0;
        $tokensUsados = $data['tokensUsados'] ?? 0;
        $tasaParticipacion = $data['tasaParticipacion'] ?? 0.0;
        $estado = $data['estado'] ?? 'ACTIVA';
    ?>

        <!-- Métricas del Escrutinio -->
        <div class="stat-grid">
            <div class="stat-card">
                <div class="stat-icon primary">🗳️</div>
                <div>
                    <div class="stat-value"><?= $totalVotos ?></div>
                    <div class="stat-label">Votos Escrutados</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon success">📊</div>
                <div>
                    <div class="stat-value"><?= $tasaParticipacion ?>%</div>
                    <div class="stat-label">Tasa de Participación</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon warning">🔑</div>
                <div>
                    <div class="stat-value"><?= $tokensUsados ?> / <?= $totalTokens ?></div>
                    <div class="stat-label">OTPs Quemados / Total</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon info">
                    <?= $estado === 'ACTIVA' ? '🟢' : '⏹' ?>
                </div>
                <div>
                    <div class="stat-value"><?= $estado ?></div>
                    <div class="stat-label">Estado de la Consulta</div>
                </div>
            </div>
        </div>

        <!-- Barras de Distribución de Porcentajes -->
        <div class="card" style="margin-bottom: 2rem;">
            <div class="card-header">
                <h3>Distribución y Porcentajes por Opción</h3>
                <span style="font-size: 0.85rem; color: var(--text-muted);">Garantía de Secreto: Voto numérico desacoplado de identidad</span>
            </div>

            <div style="padding: 1rem 0;">
                <?php foreach ($data['opciones'] ?? [] as $op): 
                    $pct = $op['porcentaje'] ?? 0.0;
                    $votos = $op['votos'] ?? 0;
                ?>
                    <div class="result-row">
                        <div class="result-header">
                            <div>
                                <span style="font-size: 1.05rem;"><?= htmlspecialchars($op['textoOpcion'] ?? '') ?></span>
                            </div>
                            <div style="display: flex; gap: 1rem; align-items: baseline;">
                                <span class="font-mono" style="color: var(--text-muted); font-size: 0.9rem;"><?= $votos ?> votos</span>
                                <span class="font-mono" style="font-size: 1.15rem; font-weight: 800; color: var(--primary);"><?= $pct ?>%</span>
                            </div>
                        </div>
                        <div class="progress-track">
                            <div class="progress-fill" style="width: <?= $pct ?>%;"></div>
                        </div>
                    </div>
                <?php endforeach; ?>
            </div>
        </div>

        <div class="alert alert-info">
            <span>🛡️</span>
            <div>
                <strong>Auditoría Electoral de Integridad:</strong> De conformidad con la <strong>REGLA 4 (Secreto Absoluto del Voto)</strong>, la base de datos MySQL no almacena ninguna correlación entre el estudiante o el token con la opción sufragada. El contador de cada opción se incrementa exclusivamente de forma acumulativa numérica.
            </div>
        </div>

    <?php endif; ?>

</main>

<?php include __DIR__ . '/../includes/footer.php'; ?>
