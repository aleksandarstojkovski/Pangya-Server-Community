<?php
//desenvolvimento Luiz MK, luizinrc@hotmail.com
namespace PangyaIFF\Parser;

require_once __DIR__ . '/IFFCommon.php';
require_once __DIR__ . '/IFFHeader.php';
require_once __DIR__ . '/IFFDesc.php';
require_once __DIR__ . '/utils.php';
   

/**
 * Procura uma descrição dentro do arquivo Desc.iff pelo type_id.
 * 
 * Estrutura:
 * - Header: 8 bytes (ushort count, ushort reserved, uint version)
 * - Cada item: 516 bytes (4 de type_id + 512 de string/descrição)
 * 
 * @param int $target_id ID que será procurado.
 * @return array|null Retorna o item encontrado ou null se não existir.
 */  
 function find_iff_desc(int $target_id, string $iff_file_path): ?array {
    global $IFF_HEADER_FORMAT, $IFF_DESC_FORMAT;

    $entry_name = basename($iff_file_path);
    $archive_path = \PangyaIFF\Parser\IFFArchive::getArchivePath();
    
    $binary_data = '';
    
    $zip = new \ZipArchive();
    if ($zip->open($archive_path) === TRUE) {
        for ($i = 0; $i < $zip->numFiles; $i++) {
            $name = $zip->getNameIndex($i);
            if (strcasecmp(basename($name), $entry_name) === 0) {
                $binary_data = $zip->getFromIndex($i);
                break;
            }
        }
        $zip->close();
    }

    if ($binary_data === false || $binary_data === '') {
        if (is_file($iff_file_path)) {
            $binary_data = file_get_contents($iff_file_path);
        }
    }

    if (empty($binary_data)) {
        echo "[DEBUG find_iff_desc] Arquivo Desc.iff não pudo ser lido.<br>\n";
        return null;
    }

    $offset = 0;
    $data_length = strlen($binary_data);

    if ($data_length < IFF_HEADER_SIZE) return null;
    
    $header_raw = substr($binary_data, 0, IFF_HEADER_SIZE);
    $offset += IFF_HEADER_SIZE;
    
    $header = unpack($IFF_HEADER_FORMAT, $header_raw);
    $count = $header['count'] ?? 0;
    
    echo "[DEBUG find_iff_desc] Total de registros no Desc.iff: {$count} | Procurando ID: {$target_id}<br>\n";

    if ($count <= 0) return null;

     $remaining_data_size = $data_length - IFF_HEADER_SIZE;
    if ($count <= 0 || $remaining_data_size % $count !== 0) {
        return null;
    }

    $real_item_size = intdiv($remaining_data_size, $count);
    
    // Define o formato dinamicamente com base no tamanho real encontrado (ex: 4 bytes para o ID + o restante para a string info)
    $string_size = $real_item_size - 4;
    $IFF_DESC_FORMAT = "Vtype_id/a{$string_size}info";

    // Testa os primeiros 5 registros para você ver o que está vindo no binário
    $debug_limit = 5;

    for ($i = 0; $i < $count; $i++) {
        if ($offset + IFF_DESC_SIZE > $data_length) {
            break;
        }

        $desc_raw = substr($binary_data, $offset, IFF_DESC_SIZE);
        $offset += IFF_DESC_SIZE;

        $item = unpack($IFF_DESC_FORMAT, $desc_raw);
        $type_id = isset($item['type_id']) ? (int)$item['type_id'] : 0;

        if ($i < $debug_limit) {
            echo "[DEBUG Registro #{$i}] Lido type_id: {$type_id}<br>\n";
        }

        if ($type_id === $target_id) {
            echo "[DEBUG find_iff_desc] SUCESSO! ID {$target_id} encontrado na posição {$i}.<br>\n";
            if (isset($item['info']) && is_string($item['info'])) {
                $item['info'] = trim_nulls($item['info']);
            }
            return $item;
        }
    }

    echo "[DEBUG find_iff_desc] ID {$target_id} percorrido, mas não encontrado.<br>\n";
    return null;
}

 function find_iff_item(int $target_id, string $iff_file_path): ?array {
    global $IFF_HEADER_FORMAT, $IFF_COMMON_FORMAT;

    $entry_name = basename($iff_file_path);
    $archive_path = IFFArchive::getArchivePath();
    
    $binary_data = '';
    
    // Tenta carregar o arquivo de dentro do ZIP mapeando possíveis caminhos internos
    $zip = new \ZipArchive();
    if ($zip->open($archive_path) === TRUE) {
        // Tenta buscar pelo nome exato ou procurando em qualquer subpasta do ZIP
        $found_index = false;
        for ($i = 0; $i < $zip->numFiles; $i++) {
            $name = $zip->getNameIndex($i);
            if (strcasecmp(basename($name), $entry_name) === 0) {
                $binary_data = $zip->getFromIndex($i);
                $found_index = true;
                break;
            }
        }
        $zip->close();
    }

    // Fallback caso não esteja no ZIP: tenta ler o arquivo físico solto no disco
    if ($binary_data === false || $binary_data === '') {
        if (is_file($iff_file_path)) {
            $binary_data = file_get_contents($iff_file_path);
        }
    }

    if (empty($binary_data)) {
        return null;
    }

    // Restante da sua lógica de leitura binária continua igual...
    $offset = 0;
    $data_length = strlen($binary_data);

    if ($data_length < IFF_HEADER_SIZE) return null;
    
    $header_raw = substr($binary_data, 0, IFF_HEADER_SIZE);
    $offset += IFF_HEADER_SIZE;
    
    $header = unpack($IFF_HEADER_FORMAT, $header_raw);
    $count = $header['count'] ?? 0;
    if ($count <= 0) return null;

    $remaining_data_size = $data_length - IFF_HEADER_SIZE;
    if ($remaining_data_size <= 0 || $remaining_data_size % $count !== 0) {
        return null;
    }

    $real_item_size = intdiv($remaining_data_size, $count);
    if ($real_item_size < IFF_COMMON_SIZE) return null;

    $specific_size = $real_item_size - IFF_COMMON_SIZE;

    for ($i = 0; $i < $count; $i++) {
        if ($offset + IFF_COMMON_SIZE > $data_length) {
            break;
        }

        $common_raw = substr($binary_data, $offset, IFF_COMMON_SIZE);
        $offset += IFF_COMMON_SIZE;

        $item = unpack($IFF_COMMON_FORMAT, $common_raw);
        $type_id = isset($item['type_id']) ? (int)$item['type_id'] : 0;

        $specific_raw = '';
        if ($specific_size > 0) {
            if ($offset + $specific_size > $data_length) {
                break;
            }
            $specific_raw = substr($binary_data, $offset, $specific_size);
            $offset += $specific_size;
        }

        if ($type_id === $target_id) {
            foreach ($item as $k => $v) {
                if (is_string($v)) {
                    $item[$k] = trim_nulls($v);
                }
            }
            $item['specific_raw'] = $specific_raw;
            return $item;
        }
    }

    return null;
}

