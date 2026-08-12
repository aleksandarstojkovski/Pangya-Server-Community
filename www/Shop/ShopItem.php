<?php

require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';
require_once __DIR__ . '/ShopCatalog.php';

$term = trim((string) ($_GET['q'] ?? ''));
$category = trim((string) ($_GET['category'] ?? ''));
$types = [
    'all' => ['label' => 'Todos', 'icon' => ''],
    'card' => ['label' => 'Cards', 'icon' => '/assets/img/bar/BtnCard.png'],
    'setitem' => ['label' => 'Sets', 'icon' => '/assets/img/bar/BtnSet.png'],
    'part' => ['label' => 'Parts', 'icon' => '/assets/img/bar/BtnPart.png'],
    'item' => ['label' => 'Itens', 'icon' => '/assets/img/bar/BtnItem.png'],
    'skin' => ['label' => 'Skins', 'icon' => '/assets/img/bar/BtnSkin.png'],
    'clubset' => ['label' => 'ClubSets', 'icon' => '/assets/img/bar/BtnClub.png'],
    'caddie' => ['label' => 'Caddies', 'icon' => '/assets/img/bar/BtnCaddie.png'],
    'auxpart' => ['label' => 'Rings', 'icon' => '/assets/img/bar/BtnAuxPart.png'],
    'ball' => ['label' => 'Balls', 'icon' => '/assets/img/bar/BtnBall.png'],
    'mascot' => ['label' => 'Mascotes', 'icon' => '/assets/img/bar/BtnMascot.png'],
];

if (!array_key_exists($category, $types)) {
    $category = '';
}

$items = (new ShopCatalog())->search($term, $category);
$itemsPerPage = 12;
$page = max(1, (int) ($_GET['page'] ?? 1));
$totalItems = count($items);
$totalPages = max(1, (int) ceil($totalItems / $itemsPerPage));
$page = min($page, $totalPages);
$visibleItems = array_slice($items, ($page - 1) * $itemsPerPage, $itemsPerPage);

$pageTitle = t('shop_items');
require __DIR__ . '/../includes/header.php';
?>

<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
    <h2 class="mb-0"><?= htmlspecialchars(t('shop_items')) ?></h2>
    <div class="btn-group">
        <a class="btn btn-outline-light btn-sm" href="ShopCash.php">
            <?= htmlspecialchars(t('shop_cash')) ?>
        </a>
        <a class="btn btn-primary btn-sm" href="ShopSale.php">
            <?= htmlspecialchars(t('marketplace')) ?>
        </a>
    </div>
</div>

<form class="card p-3 mb-4" method="get">
    <div class="row g-2">
        <div class="col-md-8">
            <label class="visually-hidden" for="shopSearch"><?= htmlspecialchars(t('search_item')) ?></label>
            <input
                id="shopSearch"
                class="form-control"
                name="q"
                value="<?= htmlspecialchars($term) ?>"
                placeholder="<?= htmlspecialchars(t('search_item')) ?>"
            >
        </div>
        <div class="col-md-4">
            <button class="btn btn-primary w-100" type="submit">
                <?= htmlspecialchars(t('shop_items')) ?>
            </button>
        </div>
    </div>
</form>

<div class="d-flex gap-2 overflow-auto pb-2 mb-4" aria-label="Categorias da loja" style="
    position: relative;
    left: 36px;
">
    <?php foreach ($types as $key => $type): ?>
        <?php
        $selected = ($key === 'all' && $category === '') || $category === $key;
        $query = array_filter([
            'q' => $term,
            'category' => $key === 'all' ? null : $key,
        ], static fn ($value) => $value !== null && $value !== '');
        ?>
        <a
            class="btn btn-sm <?= $selected ? 'btn-primary' : 'btn-outline-secondary text-light' ?> d-inline-flex align-items-center gap-1"
            href="?<?= htmlspecialchars(http_build_query($query)) ?>"
        >
            <?php if ($type['icon'] !== ''): ?>
                <img
                    src="<?= htmlspecialchars($type['icon']) ?>"
                    alt="" 
                    height="42"
                    loading="lazy"
                >
            <?php endif; ?>
            <span><?= htmlspecialchars($type['label']) ?></span>
        </a>
    <?php endforeach; ?>
</div>

<p class="text-secondary small">
    <?= $totalItems ?> itens encontrados. Use a busca para filtrar por nome ou TypeId.
</p>

<div class="row g-3">
    <?php foreach ($visibleItems as $item): ?>
        <div class="col-sm-6 col-lg-4">
            <div class="card p-3 h-100">
                <div class="d-flex align-items-center gap-3 mb-3">
                    <img
                        src="/assets/img/items/<?= rawurlencode($item['icon']) ?>.png"
                        width="64"
                        height="64"
                        alt=""
                        loading="lazy"
                        onerror="this.style.display='none'"
                    >
                    <div>
                        <div class="fw-bold"><?= htmlspecialchars($item['name']) ?></div>
                        <div class="text-secondary small">TypeId: <?= (int) $item['typeid'] ?></div>
                    </div>
                </div>
                <form method="post" action="shop_action.php" class="mt-auto">
    <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(csrfToken()) ?>">
    <input type="hidden" name="action" value="buy_shop_item">
    <input type="hidden" name="typeid" value="<?= (int) $item['typeid'] ?>">
    
    <div class="d-flex gap-2">
        <a class="btn btn-outline-secondary btn-sm" href="/item_detail.php?id=<?= (int) $item['typeid'] ?>" title="Ver Detalhes">
            <i class="bi bi-info-circle"></i> Detalhes
        </a>
        <button type="submit" class="btn btn-primary btn-sm w-50">
           <img src="/assets/img/bar/bar_papel.png" alt="PangYa Community" height="42"> <?= htmlspecialchars(t('buy')) ?>
        </button>
    </div>
</form>
            </div>
        </div>
    <?php endforeach; ?>
</div>

<?php if ($items === []): ?>
    <p class="text-secondary"><?= htmlspecialchars(t('no_items_found')) ?></p>
<?php endif; ?>

<?php if ($totalPages > 1): ?>
    <nav class="mt-4" aria-label="Paginação da loja">
        <ul class="pagination justify-content-center">
            <li class="page-item <?= $page === 1 ? 'disabled' : '' ?>">
                <a class="page-link" href="?<?= htmlspecialchars(http_build_query(['q' => $term, 'category' => $category, 'page' => $page - 1])) ?>">
                    <?= htmlspecialchars(t('previous')) ?>
                </a>
            </li>
            <li class="page-item active">
                <span class="page-link"><?= $page ?> / <?= $totalPages ?></span>
            </li>
            <li class="page-item <?= $page === $totalPages ? 'disabled' : '' ?>">
                <a class="page-link" href="?<?= htmlspecialchars(http_build_query(['q' => $term, 'category' => $category, 'page' => $page + 1])) ?>">
                    <?= htmlspecialchars(t('next')) ?>
                </a>
            </li>
        </ul>
    </nav>
<?php endif; ?>

<?php require __DIR__ . '/../includes/footer.php'; ?>
