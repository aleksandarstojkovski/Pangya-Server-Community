<?php
/**
 * test_archive.php
 * -----------------------------------------------------------------------
 * Diagnóstico rápido da leitura do pacote 'pangya_jp.iff' (ZIP).
 * Rode via CLI (php iff/test_archive.php) ou navegador.
 * -----------------------------------------------------------------------
 */

// Carrega primeiro o iff.php que inicializa o ambiente e dependências
require_once __DIR__ . '/iff.php';

use PangyaIFF\Parser\IFFArchive;

$isCli = PHP_SAPI === 'cli';
function line(string $text, bool $isCli): void {
    echo $isCli ? ($text . "\n") : ($text . "<br>\n");
}

line('=== Teste do pacote pangya_jp.iff ===', $isCli);
line('Extensão zip do PHP: ' . (class_exists('ZipArchive') ? '✔ disponível' : '✘ NÃO disponível (habilite ext-zip)'), $isCli);

$archivePath = IFFArchive::getArchivePath();
line('Caminho configurado: ' . $archivePath, $isCli);
line('Arquivo existe: ' . (is_file($archivePath) ? '✔ sim' : '✘ não encontrado'), $isCli);

if (is_file($archivePath)) {
    $entries = IFFArchive::listEntries();
    line('Entradas dentro do pacote: ' . count($entries), $isCli);
    foreach ($entries as $entry) {
        line('  - ' . $entry, $isCli);
    }

    line('', $isCli);
    line('--- Testando leitura de cada arquivo esperado ---', $isCli);
    foreach (['Item.iff', 'Part.iff', 'SetItem.iff', 'Card.iff', 'Desc.iff'] as $expected) {
        $found = IFFArchive::has($expected);
        line(($found ? '✔ ' : '✘ ') . $expected . ($found ? ' encontrado no pacote' : ' NÃO encontrado no pacote'), $isCli);
    }
} else {
    line('Nada para listar — verifique o caminho acima.', $isCli);
}

line('', $isCli);
line('--- Testando find_item() de ponta a ponta ---', $isCli);

$testId = isset($argv[1]) ? (int)$argv[1] : (int)($_GET['id'] ?? 0);
if ($testId > 0) {
    $item = find_all($testId);
    if ($item) {
        line('Item ' . $testId . ' encontrado (fonte: ' . ($item['_source'] ?? '?') . '): ' . ($item['item_name'] ?? '(sem nome)'), $isCli);
    } else {
        line('Item ' . $testId . ' NÃO encontrado em nenhum dos arquivos.', $isCli);
    }
} else {
    $hint = $isCli ? 'php iff/test_archive.php <type_id>' : '?id=<type_id>';
    line('Dica: passe um type_id para testar a busca completa (' . $hint . ').', $isCli);
}