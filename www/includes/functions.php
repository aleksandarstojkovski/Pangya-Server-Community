<?php
/**
 * includes/functions.php
 * -----------------------------------------------------------------------
 * Funções auxiliares usadas em várias páginas do site.
 * -----------------------------------------------------------------------
 */

/**
 * Sanitiza uma string simples vinda de $_POST/$_GET.
 */
function clean(?string $value): string
{
    return htmlspecialchars(trim($value ?? ''), ENT_QUOTES, 'UTF-8');
}

/**
 * Verifica se existe um usuário autenticado na sessão.
 */
function isLoggedIn(): bool
{
    return isset($_SESSION['uid']) && (int)$_SESSION['uid'] > 0;
}

function redirectIfLoggedIn(): void
{
    if (isLoggedIn()) {
        header('Location: dashboard.php');
        exit;
    }
}

/**
 * Força autenticação: redireciona para login.php se não estiver logado.
 */
function requireLogin(): void
{
    if (!isLoggedIn()) {
        header('Location: login.php');
        exit;
    }
}

/**
 * Exibe uma mensagem "flash" armazenada na sessão (sucesso/erro) e a remove.
 */
function flashMessage(): void
{
    if (!empty($_SESSION['flash'])) {
        $type = $_SESSION['flash']['type'] ?? 'info';
        $text = $_SESSION['flash']['text'] ?? '';
        $cssClass = $type === 'error' ? 'danger' : $type;

        echo '<div class="alert alert-' . htmlspecialchars($cssClass) . ' alert-dismissible fade show" role="alert">'
            . htmlspecialchars($text)
            . '<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Fechar"></button>'
            . '</div>';

        unset($_SESSION['flash']);
    }
}

/**
 * Define uma mensagem flash a ser exibida na próxima página carregada.
 */
function setFlash(string $type, string $text): void
{
    $_SESSION['flash'] = ['type' => $type, 'text' => $text];
}

/**
 * Garante que exista um token CSRF na sessão e o retorna.
 * Use no <form> (campo hidden) e valide no script que recebe o POST.
 */
function csrfToken(): string
{
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf_token'];
}

/**
 * Valida o token CSRF enviado por um POST contra o da sessão.
 */
function csrfValid(?string $token): bool
{
    return isset($_SESSION['csrf_token'], $token) && hash_equals($_SESSION['csrf_token'], $token);
}

/**
 * Renderiza o ícone de um item pelo TypeId.
 *
 * Convenção de arquivos: assets/img/items/{typeid}.png
 * Se a imagem daquele TypeId ainda não existir no servidor, mostra
 * automaticamente um ícone genérico (bi-box-seam) no lugar — não gera
 * erro/imagem quebrada. Basta ir soltando os PNGs em assets/img/items/
 * conforme forem extraídos do IFF/PAK que os ícones corretos aparecem
 * sozinhos, sem precisar mexer no código.
 */
function itemIconTag(int $typeid, int $size = 40): string
{
    $src = 'assets/img/items/' . $typeid . '.png';
    return '<span class="item-icon" style="width:' . $size . 'px;height:' . $size . 'px;">'
        . '<i class="bi bi-box-seam item-icon-fallback"></i>'
        . '<img src="' . htmlspecialchars($src) . '" alt="Item ' . $typeid . '" '
        . 'loading="lazy" onerror="this.style.display=\'none\'">'
        . '</span>';
}
