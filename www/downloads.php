<?php
require_once __DIR__ . '/Config/config.php';
require_once __DIR__ . '/includes/functions.php';

$pageTitle = t('downloads');

require __DIR__ . '/includes/header.php';

$downloads = [
    [
        'name'        => t('server'),
        'description' => t('server_desc'),
        'url'         => 'https://github.com/luismk/Pangya-Server-Community',
        'icon'        => 'bi-database-fill',
        'badge'       => 'GitHub',
        'badge_bg'    => 'bg-primary',
    ],
    [
        'name'        => t('tools'),
        'description' => t('tools_desc'),
        'url'         => 'https://github.com/luismk/PangYa-Suite-Tools',
        'icon'        => 'bi-tools',
        'badge'       => 'GitHub',
        'badge_bg'    => 'bg-primary',
    ],
    [
        'name'        => t('client'),
        'description' => t('download_game'),
        'url'         => 'https://www.mediafire.com/file/j33ghvb4lk2bjtn/JP_Building.rar/file',
        'icon'        => 'bi-controller',
        'badge'       => 'MediaFire',
        'badge_bg'    => 'bg-warning text-dark',
    ],
];
?>

<style>
    /* Wrapper para garantir respiro entre conteúdo e footer */
    .downloads-wrapper {
        min-height: calc(100vh - 200px); /* Garante que o footer não suba em telas grandes */
        padding-bottom: 4rem; /* Espaço de sobra antes do footer */
    }

    .download-card {
        transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
        border: 1px solid rgba(255, 255, 255, 0.12);
        background: rgba(20, 24, 33, 0.65);
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
    }
    .download-card:hover {
        transform: translateY(-6px);
        box-shadow: 0 12px 28px rgba(0, 0, 0, 0.5);
        border-color: rgba(255, 255, 255, 0.3);
    }
    .icon-wrapper {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        background: rgba(255, 255, 255, 0.1);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-size: 1.4rem;
    }
    .card-title-text {
        font-size: 1.2rem;
        font-weight: 700;
        line-height: 1.3;
    }
    .card-desc-text {
        font-size: 0.875rem;
        color: rgba(255, 255, 255, 0.65);
        line-height: 1.4;
    }
</style>

<div class="container downloads-wrapper pt-4">
    <!-- Cabeçalho -->
    <div class="text-center mb-5">
        <h1 class="fw-bold text-white mb-2 d-flex align-items-center justify-content-center gap-2">
            <i class="bi bi-download"></i>
            <span><?= htmlspecialchars(t('downloads')) ?></span>
        </h1>
        <p class="text-white-50 fs-6 mb-0">
            Baixe o cliente, servidor e ferramentas necessárias para o projeto Pangya.
        </p>
    </div>

    <!-- Grid de Downloads -->
    <div class="row g-4 justify-content-center">
        <?php foreach ($downloads as $item): ?>
            <div class="col-12 col-md-6 col-lg-4">
                <div class="card download-card h-100 text-light rounded-4 p-4 d-flex flex-column justify-content-between">
                    <div>
                        <!-- Topo: Ícone e Badge -->
                        <div class="d-flex align-items-center justify-content-between mb-3">
                            <div class="icon-wrapper text-info">
                                <i class="bi <?= htmlspecialchars($item['icon']) ?>"></i>
                            </div>
                            <span class="badge rounded-pill <?= htmlspecialchars($item['badge_bg']) ?> px-3 py-2 fw-semibold">
                                <?= htmlspecialchars($item['badge']) ?>
                            </span>
                        </div>

                        <!-- Título e Descrição -->
                        <h3 class="card-title-text text-white mb-2">
                            <?= htmlspecialchars($item['name']) ?>
                        </h3>
                        <p class="card-desc-text mb-4">
                            <?= htmlspecialchars($item['description']) ?>
                        </p>
                    </div>

                    <!-- Botão de Ação -->
                    <a 
                        href="<?= htmlspecialchars($item['url']) ?>" 
                        target="_blank" 
                        rel="noopener noreferrer" 
                        class="btn btn-outline-light w-100 d-inline-flex align-items-center justify-content-center gap-2 rounded-3 py-2 fw-semibold"
                    >
                        <span><?= htmlspecialchars(t('open_project')) ?></span>
                        <i class="bi bi-box-arrow-up-right fs-6"></i>
                    </a>
                </div>
            </div>
        <?php endforeach; ?>
    </div>
</div>

<?php require __DIR__ . '/includes/footer.php'; ?>
