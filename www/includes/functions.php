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
