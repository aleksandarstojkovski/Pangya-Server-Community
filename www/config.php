<?php
/**
 * config.php
 * -----------------------------------------------------------------------
 * Arquivo central de conexão com o banco de dados SQL Server através de
 * uma fonte de dados ODBC do sistema (System DSN), configurada previamente
 * no Painel de Controle do Windows / Servidor
 * (Ferramentas Administrativas > Fontes de Dados ODBC (64 bits)).
 *
 * IMPORTANTE:
 *  - Se o DSN já tiver usuário/senha do SQL Server configurados, você
 *    pode deixar UID/PWD do PDO em branco. Caso o DSN use autenticação
 *    do Windows, também não é necessário enviar UID/PWD aqui.
 * -----------------------------------------------------------------------
 */

// Nome do System DSN configurado no servidor
define('DSN_NAME', 'pangya');

// Credenciais do SQL Server (deixe em branco se o DSN já as define)
define('DB_USER', 'sa');
define('DB_PASS', '@pangya');

// Sessão precisa estar ativa em (quase) todas as páginas
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

require_once __DIR__ . '/includes/i18n.php';

/**
 * Retorna uma conexão PDO ativa usando o driver PDO_ODBC.
 * Lança PDOException em caso de falha (tratar no chamador).
 */
function getConnection(): PDO
{
    static $pdo = null;

    if ($pdo === null) {
        // A opção 'CharSet=UTF-8' na string de conexão força o driver ODBC a transmitir em UTF-8
        $connStr = 'odbc:DSN=' . DSN_NAME . ';CharSet=UTF-8;ClientCharset=UTF-8';

        try {
            $pdo = new PDO($connStr, DB_USER, DB_PASS, [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            ]);
        } catch (PDOException $e) {
            error_log('Falha na conexão com o banco (System DSN): ' . $e->getMessage());
            throw $e;
        }
    }

    return $pdo;
}

/**
 * Captura o IP real do cliente, considerando proxies/load balancers comuns.
 */
function getClientIp(): string
{
    $headers = ['HTTP_CF_CONNECTING_IP', 'HTTP_X_FORWARDED_FOR', 'HTTP_CLIENT_IP', 'REMOTE_ADDR'];

    foreach ($headers as $header) {
        if (!empty($_SERVER[$header])) {
            $ipList = explode(',', $_SERVER[$header]);
            $ip = trim($ipList[0]);
            if (filter_var($ip, FILTER_VALIDATE_IP)) {
                return substr($ip, 0, 20); // respeita NVARCHAR(20) do banco
            }
        }
    }

    return '0.0.0.0';
}

/**
 * Gera o hash de senha no formato esperado pelas procedures
 * (MD5 em maiúsculas).
 */
function hashPassword(string $plainPassword): string
{
    return strtoupper(md5($plainPassword));
}
