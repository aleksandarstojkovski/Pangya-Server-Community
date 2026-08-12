<?php

require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';

requireLogin();

// Gera o token CSRF se ainda não existir
if (empty($_SESSION['csrf_token'])) {
    $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
}

$balances = ['Pang' => 0, 'Cookie' => 0];

try {
    $statement = getConnection()->prepare(
        'SELECT [Pang], [Cookie] FROM pangya.user_info WHERE [UID] = ?'
    );
    $statement->execute([(int) $_SESSION['uid']]);
    $balances = $statement->fetch() ?: $balances;
} catch (PDOException $exception) {
    error_log('Saldo da loja: ' . $exception->getMessage());
}

// Configuração dos Pacotes de Compra
$packages = [
    'cookies' => [
        'title' => 'Pacotes de Cookies',
        'currency' => 'Cookie',
        'badge' => 'bg-warning text-dark',
        'items' => [
            ['id' => 'cookie_10',  'amount' => 100,  'price' => 10.00],
            ['id' => 'cookie_25',  'amount' => 270,  'price' => 25.00, 'bonus' => '+20 Bônus'],
            ['id' => 'cookie_50',  'amount' => 560,  'price' => 50.00, 'bonus' => '+60 Bônus'],
            ['id' => 'cookie_100', 'amount' => 1200, 'price' => 100.00, 'bonus' => '+200 Bônus', 'popular' => true],
        ]
    ],
    'pangs' => [
        'title' => 'Pacotes de Pangs',
        'currency' => 'Pang',
        'badge' => 'bg-info text-dark',
        'items' => [
            ['id' => 'pang_10',  'amount' => 50000,   'price' => 10.00],
            ['id' => 'pang_25',  'amount' => 140000,  'price' => 25.00, 'bonus' => '+15k Bônus'],
            ['id' => 'pang_50',  'amount' => 300000,  'price' => 50.00, 'bonus' => '+50k Bônus'],
            ['id' => 'pang_100', 'amount' => 700000,  'price' => 100.00, 'bonus' => '+200k Bônus'],
        ]
    ]
];

$pageTitle = t('shop_cash');
require __DIR__ . '/../includes/header.php';
?>

<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
    <h2 class="mb-0"><?= htmlspecialchars(t('shop_cash')) ?></h2>
    <div class="btn-group">
        <a class="btn btn-outline-light btn-sm" href="ShopItem.php">
            <?= htmlspecialchars(t('shop_items')) ?>
        </a>
        <a class="btn btn-outline-light btn-sm" href="ShopSale.php">
            <?= htmlspecialchars(t('marketplace')) ?>
        </a>
    </div>
</div>

<!-- Alertas do Sistema (Sessão ou AJAX) -->
<div id="alert-container">
    <?php if (isset($_SESSION['flash_success'])): ?>
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <?= htmlspecialchars($_SESSION['flash_success']) ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
        <?php unset($_SESSION['flash_success']); ?>
    <?php endif; ?>

    <?php if (isset($_SESSION['flash_error'])): ?>
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <?= htmlspecialchars($_SESSION['flash_error']) ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
        <?php unset($_SESSION['flash_error']); ?>
    <?php endif; ?>
</div>

<!-- Saldo Atual -->
<div class="row g-3 mb-4">
    <div class="col-md-6">
        <div class="card p-3 bg-dark text-white border-secondary">
            <div class="d-flex align-items-center justify-content-between">
                <div>
                    <div class="text-secondary small fw-bold">SEU SALDO ATUAL</div>
                    <div class="display-6 fw-bold text-info">
                        <span id="pang-balance"><?= number_format((int) $balances['Pang'], 0, ',', '.') ?></span> 
                        <small class="fs-6">Pangs</small>
                    </div>
                </div>
                <img src="/assets/img/bar/BtnPang.png" alt="Pang" width="40" height="40" onerror="this.style.display='none'">
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card p-3 bg-dark text-white border-secondary">
            <div class="d-flex align-items-center justify-content-between">
                <div>
                    <div class="text-secondary small fw-bold">SEU SALDO ATUAL</div>
                    <div class="display-6 fw-bold text-warning">
                        <span id="cookie-balance"><?= number_format((int) $balances['Cookie'], 0, ',', '.') ?></span> 
                        <small class="fs-6">Cookies</small>
                    </div>
                </div>
                <img src="/assets/img/bar/BtnCookie.png" alt="Cookie" width="40" height="40" onerror="this.style.display='none'">
            </div>
        </div>
    </div>
</div>

