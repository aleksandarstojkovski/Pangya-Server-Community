<?php

final class AuditLogger
{
    public function __construct(private PDO $pdo)
    {
    }

    public function record(string $action, ?int $itemId, array $details = []): void
    {
        $statement = $this->pdo->prepare(
            'INSERT INTO pangya.web_audit_log ([actor_uid], [action], [item_id], [ip_address], [details])
             VALUES (?, ?, ?, ?, ?)'
        );

        $statement->execute([
            (int) ($_SESSION['uid'] ?? 0),
            $action,
            $itemId,
            getClientIp(),
            json_encode($details, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
        ]);
    }
}
