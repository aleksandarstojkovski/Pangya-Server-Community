<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/functions.php';
require_once __DIR__ . '/iff/generate_cache.php';

$items_per_page = 10;
$current_page = isset($_GET['page']) && is_numeric($_GET['page']) ? max(1, (int)$_GET['page']) : 1;

// Sanitização dos parâmetros de entrada
$search_query = isset($_GET['s']) ? trim(strip_tags($_GET['s'])) : '';  
$filter_type = isset($_GET['type']) ? strtolower(trim(strip_tags($_GET['type']))) : '';  

$is_search_active = !empty($search_query);

// 1. Obtém os dados (já filtrados no cache)
$filtered_items = get_full_gallery_data($search_query, $is_search_active, $filter_type);

if (!is_array($filtered_items)) {
    $filtered_items = [];
}

// 2. Cálculo da Paginação
$total_items = count($filtered_items);
$total_pages = $total_items > 0 ? (int)ceil($total_items / $items_per_page) : 1;

// Ajusta a página atual dentro dos limites válidos
if ($current_page > $total_pages) {
    $current_page = $total_pages;
}

$offset = ($current_page - 1) * $items_per_page;

// 3. Extrai APENAS os 10 itens que serão renderizados na tela
$items_to_display = array_slice($filtered_items, $offset, $items_per_page);
$search_param = urlencode($search_query);

$pageTitle = t('Wikipedia');
require __DIR__ . '/includes/header.php';
?>

<div class="page-container">
    <div class="page-banner gallery-banner">
        <div class="banner-content">
            <h1 class="page-title"><i class="fas fa-archive"></i> Catálogo de Itens IFF</h1>
            <p>Explore os <?php echo number_format($total_items, 0, '', '.'); ?> itens disponíveis no jogo.</p>
        </div>
    </div>

    <div class="search-bar-container">
        <form action="item_gallery.php" method="GET" class="search-form">
            <input type="hidden" name="type" value="<?php echo htmlspecialchars($filter_type); ?>">
            <input type="text" name="s" placeholder="Buscar por nome ou ID do item..." value="<?php echo htmlspecialchars($search_query); ?>">
            <button type="submit"><i class="fas fa-search"></i> Buscar</button>
        </form>
    </div>
    
    <div class="type-filter-container">
       <?php
$types = [
    'all'       => ['label' => 'Todos',      'icon' => ''],
    'card'      => ['label' => 'Cards',      'icon' => 'assets/img/bar/BtnCard.png'],
    'setitem'   => ['label' => 'Sets',       'icon' => 'assets/img/bar/BtnSet.png'],
    'part'      => ['label' => 'Parts',      'icon' => 'assets/img/bar/BtnPart.png'],
    'item'      => ['label' => 'Itens',      'icon' => 'assets/img/bar/BtnItem.png'],
    'skin'      => ['label' => 'Skins',      'icon' => 'assets/img/bar/BtnSkin.png'],
    'clubset'   => ['label' => 'ClubSets',   'icon' => 'assets/img/bar/BtnClub.png'],
    'character' => ['label' => 'Characters', 'icon' => 'assets/img/bar/nuri_renew.png'],
    'caddie'    => ['label' => 'Caddies',    'icon' => 'assets/img/bar/BtnCaddie.png'],
    'auxpart'   => ['label' => 'Rings',      'icon' => 'assets/img/bar/BtnAuxPart.png'],
    'ball'      => ['label' => 'Balls',      'icon' => 'assets/img/bar/BtnBall.png'],
    'mascot'    => ['label' => 'Mascotes',   'icon' => 'assets/img/bar/BtnMascot.png'],
];

