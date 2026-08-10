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
    <link rel="icon" type="image/png" href="assets/img/logo.png">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="assets/css/style.css?v=1" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flag-icons@7.2.3/css/flag-icons.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"> 
    
    <style>
        /* Estilização moderna estilo Glassmorphism Floating Navbar */
        .custom-navbar {
            background: rgba(15, 20, 28, 0.75) !important;
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 16px;
            box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
        }

        .custom-navbar .nav-link {
            color: #d1d5db !important;
            font-weight: 500;
            padding: 0.5rem 0.9rem !important;
            border-radius: 8px;
            transition: all 0.2s ease-in-out;
        }

        .custom-navbar .nav-link:hover,
        .custom-navbar .nav-link.active {
            color: #ffffff !important;
            background: rgba(255, 255, 255, 0.08);
        }

        .custom-navbar .navbar-brand img {
            transition: transform 0.2s ease;
        }

        .custom-navbar .navbar-brand:hover img {
            transform: scale(1.05);
        }
    </style>
</head>
<body id="bg-slider">

<header class="container pt-3">
    <nav class="navbar navbar-expand-lg navbar-dark custom-navbar px-3">
        <div class="container-fluid">
            <!-- Logo -->
            <a class="navbar-brand d-flex align-items-center" href="index.php">
                <img src="assets/img/logo.png" alt="PangYa Community" height="42">
            </a> 
            
            <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu">
                <span class="navbar-toggler-icon"></span>
            </button>

            <div class="collapse navbar-collapse" id="navMenu">
                <ul class="navbar-nav ms-auto align-items-lg-center gap-1 mt-2 mt-lg-0">
                    
                    <li class="nav-item">
                        <a class="nav-link" href="index.php">
                            <i class="bi bi-house-door me-1 text-primary"></i> <?= htmlspecialchars(t('home')) ?>
                        </a>
                    </li>

                    <li class="nav-item">
                        <a class="nav-link" href="downloads.php">
                            <i class="bi bi-download me-1 text-info"></i> <?= htmlspecialchars(t('downloads')) ?>
                        </a>
                    </li>

                    <li class="nav-item">
                        <a class="nav-link" href="wikipedia.php">
                            <i class="bi bi-journal-text me-1 text-warning"></i> <?= htmlspecialchars(t('wikipedia') ?? 'Wikipedia') ?>
                        </a>
                    </li>

                    <?php if (function_exists('isLoggedIn') && isLoggedIn()): ?>
                        <li class="nav-item">
                            <a class="nav-link" href="dashboard.php">
                                <i class="bi bi-speedometer2 me-1 text-success"></i> <?= htmlspecialchars(t('dashboard')) ?>
                            </a>
                        </li>
                        <li class="nav-item ms-lg-2">
                            <a class="btn btn-outline-danger btn-sm px-3 rounded-pill" href="logout.php">
                                <i class="bi bi-box-arrow-right me-1"></i> <?= htmlspecialchars(t('logout')) ?>
                            </a>
                        </li>
                    <?php else: ?>
                        <li class="nav-item ms-lg-2">
                            <a class="nav-link" href="login.php">
                                <i class="bi bi-box-arrow-in-right me-1"></i> <?= htmlspecialchars(t('login')) ?>
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="btn btn-primary btn-sm px-3 rounded-pill fw-semibold shadow-sm" href="register.php">
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