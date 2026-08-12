<?php
require_once __DIR__ . '/Config/config.php';
require_once __DIR__ . '/includes/functions.php'; 
require_once __DIR__ . '/iff/iff.php';     

$item_id = null;
if (isset($_GET['id']) && is_numeric($_GET['id'])) {
    $item_id = (int)$_GET['id'];
}

$item_name = 'Item Não Encontrado';
$item_description = 'Detalhes do item não puderam ser carregados.';
$item_price_pang = 0;
$item_price_cookie = 0;
$item_shop_normal = 0;
$item_shop_gif = 0;
$item_shop_hide = 0;
$item_shop_ps = 0;
$item_shop = 0;
$item_shop_sale = 0;
$item_icon = 'default_item';

$item_data = find_all($item_id);
if ($item_data) {
    $flags = buildItemUI($item_data);

    $item_name = $item_data['item_name'] ?? 'Desconhecido';
$descData = find_desc($item_id);
$item_description = ($descData && !empty($descData['info'])) 
    ? $descData['info'] 
    : 'Nenhuma descrição disponível.';
    $item_shop_normal = $flags['IsNormal'];
    $item_shop_gif = $flags['IsGift'];
    $item_shop_ps = $flags['IsPSQ'];
    $item_shop = $flags['IsShop'];
    $item_shop_sale = $flags['IsSaleable'];
    $item_shop_hide = $flags['IsHide'];
    
    if ($flags['IsPang'])           
        $item_price_pang = $item_data['item_price'] ?? 0;
    else           
        $item_price_cookie = $item_data['item_price'] ?? 0;
          
    $item_icon = !empty($item_data['icon']) ? $item_data['icon'] : 'default_item';
}

$display_price_pang = number_format($item_price_pang, 0, '', '.');
$display_price_cookie = number_format($item_price_cookie, 0, '', '.');
$pageTitle = t('Wikipedia');
require __DIR__ . '/includes/header.php';
?>
 
<div class="page-banner papel-banner">
    <div class="banner-content">
        <h1 class="page-title"><i class="fas fa-coins"></i> Detail Items</h1>
        <p>Visualizacao dos dados do item.</p>
    </div>
</div>

 <div class="item-detail-container">
    
    <div class="item-header">
        <h1><?php echo htmlspecialchars($item_name); ?></h1>
    </div>

    <?php if ($item_id !== null && $item_name !== 'Item Não Encontrado'): ?>
    <div class="item-content">
        
        <div class="item-image-area">
            <img class="item-icon-detail" src="/assets/img/items/<?php echo htmlspecialchars($item_icon); ?>.png" alt="<?php echo htmlspecialchars($item_name); ?>">
            
            <div class="price-box">
                <h2>Shop Info</h2>
                <?php if ($item_price_pang > 0): ?>
                    <div class="price-line">
                        <span class="price-label">Pang:</span>
                        <span class="price-value"><?php echo $display_price_pang; ?></span>
                    </div>
                <?php endif; ?>
                
                <?php if ($item_price_cookie > 0): ?>
                    <div class="price-line">
                        <span class="price-label">Cookie:</span>
                        <span class="price-value"><?php echo $display_price_cookie; ?></span>
                    </div>
                <?php endif; ?>
                
                <?php if ($item_shop_hide === 0): ?>
                    <div class="price-line">
                        <span class="price-label">Item Normal:</span>
                        <span class="price-value"><img src="/assets/img/bar/<?php echo $item_shop_normal === 1 ? 'bar_apply' : 'bar_deleted'; ?>.png"></span>
                    </div>
                    <div class="price-line">
                        <span class="price-label">Gif Shop:</span>
                        <span class="price-value"><img alt="<?php echo $item_shop_gif; ?>" src="/assets/img/bar/<?php echo $item_shop_gif === 1 ? 'bar_apply' : 'bar_deleted'; ?>.png"></span>
                    </div> 
                    <div class="price-line">
                        <span class="price-label">Personal Shop:</span>
                        <span class="price-value"><img alt="<?php echo $item_shop_ps; ?>" src="/assets/img/bar/<?php echo $item_shop_ps === 1 ? 'bar_apply' : 'bar_deleted'; ?>.png"></span>
                    </div> 
                    <div class="price-line">
                        <span class="price-label">Sale Shop:</span>
                        <span class="price-value"><img alt="<?php echo $item_shop_sale; ?>" src="/assets/img/bar/<?php echo $item_shop_sale === 1 ? 'bar_apply' : 'bar_deleted'; ?>.png"></span>
                    </div> 
                    <div class="price-line">
                        <span class="price-label">In Shop:</span>
                        <span class="price-value"><img alt="<?php echo $item_shop; ?>" src="/assets/img/bar/<?php echo $item_shop === 1 ? 'bar_apply' : 'bar_deleted'; ?>.png"></span>
                    </div> 
                <?php endif; ?>
                
                <?php if ($item_shop_hide > 0): ?>
                    <div class="price-line">
                        <span class="price-label">Hide Item:</span>
                        <span class="price-value"><img src="/assets/img/bar/<?php echo $item_shop_hide === 1 ? 'bar_apply' : 'bar_deleted'; ?>.png"></span>
                    </div>
                <?php endif; ?> 
                
                <?php if ($item_price_pang === 0 && $item_price_cookie === 0): ?>
                    <div class="price-line">
                        <span class="price-value" style="color: var(--color-neon-pink, #ff00ff);">GRÁTIS / Não vendível</span>
                    </div>
                <?php endif; ?>
            </div>
        </div>

        <div class="item-info-area">
            <h2>Info</h2>
            <p><?php echo nl2br(htmlspecialchars($item_description)); ?></p> 
        </div>
        
    </div>
    
    <?php else: ?>
    <p style="color: var(--color-neon-pink, #ff00ff); text-align: center;">
        <i class="fas fa-exclamation-triangle"></i> Por favor, forneça um ID de item válido.
    </p>
    <?php endif; ?>
    
