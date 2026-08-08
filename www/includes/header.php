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
    <link href="assets/css/style.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flag-icons@7.2.3/css/flag-icons.min.css">
</head>
<body>

<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container">
        <a class="navbar-brand" href="index.php">
            <img src="assets/img/logo.png" alt="PangYa Community" height="40">
        </a>
        
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="navMenu">
            <ul class="navbar-nav ms-auto align-items-lg-center">
                <li class="nav-item">
                    <a class="nav-link" href="index.php"><?= htmlspecialchars(t('home')) ?></a>
                </li>

                <?php if (!function_exists('isLoggedIn') || !isLoggedIn()): ?>
                    <li class="nav-item">
                        <a class="nav-link" href="register.php"><?= htmlspecialchars(t('register')) ?></a>
                    </li>
                <?php endif; ?>

                <li class="nav-item">
                    <a class="nav-link" href="downloads.php"><?= htmlspecialchars(t('downloads')) ?></a>
                </li>

                <?php if (function_exists('isLoggedIn') && isLoggedIn()): ?>
                    <li class="nav-item">
                        <a class="nav-link" href="dashboard.php"><?= htmlspecialchars(t('dashboard')) ?></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="logout.php"><?= htmlspecialchars(t('logout')) ?></a>
                    </li>
                <?php else: ?>
                    <li class="nav-item">
                        <a class="nav-link" href="login.php"><?= htmlspecialchars(t('login')) ?></a>
                    </li>
                <?php endif; ?>

                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle d-flex align-items-center gap-2" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <span class="fi fi-<?= $language === 'pt-BR' ? 'br' : 'us' ?>"></span>
                        <?= htmlspecialchars(availableLanguages()[$language]) ?>
                    </a>
                    <ul class="dropdown-menu dropdown-menu-dark">
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

<main class="container py-5">
    <?php 
    if (function_exists('flashMessage')) {
        flashMessage();
    } 
    ?>