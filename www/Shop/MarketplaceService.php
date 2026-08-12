<?php

final class MarketplaceService
{
    public function __construct(private PDO $pdo)
    {
    }

    public function createListing(int $sellerUid, int $itemId, int $price, string $currency): void
    {
        $currency = $this->currencyColumn($currency);
        $this->pdo->beginTransaction();

        try {
            $item = $this->pdo->prepare(
                'SELECT [typeid] FROM pangya.pangya_item_warehouse WITH (UPDLOCK, HOLDLOCK)
                 WHERE [UID] = ? AND [item_id] = ? AND [valid] = 1'
            );
            $item->execute([$sellerUid, $itemId]);
            $row = $item->fetch();
            if (!$row) {
                throw new RuntimeException('O item informado não está disponível no seu armazém.');
            }

            $statement = $this->pdo->prepare(
                'INSERT INTO pangya.web_marketplace_listing ([seller_uid], [item_id], [typeid], [price], [currency]) VALUES (?, ?, ?, ?, ?)'
            );
            $statement->execute([$sellerUid, $itemId, (int) $row['typeid'], $price, $currency]);
            $this->pdo->commit();
        } catch (Throwable $exception) {
            if ($this->pdo->inTransaction()) {
                $this->pdo->rollBack();
            }

            throw $exception;
        }
    }

    public function buy(int $buyerUid, int $listingId): void
    {
        $this->pdo->beginTransaction();

        try {
            $listing = $this->pdo->prepare(
                'SELECT [listing_id], [seller_uid], [item_id], [typeid], [price], [currency]
                 FROM pangya.web_marketplace_listing WITH (UPDLOCK, HOLDLOCK)
                 WHERE [listing_id] = ? AND [status] = ?'
            );
            $listing->execute([$listingId, 'active']);
            $row = $listing->fetch();
            if (!$row || (int) $row['seller_uid'] === $buyerUid) {
                throw new RuntimeException('Esta oferta não está disponível.');
            }

            $currency = $this->currencyColumn((string) $row['currency']);
            $price = (int) $row['price'];
            $debit = $this->pdo->prepare(
                "UPDATE pangya.user_info
                 SET [$currency] = [$currency] - ?
                 WHERE [UID] = ? AND [$currency] >= ?"
            );
            $debit->execute([$price, $buyerUid, $price]);
            if ($debit->rowCount() !== 1) {
                throw new RuntimeException('Saldo insuficiente.');
            }

            $credit = $this->pdo->prepare(
                "UPDATE pangya.user_info SET [$currency] = [$currency] + ? WHERE [UID] = ?"
            );
            $credit->execute([$price, (int) $row['seller_uid']]);

            $move = $this->pdo->prepare(
                'UPDATE pangya.pangya_item_warehouse
                 SET [UID] = ?
                 WHERE [UID] = ? AND [item_id] = ? AND [valid] = 1'
            );
            $move->execute([$buyerUid, (int) $row['seller_uid'], (int) $row['item_id']]);
            if ($move->rowCount() !== 1) {
                throw new RuntimeException('O item não está mais disponível.');
            }

            $finish = $this->pdo->prepare(
                'UPDATE pangya.web_marketplace_listing
                 SET [status] = ?, [buyer_uid] = ?, [sold_at] = SYSUTCDATETIME()
                 WHERE [listing_id] = ?'
            );
            $finish->execute(['sold', $buyerUid, $listingId]);
            $transaction = $this->pdo->prepare(
                'INSERT INTO pangya.web_shop_transaction
                    ([buyer_uid], [seller_uid], [item_id], [typeid], [amount], [currency], [kind])
                 VALUES (?, ?, ?, ?, ?, ?, ?)'
            );
            $transaction->execute([$buyerUid, (int) $row['seller_uid'], (int) $row['item_id'], (int) $row['typeid'], $price, $currency, 'marketplace']);
            $this->pdo->commit();
        } catch (Throwable $exception) {
            if ($this->pdo->inTransaction()) {
                $this->pdo->rollBack();
            }

            throw $exception;
        }
    }

    private function currencyColumn(string $currency): string
    {
        if (!in_array($currency, ['Pang', 'Cookie'], true)) {
            throw new InvalidArgumentException('Moeda inválida.');
        }

        return $currency;
    }
}
