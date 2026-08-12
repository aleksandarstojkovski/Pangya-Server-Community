<?php

require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/IffCatalog.php';

header('Content-Type: application/json; charset=utf-8');

$query = trim((string) ($_GET['q'] ?? ''));
if (mb_strlen($query) < 2) {
    echo json_encode([], JSON_UNESCAPED_UNICODE);
    exit;
}

try {
    echo json_encode((new IffCatalog())->search($query), JSON_UNESCAPED_UNICODE);
} catch (Throwable $exception) {
    http_response_code(500);
    echo json_encode(['error' => 'Não foi possível consultar o catálogo de itens.'], JSON_UNESCAPED_UNICODE);
}
