<?php

require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';
require_once __DIR__ . '/../includes/AuditLogger.php';
require_once __DIR__ . '/MarketplaceService.php';

requireLogin();
if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValid($_POST['csrf_token'] ?? null)) {
    App::flash('error', 'Solicitação inválida ou sessão expirada.');
    App::redirect('ShopSale.php');
}

try {
    $pdo = getConnection();
    $service = new MarketplaceService($pdo);
    $action = $_POST['action'] ?? '';
    $uid = (int) $_SESSION['uid'];

    if ($action === 'create') {
        $itemId = filter_var($_POST['item_id'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
        $price = filter_var($_POST['price'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
        if ($itemId === false || $price === false) { throw new InvalidArgumentException('Item ou preço inválido.'); }
        $service->createListing($uid, $itemId, $price, (string) ($_POST['currency'] ?? ''));
        (new AuditLogger($pdo))->record('marketplace.create', $itemId, ['price' => $price]);
        App::flash('success', 'Oferta criada com sucesso.');
    } elseif ($action === 'buy') {
        $listingId = filter_var($_POST['listing_id'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
        if ($listingId === false) { throw new InvalidArgumentException('Oferta inválida.'); }
        $service->buy($uid, $listingId);
        (new AuditLogger($pdo))->record('marketplace.buy', null, ['listing_id' => $listingId]);
        App::flash('success', 'Compra concluída com sucesso.');
    } else { throw new InvalidArgumentException('Ação inválida.'); }
} catch (Throwable $exception) {
    error_log('Marketplace: ' . $exception->getMessage());
    App::flash('error', $exception instanceof InvalidArgumentException || $exception instanceof RuntimeException ? $exception->getMessage() : 'Não foi possível concluir a operação.');
}

App::redirect('ShopSale.php');