<!-- Navegação por Abas (Tabs) -->
<ul class="nav nav-tabs border-secondary mb-4" id="rechargeTabs" role="tablist">
    <li class="nav-item" role="presentation">
        <button class="nav-link active fw-bold" id="buy-tab" data-bs-toggle="tab" data-bs-target="#buy-panel" type="button" role="tab" aria-controls="buy-panel" aria-selected="true">
            💳 Comprar Pacotes
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link fw-bold" id="epin-tab" data-bs-toggle="tab" data-bs-target="#epin-panel" type="button" role="tab" aria-controls="epin-panel" aria-selected="false">
            🎟️ Resgatar EPIN / Gift Card
        </button>
    </li>
</ul>

<div class="tab-content" id="rechargeTabsContent">
    
    <!-- ABA 1: COMPRA DE PACOTES -->
    <div class="tab-pane fade show active" id="buy-panel" role="tabpanel" aria-labelledby="buy-tab">
        <?php foreach ($packages as $section): ?>
            <h4 class="mb-3 d-flex align-items-center gap-2">
                <span><?= htmlspecialchars($section['title']) ?></span>
                <span class="badge <?= $section['badge'] ?> fs-6"><?= $section['currency'] ?></span>
            </h4>

            <div class="row g-3 mb-5">
                <?php foreach ($section['items'] as $item): ?>
                    <div class="col-sm-6 col-lg-3">
                        <div class="card h-100 bg-dark text-white border-secondary position-relative hover-shadow">
                            <?php if (!empty($item['popular'])): ?>
                                <span class="position-absolute top-0 start-50 translate-middle badge rounded-pill bg-danger">
                                    Mais Popular
                                </span>
                            <?php endif; ?>

                            <div class="card-body d-flex flex-column text-center p-4">
                                <h5 class="card-title text-secondary mb-1"><?= number_format($item['amount'], 0, ',', '.') ?> <?= $section['currency'] ?></h5>
                                
                                <?php if (!empty($item['bonus'])): ?>
                                    <span class="badge bg-success w-auto mx-auto mb-3"><?= htmlspecialchars($item['bonus']) ?></span>
                                <?php else: ?>
                                    <div class="mb-3" style="height: 21px;"></div>
                                <?php endif; ?>

                                <div class="my-auto py-2">
                                    <span class="fs-3 fw-bold">R$ <?= number_format($item['price'], 2, ',', '.') ?></span>
                                </div>

                                <form method="post" action="/Donation/process_checkout.php" class="mt-auto form-checkout">
                                    <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($_SESSION['csrf_token']) ?>">
                                    <input type="hidden" name="package_id" value="<?= htmlspecialchars($item['id']) ?>">
                                    <input type="hidden" name="payment_method" value="mercadopago">

                                    <button class="btn btn-primary w-100 d-flex align-items-center justify-content-center gap-2 btn-submit" type="submit">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-credit-card-fill" viewBox="0 0 16 16">
                                            <path d="M0 4a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v1H0zm0 3v5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7zm3 2h1a1 1 0 0 1 1 1v1a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1v-1a1 1 0 0 1 1-1"/>
                                        </svg>
                                        Comprar via Mercado Pago
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                <?php endforeach; ?>
            </div>
        <?php endforeach; ?>

        <!-- Banner Informativo -->
        <div class="card p-4 bg-dark text-white border-secondary mb-4">
            <div class="d-flex align-items-center gap-3">
                <div class="fs-1 text-info">
                    <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" fill="currentColor" class="bi bi-shield-check" viewBox="0 0 16 16">
                        <path d="M5.338 1.59a61 61 0 0 0-2.837.856.48.48 0 0 0-.328.39c-.554 4.157.726 7.19 2.253 9.188a10.7 10.7 0 0 0 2.287 2.233c.346.244.652.42.893.533q.18.085.293.118a1 1 0 0 0 .101.025 1 1 0 0 0 .1-.025q.114-.034.294-.118c.24-.113.546-.29.893-.533a10.7 10.7 0 0 0 2.287-2.233c1.527-1.997 2.807-5.031 2.253-9.188a.48.48 0 0 0-.328-.39c-.651-.213-1.75-.56-2.837-.855C9.552 1.29 8.531 1.067 8 1.067c-.53 0-1.552.223-2.662.524zM5.072.56C6.157.265 7.31 0 8 0s1.843.265 2.928.56c1.11.3 2.229.655 2.887.87a1.5 1.5 0 0 1 1.044 1.262c.596 4.477-.787 7.795-2.465 9.99a11.7 11.7 0 0 1-2.517 2.453 7 7 0 0 1-1.048.625c-.28.132-.581.24-.829.24s-.548-.108-.829-.24a7 7 0 0 1-1.048-.625 11.7 11.7 0 0 1-2.517-2.453C1.928 10.487.545 7.169 1.141 2.692A1.5 1.5 0 0 1 2.185 1.43C2.844 1.215 3.962.86 5.072.56"/>
                        <path d="M10.854 5.146a.5.5 0 0 1 0 .708l-3 3a.5.5 0 0 1-.708 0l-1.5-1.5a.5.5 0 1 1 .708-.708L7.5 7.793l2.646-2.647a.5.5 0 0 1 .708 0"/>
                    </svg>
                </div>
                <div>
                    <h5 class="mb-1">Pagamentos processados com segurança pelo Mercado Pago</h5>
                    <p class="mb-0 text-secondary small">
                        Aceitamos Pix, Cartão de Crédito e Boleto. Os créditos serão adicionados automaticamente à sua conta assim que o pagamento for aprovado.
                    </p>
                </div>
            </div>
        </div>
    </div>

    <!-- ABA 2: RESGATE DE EPIN -->
    <div class="tab-pane fade" id="epin-panel" role="tabpanel" aria-labelledby="epin-tab">
        <div class="row justify-content-center">
            <div class="col-md-8 col-lg-6">
                <div class="card bg-dark text-white border-secondary p-4">
                    <h4 class="mb-3 text-center">Ativar Código EPIN</h4>
                    <p class="text-secondary text-center small mb-4">
                        Insira abaixo o código PIN fornecido em eventos ou revendedores autorizados para creditar os valores em sua conta.
                    </p>

                    <form id="form-epin" method="post" action="/Donation/process_epin.php">
                        <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($_SESSION['csrf_token']) ?>">

                        <div class="mb-3">
                            <label for="epin_code" class="form-label fw-bold">Código do EPIN</label>
                            <input 
                                type="text" 
                                id="epin_code" 
                                name="epin_code" 
                                class="form-control form-control-lg bg-dark text-white border-secondary text-center text-uppercase font-monospace" 
                                placeholder="XXXX-XXXX-XXXX-XXXX" 
                                required 
                                maxlength="36"
                                autocomplete="off"
                            >
                        </div>

                        <button type="submit" id="btn-redeem-epin" class="btn btn-success btn-lg w-100 mt-2">
                            Resgatar Código
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>

