<?php

require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';
require_once __DIR__ . '/../includes/IffCatalog.php';
require_once __DIR__ . '/../includes/WarehouseService.php';
require_once __DIR__ . '/../includes/AuditLogger.php';

requireGameMaster();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    App::redirect('dashboard.php');
}

if (!csrfValid($_POST['csrf_token'] ?? null)) {
    setFlash('error', 'Sessão expirada. Tente novamente.');
    App::redirect('dashboard.php');
}

$uid = (int) $_SESSION['uid'];
$action = $_POST['action'] ?? '';
$typeId = filter_var($_POST['typeid'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
$itemId = filter_var($_POST['item_id'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);

if ($action === 'add' && ($typeId === false || $typeId === null)) {
    setFlash('error', 'Selecione um item válido.');
    App::redirect('dashboard.php');
}

try {
    $pdo = getConnection();
    $service = new WarehouseService($pdo, new IffCatalog());
    $audit = new AuditLogger($pdo);

    if ($action === 'add') {
        $itemName = $service->add($uid, $typeId);
        $audit->record('warehouse.add', null, ['typeid' => $typeId, 'name' => $itemName]);
        setFlash('success', $itemName . ' foi adicionado ao armazém.');
    } elseif ($action === 'remove') {
        if ($itemId === false || $itemId === null) {
            throw new InvalidArgumentException('ID do item inválido.');
        }
        $removed = $service->removeByItemId($uid, $itemId);
        if ($removed) {
            $audit->record('warehouse.remove', $itemId);
        }
        setFlash($removed ? 'success' : 'error', $removed ? 'Item removido do armazém.' : 'Nenhuma unidade desse item foi encontrada.');
    } else {
        setFlash('error', 'Ação inválida.');
    }
} catch (InvalidArgumentException $exception) {
    setFlash('error', $exception->getMessage());
} catch (Throwable $exception) {
    error_log('Erro ao atualizar armazém: ' . $exception->getMessage());
    setFlash('error', 'Não foi possível salvar as alterações no armazém.');
}

App::redirect('dashboard.php');
