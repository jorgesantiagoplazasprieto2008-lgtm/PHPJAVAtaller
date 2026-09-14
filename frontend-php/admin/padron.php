<?php
require_once __DIR__ . '/../includes/auth_check.php';
requireRole('ADMIN');
require_once __DIR__ . '/../services/ApiService.php';

$encuestaId = (int)($_GET['id'] ?? 0);
if ($encuestaId <= 0) {
    header('Location: /admin/dashboard.php');
    exit;
}

$respTokens = ApiService::getTokensEncuesta($encuestaId);
$tokens = $respTokens['success'] ? ($respTokens['data'] ?? []) : [];

$pageTitle = "Padrón Electoral y Tokens OTP - " . APP_NAME;
include __DIR__ . '/../includes/header.php';
?>

<main class="container">

    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem;">
        <div>
            <h2>Padrón Electoral y Credenciales OTP</h2>
            <p style="color: var(--text-muted); font-size: 0.9rem;">
                Encuesta institucional #<?= $encuestaId ?> &bull; Tokens únicos de un solo uso por estudiante registrado
            </p>
        </div>
        <div style="display: flex; gap: 0.5rem;">
            <a href="/admin/dashboard.php" class="btn btn-outline btn-sm">⬅ Volver al Panel</a>
            <a href="/admin/dashboard.php?generar_otps=1&id=<?= $encuestaId ?>" class="btn btn-primary btn-sm">⚡ Regenerar / Asignar Faltantes</a>
        </div>
    </div>

    <!-- Resumen de Estado del Padrón -->
    <?php
    $total = count($tokens);
    $usados = 0;
    $disponibles = 0;
    foreach ($tokens as $t) {
        if (($t['estado'] ?? '') === 'USADO') $usados++;
        else $disponibles++;
    }
    ?>

    <div class="stat-grid" style="grid-template-columns: repeat(3, 1fr);">
        <div class="stat-card">
            <div class="stat-icon primary">👥</div>
            <div>
                <div class="stat-value"><?= $total ?></div>
                <div class="stat-label">Aprendices en Padrón</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon success">🔑</div>
            <div>
                <div class="stat-value"><?= $disponibles ?></div>
                <div class="stat-label">Tokens Disponibles</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon warning">🔥</div>
            <div>
                <div class="stat-value"><?= $usados ?></div>
                <div class="stat-label">Tokens Quemados (Usados)</div>
            </div>
        </div>
    </div>

    <div class="card">
        <div class="card-header">
            <h3>Listado de Credenciales OTP por Aprendiz</h3>
            <span style="font-size: 0.8rem; color: var(--text-muted);">Cumplimiento REGLA 1: UNIQUE(encuesta_id, usuario_id)</span>
        </div>

        <?php if (empty($tokens)): ?>
            <div style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                <p style="font-size: 2.5rem; margin-bottom: 0.5rem;">🔒</p>
                <p>No se han generado tokens OTP para esta encuesta aún.</p>
                <p style="margin-top: 0.5rem;">
                    <a href="/admin/dashboard.php?generar_otps=1&id=<?= $encuestaId ?>" class="btn btn-primary btn-sm">
                        Generar Padrón de Tokens Ahora
                    </a>
                </p>
            </div>
        <?php else: ?>
            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Documento</th>
                            <th>Aprendiz / Estudiante</th>
                            <th>Token OTP Asignado</th>
                            <th>Estado Token</th>
                            <th>Expira En</th>
                            <th style="text-align: right;">Acción Rápida</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach ($tokens as $t): 
                            $esUsado = ($t['estado'] ?? '') === 'USADO';
                        ?>
                            <tr>
                                <td class="font-mono"><?= htmlspecialchars($t['usuarioDocumento'] ?? '') ?></td>
                                <td>
                                    <strong><?= htmlspecialchars($t['usuarioNombre'] ?? '') ?></strong>
                                </td>
                                <td>
                                    <span class="font-mono" style="font-size: 1.05rem; font-weight: 700; color: <?= $esUsado ? 'var(--text-dim)' : 'var(--primary)' ?>; background: var(--bg-card); padding: 0.25rem 0.6rem; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);">
                                        <?= htmlspecialchars($t['token'] ?? '') ?>
                                    </span>
                                </td>
                                <td>
                                    <?php if ($esUsado): ?>
                                        <span class="badge badge-cerrada">🔥 USADO</span>
                                    <?php else: ?>
                                        <span class="badge badge-activa">✓ DISPONIBLE</span>
                                    <?php endif; ?>
                                </td>
                                <td style="font-size: 0.85rem; color: var(--text-muted);">
                                    <?= htmlspecialchars($t['expiraEn'] ?? '') ?>
                                </td>
                                <td style="text-align: right;">
                                    <?php if (!$esUsado): ?>
                                        <button type="button" class="btn btn-outline btn-sm btn-copy" data-clipboard="<?= htmlspecialchars($t['token'] ?? '') ?>" title="Copiar token para probar voto">
                                            📋 Copiar Token
                                        </button>
                                    <?php else: ?>
                                        <span style="font-size: 0.8rem; color: var(--text-dim);">Sufragio ejercido</span>
                                    <?php endif; ?>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
        <?php endif; ?>
    </div>

</main>

<?php include __DIR__ . '/../includes/footer.php'; ?>