</div>

<!-- Script do lado do cliente para envios AJAX e atualização dos saldos sem reload -->
<script>
document.addEventListener('DOMContentLoaded', function() {
    const alertContainer = document.getElementById('alert-container');

    function showAlert(type, message) {
        alertContainer.innerHTML = `
            <div class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;
    }

    // Tratamento AJAX para Resgate de EPIN
    const epinForm = document.getElementById('form-epin');
    if (epinForm) {
        epinForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const btn = document.getElementById('btn-redeem-epin');
            btn.disabled = true;
            btn.innerText = 'Processando...';

            const formData = new FormData(epinForm);

            fetch(epinForm.action, {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(res => res.json())
            .then(data => {
                btn.disabled = false;
                btn.innerText = 'Resgatar Código';

                if (data.status === 'success') {
                    showAlert('success', data.message);
                    
                    // Atualiza saldos dinamicamente na página
                    if (data.new_cookie_balance !== undefined) {
                        document.getElementById('cookie-balance').innerText = new Intl.NumberFormat('pt-BR').format(data.new_cookie_balance);
                    }
                    if (data.new_pang_balance !== undefined) {
                        document.getElementById('pang-balance').innerText = new Intl.NumberFormat('pt-BR').format(data.new_pang_balance);
                    }

                    epinForm.reset();
                } else {
                    showAlert('danger', data.message || 'Erro ao resgatar o código EPIN.');
                }
            })
            .catch(() => {
                btn.disabled = false;
                btn.innerText = 'Resgatar Código';
                showAlert('danger', 'Ocorreu um erro ao comunicar com o servidor.');
            });
        });
    }

    // Tratamento AJAX para Formulários de Checkout
    document.querySelectorAll('.form-checkout').forEach(form => {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            const btn = form.querySelector('.btn-submit');
            const originalText = btn.innerHTML;
            btn.disabled = true;
            btn.innerText = 'Aguarde...';

            const formData = new FormData(form);

            fetch(form.action, {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(res => res.json())
            .then(data => {
                if (data.status === 'success' && data.redirect_url) {
                    window.location.href = data.redirect_url;
                } else {
                    btn.disabled = false;
                    btn.innerHTML = originalText;
                    showAlert('danger', data.message || 'Falha ao iniciar o processo de pagamento.');
                }
            })
            .catch(() => {
                btn.disabled = false;
                btn.innerHTML = originalText;
                showAlert('danger', 'Erro na requisição. Tente novamente.');
            });
        });
    });
});
</script>

<?php require __DIR__ . '/../includes/footer.php'; ?>