foreach ($types as $type_value => $info) {
    $active_class = ($filter_type === $type_value) ? 'active' : '';
    $type_link = 'wikipedia.php?s=' . urlencode($search_param) . '&type=' . $type_value . '&page=1';
    
    echo '<a href="' . htmlspecialchars($type_link) . '" class="type-filter-button ' . $active_class . '">';
    
    // Ícone da categoria
    if (!empty($info['icon'])) {
        echo '<img src="' . htmlspecialchars($info['icon']) . '" alt="' . htmlspecialchars($info['label']) . '" height="42" class="me-1">';
    }
    
    echo '<span>' . htmlspecialchars($info['label']) . '</span>';
    echo '</a>';
}
?>
    </div>

    <div class="item-grid">
        <?php if (empty($items_to_display)): ?>
            <p class="no-results-message">Nenhum item encontrado com os filtros atuais (<?php echo htmlspecialchars($search_query); ?> / <?php echo empty($filter_type) ? 'Todos' : htmlspecialchars(strtoupper($filter_type)); ?>).</p>
        <?php endif; ?>

        <?php foreach ($items_to_display as $item): ?>
            <?php
            $icon_name = !empty($item['icon']) ? $item['icon'] : 'default_item';
            $item_id = $item['TypeID'] ?? $item['type_id'] ?? $item['ID'] ?? 0;
            if ($item_id === 0) continue; 
            ?>
            <a href="item_detail.php?id=<?php echo $item_id; ?>" class="item-card">
                <div class="card-icon-wrapper">
                    <img src="/assets/img/items/<?php echo htmlspecialchars($icon_name); ?>.png"
                         alt="<?php echo htmlspecialchars($item['item_name'] ?? 'Item Desconhecido'); ?>" class="item-icon">
                </div>
                <div class="card-info">
                    <span class="item-name"><?php echo htmlspecialchars($item['item_name'] ?? '---'); ?></span>
                    <span class="item-id">ID: <?php echo $item_id; ?></span>
                </div>
            </a>
        <?php endforeach; ?>
    </div>

    <div class="pagination-controls">
        <?php if ($total_pages > 1): ?>
            <?php 
            $base_pagination_url = '?s=' . $search_param . '&type=' . urlencode($filter_type) . '&page=';
            $max_links = 5;
            
            $start_page = max(1, $current_page - floor($max_links / 2));
            $end_page = min($total_pages, $start_page + $max_links - 1);

            if ($end_page - $start_page + 1 < $max_links) {
                $start_page = max(1, $end_page - $max_links + 1);
            }
            ?>
            
            <?php if ($current_page > 1): ?>
                <a href="<?php echo $base_pagination_url . ($current_page - 1); ?>" class="page-link nav-link"><i class="fas fa-angle-left"></i> Anterior</a>
            <?php endif; ?>

            <?php for ($i = $start_page; $i <= $end_page; $i++): ?>
                <?php $active_class = ($i === $current_page) ? 'active' : ''; ?>
                <a href="<?php echo $base_pagination_url . $i; ?>" class="page-link num-link <?php echo $active_class; ?>">
                    <?php echo $i; ?>
                </a>
            <?php endfor; ?>

            <?php if ($current_page < $total_pages): ?>
                <a href="<?php echo $base_pagination_url . ($current_page + 1); ?>" class="page-link nav-link">Próxima <i class="fas fa-angle-right"></i></a>
            <?php endif; ?>

        <?php endif; ?>
    </div>
</div>

<style>
:root {
    --color-neon-blue: #00eaff;
    --color-neon-pink: #ff00ff;
    --color-background-panel: #141420;
    --color-background-dark: #1e1e2d;
    --color-border-main: #333345;
    --color-text-main: #FFFFFF;
    --color-tertiary: #ffc800;
}

.page-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
}

.page-banner.gallery-banner {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 30px;
    background-color: var(--color-background-panel);
    border: 1px solid var(--color-border-main);
    border-radius: 8px;
    margin-bottom: 25px;
    box-shadow: 0 0 10px rgba(0, 234, 255, 0.1);
}

.banner-content h1 {
    margin: 0 0 8px 0;
    font-size: 1.8rem;
    color: var(--color-text-main);
}

.banner-content p {
    margin: 0;
    color: #a0a0b0;
    font-size: 0.95rem;
}

