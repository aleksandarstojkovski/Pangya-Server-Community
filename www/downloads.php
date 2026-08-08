<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/functions.php';

$pageTitle = t('downloads');

require __DIR__ . '/includes/header.php';

$downloads = [
    [
        'name'        => t('server'),
        'description' => t('server_desc'),
        'url'         => 'https://github.com/luismk/Pangya-Server-Community',
    ],
    [
        'name'        => t('server'),
        'description' => t('server_desc'),
        'url'         => 'https://github.com/luismk/Pangya-Server-Community',
    ],
    [
        'name'        => t('tools'),
        'description' => t('tools_desc'),
        'url'         => 'https://github.com/luismk/PangYa-Suite-Tools',
    ],
    [
        'name'        => 'PangYa Community Client',
        'description' => t('download_game'),
        'url'         => 'https://www.mediafire.com/file/j33ghvb4lk2bjtn/JP_Building.rar/file',
    ],
];
?>

<h2 class="mb-4"><?= htmlspecialchars(t('downloads')) ?></h2>

<div class="list-group">
    <?php foreach ($downloads as $item): ?>
        <a 
            href="<?= htmlspecialchars($item['url']) ?>" 
            target="_blank" 
            rel="noopener noreferrer" 
            class="list-group-item list-group-item-action bg-transparent text-light mb-2 rounded card"
        >
            <div class="d-flex w-100 justify-content-between">
                <h5 class="mb-1"><?= htmlspecialchars($item['name']) ?></h5>
                <span><?= htmlspecialchars(t('open_project')) ?> ↗</span>
            </div>
            <p class="mb-1"><?= htmlspecialchars($item['description']) ?></p>
        </a>
    <?php endforeach; ?>
</div>

<?php require __DIR__ . '/includes/footer.php'; ?>