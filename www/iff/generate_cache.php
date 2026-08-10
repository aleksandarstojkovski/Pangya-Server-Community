<?php
// data_cache.php - Gerencia o cache e a obtenção de dados da galeria

include_once($_SERVER['DOCUMENT_ROOT'] . '/iff/find_iff_item.php'); 

use PangyaIFF\Parser\IFFArchive;
use PangyaIFF\Parser;

// --- CONFIGURAÇÕES DO CACHE ---
const CACHE_FILE = __DIR__ . '/../data/iff_cache.json'; 
const CACHE_LIFETIME_SECONDS = 3600; // 1 hora de validade

/**
 * Carrega a lista completa de itens IFF do cache ou a gera se necessário.
 *
 * @return array A lista completa de todos os itens IFF.
 */
function load_full_iff_list_from_cache(): array {
    // 1. Verifica a existência e validade do cache
    if (file_exists(CACHE_FILE) && (time() - filemtime(CACHE_FILE) < CACHE_LIFETIME_SECONDS)) {
        $json = file_get_contents(CACHE_FILE);
        $data = json_decode($json, true);
        if (is_array($data)) {
            $full_list = [];
            foreach ($data as $item) { 
                if (isset($item['type_id'])) {  
                    $full_list[] = $item;
                }
            }
            return $full_list;
        }
    }
    
    // 2. Cache inválido/inexistente: Gera a lista bruta lendo do IFFArchive (ZIP) ou disco
    $iff_files = [
        'Card.iff',
            'SetItem.iff',
            'Part.iff',
            'Item.iff',
			'Skin.iff',
			'ClubSet.iff',
			'Character.iff',
			'Caddie.iff',
			'AuxPart.iff',
			'Mascot.iff',
    ];

    $full_list = [];

    foreach ($iff_files as $filename) {
        $path = __DIR__ . '/../data/' . $filename;
        
        // Valida se existe no ZIP ou fisicamente no disco
        $hasInZip = IFFArchive::has($filename);
        $hasInDisk = file_exists($path);

        if (!$hasInZip && !$hasInDisk) {
            continue;
        }

        // --- Leitura do IFF ---
        $items_from_iff = Parser\list_iff_items($path);
        $iff_type = strtolower(pathinfo($filename, PATHINFO_FILENAME));  

        // --- ACÚMULO E FILTRAGEM ---
        foreach ($items_from_iff as $item) {  
            if (isset($item['type_id']) && $item['type_id'] > 0) {  
                $item['_source'] = $filename;
                $item['item_type'] = $iff_type;  
                $full_list[] = $item;
            }
        }
    }
    
    if (empty($full_list)) {
        return [];
    }

    // --- FUNÇÕES DE ENCODING ---
    function fix_encoding_recursive($data) {
        if (is_array($data)) {
            foreach ($data as $key => $value) {
                $data[$key] = fix_encoding_recursive($value);
            }
        } elseif (is_string($data)) {
            $enc = mb_detect_encoding($data, ['UTF-8', 'SJIS', 'EUC-KR', 'CP1252', 'ISO-8859-1'], true);
            if ($enc !== 'UTF-8') {
                $data = mb_convert_encoding($data, 'UTF-8', $enc ?: 'CP1252');
            }
            $data = preg_replace('/[^\PC\s]/u', '', $data);
        }
        return $data;
    }

    // 3. Sanitiza os dados antes do JSON
    $full_list = fix_encoding_recursive($full_list);

    $json_data = json_encode(array_values($full_list), JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);

    // 5. Verifica erros de codificação
    if ($json_data === false) {
        file_put_contents(__DIR__ . '/../data/iff_json_error_dump.txt', print_r($full_list, true));
        return [];
    }

    // --- GARANTINDO QUE O DIRETÓRIO EXISTA ---
    $cache_dir = dirname(CACHE_FILE);
    if (!is_dir($cache_dir)) {
        if (!mkdir($cache_dir, 0777, true)) {
            trigger_error("ERRO: Não foi possível criar o diretório de cache: " . $cache_dir, E_USER_ERROR);
            return [];
        }
    }

    if (file_put_contents(CACHE_FILE, $json_data) === false) {
        return [];
    }

    clearstatcache(true, CACHE_FILE);
    if (filesize(CACHE_FILE) <= 100) {
        return [];
    }

    return $full_list;
}

/**
 * Busca ultra-rápida (O(1)) por ID usando índice estático em memória RAM.
 */
function find_cache(int $search_id): array {
    static $indexed_list = null;

    // Converte a lista em uma Tabela Hash [type_id => item] apenas UMA vez por requisição
    if ($indexed_list === null) {
        $indexed_list = [];
        $full_list = load_full_iff_list_from_cache();

        foreach ($full_list as $item) {
            if (isset($item['type_id'])) {
                $indexed_list[(int)$item['type_id']] = $item;
            }
        }
    }

    // Busca O(1) instantânea
    if (isset($indexed_list[$search_id])) {
        $item = $indexed_list[$search_id];

        $icon = !empty($item['icon']) ? $item['icon'] : 'default';
        $source = !empty($item['_source']) ? $item['_source'] : '';
        $name = !empty($item['item_name']) ? $item['item_name'] : (!empty($item['name']) ? $item['name'] : ('Item #' . $search_id));

        return [
            'typeid'    => $search_id,
            'name'      => $name,
            '_source'   => $source,
            'item_name' => $name,
            'icon'      => $icon,
            'raw'       => $item
        ];
    }

    // Retorno padrão caso não encontre
    return [
        'typeid'    => $search_id,
        'name'      => 'Item #' . $search_id,
        '_source'   => '',
        'item_name' => 'Item #' . $search_id,
        'icon'      => 'default',
        'raw'       => []
    ];
}


function get_full_gallery_data(string $search_query = '', bool $is_search_active = false, string $filter_type = ''): array {
    
    // 1. Carrega a lista COMPLETA de itens A PARTIR DO CACHE (rápido)
    $filtered_list = load_full_iff_list_from_cache();  
    
    // 2. Filtra por TIPO
    $filter_type_lower = strtolower($filter_type);
    if (!empty($filter_type_lower) && $filter_type_lower !== 'all') {
        $filtered_list = array_filter($filtered_list, function($item) use ($filter_type_lower) {
            return ($item['item_type'] ?? '') === $filter_type_lower;
        });
    }

    // 3. Filtra por TERMO DE BUSCA
    if (!empty($search_query)) {
        $search_term_lower = strtolower($search_query);
        $filtered_list = array_values(array_filter($filtered_list, function($item) use ($search_query, $search_term_lower) {
            $name = strtolower($item['item_name'] ?? '');
            $id = (string)($item['type_id'] ?? '');
            
            return str_contains($name, $search_term_lower) || $id === $search_query;
        }));
    }

    // 4. Adiciona a descrição
    foreach ($filtered_list as $key => $item) {
        $filtered_list[$key]['description'] = 'Descrição não disponível (Desc.iff).';
    }
    
    return $filtered_list;
}
?>