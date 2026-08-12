<?php
session_start();

require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';

// Garante requisição via POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: /Donation/index.php');
    exit;
}

$isAjax = !empty($_SERVER['HTTP_X_REQUESTED_WITH']) && strtolower($_SERVER['HTTP_X_REQUESTED_WITH']) === 'xmlhttprequest';

// 1. Validação de Autenticação
if (!isset($_SESSION['uid']) || empty($_SESSION['uid'])) {
    if ($isAjax) {
        header('Content-Type: application/json');
        echo json_encode(['status' => 'error', 'message' => 'Sessão expirada. Faça login novamente.']);
        exit;
    }
    header('Location: /login.php');
    exit;
}

// 2. Validação CSRF
$csrfToken = $_POST['csrf_token'] ?? '';
if (empty($csrfToken) || !hash_equals($_SESSION['csrf_token'] ?? '', $csrfToken)) {
    if ($isAjax) {
        header('Content-Type: application/json');
        echo json_encode(['status' => 'error', 'message' => 'Token de segurança inválido. Recarregue a página.']);
        exit;
    }
    die('Erro de validação de segurança (CSRF).');
}

$uid = (int) $_SESSION['uid'];

// Captura e limpa o código enviado (suporta 'epin_code' do HTML fornecido ou 'epin')
$rawEpin = $_POST['epin_code'] ?? $_POST['epin'] ?? '';
$epin = strtoupper(trim(filter_var($rawEpin, FILTER_SANITIZE_SPECIAL_CHARS)));

if (empty($epin)) {
    if ($isAjax) {
        header('Content-Type: application/json');
        echo json_encode(['status' => 'error', 'message' => 'Por favor, informe o código do EPIN.']);
        exit;
    }
    die('Código EPIN não fornecido.');
}

try {
    $pdo = getConnection();

    // 1. Verifica no banco de dados se o EPIN é válido e ativo
    $sqlCheck = "SELECT 
                    a.`index`,
                    a.`qntd`,
                    b.`gross_amount`
                 FROM pangya.pangya_donation_epin a 
                 INNER JOIN pangya.pangya_donation_new b ON a.`UID` = b.`UID` AND a.`index` = b.`epin_id`
                 WHERE a.`epin` = :epin 
                   AND a.`valid` = 1 
                   AND a.`retrive_uid` IS NULL 
                   AND b.`status` IN (3, 4)";

    $stmtCheck = $pdo->prepare($sqlCheck);
    $stmtCheck->bindValue(':epin', $epin, PDO::PARAM_STR);
    $stmtCheck->execute();

    $epinData = $stmtCheck->fetch(PDO::FETCH_ASSOC);

    if (!$epinData) {
        $msg = 'Código EPIN inválido, expirado, já resgatado ou com pagamento pendente.';
        if ($isAjax) {
            header('Content-Type: application/json');
            echo json_encode(['status' => 'error', 'message' => $msg]);
            exit;
        }
        die($msg);
    }

    // 2. Executa a procedure de resgate de pontos
    $stmtExchange = $pdo->prepare("CALL pangya.ProcExchangeCookiePointByEpin(:epin, :uid)");
    $stmtExchange->bindValue(':epin', $epin, PDO::PARAM_STR);
    $stmtExchange->bindValue(':uid', $uid, PDO::PARAM_INT);
    $stmtExchange->execute();
    $stmtExchange->closeCursor();

    // 3. Consulta o saldo atualizado de Cookies e Pangs
    $stmtBalance = $pdo->prepare("SELECT [Pang], [Cookie] FROM pangya.user_info WHERE [UID] = :uid");
    $stmtBalance->bindValue(':uid', $uid, PDO::PARAM_INT);
    $stmtBalance->execute();
    $updatedBalances = $stmtBalance->fetch(PDO::FETCH_ASSOC);

    $cookiesAdded = (int) ($epinData['qntd'] ?? 0);

    if ($isAjax) {
        header('Content-Type: application/json');
        echo json_encode([
            'status' => 'success',
            'message' => 'EPIN resgatado com sucesso!',
            'cookies_added' => $cookiesAdded,
            'new_cookie_balance' => (int) ($updatedBalances['Cookie'] ?? 0),
            'new_pang_balance' => (int) ($updatedBalances['Pang'] ?? 0)
        ]);
        exit;
    }

    // Retorno visual padrão via sessão para a página do Shop
    $_SESSION['flash_success'] = "EPIN resgatado com sucesso! Você recebeu " . number_format($cookiesAdded, 0, ',', '.') . " créditos.";
    header('Location: /Donation/index.php');
    exit;

} catch (PDOException $e) {
    error_log('Erro ao processar EPIN: ' . $e->getMessage());
    if ($isAjax) {
        header('Content-Type: application/json');
        echo json_encode(['status' => 'error', 'message' => 'Erro interno ao processar o resgate do EPIN.']);
        exit;
    }
    die('Ocorreu um erro ao processar o resgate do código.');
}