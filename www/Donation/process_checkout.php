<?php
session_start();

require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';

// Garante que a requisição seja via POST
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

// 2. Validação do Token CSRF
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
$paymentMethod = filter_input(INPUT_POST, 'payment_method', FILTER_SANITIZE_SPECIAL_CHARS) ?? 'mercadopago';
$packageId = filter_input(INPUT_POST, 'package_id', FILTER_SANITIZE_SPECIAL_CHARS);
$amountInput = filter_input(INPUT_POST, 'amount', FILTER_VALIDATE_FLOAT);

// Mapeamento dos pacotes da interface (Garante a integridade do preço no servidor)
$packagesMap = [
    'cookie_10'  => ['price' => 10.00,  'currency' => 'Cookie', 'amount' => 100],
    'cookie_25'  => ['price' => 25.00,  'currency' => 'Cookie', 'amount' => 270],
    'cookie_50'  => ['price' => 50.00,  'currency' => 'Cookie', 'amount' => 560],
    'cookie_100' => ['price' => 100.00, 'currency' => 'Cookie', 'amount' => 1200],
    'pang_10'    => ['price' => 10.00,  'currency' => 'Pang',   'amount' => 50000],
    'pang_25'    => ['price' => 25.00,  'currency' => 'Pang',   'amount' => 140000],
    'pang_50'    => ['price' => 50.00,  'currency' => 'Pang',   'amount' => 300000],
    'pang_100'   => ['price' => 100.00, 'currency' => 'Pang',   'amount' => 700000],
];

$finalPrice = 0.00;

if ($packageId && isset($packagesMap[$packageId])) {
    $finalPrice = $packagesMap[$packageId]['price'];
} elseif ($amountInput && $amountInput > 0) {
    $finalPrice = (float) $amountInput;
} else {
    if ($isAjax) {
        header('Content-Type: application/json');
        echo json_encode(['status' => 'error', 'message' => 'Pacote ou valor de doação inválido.']);
        exit;
    }
    die('Pacote de doação inválido.');
}

try {
    $pdo = getConnection();

    // Registra a intenção de doação via Stored Procedure
    $stmt = $pdo->prepare("CALL pangya.ProcInsertDonationNew(:uid, :amount, :type, :plataforma, @out_donation_id)");
    $stmt->bindValue(':uid', $uid, PDO::PARAM_INT);
    $stmt->bindValue(':amount', $finalPrice);
    $stmt->bindValue(':type', 1, PDO::PARAM_INT);
    $stmt->bindValue(':plataforma', $paymentMethod, PDO::PARAM_STR);
    $stmt->execute();
    $stmt->closeCursor();

    // Recupera o ID gerado da transação
    $donationId = $pdo->query("SELECT @out_donation_id AS donation_id")->fetchColumn();

    if (!$donationId) {
        throw new Exception("Não foi possível gerar a ordem de pagamento no banco de dados.");
    }

    // Integração Mercado Pago (Exemplo de geração de link/preference)
    // Aqui você conecta com a API SDK do Mercado Pago ou direciona para seu fluxo
    $redirectUrl = "/Donation/mercadopago_gateway.php?donation_id=" . $donationId;

    if ($isAjax) {
        header('Content-Type: application/json');
        echo json_encode([
            'status' => 'success',
            'message' => 'Redirecionando para o pagamento...',
            'donation_id' => $donationId,
            'redirect_url' => $redirectUrl
        ]);
        exit;
    }

    // Redirecionamento padrão para formulário HTML clássico
    header("Location: " . $redirectUrl);
    exit;

} catch (Exception $e) {
    error_log('Erro Checkout: ' . $e->getMessage());
    if ($isAjax) {
        header('Content-Type: application/json');
        echo json_encode(['status' => 'error', 'message' => 'Erro interno ao processar checkout.']);
        exit;
    }
    die('Ocorreu um erro ao processar seu pedido. Tente novamente mais tarde.');
}