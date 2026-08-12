<?php
require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';
require_once __DIR__ . '/AuthService.php';

redirectIfLoggedIn();

$errorMsg = '';
$id_in = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $id_in = clean($_POST['id_in'] ?? '');
    $pass = (string)($_POST['pass_in'] ?? '');

    if ($id_in === '' || $pass === '') {
        $errorMsg = t('fill_login');
    } else {
        try {
            $user = (new AuthService())->login($id_in, $pass);

            if ($user) {
                session_regenerate_id(true);
                $_SESSION['uid'] = (int)$user['UID'];
                $_SESSION['login_id'] = $user['ID'];
                $_SESSION['capability'] = (int) $user['capability'];

                header('Location: dashboard.php');
                exit;
            }

            $errorMsg = t('invalid_login');
        } catch (PDOException $e) {
            error_log('Login error: ' . $e->getMessage());
            $errorMsg = t('invalid_login');
        }
    }
}

$pageTitle = t('login');
require __DIR__ . '/../includes/header.php';
?>

<!-- Estilos Customizados da Página de Login -->
<style>
    .game-card-login {
        background: rgba(13, 17, 23, 0.88) !important;
        backdrop-filter: blur(16px);
        -webkit-backdrop-filter: blur(16px);
        border: 1px solid rgba(255, 255, 255, 0.08) !important;
        border-top: 3px solid #0d6efd !important;
        box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6), 0 0 30px rgba(13, 110, 253, 0.15) !important;
    }

    .icon-avatar-glow {
        background: linear-gradient(135deg, rgba(13, 110, 253, 0.2), rgba(111, 66, 193, 0.2));
        border: 1px solid rgba(13, 110, 253, 0.3);
        box-shadow: 0 0 20px rgba(13, 110, 253, 0.3);
        width: 70px;
        height: 70px;
    }

    .game-input-group .form-control, 
    .game-input-group .input-group-text,
    .game-input-group .btn {
        background-color: rgba(22, 27, 34, 0.8) !important;
        border-color: rgba(255, 255, 255, 0.12) !important;
        color: #f0f6fc !important;
        transition: all 0.25s ease-in-out;
    }

    .game-input-group .form-control:focus {
        border-color: #0d6efd !important;
        box-shadow: 0 0 12px rgba(13, 110, 253, 0.4) !important;
    }

    .btn-game-login {
        background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%);
        border: none;
        letter-spacing: 0.5px;
        transition: all 0.3s ease;
        box-shadow: 0 4px 15px rgba(13, 110, 253, 0.3);
    }

    .btn-game-login:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(13, 110, 253, 0.5);
        background: linear-gradient(135deg, #1573f4 0%, #0b5ed7 100%);
    }

    .btn-game-login:active {
        transform: translateY(0);
    }
</style>

<div class="row justify-content-center align-items-center py-5">
    <div class="col-12 col-sm-10 col-md-8 col-lg-5 col-xl-4">
        
        <div class="card game-card-login text-light rounded-4 overflow-hidden p-2 p-md-3">
            <div class="card-body p-4">
                
                <!-- Ícone & Título Premium -->
                <div class="text-center mb-4">
                    <div class="d-inline-flex align-items-center justify-content-center icon-avatar-glow rounded-circle mb-3">
                        <i class="bi bi-shield-lock-fill fs-2 text-primary"></i>
                    </div>
                    <h3 class="fw-bold mb-1 text-white text-uppercase" style="letter-spacing: 1px;">
                        <?= htmlspecialchars(t('account_panel')) ?>
                    </h3>
                    <p class="text-secondary small mb-0">Informe suas credenciais para entrar na partida</p>
                </div>

                <!-- Mensagem de Erro Alerta de Jogo -->
                <?php if ($errorMsg): ?>
                    <div class="alert alert-danger d-flex align-items-center gap-2 border-0 bg-danger bg-opacity-20 text-danger-emphasis rounded-3 py-2 px-3 mb-4 small" role="alert" style="border-left: 3px solid #dc3545 !important;">
                        <i class="bi bi-exclamation-octagon-fill fs-5 text-danger"></i>
                        <div><?= htmlspecialchars($errorMsg) ?></div>
                    </div>
                <?php endif; ?>

                <!-- Form Login -->
                <form action="login.php" method="post" novalidate>
                    
                    <!-- Campo Usuário -->
                    <div class="mb-3">
                        <label for="id_in" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('username')) ?></label>
                        <div class="input-group game-input-group">
                            <span class="input-group-text">
                                <i class="bi bi-person-badge text-primary"></i>
                            </span>
                            <input type="text" 
                                   class="form-control" 
                                   id="id_in" 
                                   name="id_in" 
                                   value="<?= htmlspecialchars($id_in) ?>" 
                                   placeholder="Nome de Usuário"
                                   required 
                                   autofocus>
                        </div>
                    </div>

                    <!-- Campo Senha -->
                    <div class="mb-4">
                        <label for="pass_in" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('password')) ?></label>
                        <div class="input-group game-input-group">
                            <span class="input-group-text">
                                <i class="bi bi-key-fill text-primary"></i>
                            </span>
                            <input type="password" 
                                   class="form-control" 
                                   id="pass_in" 
                                   name="pass_in" 
                                   placeholder="••••••••"
                                   required>
                            <button class="btn" type="button" id="togglePassword">
                                <i class="bi bi-eye-fill text-secondary" id="toggleIcon"></i>
                            </button>
                        </div>
                    </div>

                    <!-- Botão Entrar -->
                    <button type="submit" class="btn btn-primary btn-game-login w-100 fw-bold py-2.5 rounded-3 d-flex align-items-center justify-content-center gap-2 text-uppercase">
                        <span><?= htmlspecialchars(t('enter')) ?></span>
                        <i class="bi bi-arrow-right-circle-fill"></i>
                    </button>
                </form>

                <!-- Divisor Neon -->
                <div class="d-flex align-items-center my-4">
                    <hr class="flex-grow-1 border-secondary opacity-25 m-0">
                    <span class="px-3 text-secondary small" style="font-size: 0.75rem;">OU</span>
                    <hr class="flex-grow-1 border-secondary opacity-25 m-0">
                </div>

                <p class="mb-0 text-center text-secondary small">
                    <?= htmlspecialchars(t('no_account')) ?> 
                    <a href="register.php" class="text-primary text-decoration-none fw-bold ms-1">
                        <?= htmlspecialchars(t('sign_up')) ?>
                    </a>
                </p>

            </div>
        </div>

    </div>
</div>

<script>
document.getElementById('togglePassword')?.addEventListener('click', function () {
    const passwordInput = document.getElementById('pass_in');
    const icon = document.getElementById('toggleIcon');
    
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        icon.classList.remove('bi-eye-fill');
        icon.classList.add('bi-eye-slash-fill');
    } else {
        passwordInput.type = 'password';
        icon.classList.remove('bi-eye-slash-fill');
        icon.classList.add('bi-eye-fill');
    }
});
</script>

<?php require __DIR__ . '/../includes/footer.php'; ?>
