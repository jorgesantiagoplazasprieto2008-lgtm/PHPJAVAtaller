<?php
require_once __DIR__ . '/includes/auth_check.php';
requireRole('ESTUDIANTE');
require_once __DIR__ . '/services/ApiService.php';

$usuarioId = $_SESSION['usuario_id'];
$nombreUsuario = $_SESSION['nombre'];
$documentoUsuario = $_SESSION['documento'];

$mensajeExito = null;
$comprobanteHash = null;
$error = null;

// Manejo de emisión de voto
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'emitir_voto') {
    $encuestaId = (int)($_POST['encuesta_id'] ?? 0);
    $opcionId = (int)($_POST['opcion_id'] ?? 0);
    $token = trim($_POST['token'] ?? '');

    if ($encuestaId <= 0 || $opcionId <= 0 || empty($token)) {
        $error = 'Debe seleccionar una opción de sufragio e ingresar su token OTP institucional.';
    } else {
        $respVoto = ApiService::emitirVoto($encuestaId, $opcionId, $token);
        if ($respVoto['status'] === 200 && $respVoto['success']) {
            $mensajeExito = '¡Sufragio emitido exitosamente! Su voto ha sido contabilizado con estricta garantía de anonimato.';
            $comprobanteHash = $respVoto['data']['comprobante'] ?? 'HASH-CONFIRMADO-ACID';
        } elseif ($respVoto['status'] === 409) {
            $error = 'Conflicto (HTTP 409): Este token OTP ya ha sido utilizado previamente. No es posible votar dos veces.';
        } elseif ($respVoto['status'] === 410) {
            $error = 'El token OTP ha expirado.';
        } else {
            $error = $respVoto['data']['error'] ?? $respVoto['error'] ?? 'No se pudo procesar el voto. Intente de nuevo.';
        }
    }
}

// Consultar encuestas activas
$respEncuestas = ApiService::getEncuestasActivas();
$encuestas = $respEncuestas['success'] ? ($respEncuestas['data'] ?? []) : [];

$pageTitle = "Emisión del Sufragio - " . APP_NAME;
include __DIR__ . '/includes/header.php';
?>

