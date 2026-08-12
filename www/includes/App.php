<?php

final class App
{
    public static function clean(?string $value): string
    {
        return htmlspecialchars(trim($value ?? ''), ENT_QUOTES, 'UTF-8');
    }

    public static function isLoggedIn(): bool
    {
        return isset($_SESSION['uid']) && (int) $_SESSION['uid'] > 0;
    }

    public static function redirect(string $path): void
    {
        header('Location: ' . $path);
        exit;
    }

    public static function requireLogin(): void
    {
        if (!self::isLoggedIn()) {
            self::redirect('/Account/login.php');
        }
    }

    public static function isGameMaster(): bool
    {
        return self::hasCapability(CAPABILITY_GAME_MASTER);
    }

    public static function canEditItemsOnWeb(): bool
    {
        return self::hasCapability(CAPABILITY_GAME_MASTER)
            && self::hasCapability(CAPABILITY_WEB_ADMIN_EDIT)
            && !self::hasCapability(CAPABILITY_BLOCK_ITEM_SPAWN_GM);
    }

    public static function hasCapability(int $flag): bool
    {
        $capability = (int) ($_SESSION['capability'] ?? 0);

        return ($capability & $flag) === $flag;
    }

    public static function requireGameMaster(): void
    {
        self::requireLogin();

        if (!self::canEditItemsOnWeb()) {
            self::flash('error', t('access_denied'));
            self::redirect('/Account/dashboard.php');
        }
    }

    public static function flash(string $type, string $text): void
    {
        $_SESSION['flash'] = ['type' => $type, 'text' => $text];
    }

    public static function renderFlash(): void
    {
        if (empty($_SESSION['flash'])) {
            return;
        }

        $type = $_SESSION['flash']['type'] ?? 'info';
        $text = $_SESSION['flash']['text'] ?? '';
        $cssClass = $type === 'error' ? 'danger' : $type;

        echo '<div class="alert alert-' . htmlspecialchars($cssClass) . ' alert-dismissible fade show" role="alert">'
            . htmlspecialchars($text)
            . '<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Fechar"></button>'
            . '</div>';

        unset($_SESSION['flash']);
    }

    public static function csrfToken(): string
    {
        if (empty($_SESSION['csrf_token'])) {
            $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
        }

        return $_SESSION['csrf_token'];
    }

    public static function csrfValid(?string $token): bool
    {
        return isset($_SESSION['csrf_token'], $token)
            && hash_equals($_SESSION['csrf_token'], $token);
    }
}
