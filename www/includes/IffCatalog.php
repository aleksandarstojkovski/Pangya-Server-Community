<?php

require_once __DIR__ . '/../iff/generate_cache.php';

final class IffCatalog
{
    public function find(int $typeId): ?array
    {
        $item = find_cache($typeId);

        return empty($item['raw']) ? null : $item;
    }

    public function search(string $query, int $limit = 20): array
    {
        $query = trim($query);
        if ($query === '') {
            return [];
        }

        $matches = [];
        foreach (load_full_iff_list_from_cache() as $item) {
            $typeId = (int) ($item['type_id'] ?? 0);
            $name = (string) ($item['item_name'] ?? $item['name'] ?? '');

            if ($typeId > 0 && (str_contains((string) $typeId, $query) || stripos($name, $query) !== false)) {
                $matches[] = [
                    'typeid' => $typeId,
                    'name' => $name !== '' ? $name : 'Item #' . $typeId,
                    'icon' => (string) ($item['icon'] ?? 'default'),
                ];
            }

            if (count($matches) >= $limit) {
                break;
            }
        }

        return $matches;
    }
}
