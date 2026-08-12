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

/**
 * -----------------------------------------------------------------------
 * OPCIONAL: driver sqlsrv/pdo_sqlsrv (recomendado para Unicode/Shift-JIS)
 * -----------------------------------------------------------------------
 * Preencha DB_SERVER e DB_DATABASE abaixo e instale a extensão
 * "pdo_sqlsrv" (Microsoft Drivers for PHP for SQL Server) para que o
 * site passe a usar esse driver automaticamente em vez do PDO_ODBC.
 * Deixe DB_SERVER em branco para continuar usando o DSN/ODBC atual.
 *
 * Exemplo: define('DB_SERVER', 'localhost\\SQLEXPRESS');
 */
define('DB_SERVER', 'localhost');      // ex.: 'localhost' ou 'NOME_DO_SERVIDOR\\INSTANCIA'
define('DB_DATABASE', 'pangya');

// Sessão precisa estar ativa em (quase) todas as páginas
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

require_once __DIR__ . '/../includes/i18n.php';
require_once __DIR__ . '/permissions.php';

/**
 * Retorna uma conexão PDO ativa.
 * Lança PDOException em caso de falha (tratar no chamador).
 *
 * -----------------------------------------------------------------------
 * SOBRE O PROBLEMA DE SHIFT-JIS / CARACTERES NÃO-LATINOS NÃO SEREM SALVOS
 * -----------------------------------------------------------------------
 * A opção 'CharSet=UTF-8;ClientCharset=UTF-8' que estava na string de
 * conexão NÃO é reconhecida pelo driver ODBC oficial da Microsoft
 * ("ODBC Driver 17/18 for SQL Server") nem pelo driver "SQL Server"
 * nativo do Windows — esses parâmetros são específicos do FreeTDS.
 * Ou seja, na prática ela era ignorada e não fazia nada.
 *
 * O motivo real é mais profundo: a extensão PDO_ODBC do PHP para Windows
 * é compilada usando as funções ANSI do ODBC (SQLDriverConnectA e afins),
 * e não as funções Unicode (SQLDriverConnectW). Isso significa que TODO
 * texto que passa por ela é convertido para o "code page" ANSI ativo no
 * Windows (ex.: CP1252 no Brasil) antes de ir para o banco — e o CP1252
 * não tem como representar caracteres japoneses/coreanos. O resultado é
 * "?" ou dados corrompidos, mesmo que a coluna no banco seja NVARCHAR e
 * mesmo com qualquer parâmetro de charset na connection string. Isso é
 * uma limitação conhecida do PDO_ODBC no Windows, não um bug do seu código.
 *
 * Duas formas de resolver de verdade:
 *
 *   1) [Recomendado] Trocar o driver de PDO_ODBC para PDO_SQLSRV
 *      (extensão oficial "Microsoft Drivers for PHP for SQL Server").
 *      Esse driver converte corretamente UTF-8 <-> UTF-16/NVARCHAR e
 *      aceita qualquer caractere Unicode, incluindo Shift-JIS/kanji.
 *      Preencha DB_SERVER/DB_DATABASE no topo deste arquivo e instale
 *      a extensão "pdo_sqlsrv" — o código abaixo passa a usá-la
 *      automaticamente quando disponível.
 *
 *   2) [Workaround, sem trocar driver] No Windows do servidor, ir em
 *      Painel de Controle > Região > Administrativo > Alterar
 *      configurações do sistema... > marcar "Beta: Usar Unicode UTF-8
 *      para suporte a idiomas em todo o mundo" e reiniciar o servidor.
 *      Isso faz o code page ANSI do Windows virar UTF-8, então a
 *      conversão que o PDO_ODBC faz por baixo dos panos deixa de
 *      truncar os caracteres. Funciona, mas pode ter efeitos colaterais
 *      em outros programas do servidor que dependam do code page antigo.
 *
 * Confirme também que a coluna [NICK] (e qualquer outro campo editável
 * que precise aceitar japonês/coreano) é NVARCHAR e não VARCHAR — VARCHAR
 * usa a collation/code page do banco e nunca vai guardar Shift-JIS
 * corretamente, independente do driver PHP usado.
 * -----------------------------------------------------------------------
 */
function getConnection(): PDO
{
    static $pdo = null;

    if ($pdo === null) {
        $pdoOptions = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ];

        // Usa PDO_SQLSRV automaticamente se DB_SERVER foi preenchido e a
        // extensão estiver instalada — é o caminho recomendado para
        // suporte correto a Unicode (ver comentário acima).
        if (DB_SERVER !== '' && in_array('sqlsrv', PDO::getAvailableDrivers(), true)) {
            $connStr = 'sqlsrv:Server=' . DB_SERVER . ';Database=' . DB_DATABASE . ';CharacterSet=UTF-8';

            try {
                $pdo = new PDO($connStr, DB_USER, DB_PASS, $pdoOptions);
                return $pdo;
            } catch (PDOException $e) {
                error_log('Falha na conexão com o banco (PDO_SQLSRV): ' . $e->getMessage());
                throw $e;
            }
        }

        // Caminho padrão: PDO_ODBC via System DSN (sujeito à limitação de
        // Unicode descrita acima para caracteres fora do code page ANSI).
        $connStr = 'odbc:DSN=' . DSN_NAME;

        try {
            $pdo = new PDO($connStr, DB_USER, DB_PASS, $pdoOptions);
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
