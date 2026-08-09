<?php
/**
 * update_collection.php
 * -----------------------------------------------------------------------
 * Processa a edição de um item da "coleção" do jogador a partir dos
 * modais em dashboard.php: Personagem, Caddie ou Mascote.
 * O campo POST "type" decide qual tabela/colunas são atualizadas.
 *
 * Toda query usa "WHERE [UID] = ? AND [typeid] = ?" para garantir que o
 * usuário logado só consiga editar registros que pertencem a ele mesmo
 * (nunca confie apenas no typeid vindo do formulário).
 * -----------------------------------------------------------------------
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/functions.php';

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

$uid    = (int)$_SESSION['uid'];
$type   = $_POST['type'] ?? '';
$typeid = filter_var($_POST['typeid'] ?? '', FILTER_VALIDATE_INT);

if ($typeid === false || $typeid === null) {
    setFlash('error', 'TypeId inválido.');
    header('Location: dashboard.php');
    exit;
}

try {
    $pdo = getConnection();

    switch ($type) {
        case 'character':
            $mastery = filter_var($_POST['mastery'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 0]]);
            $hair    = filter_var($_POST['default_hair'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 0]]);
            $shirts  = filter_var($_POST['default_shirts'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 0]]);

            if ($mastery === false || $hair === false || $shirts === false) {
                setFlash('error', 'Valores inválidos para Mastery/cabelo/roupa.');
                break;
            }

            $stmt = $pdo->prepare(
                'UPDATE pangya.pangya_character_information
                 SET [Mastery] = ?, [default_hair] = ?, [default_shirts] = ?
                 WHERE [UID] = ? AND [typeid] = ?'
            );
            $stmt->execute([$mastery, $hair, $shirts, $uid, $typeid]);
            setFlash('success', 'Personagem atualizado com sucesso.');
            break;

        case 'caddie':
            $clevel   = filter_var($_POST['clevel'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 0]]);
            $exp      = filter_var($_POST['exp'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 0]]);
            $rentflag = isset($_POST['rentflag']) ? 1 : 0;

            if ($clevel === false || $exp === false) {
                setFlash('error', 'Valores inválidos para nível/exp do caddie.');
                break;
            }

            $stmt = $pdo->prepare(
                'UPDATE pangya.pangya_caddie_information
                 SET [cLevel] = ?, [Exp] = ?, [RentFlag] = ?
                 WHERE [UID] = ? AND [typeid] = ? AND [Valid] = 1'
            );
            $stmt->execute([$clevel, $exp, $rentflag, $uid, $typeid]);
            setFlash('success', 'Caddie atualizado com sucesso.');
            break;

        case 'mascot':
            $mlevel = filter_var($_POST['mlevel'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 0]]);
            $mexp   = filter_var($_POST['exp'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 0]]);

            if ($mlevel === false || $mexp === false) {
                setFlash('error', 'Valores inválidos para nível/exp do mascote.');
                break;
            }

            $stmt = $pdo->prepare(
                'UPDATE pangya.pangya_mascot_info
                 SET [mLevel] = ?, [mExp] = ?
                 WHERE [UID] = ? AND [typeid] = ? AND [Valid] = 1'
            );
            $stmt->execute([$mlevel, $mexp, $uid, $typeid]);
            setFlash('success', 'Mascote atualizado com sucesso.');
            break;

        default:
            setFlash('error', 'Tipo de edição desconhecido.');
    }
} catch (PDOException $e) {
    error_log('Erro ao atualizar coleção (' . $type . '): ' . $e->getMessage());
    setFlash('error', 'Não foi possível salvar as alterações. Tente novamente mais tarde.');
}

header('Location: dashboard.php');
exit;
