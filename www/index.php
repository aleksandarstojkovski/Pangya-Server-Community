<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/functions.php';

$pageTitle = t('home');

// Consulta as informações do servidor direto no banco de dados
$serverInfo = null;
try {
    $sql = '
        SELECT 
            CAST(pangya_server_list.[Name] AS NVARCHAR(50)) AS [Name], 
            pangya_server_list.[UID], 
            pangya_server_list.[IP], 
            pangya_server_list.[Port], 
            pangya_server_list.MaxUser, 
            pangya_server_list.CurrUser, 
            pangya_server_list.property, 
            pangya_server_list.AngelicWingsNum, 
            pangya_server_list.EventFlag, 
            pangya_server_list.EventMap, 
            pangya_server_list.ImgNo, 
            pangya_server_list.AppRate, 
            pangya_server_list.ScratchRate
        FROM pangya.pangya_server_list
        WHERE 
            pangya_server_list.[Type] = 1 AND 
            pangya_server_list.UpdateTime > DATEADD(second, -8, GETDATE()) AND 
            pangya_server_list.[State] = 1
    ';

    $stmt = getConnection()->query($sql);
    $serverInfo = $stmt->fetch(PDO::FETCH_ASSOC);
} catch (PDOException $e) {
    error_log('Erro ao buscar lista de servidores: ' . $e->getMessage());
}

require __DIR__ . '/includes/header.php';
?>

<!-- Hero Banner -->
<div class="hero-banner rounded-3 mb-4">
    <div class="hero-overlay p-5">
        <h1 class="display-6 fw-bold"><?= htmlspecialchars(t('welcome')) ?></h1>
        <p class="col-md-8 fs-5"><?= htmlspecialchars(t('hero')) ?></p>
        
        <div class="d-flex flex-wrap gap-2 mt-3">
            <?php if (isLoggedIn()): ?>
                <a class="btn btn-primary btn-lg" href="dashboard.php">
                    <?= htmlspecialchars(t('dashboard')) ?>
                </a>
            <?php else: ?>
                <a class="btn btn-primary btn-lg" href="register.php">
                    <?= htmlspecialchars(t('create_account')) ?>
                </a>
            <?php endif; ?>

            <a class="btn btn-outline-light btn-lg" href="downloads.php">
                <?= htmlspecialchars(t('download_game')) ?>
            </a>

            <a class="btn btn-dark btn-lg border-secondary d-flex align-items-center gap-2" href="https://github.com/luismk/Pangya-Server-Community" target="_blank" rel="noopener noreferrer">
                <span>💻 GitHub</span>
            </a>
        </div>
    </div>
</div>

<!-- Banner de Eventos -->
<div id="communityCarousel" class="carousel slide rounded-3 mb-5 shadow" data-bs-ride="carousel">
    <!-- Indicadores (bolinhas embaixo) -->
    <div class="carousel-indicators">
        <button type="button" data-bs-target="#communityCarousel" data-bs-slide-to="0" class="active" aria-current="true" aria-label="Slide 1"></button>
        <button type="button" data-bs-target="#communityCarousel" data-bs-slide-to="1" aria-label="Slide 2"></button>
        <button type="button" data-bs-target="#communityCarousel" data-bs-slide-to="2" aria-label="Slide 3"></button>
    </div>

    <!-- Imagens do Slider -->
    <div class="carousel-inner rounded-3">
        <!-- Slide 1 -->
        <div class="carousel-item active">
            <img src="assets/img/beach-event-banner.jpg" class="d-block w-100 rounded-3" alt="Evento de Praia">
            <div class="carousel-caption d-none d-md-block bg-dark bg-opacity-50 rounded p-2">
                <h5>Pangya Community - Evento de Verão</h5>
                <p>Participe dos eventos exclusivos da temporada na comunidade.</p>
            </div>
        </div>

        <!-- Slide 2 -->
        <div class="carousel-item">
            <img src="assets/img/beach-event-banner.jpg" class="d-block w-100 rounded-3" alt="Novidades do Servidor">
            <div class="carousel-caption d-none d-md-block bg-dark bg-opacity-50 rounded p-2">
                <h5>Atualizações e Ferramentas</h5>
                <p>Explore o PangYa Suite Tools e gerencie seus arquivos IFF com facilidade.</p>
            </div>
        </div>

        <!-- Slide 3 -->
        <div class="carousel-item">
            <img src="assets/img/beach-event-banner.jpg" class="d-block w-100 rounded-3 bg-secondary" alt="Comunidade Open Source">
            <div class="carousel-caption d-none d-md-block bg-dark bg-opacity-50 rounded p-2">
                <h5>Código Aberto</h5>
                <p>Contribua com o projeto no nosso repositório oficial do GitHub.</p>
            </div>
        </div>
    </div>

    <!-- Botões de Navegação (Voltar / Avançar) -->
    <button class="carousel-control-prev" type="button" data-bs-target="#communityCarousel" data-bs-slide="prev">
        <span class="carousel-control-prev-icon" aria-hidden="true"></span>
        <span class="visually-hidden">Anterior</span>
    </button>
    <button class="carousel-control-next" type="button" data-bs-target="#communityCarousel" data-bs-slide="next">
        <span class="carousel-control-next-icon" aria-hidden="true"></span>
        <span class="visually-hidden">Próximo</span>
    </button>
</div>

