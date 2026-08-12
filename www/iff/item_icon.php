<?php

require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/IffCatalog.php';

header('Content-Type: application/json; charset=utf-8');
$typeId = filter_var($_GET['typeid'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);

if ($typeId === false || $typeId === null) {
    http_response_code(422);
    echo json_encode(['error' => 'TypeId inválido.']);
    exit;
}

$item = (new IffCatalog())->find($typeId);
$icon = (string) ($item['icon'] ?? 'default');
if (!preg_match('/^[a-zA-Z0-9_-]+$/', $icon)) {
    $icon = 'default';
}

echo json_encode(['typeid' => $typeId, 'url' => '/assets/img/items/' . $icon . '.png']);