.search-bar-container {
    margin-bottom: 20px;
    display: flex;
    justify-content: center;
}

.search-form {
    display: flex;
    justify-content: center;
    width: 100%;
}

.search-form input[type="text"] {
    padding: 10px 15px; width: 60%; max-width: 400px; border: 1px solid var(--color-neon-blue);
    background-color: var(--color-background-panel); color: var(--color-text-main);
    border-radius: 6px 0 0 6px; font-size: 1em; outline: none;
}

.search-form button {
    padding: 10px 15px; border: none; background-color: var(--color-neon-blue);
    color: var(--color-background-dark); font-weight: bold; cursor: pointer;
    border-radius: 0 6px 6px 0; transition: background-color 0.3s;
}

.search-form button:hover { background-color: var(--color-neon-pink); }

.type-filter-container {
    text-align: center; margin-bottom: 30px; padding: 10px;
    background-color: var(--color-background-panel); border-radius: 8px;
    border: 1px solid var(--color-border-main); display: flex;
    flex-wrap: wrap; justify-content: center; gap: 10px;
}

.type-filter-button {
    display: inline-block; padding: 8px 12px; border: 1px solid var(--color-neon-pink);
    color: var(--color-neon-pink); background-color: transparent; text-decoration: none;
    border-radius: 6px; font-size: 0.9em; font-weight: bold; cursor: pointer;
    transition: all 0.3s; white-space: nowrap; 
}

.type-filter-button:hover {
    background-color: var(--color-neon-pink); color: var(--color-background-dark);
    box-shadow: 0 0 10px var(--color-neon-pink);
}

.type-filter-button.active {
    background-color: var(--color-neon-blue); border-color: var(--color-neon-blue);
    color: var(--color-background-dark); box-shadow: 0 0 15px var(--color-neon-blue);
}

.item-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 20px; justify-content: center; margin-bottom: 30px;
}

.item-card {
    background-color: var(--color-background-panel); border: 1px solid var(--color-border-main);
    border-radius: 8px; padding: 15px; text-align: center; text-decoration: none;
    color: var(--color-text-main); box-shadow: 0 0 5px var(--color-neon-blue);
    transition: all 0.3s ease;
}

.item-card:hover {
    transform: translateY(-5px); border-color: var(--color-neon-blue);
    box-shadow: 0 0 15px var(--color-neon-blue);
}

.card-icon-wrapper { min-height: 100px; display: flex; align-items: center; justify-content: center; margin-bottom: 10px; }
.item-icon { max-width: 90%; max-height: 90px; }
.card-info { display: flex; flex-direction: column; gap: 4px; }
.item-name {
    font-weight: bold; color: var(--color-tertiary); white-space: nowrap;
    overflow: hidden; text-overflow: ellipsis; 
}

.item-id { font-size: 0.8em; color: var(--color-text-main); }

.no-results-message {
    grid-column: 1 / -1; text-align: center; color: var(--color-neon-pink);
    padding: 30px; background-color: #1a1a25; border: 1px dashed var(--color-neon-pink);
    border-radius: 8px;
}

.pagination-controls {
    text-align: center;
    margin-top: 20px;
    padding: 10px 0; 
}

.page-link {
    display: inline-block;
    padding: 8px 14px;
    margin: 0 5px;
    border: 1px solid var(--color-neon-pink);
    color: var(--color-neon-pink);
    background-color: var(--color-background-panel);
    text-decoration: none;
    border-radius: 4px;
    transition: all 0.3s;
}

.page-link.active,
.page-link:hover {
    background-color: var(--color-neon-pink);
    color: var(--color-background-dark);
    font-weight: bold;
}

.page-link i { margin: 0 5px; }

.page-link.num-link {
    min-width: 35px; 
    text-align: center;
    padding: 8px 0; 
}

.page-link.nav-link {
    font-weight: bold; 
    min-width: 100px;
}
</style>

<?php require __DIR__ . '/includes/footer.php'; ?>