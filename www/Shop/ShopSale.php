<?php

require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';
require_once __DIR__ . '/../includes/IffCatalog.php';

requireLogin();

$uid = (int) $_SESSION['uid'];
$catalog = new IffCatalog();

$types = [
    'all'       => ['label' => 'Todos', 'icon' => ''],
    'card'      => ['label' => 'Cards', 'icon' => '/assets/img/bar/BtnCard.png'],
    'part'      => ['label' => 'Parts', 'icon' => '/assets/img/bar/BtnPart.png'],
    'item'      => ['label' => 'Itens', 'icon' => '/assets/img/bar/BtnItem.png'],
    'clubset'   => ['label' => 'ClubSets', 'icon' => '/assets/img/bar/BtnClub.png'],
    'auxpart'   => ['label' => 'Rings', 'icon' => '/assets/img/bar/BtnAuxPart.png']
];

$selectedType = $_GET['type'] ?? 'all';
if (!array_key_exists($selectedType, $types)) {
    $selectedType = 'all';
}

$searchQuery = trim((string) ($_GET['q'] ?? ''));

// Configuração de Paginação
$pageInventory = max(1, (int) ($_GET['p_inv'] ?? 1));
$pageMarket    = max(1, (int) ($_GET['p_mkt'] ?? 1));
$perPage       = 12;

$listings = [];
$availableItems = [];
$totalListings = 0;

try {
    $pdo = getConnection();

    // 1. Marketplace (Listagem Ativa com Paginação)
    $countStatement = $pdo->prepare(
        'SELECT COUNT(*) FROM pangya.web_marketplace_listing WHERE [status] = ?'
    );
    $countStatement->execute(['active']);
    $totalListings = (int) $countStatement->fetchColumn();

    $offsetMarket = ($pageMarket - 1) * $perPage;
    $listingStatement = $pdo->prepare(
'SELECT [listing_id], [item_id], [typeid], [price], [currency], [created_at], [seller_uid]
     FROM pangya.web_marketplace_listing
     WHERE [status] = ?
     ORDER BY [created_at] DESC
     OFFSET ? ROWS FETCH NEXT ? ROWS ONLY'
    );
    $listingStatement->bindValue(1, 'active', PDO::PARAM_STR);
    $listingStatement->bindValue(2, $offsetMarket, PDO::PARAM_INT);
    $listingStatement->bindValue(3, $perPage, PDO::PARAM_INT);
    $listingStatement->execute();
    $listings = $listingStatement->fetchAll();

    // 2. Inventário do Usuário (Filtrável e Paginado)
    $inventoryStatement = $pdo->prepare(
        'SELECT [item_id], [typeid]
         FROM pangya.pangya_item_warehouse AS warehouse
         WHERE [UID] = ?
           AND [valid] = 1
           AND NOT EXISTS (
               SELECT 1
               FROM pangya.web_marketplace_listing AS listing
               WHERE listing.[item_id] = warehouse.[item_id]
                 AND listing.[status] = ?
           )
         ORDER BY [item_id] DESC'
    );
    $inventoryStatement->execute([$uid, 'active']);
    $rawInventory = $inventoryStatement->fetchAll();

    $filteredInventory = [];
    foreach ($rawInventory as $item) {
        $typeId = (int) $item['typeid'];
        $iffItem = $catalog->find($typeId) ?? [];
        $itemGroup = strtolower((string) ($iffItem['group'] ?? $iffItem['type'] ?? ''));
        $itemName  = (string) ($iffItem['item_name'] ?? $iffItem['name'] ?? ('Item #' . $typeId));

        // Filtro por categoria ($types)
        if ($selectedType !== 'all' && $itemGroup !== $selectedType) {
            continue;
        }

        // Filtro por palavra-chave
        if ($searchQuery !== '' && mb_strpos(mb_strtolower($itemName), mb_strtolower($searchQuery)) === false) {
            continue;
        }

        $icon = (string) ($iffItem['icon'] ?? 'default');
        $filteredInventory[] = [
            'item_id' => (int) $item['item_id'],
            'typeid'  => $typeId,
            'name'    => $itemName,
            'icon'    => preg_match('/^[a-zA-Z0-9_-]+$/', $icon) ? $icon : 'default',
        ];
    }

    $totalInventory = count($filteredInventory);
    $offsetInv = ($pageInventory - 1) * $perPage;
    $availableItems = array_slice($filteredInventory, $offsetInv, $perPage);

    $totalPagesInv = (int) ceil($totalInventory / $perPage);
    $totalPagesMkt = (int) ceil($totalListings / $perPage);

} catch (PDOException $exception) {
    error_log('Marketplace: ' . $exception->getMessage());
}