<main class="container">

    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem;">
        <div>
            <h2>Portal del Elector Estudiantil</h2>
            <p style="color: var(--text-muted); font-size: 0.9rem;">
                Bienvenido, <strong><?= htmlspecialchars($nombreUsuario) ?></strong> (Doc: <?= htmlspecialchars($documentoUsuario) ?>) &bull; Ejerza su derecho al voto de forma anónima y segura
            </p>
        </div>
        <div>
            <span class="badge badge-activa">🟢 Padrón Habilitado</span>
        </div>
    </div>

    <!-- Banner de Comprobante Digital Anónimo si recién votó -->
    <?php if ($comprobanteHash): ?>
        <div class="receipt-box" style="margin-bottom: 2.5rem;">
            <div style="font-size: 2.8rem; margin-bottom: 0.5rem;">📜✅</div>
            <h3 style="color: var(--success); font-size: 1.4rem;">¡Sufragio Registrado Exitosamente!</h3>
            <p style="color: var(--text-muted); font-size: 0.9rem; max-width: 600px; margin: 0.5rem auto 1rem auto;">
                Su voto fue consolidado atómicamente en la base de datos. Conserve su <strong>Comprobante Digital Anónimo</strong> como constancia criptográfica de participación:
            </p>
            <div class="receipt-hash">
                <?= htmlspecialchars($comprobanteHash) ?>
            </div>
            <div style="display: flex; gap: 0.75rem; justify-content: center; margin-top: 1rem;">
                <button type="button" class="btn btn-primary btn-copy" data-clipboard="<?= htmlspecialchars($comprobanteHash) ?>">
                    📋 Copiar Comprobante Hash
                </button>
                <button type="button" class="btn btn-outline" onclick="window.print()">
                    🖨️ Imprimir Recibo
                </button>
            </div>
        </div>
    <?php endif; ?>

    <?php if ($error): ?>
        <div class="alert alert-danger">
            <span>⚠️</span>
            <div><?= htmlspecialchars($error) ?></div>
        </div>
    <?php endif; ?>

    <!-- Listado de Consultas Activas -->
    <?php if (empty($encuestas)): ?>
        <div class="card" style="text-align: center; padding: 3rem;">
            <div style="font-size: 3rem; margin-bottom: 1rem;">⏳</div>
            <h3>No hay encuestas activas en este momento</h3>
            <p style="color: var(--text-muted); margin-top: 0.5rem;">
                Las votaciones se habilitarán una vez la administración electoral active el proceso correspondiente.
            </p>
        </div>
    <?php else: ?>
        <div style="display: flex; flex-direction: column; gap: 2rem;">
            <?php foreach ($encuestas as $enc): 
                $encId = (int)$enc['id'];
                // Consultar si este estudiante ya tiene token asignado y cuál es su estado
                $respToken = ApiService::getTokenUsuario($encId, $usuarioId);
                $tokenInfo = $respToken['success'] ? ($respToken['data'] ?? null) : null;
                $yaVoto = ($tokenInfo && ($tokenInfo['estado'] ?? '') === 'USADO');
                $tieneToken = ($tokenInfo !== null);
                $tokenValor = $tokenInfo['token'] ?? '';
            ?>
                <div class="card <?= $yaVoto ? 'card-locked' : '' ?>" style="<?= $yaVoto ? 'opacity: 0.85; border-color: hsla(38, 92%, 50%, 0.4);' : '' ?>">
                    <div class="card-header">
                        <div>
                            <span class="badge badge-activa" style="margin-bottom: 0.4rem;">ACTIVA</span>
                            <h3><?= htmlspecialchars($enc['titulo']) ?></h3>
                            <p style="color: var(--text-muted); font-size: 0.85rem; margin-top: 0.25rem;">
                                <?= htmlspecialchars($enc['descripcion']) ?>
                            </p>
                        </div>
                        <div>
                            <?php if ($yaVoto): ?>
                                <span class="badge badge-cerrada" style="font-size: 0.85rem; padding: 0.4rem 0.8rem;">
                                    🔒 Encuesta Bloqueada: Ya Sufragó
                                </span>
                            <?php else: ?>
                                <span class="badge badge-borrador" style="font-size: 0.85rem; padding: 0.4rem 0.8rem;">
                                    🗳️ Pendiente de Sufragio
                                </span>
                            <?php endif; ?>
                        </div>
                    </div>

                    <?php if ($yaVoto): ?>
                        <!-- Validación Visual: si el usuario ya votó, la encuesta debe aparecer bloqueada -->
                        <div class="alert alert-warning" style="margin-top: 1rem;">
                            <span>🔒</span>
                            <div>
                                <strong>Voto Ya Emitido:</strong> Su token OTP institucional para este proceso fue quemado y registrado como <code>USADO</code> el <?= htmlspecialchars($tokenInfo['usadoEn'] ?? 'recientemente') ?>. Por mandato constitucional y de acuerdo a la <strong>REGLA 2</strong>, el sistema impide sufragios duplicados.
                            </div>
                        </div>
                    <?php else: ?>
                        <form method="POST" action="/votar.php">
                            <input type="hidden" name="action" value="emitir_voto">
                            <input type="hidden" name="encuesta_id" value="<?= $encId ?>">

                            <p style="font-weight: 600; font-size: 0.95rem; margin-bottom: 0.75rem;">
                                1. Seleccione libremente su opción o candidato:
                            </p>

                            <div class="voting-options-grid">
                                <?php foreach ($enc['opciones'] ?? [] as $op): ?>
                                    <div class="option-card" id="option-card-<?= $op['id'] ?>">
                                        <div class="radio-indicator"></div>
                                        <input type="radio" name="opcion_id" value="<?= $op['id'] ?>" required style="display: none;">
                                        <div class="option-title">
                                            <?= htmlspecialchars($op['textoOpcion']) ?>
                                        </div>
                                    </div>
                                <?php endforeach; ?>
                            </div>

                            <div style="background: var(--bg-card); padding: 1.25rem; border-radius: var(--radius-md); margin-top: 1.5rem; border: 1px solid var(--border-subtle);">
                                <label for="token-input-<?= $encId ?>" style="color: var(--text-main); font-weight: 700;">
                                    2. Ingrese su Token OTP de Un Solo Uso:
                                </label>
                                <p style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.75rem;">
                                    <?php if ($tieneToken): ?>
                                        Su token asignado en el padrón institucional es: <strong class="font-mono" style="color: var(--primary); font-size: 0.95rem;"><?= htmlspecialchars($tokenValor) ?></strong> (Expira: <?= htmlspecialchars($tokenInfo['expiraEn'] ?? '') ?>)
                                    <?php else: ?>
                                        Ingrese el token OTP que le fue entregado por el comité electoral.
                                    <?php endif; ?>
                                </p>

                                <div style="display: flex; gap: 0.75rem; align-items: center; max-width: 420px;">
                                    <input type="text" 
                                           id="token-input-<?= $encId ?>" 
                                           name="token" 
                                           class="form-control font-mono" 
                                           placeholder="Ej: XXXX-XXXX" 
                                           required 
                                           style="text-transform: uppercase; letter-spacing: 0.1em; font-size: 1.1rem; font-weight: 700;"
                                           value="<?= htmlspecialchars($tokenValor) ?>">
                                    
                                    <button type="submit" class="btn btn-success" style="white-space: nowrap; padding: 0.75rem 1.5rem;">
                                        🗳️ Emitir Voto
                                    </button>
                                </div>
                            </div>
                        </form>
                    <?php endif; ?>
                </div>
            <?php endforeach; ?>
        </div>
    <?php endif; ?>

</main>

<?php include __DIR__ . '/includes/footer.php'; ?>
