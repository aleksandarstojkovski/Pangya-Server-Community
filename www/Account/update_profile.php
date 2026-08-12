<?php
/**
 * update_profile.php
 * -----------------------------------------------------------------------
 * Recebe o POST do modal "Editar Perfil" em dashboard.php e atualiza
 * NICK / Sex em pangya.account para o usuário logado.
 *
 * IMPORTANTE sobre acentuação/Shift-JIS/Unicode:
 *   Este script já faz a parte que cabe ao PHP corretamente: recebe o
 *   formulário como UTF-8, valida com mb_strlen() (multibyte-safe) e
 *   envia o valor via parâmetro (bindValue) para a coluna NICK, que deve
 *   ser NVARCHAR no banco. Se mesmo assim caracteres fora do alfabeto
 *   latino (japonês, coreano, etc.) chegarem corrompidos ("?" ou lixo),
 *   o problema NÃO está aqui — está no driver PDO_ODBC (veja o
 *   comentário grande em config.php::getConnection()).
 * -----------------------------------------------------------------------
 */
require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';

requireLogin();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: dashboard.php');
    exit;
}

if (!csrfValid($_POST['csrf_token'] ?? null)) {
    setFlash('error', 'Sessão expirada. Tente novamente.');
    header('Location: dashboard.php');
    exit;
}

$uid  = (int)$_SESSION['uid'];
$nick = trim($_POST['nick'] ?? '');
$sexo = filter_var($_POST['sexo'] ?? '', FILTER_VALIDATE_INT);

$errors = [];

// mb_strlen (e não strlen) porque o Nick pode conter caracteres
// multibyte (acentos, katakana, hangul, etc.) — strlen contaria bytes,
// não caracteres, e rejeitaria nicks válidos ou aceitaria nicks longos
// demais para a coluna do banco.
if ($nick === '' || mb_strlen($nick, 'UTF-8') > 20) {
    $errors[] = 'Nick deve ter entre 1 e 20 caracteres.';
}
if (!in_array($sexo, [1, 2], true)) {
    $errors[] = 'Sexo inválido.';
}

if (!empty($errors)) {
    setFlash('error', implode(' ', $errors));
    header('Location: dashboard.php');
    exit;
}

try {
    $pdo = getConnection();

    // Garante que outro UID não esteja usando esse Nick
    $check = $pdo->prepare('SELECT COUNT(*) FROM pangya.account WHERE [NICK] = ? AND [UID] <> ?');
    $check->bindValue(1, $nick, PDO::PARAM_STR);
    $check->bindValue(2, $uid, PDO::PARAM_INT);
    $check->execute();

    if ((int)$check->fetchColumn() > 0) {
        setFlash('error', 'Esse Nick já está em uso por outra conta.');
        header('Location: dashboard.php');
        exit;
    }

    $stmt = $pdo->prepare('UPDATE pangya.account SET [NICK] = ?, [Sex] = ? WHERE [UID] = ?');
    $stmt->bindValue(1, $nick, PDO::PARAM_STR);
    $stmt->bindValue(2, $sexo, PDO::PARAM_INT);
    $stmt->bindValue(3, $uid, PDO::PARAM_INT);
    $stmt->execute();

    setFlash('success', 'Perfil atualizado com sucesso.');
} catch (PDOException $e) {
    error_log('Erro ao atualizar perfil: ' . $e->getMessage());
    setFlash('error', 'Não foi possível salvar as alterações. Tente novamente mais tarde.');
}

header('Location: dashboard.php');
exit;