$pageTitle = t('marketplace');
require __DIR__ . '/../includes/header.php';
?>
<style>
    .filter-btn-group {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
        margin-bottom: 1.5rem;
    }

    .filter-btn {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.4rem 0.85rem;
        font-size: 0.85rem;
        font-weight: 500;
        border-radius: 8px;
        transition: all 0.2s ease-in-out;
        text-decoration: none;
        backdrop-filter: blur(4px);
    }

    /* Estado Inativo (Tema Escuro) */
    .filter-btn-inactive {
        background-color: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.15);
        color: #e0e0e0;
    }

    .filter-btn-inactive:hover {
        background-color: rgba(255, 255, 255, 0.15);
        border-color: rgba(255, 255, 255, 0.3);
        color: #ffffff;
        transform: translateY(-1px);
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
    }

    /* Estado Ativo */
    .filter-btn-active {
        background-color: #0d6efd;
        border: 1px solid #0d6efd;
        color: #ffffff;
        box-shadow: 0 4px 12px rgba(13, 110, 253, 0.35);
    }

    .filter-btn-active:hover {
        background-color: #0b5ed7;
        color: #ffffff;
    }

    /* Ícone */
    .filter-btn-icon {
        width: 42px;
        height: 42px;
        object-fit: contain;
        filter: drop-shadow(0 1px 2px rgba(0,0,0,0.5));
    }
</style>
<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
    <h2 class="mb-0"><?= htmlspecialchars(t('marketplace')) ?></h2>
    <div class="btn-group">
        <a class="btn btn-outline-light btn-sm" href="ShopItem.php">
            <?= htmlspecialchars(t('shop_items')) ?>
        </a>
        <a class="btn btn-outline-light btn-sm" href="ShopCash.php">
            <?= htmlspecialchars(t('shop_cash')) ?>
        </a>
    </div>
</div>

<!-- Filtros de Categoria -->
<div class="filter-btn-group" style="margin-left: 42px">
    <?php foreach ($types as $key => $typeData): ?>
        <?php $isActive = ($selectedType === $key); ?>
        <a 
            href="?type=<?= urlencode($key) ?>&q=<?= urlencode($searchQuery) ?>" 
            class="filter-btn <?= $isActive ? 'filter-btn-active' : 'filter-btn-inactive' ?>"
        >
            <?php if (!empty($typeData['icon'])): ?>
                <img 
                    src="<?= htmlspecialchars($typeData['icon']) ?>" 
                    alt="" 
                    class="filter-btn-icon"
                    loading="lazy"
                    onerror="this.style.display='none'"
                >
            <?php endif; ?>
            <span><?= htmlspecialchars($typeData['label']) ?></span>
        </a>
    <?php endforeach; ?>
</div>
<section class="mb-5">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0"><?= htmlspecialchars(t('my_available_items')) ?></h4>
        
        <!-- Formulário de Busca Rápida no Inventário -->
        <form method="get" class="d-flex gap-2">
            <input type="hidden" name="type" value="<?= htmlspecialchars($selectedType) ?>">
            <input 
                type="text" 
                name="q" 
                class="form-control form-control-sm" 
                placeholder="<?= htmlspecialchars(t('search_item')) ?>" 
                value="<?= htmlspecialchars($searchQuery) ?>"
            >
            <button type="submit" class="btn btn-sm btn-outline-light">Buscar</button>
        </form>
    </div>

    <?php if ($availableItems === []): ?>
        <div class="card p-3 text-secondary">
            <?= htmlspecialchars(t('no_sellable_items')) ?>
        </div>
    <?php else: ?>
        <div class="row g-3">
            <?php foreach ($availableItems as $item): ?>
                <div class="col-sm-6 col-lg-4">
                    <div class="card p-3 h-100">
                        <div class="d-flex align-items-center gap-3 mb-3">
                            <img
                                src="/assets/img/items/<?= rawurlencode($item['icon']) ?>.png"
                                width="48"
                                height="48"
                                alt=""
                                loading="lazy"
                                onerror="this.style.display='none'"
                            >
                            <div>
                                <div class="fw-bold"><?= htmlspecialchars($item['name']) ?></div>
                                <div class="small text-secondary">
                                    <?= htmlspecialchars(t('item_id')) ?>: <?= $item['item_id'] ?> · TypeId: <?= $item['typeid'] ?>
                                </div>
                            </div>
                        </div>

                        <form method="post" action="marketplace_action.php" class="mt-auto">
                            <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(csrfToken()) ?>">
                            <input type="hidden" name="action" value="create">
                            <input type="hidden" name="item_id" value="<?= $item['item_id'] ?>">

                            <div class="row g-2">
                                <div class="col-7">
                                    <label class="visually-hidden" for="price-<?= $item['item_id'] ?>">
                                        <?= htmlspecialchars(t('price')) ?>
                                    </label>
                                    <input
                                        id="price-<?= $item['item_id'] ?>"
                                        class="form-control form-control-sm"
                                        name="price"
                                        type="number"
                                        min="1"
                                        required
                                        placeholder="<?= htmlspecialchars(t('price')) ?>"
                                    >
                                </div>
                                <div class="col-5">
                                    <select class="form-select form-select-sm" name="currency" aria-label="Moeda">
                                        <option value="Pang">Pang</option>
                                        <option value="Cookie">Cookie</option>
                                    </select>
                                </div>
                            </div>

                            <button class="btn btn-primary btn-sm w-100 mt-2" type="submit">
                                <?= htmlspecialchars(t('sell')) ?>
                            </button>
                        </form>
                    </div>
                </div>
            <?php endforeach; ?>
        </div>

        <!-- Paginação do Inventário -->
        <?php if (($totalPagesInv ?? 1) > 1): ?>
            <nav class="mt-4" aria-label="Paginação do Inventário">
                <ul class="pagination pagination-sm justify-content-center mb-0">
                    <li class="page-item <?= $pageInventory <= 1 ? 'disabled' : '' ?>">
                        <a class="page-link" href="?<?= htmlspecialchars(http_build_query([
                            'type'  => $selectedType,
                            'q'     => $searchQuery,
                            'p_inv' => max(1, $pageInventory - 1),
                            'p_mkt' => $pageMarket
                        ])) ?>">
                            <?= htmlspecialchars(t('previous')) ?>
                        </a>
                    </li>

                    <li class="page-item active">
                        <span class="page-link"><?= $pageInventory ?> / <?= $totalPagesInv ?></span>
                    </li>

                    <li class="page-item <?= $pageInventory >= $totalPagesInv ? 'disabled' : '' ?>">
                        <a class="page-link" href="?<?= htmlspecialchars(http_build_query([
                            'type'  => $selectedType,
                            'q'     => $searchQuery,
                            'p_inv' => min($totalPagesInv, $pageInventory + 1),
                            'p_mkt' => $pageMarket
                        ])) ?>">
                            <?= htmlspecialchars(t('next')) ?>
                        </a>
                    </li>
                </ul>
            </nav>
        <?php endif; ?>
    <?php endif; ?>
