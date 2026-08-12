<?php

require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';
require_once __DIR__ . '/ServerMetrics.php';

$metrics = [
    'registered' => 0,
    'online' => 0,
    'login_online' => false,
    'game_online' => false,
    'pang_rate' => 0,
    'exp_rate' => 0,
    'peak_online' => 0,
];

try {
    $metrics = (new ServerMetrics(getConnection()))->snapshot();
} catch (PDOException $exception) {
    error_log('Página de status do servidor: ' . $exception->getMessage());
}

$pageTitle = t('server_page');
require __DIR__ . '/../includes/header.php';
?>

<h2 class="mb-4"><?= htmlspecialchars(t('server_page')) ?></h2>

<div class="row g-3">
    <div class="col-md-4">
        <div class="card p-4 h-100">
            <div class="text-secondary"><?= htmlspecialchars(t('players_online')) ?></div>
            <div class="display-6 text-success"><?= (int) $metrics['online'] ?></div>
            <div class="small text-secondary">
                <?= htmlspecialchars(t('peak_online')) ?>: <?= (int) $metrics['peak_online'] ?>
            </div>
        </div>
    </div>

    <div class="col-md-4">
        <div class="card p-4 h-100">
            <div class="text-secondary mb-2"><?= htmlspecialchars(t('service_status')) ?></div>
            <p class="mb-1">
                <?= htmlspecialchars(t('login_server')) ?>:
                <span class="badge bg-<?= $metrics['login_online'] ? 'success' : 'danger' ?>">
                    <?= htmlspecialchars($metrics['login_online'] ? t('online') : t('offline')) ?>
                </span>
            </p>
            <p class="mb-0">
                <?= htmlspecialchars(t('game_server')) ?>:
                <span class="badge bg-<?= $metrics['game_online'] ? 'success' : 'danger' ?>">
                    <?= htmlspecialchars($metrics['game_online'] ? t('online') : t('offline')) ?>
                </span>
            </p>
        </div>
    </div>

    <div class="col-md-4">
        <div class="card p-4 h-100">
            <div class="text-secondary mb-2"><?= htmlspecialchars(t('active_rates')) ?></div>
            <div>Pang: <?= htmlspecialchars((string) $metrics['pang_rate']) ?>%</div>
            <div>EXP: <?= htmlspecialchars((string) $metrics['exp_rate']) ?>%</div>
        </div>
    </div>
</div>

<?php require __DIR__ . '/../includes/footer.php'; ?>
