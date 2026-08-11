<?php

require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/functions.php';
require_once __DIR__ . '/includes/IffCatalog.php';
require_once __DIR__ . '/includes/WarehouseService.php';

requireLogin();

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

if ($typeId === false || $typeId === null) {
    setFlash('error', 'Selecione um item válido.');
    App::redirect('dashboard.php');
}

try {
    $service = new WarehouseService(getConnection(), new IffCatalog());

    if ($action === 'add') {
        $itemName = $service->add($uid, $typeId);
        setFlash('success', $itemName . ' foi adicionado ao armazém.');
    } elseif ($action === 'remove') {
        $removed = $service->removeOne($uid, $typeId);
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
