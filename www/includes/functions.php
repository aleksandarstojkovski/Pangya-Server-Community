<?php

require_once __DIR__ . '/App.php';

function clean(?string $value): string
{
    return App::clean($value);
}

function isLoggedIn(): bool
{
    return App::isLoggedIn();
}

function redirectIfLoggedIn(): void
{
    if (App::isLoggedIn()) {
        App::redirect('dashboard.php');
    }
}

function requireLogin(): void
{
    App::requireLogin();
}

function flashMessage(): void
{
    App::renderFlash();
}

function setFlash(string $type, string $text): void
{
    App::flash($type, $text);
}

function csrfToken(): string
{
    return App::csrfToken();
}

function csrfValid(?string $token): bool
{
    return App::csrfValid($token);
}

function itemIconTag(int $typeid, int $size = 40): string
{
    $src = 'assets/img/items/' . $typeid . '.png';

    return '<span class="item-icon" style="width:' . $size . 'px;height:' . $size . 'px;">'
        . '<i class="bi bi-box-seam item-icon-fallback"></i>'
        . '<img src="' . htmlspecialchars($src) . '" alt="Item ' . $typeid . '" loading="lazy" onerror="this.style.display=\'none\'">'
        . '</span>';
}