<!-- Status do Servidor (Dados Dinâmicos do SQL) -->
<div class="row g-4 mb-5">
    <div class="col-md-5">
        <div class="card h-100 p-4 border-0 bg-dark text-light shadow-sm">
            <div class="d-flex justify-content-between align-items-center mb-2">
                <h6 class="text-uppercase text mb-0">Status do Servidor</h6>
                <?php if ($serverInfo): ?>
                    <span class="badge bg-success">Online</span>
                <?php else: ?>
                    <span class="badge bg-danger">Offline</span>
                <?php endif; ?>
            </div>

            <h3 class="fw-bold mb-1">
			<?=  mb_convert_encoding($serverInfo['Name'], 'UTF-8', 'SJIS') ?>
            </h3>
            <p class="text small mb-3">
                IP: <?= htmlspecialchars($serverInfo['IP'] ?? '127.0.0.1') ?>:<?= htmlspecialchars($serverInfo['Port'] ?? '20301') ?>
            </p>

            <div class="border-top border-secondary pt-3 mt-2">
                <div class="d-flex justify-content-between small mb-2">
                    <span>Jogadores Conectados:</span>
                    <span class="fw-bold text-info">
                        <?= htmlspecialchars($serverInfo['CurrUser'] ?? '0') ?> / <?= htmlspecialchars($serverInfo['MaxUser'] ?? '2000') ?>
                    </span>
                </div>
                <div class="d-flex justify-content-between small mb-2">
                    <span>Taxa Pang / EXP (AppRate):</span>
                    <span class="fw-bold text-warning">
                        <?= htmlspecialchars($serverInfo['AppRate'] ?? '0') ?>%
                    </span>
                </div>
                <div class="d-flex justify-content-between small">
                    <span>Taxa Scratch (Papel Shop):</span>
                    <span class="fw-bold text-warning">
                        <?= htmlspecialchars($serverInfo['ScratchRate'] ?? '100') ?>%
                    </span>
                </div>
            </div>
        </div>
    </div>

    <div class="col-md-7">
    <div class="card h-100 p-4 border-0 bg-dark text-light shadow-sm">
        <h5 class="fw-bold mb-3">🛠️ Projeto Open Source & Comunidade</h5>
        <p class="text">
            Este projeto é mantido de forma colaborativa pela comunidade. O código-fonte do servidor e suas ferramentas são totalmente abertos no GitHub para estudo, melhorias e desenvolvimento contínuo.
        </p>
 
        <div class="row mt-4">
            <div class="col-12 mb-3">
                <div class="ratio ratio-16x9">
                    <iframe src="https://www.youtube.com/embed/eQ_2-_OXpL4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                </div>
            </div>
             
            <div class="col-12 text-center">
                <script src="https://apis.google.com/js/platform.js"></script>
                <div class="g-ytsubscribe" 
                     data-channelid="UCVF6-TAC1WW5eB-C9eBI31g" 
                     data-layout="full" 
                     data-count="default">
                </div>
                <p class="mt-2 text small">Acompanhe as novidades do desenvolvimento no canal!</p>
            </div>
        </div>

        <div class="d-flex flex-wrap gap-2 mt-auto pt-3">
            <a href="https://github.com/luismk/Pangya-Server-Community" target="_blank" rel="noopener noreferrer" class="btn btn-sm btn-outline-light">
                📦 Repositório do Servidor
            </a>
            <a href="https://github.com/luismk/PangYa-Suite-Tools" target="_blank" rel="noopener noreferrer" class="btn btn-sm btn-outline-light">
                🔧 PangYa Suite Tools
            </a>
            <a href="https://github.com/luismk/Pangya-Server-Community/issues" target="_blank" rel="noopener noreferrer" class="btn btn-sm btn-outline-warning">
                🐛 Reportar Issue
            </a>
        </div>
    </div>
</div>
</div>

<!-- Destaques / Features -->
<div class="row g-4 mb-5">
    <div class="col-md-4">
        <div class="card h-100 p-3">
            <h5>⛳ <?= htmlspecialchars(t('classic')) ?></h5>
            <p class="mb-0 text"><?= htmlspecialchars(t('classic_text')) ?></p>
        </div>
    </div>
    
    <div class="col-md-4">
        <div class="card h-100 p-3">
            <h5>🤝 <?= htmlspecialchars(t('community')) ?></h5>
            <p class="mb-0 text"><?= htmlspecialchars(t('community_text')) ?></p>
        </div>
    </div>
    
    <div class="col-md-4">
        <div class="card h-100 p-3">
            <h5>🎁 <?= htmlspecialchars(t('starter_items')) ?></h5>
            <p class="mb-0 text"><?= htmlspecialchars(t('starter_items_text')) ?></p>
        </div>
    </div>
</div>

<!-- Como Jogar -->
<div class="card p-4 p-md-5 mb-5 border-0 bg-dark text-light">
    <h3 class="text-center fw-bold mb-4">🚀 Como começar a jogar?</h3>
    <div class="row text-center g-4">
        <div class="col-md-4">
            <div class="p-3">
                <div class="display-5 text-primary fw-bold mb-2">1</div>
                <h5 class="fw-bold">Crie sua conta</h5>
                <p class="text small">Cadastre-se no painel em menos de 1 minuto para obter seu acesso ao jogo.</p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="p-3">
                <div class="display-5 text-primary fw-bold mb-2">2</div>
                <h5 class="fw-bold">Baixe o Cliente</h5>
                <p class="text small">Acesse a aba de downloads e baixe o cliente completo com o patch instalado.</p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="p-3">
                <div class="display-5 text-primary fw-bold mb-2">3</div>
                <h5 class="fw-bold">Entre em Campo</h5>
                <p class="text small">Execute o jogo, faça login com a conta criada e resgate seus itens iniciais!</p>
            </div>
        </div>
    </div>
    <div class="text-center mt-3">
        <a href="register.php" class="btn btn-primary btn-lg"><?= htmlspecialchars(t('create_account')) ?></a>
    </div>
</div>

<?php require __DIR__ . '/includes/footer.php'; ?>