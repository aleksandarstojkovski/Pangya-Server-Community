<?php

require_once __DIR__ . '/../includes/IffCatalog.php';

final class ShopCatalog
{
    public function search(string $term, string $category = ''): array
    {
        $catalog = new IffCatalog();

        $items = $term === ''
            ? $catalog->list($category)
            : $catalog->search($term, 0);

        if ($category === '') {
            return $items;
        }

        return array_values(array_filter($items, static function (array $item) use ($category): bool {
            return strcasecmp((string) $item['category'], $category) === 0;
        }));
    }
}
