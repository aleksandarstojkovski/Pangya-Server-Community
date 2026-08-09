<?php
/**
 * update_warehouse.php
 * -----------------------------------------------------------------------
 * Adiciona ou remove uma unidade de um item do armazém (pangya.pangya_item_warehouse).
 *
 * Como essa tabela não tem uma coluna de "quantidade", cada linha do
 * banco é uma unidade do item — a "quantidade" mostrada no painel é
 * apenas COUNT(*) agrupado por typeid (ver dashboard.php). Por isso:
 *   - "Adicionar" = INSERT de uma nova linha com aquele typeid.
 *   - "Remover"   = soft-delete (valid = 0) de UMA linha daquele typeid
 *                   (UPDATE TOP (1), para não remover o grupo inteiro).
 *
 * ATENÇÃO — SCHEMA INCOMPLETO:
 *   Só confirmamos as colunas [UID], [typeid] e [valid] nesta tabela.
 *   Se pangya.pangya_item_warehouse tiver outras colunas NOT NULL sem
 *   valor padrão (ex.: durabilidade, combinação de cores/gcode, bind
 *   flag, etc.), o INSERT abaixo vai falhar até você completá-lo com
 *   essas colunas. O erro exato fica em error_log() — ajuste o INSERT
 *   conforme a mensagem indicar.
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
$action = $_POST['action'] ?? '';
$typeid = filter_var($_POST['typeid'] ?? '', FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);

if ($typeid === false || $typeid === null) {
    setFlash('error', 'TypeId inválido.');
    header('Location: dashboard.php');
    exit;
}

try {
    $pdo = getConnection();

    if ($action === 'add') {
        // Ajuste esta lista de colunas caso sua tabela exija mais campos
        // obrigatórios (ver aviso no topo do arquivo).
        $stmt = $pdo->prepare(
            'INSERT INTO pangya.pangya_item_warehouse ([UID], [typeid], [valid])
             VALUES (?, ?, 1)'
        );
        $stmt->execute([$uid, $typeid]);
        setFlash('success', 'Item adicionado ao armazém.');

    } elseif ($action === 'remove') {
        // Remove (soft-delete) apenas UMA unidade desse typeid.
        $stmt = $pdo->prepare(
            'UPDATE TOP (1) pangya.pangya_item_warehouse
             SET [valid] = 0
             WHERE [UID] = ? AND [typeid] = ? AND [valid] = 1'
        );
        $stmt->execute([$uid, $typeid]);

        if ($stmt->rowCount() > 0) {
            setFlash('success', 'Item removido do armazém.');
        } else {
            setFlash('error', 'Nenhuma unidade desse item foi encontrada para remover.');
        }

    } else {
        setFlash('error', 'Ação desconhecida.');
    }
} catch (PDOException $e) {
    error_log('Erro ao atualizar armazém (' . $action . ', typeid=' . $typeid . '): ' . $e->getMessage());
    setFlash('error', 'Não foi possível salvar as alterações. Verifique se todos os campos obrigatórios da tabela foram preenchidos.');
}

header('Location: dashboard.php');
exit;