/**
 * Lista todos os itens do IFF retornando array de arrays simples (type_id, item_name, icon)
 */
function list_iff_items(string $iff_file_path): array {
    global $IFF_HEADER_FORMAT, $IFF_COMMON_FORMAT;

    $entry_name = basename($iff_file_path);
    $archive_path = IFFArchive::getArchivePath();
    $binary_data = '';
    
    // Tenta carregar o arquivo inteiro de dentro do ZIP direto para a memória
$zip = new \ZipArchive();
    if ($zip->open($archive_path) === TRUE) {
        $binary_data = $zip->getFromName($entry_name);
        $zip->close();
    }

    // Fallback: se não estiver no ZIP, tenta ler o arquivo físico do disco
    if ($binary_data === false || $binary_data === '') {
        if (is_file($iff_file_path)) {
            $binary_data = file_get_contents($iff_file_path);
        }
    }

    if (empty($binary_data)) {
        return [];
    }

    $offset = 0;
    $data_length = strlen($binary_data);

    // Lê o header (8 bytes)
    if ($data_length < IFF_HEADER_SIZE) return [];
    
    $header_raw = substr($binary_data, 0, IFF_HEADER_SIZE);
    $offset += IFF_HEADER_SIZE;
    
    $header = unpack($IFF_HEADER_FORMAT, $header_raw);
    $count = $header['count'] ?? 0;
    if ($count <= 0) return [];

    $remaining_data_size = $data_length - IFF_HEADER_SIZE;
    if ($remaining_data_size <= 0 || $remaining_data_size % $count !== 0) {
        return [];
    }

    $real_item_size = intdiv($remaining_data_size, $count);
    if ($real_item_size < IFF_COMMON_SIZE) return [];

    $specific_size = $real_item_size - IFF_COMMON_SIZE;
    $out = [];

    // Itera diretamente sobre a string binária em memória
    for ($i = 0; $i < $count; $i++) {
        if ($offset + IFF_COMMON_SIZE > $data_length) {
            break;
        }

        $common_raw = substr($binary_data, $offset, IFF_COMMON_SIZE);
        $offset += IFF_COMMON_SIZE;

        $item = unpack($IFF_COMMON_FORMAT, $common_raw);

        // Pula o bloco specific se houver
        if ($specific_size > 0) {
            if ($offset + $specific_size > $data_length) {
                break;
            }
            $offset += $specific_size;
        }

        foreach ($item as $k => $v) {
            if (is_string($v)) {
                $item[$k] = trim_nulls($v);
            }
        }
        
        $item['type_id'] = (int)($item['type_id'] ?? 0);
        $out[] = $item; 
    }

    return $out;
}
function list_desc_iff_items(): array {
    $iff_file_path = __DIR__ . '/../data/Desc.iff';
    $entry_name = 'Desc.iff';
    $archive_path = IFFArchive::getArchivePath();

    global $IFF_HEADER_FORMAT, $IFF_DESC_FORMAT;

    $binary_data = '';
    $zip = new \ZipArchive();
    if ($zip->open($archive_path) === TRUE) {
        for ($i = 0; $i < $zip->numFiles; $i++) {
            $name = $zip->getNameIndex($i);
            if (strcasecmp(basename($name), $entry_name) === 0) {
                $binary_data = $zip->getFromIndex($i);
                break;
            }
        }
        $zip->close();
    }

    if ($binary_data === false || $binary_data === '') {
        if (is_file($iff_file_path)) {
            $binary_data = file_get_contents($iff_file_path);
        }
    }

    if (empty($binary_data)) return [];

    $data_length = strlen($binary_data);
    if ($data_length < IFF_HEADER_SIZE) return [];

    $header_raw = substr($binary_data, 0, IFF_HEADER_SIZE);
    $header = unpack($IFF_HEADER_FORMAT, $header_raw);
    $count = $header['count'] ?? 0;
    if ($count <= 0) return [];

    $remaining_size = $data_length - IFF_HEADER_SIZE;
    if ($remaining_size % $count !== 0) return [];

    $real_item_size = intdiv($remaining_size, $count);
    if ($real_item_size < IFF_DESC_SIZE) return [];

    $out = [];
    $offset = IFF_HEADER_SIZE;

    for ($i = 0; $i < $count; $i++) {
        if ($offset + IFF_DESC_SIZE > $data_length) break;

        $DESC_raw = substr($binary_data, $offset, IFF_DESC_SIZE);
        $offset += $real_item_size;

        $item = unpack($IFF_DESC_FORMAT, $DESC_raw);
        if ($item === false) continue;

        foreach ($item as $k => $v) {
            if (is_string($v)) {
                $item[$k] = trim_nulls($v);
            }
        }
        $item['type_id'] = (int)($item['type_id'] ?? 0);

        $out[] = $item;
    }

    return $out;
}