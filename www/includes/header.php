<?php
$pageTitle = $pageTitle ?? 'PangYa Community';
$language = currentLanguage();

header('Content-Type: text/html; charset=utf-8');
?>
<!DOCTYPE html>
<html lang="<?= $language === 'pt-BR' ? 'pt-BR' : 'en' ?>">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= htmlspecialchars($pageTitle) ?> · PangYa Community</title>
	<link rel="icon" type="image/x-icon" href="/assets/img/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="/assets/css/style.css?v=2" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flag-icons@7.2.3/css/flag-icons.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"> 
</head>
<body id="bg-slider">
<div id="page-loader" class="page-loader" role="status" aria-live="polite">
    <div class="loader-content">
        <img src="/assets/img/bg/loading.gif" alt="<?= htmlspecialchars(t('loading')) ?>">
        <span><?= htmlspecialchars(t('loading')) ?></span>
    </div>
</div>
<header class="container pt-3">
    <nav class="navbar navbar-expand-lg navbar-dark custom-navbar px-3">
        <div class="container-fluid">
            <!-- Logo -->
            <a class="navbar-brand d-flex align-items-center" href="/index.php">
                <img src="/assets/img/logo.png" alt="PangYa Community" height="42">
            </a> 
            
            <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu">
                <span class="navbar-toggler-icon"></span>
            </button>

            <div class="collapse navbar-collapse" id="navMenu">
                <ul class="navbar-nav ms-auto align-items-lg-center gap-1 mt-2 mt-lg-0">
                    
                    <li class="nav-item">
                        <a class="nav-link" href="/index.php">
                          <img src="/assets/img/bar/bar_home.png" alt="PangYa Community" height="42"><?= htmlspecialchars(t('home')) ?>
                        </a>
                    </li>

                    <li class="nav-item">
                        <a class="nav-link" href="/downloads.php">
                           <img src="/assets/img/bar/bar_tools.png" alt="PangYa Community" height="42"> <?= htmlspecialchars(t('downloads')) ?>
                        </a>
                    </li>

                    <li class="nav-item">
                        <a class="nav-link" href="/wikipedia.php">
                            <img src="/assets/img/bar/bar_wiki.png" alt="PangYa Community" height="42"><?= htmlspecialchars(t('wikipedia')) ?>
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/Shop/ShopItem.php">
                            <i class="bi bi-bag me-1 text-warning"></i> <?= htmlspecialchars(t('shop')) ?>
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/Shop/ShopSale.php">
                            <i class="bi bi-arrow-left-right me-1 text-info"></i> <?= htmlspecialchars(t('marketplace')) ?>
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/Server/server.php">
                            <i class="bi bi-hdd-network me-1 text-success"></i> <?= htmlspecialchars(t('server_page')) ?>
                        </a>
                    </li>

                    <?php if (function_exists('isLoggedIn') && isLoggedIn()): ?>
                        <li class="nav-item">
                            <a class="nav-link" href="/Account/dashboard.php">
                              <img src="/assets/img/bar/bar_login.png" alt="PangYa Community" height="42"><?= htmlspecialchars(t('dashboard')) ?>
                            </a>
                        </li>
                        <li class="nav-item ms-lg-2">
                            <a class="btn btn-outline-danger btn-sm px-3 rounded-pill" href="/Account/logout.php">
                            <img src="/assets/img/bar/bar_exit.png" alt="PangYa Community" height="42">
                                <i class="bi bi-box-arrow-right me-1"></i> <?= htmlspecialchars(t('logout')) ?>
                            </a>
                        </li>
                    <?php else: ?>
                        <li class="nav-item ms-lg-2">
                            <a class="nav-link" href="/Account/login.php">
                                <i class="bi bi-box-arrow-in-right me-1"></i> <?= htmlspecialchars(t('login')) ?>
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="btn btn-primary btn-sm px-3 rounded-pill fw-semibold shadow-sm" href="/Account/register.php">
                                <?= htmlspecialchars(t('register')) ?>
                            </a>
                        </li>
                    <?php endif; ?>

                    <!-- Idioma -->
                    <li class="nav-item dropdown ms-lg-2 border-start border-secondary border-opacity-25 ps-lg-2 mt-2 mt-lg-0">
                        <a class="nav-link dropdown-toggle d-flex align-items-center gap-2" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                            <span class="fi fi-<?= $language === 'pt-BR' ? 'br' : 'us' ?> rounded-1"></span>
                            <span class="small"><?= htmlspecialchars(availableLanguages()[$language]) ?></span>
                        </a>
                        <ul class="dropdown-menu dropdown-menu-dark dropdown-menu-end shadow border-secondary border-opacity-25">
                            <li>
                                <a class="dropdown-item d-flex align-items-center gap-2" href="?lang=pt-BR">
                                    <span class="fi fi-br"></span> Português
                                </a>
                            </li>
                            <li>
                                <a class="dropdown-item d-flex align-items-center gap-2" href="?lang=en">
                                    <span class="fi fi-us"></span> English
                                </a>
                            </li>
                        </ul>
                    </li>

                </ul>
            </div>
        </div>
    </nav>
</header>

<main class="container py-4">
    <?php 
    if (function_exists('flashMessage')) {
        flashMessage();
    } 
    ?>