</div>

<style>
.page-banner.papel-banner {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 30px;
    background-color: var(--color-background-panel, #141420);
    border: 1px solid var(--color-border-main, #333345);
    border-radius: 8px;
    margin-bottom: 25px;
    box-shadow: 0 0 10px rgba(0, 234, 255, 0.1);
}

.banner-content h1 {
    margin: 0 0 8px 0;
    font-size: 1.8rem;
    color: var(--color-text-main, #FFFFFF);
}

.banner-content p {
    margin: 0;
    color: #a0a0b0;
    font-size: 0.95rem;
}

.item-detail-container {
    max-width: 800px;
    margin: 40px auto;
    background-color: var(--color-background-panel, #141420);
    border: 1px solid var(--color-border-main, #333345);
    border-radius: 12px;
    box-shadow: 0 0 15px var(--color-neon-blue, #00eaff);
    padding: 25px;
    color: var(--color-text-main, #FFFFFF);
}

.item-header {
    display: flex;
    align-items: center;
    border-bottom: 2px solid var(--color-neon-pink, #ff00ff);
    padding-bottom: 15px;
    margin-bottom: 20px;
}

.item-header h1 {
    font-size: 2.2em;
    color: var(--color-neon-blue, #00eaff);
    margin: 0;
    text-shadow: 0 0 5px var(--color-neon-blue, #00eaff);
}

.item-content {
    display: flex;
    gap: 30px;
}

.item-image-area {
    flex: 0 0 40%;
    text-align: center;
}

.item-image-area img.item-icon-detail {
    max-width: 100%;
    height: auto;
    border: 3px solid var(--color-neon-pink, #ff00ff);
    border-radius: 8px;
    box-shadow: 0 0 10px var(--color-neon-pink, #ff00ff);
    transition: transform 0.3s ease;
}

.item-info-area {
    flex: 1;
}

.item-info-area h2 {
    color: var(--color-tertiary, #ffc800);
    font-size: 1.5em;
    margin-top: 0;
}

.price-box {
    margin-top: 20px;
    padding: 15px;
    background-color: #1f1f33;
    border-radius: 8px;
    border: 1px solid var(--color-neon-blue, #00eaff);
    box-shadow: inset 0 0 5px var(--color-neon-blue, #00eaff);
}

.price-line {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 1.1em;
    font-weight: bold;
    margin: 8px 0;
}

.price-label {
    color: var(--color-text-main, #FFFFFF);
}

.price-value {
    color: var(--color-tertiary, #ffc800);
}
</style>

<?php require __DIR__ . '/includes/footer.php'; ?>