</section>

<section>
    <h4 class="mb-3"><?= htmlspecialchars(t('marketplace')) ?></h4>

    <div class="card p-3">
        <div class="table-responsive">
            <table class="table table-dark table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th><?= htmlspecialchars(t('item_id')) ?></th>
                        <th>TypeId</th>
                        <th><?= htmlspecialchars(t('price')) ?></th>
                        <th>Moeda</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach ($listings as $listing): ?>
                        <tr>
                            <td><?= (int) $listing['item_id'] ?></td>
                            <td><?= (int) $listing['typeid'] ?></td>
                            <td><?= number_format((int) $listing['price'], 0, ',', '.') ?></td>
                            <td><?= htmlspecialchars($listing['currency']) ?></td>
                            <td class="text-end">
    <?php if ((int) $listing['seller_uid'] === $uid): ?>
        <button class="btn btn-sm btn-secondary" disabled title="Você não pode comprar seu próprio item">
            <?= htmlspecialchars(t('My') ?? 'My') ?>
        </button>
    <?php else: ?>
        <form method="post" action="marketplace_action.php" class="d-inline">
            <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(csrfToken()) ?>">
            <input type="hidden" name="action" value="buy">
            <input type="hidden" name="listing_id" value="<?= (int) $listing['listing_id'] ?>">
            <button class="btn btn-sm btn-outline-primary" type="submit">
                <?= htmlspecialchars(t('buy')) ?>
            </button>
        </form>
    <?php endif; ?>
</td>
                        </tr>
                    <?php endforeach; ?>

                    <?php if ($listings === []): ?>
                        <tr>
                            <td colspan="5" class="text-secondary text-center py-3">
                                <?= htmlspecialchars(t('no_items_found')) ?>
                            </td>
                        </tr>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>

        <!-- Paginação do Marketplace com Botões Anterior / Próximo -->
        <?php if (($totalPagesMkt ?? 1) > 1): ?>
            <nav class="mt-4" aria-label="Paginação do Marketplace">
                <ul class="pagination pagination-sm justify-content-center mb-0">
                    <li class="page-item <?= $pageMarket <= 1 ? 'disabled' : '' ?>">
                        <a class="page-link" href="?<?= htmlspecialchars(http_build_query([
                            'type'  => $selectedType,
                            'q'     => $searchQuery,
                            'p_inv' => $pageInventory,
                            'p_mkt' => max(1, $pageMarket - 1)
                        ])) ?>">
                            <?= htmlspecialchars(t('previous')) ?>
                        </a>
                    </li>

                    <li class="page-item active">
                        <span class="page-link"><?= $pageMarket ?> / <?= $totalPagesMkt ?></span>
                    </li>

                    <li class="page-item <?= $pageMarket >= $totalPagesMkt ? 'disabled' : '' ?>">
                        <a class="page-link" href="?<?= htmlspecialchars(http_build_query([
                            'type'  => $selectedType,
                            'q'     => $searchQuery,
                            'p_inv' => $pageInventory,
                            'p_mkt' => min($totalPagesMkt, $pageMarket + 1)
                        ])) ?>">
                            <?= htmlspecialchars(t('next')) ?>
                        </a>
                    </li>
                </ul>
            </nav>
        <?php endif; ?>
    </div>
</section>

<?php require __DIR__ . '/../includes/footer.php'; ?>