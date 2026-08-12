<?php

final class WarehouseService
{
    public function __construct(private PDO $pdo, private IffCatalog $catalog)
    {
    }

    public function add(int $uid, int $typeId): string
    {
        $item = $this->catalog->find($typeId);
        if ($item === null) {
            throw new InvalidArgumentException('O item informado não existe no catálogo IFF.');
        }

        $stmt = $this->pdo->prepare(
            'INSERT INTO pangya.pangya_item_warehouse ([UID], [typeid], [valid]) VALUES (?, ?, 1)'
        );
        $stmt->execute([$uid, $typeId]);

        return (string) ($item['item_name'] ?? $item['name'] ?? ('Item #' . $typeId));
    }

    public function removeByItemId(int $uid, int $itemId): bool
    {
        $stmt = $this->pdo->prepare(
            'UPDATE pangya.pangya_item_warehouse
             SET [valid] = 0
             WHERE [UID] = ? AND [item_id] = ? AND [valid] = 1'
        );
        $stmt->execute([$uid, $itemId]);

        return $stmt->rowCount() > 0;
    }
}
