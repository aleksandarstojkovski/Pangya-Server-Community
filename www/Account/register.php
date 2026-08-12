<?php
require_once __DIR__ . '/../Config/config.php';
require_once __DIR__ . '/../includes/functions.php';

redirectIfLoggedIn();

$referrerFromUrl = clean($_GET['ref'] ?? '');
$pageTitle = t('create_title');

require __DIR__ . '/../includes/header.php';
?>

<!-- Estilos Customizados da Página de Cadastro -->
<style>
    .game-card-register {
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

    .section-divider {
        font-size: 0.75rem;
        letter-spacing: 1px;
        color: #0d6efd;
        font-weight: 700;
        text-transform: uppercase;
        border-bottom: 1px solid rgba(13, 110, 253, 0.2);
        padding-bottom: 0.5rem;
    }

    .game-input-group .form-control, 
    .game-input-group .form-select,
    .game-input-group .input-group-text,
    .game-input-group .btn {
        background-color: rgba(22, 27, 34, 0.8) !important;
        border-color: rgba(255, 255, 255, 0.12) !important;
        color: #f0f6fc !important;
        transition: all 0.25s ease-in-out;
    }

    .game-input-group .form-control:focus,
    .game-input-group .form-select:focus {
        border-color: #0d6efd !important;
        box-shadow: 0 0 12px rgba(13, 110, 253, 0.4) !important;
    }

    .game-input-group .form-select option {
        background-color: #161b22;
        color: #f0f6fc;
    }

    .btn-game-register {
        background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%);
        border: none;
        letter-spacing: 0.5px;
        transition: all 0.3s ease;
        box-shadow: 0 4px 15px rgba(13, 110, 253, 0.3);
    }

    .btn-game-register:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(13, 110, 253, 0.5);
        background: linear-gradient(135deg, #1573f4 0%, #0b5ed7 100%);
    }

    .btn-game-register:active {
        transform: translateY(0);
    }
</style>

<div class="row justify-content-center align-items-center py-5">
    <div class="col-12 col-lg-8 col-xl-7">
        
        <div class="card game-card-register text-light rounded-4 overflow-hidden p-2 p-md-3">
            <div class="card-body p-4 p-md-5">
                
                <!-- Ícone & Título Premium -->
                <div class="text-center mb-4">
                    <div class="d-inline-flex align-items-center justify-content-center icon-avatar-glow rounded-circle mb-3">
                        <i class="bi bi-person-plus-fill fs-2 text-primary"></i>
                    </div>
                    <h3 class="fw-bold mb-1 text-white text-uppercase" style="letter-spacing: 1px;">
                        <?= htmlspecialchars(t('create_title')) ?>
                    </h3>
                    <p class="text-secondary small mb-0">Crie sua conta para começar sua jornada em campo</p>
                </div>

                <!-- Form Cadastro -->
                <form action="process_register.php" method="post" novalidate>
                    
                    <!-- SEÇÃO 1: DADOS PESSOAIS -->
                    <div class="section-divider mb-3 d-flex align-items-center gap-2">
                        <i class="bi bi-card-heading"></i>
                        <span>Dados Pessoais</span>
                    </div>

                    <!-- Nome Completo -->
                    <div class="mb-3">
                        <label for="NomeCompleto" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('full_name')) ?> *</label>
                        <div class="input-group game-input-group">
                            <span class="input-group-text"><i class="bi bi-person text-primary"></i></span>
                            <input type="text" class="form-control" id="NomeCompleto" name="NomeCompleto" maxlength="100" placeholder="Seu nome completo" required autofocus>
                        </div>
                    </div>

                    <div class="row">
                        <!-- Data de Nascimento -->
                        <div class="col-md-6 mb-3">
                            <label for="Birthday" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('birth_date')) ?></label>
                            <div class="input-group game-input-group">
                                <span class="input-group-text"><i class="bi bi-calendar-event text-primary"></i></span>
                                <input type="date" class="form-control" id="Birthday" name="Birthday">
                            </div>
                        </div>

                        <!-- Gênero -->
                        <div class="col-md-6 mb-3">
                            <label for="Sexo" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('gender')) ?> *</label>
                            <div class="input-group game-input-group">
                                <span class="input-group-text"><i class="bi bi-gender-ambiguous text-primary"></i></span>
                                <select class="form-select" id="Sexo" name="Sexo" required>
                                    <option value="" selected disabled><?= htmlspecialchars(t('select')) ?></option>
                                    <option value="1"><?= htmlspecialchars(t('male')) ?></option>
                                    <option value="2"><?= htmlspecialchars(t('female')) ?></option>
                                </select>
                            </div>
                        </div>
                    </div>

                    <!-- E-mail -->
                    <div class="mb-4">
                        <label for="email_in" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('email')) ?> *</label>
                        <div class="input-group game-input-group">
                            <span class="input-group-text"><i class="bi bi-envelope-at text-primary"></i></span>
                            <input type="email" class="form-control" id="email_in" name="email_in" maxlength="100" placeholder="seu@email.com" required>
                        </div>
                    </div>

                    <!-- SEÇÃO 2: CREDENCIAIS DA CONTA -->
                    <div class="section-divider mb-3 d-flex align-items-center gap-2">
                        <i class="bi bi-shield-lock"></i>
                        <span>Acesso & Segurança</span>
                    </div>

                    <div class="row">
                        <!-- Usuário -->
                        <div class="col-md-6 mb-3">
                            <label for="id_in" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('username')) ?> *</label>
                            <div class="input-group game-input-group">
                                <span class="input-group-text"><i class="bi bi-person-badge text-primary"></i></span>
                                <input type="text" class="form-control" id="id_in" name="id_in" maxlength="25" required pattern="[A-Za-z0-9_]{3,25}" placeholder="ex: Jogador123">
                            </div>
                        </div>

                        <!-- Senha -->
                        <div class="col-md-6 mb-3">
                            <label for="pass_in" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('password')) ?> *</label>
                            <div class="input-group game-input-group">
                                <span class="input-group-text"><i class="bi bi-key text-primary"></i></span>
                                <input type="password" class="form-control" id="pass_in" name="pass_in" maxlength="40" required minlength="4" placeholder="••••••••">
                                <button class="btn" type="button" id="togglePassword">
                                    <i class="bi bi-eye-fill text-secondary" id="toggleIcon"></i>
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <!-- Pergunta Secreta -->
                        <div class="col-md-6 mb-3">
                            <label for="Pergunta" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('security_question')) ?> *</label>
                            <div class="input-group game-input-group">
                                <span class="input-group-text"><i class="bi bi-question-circle text-primary"></i></span>
                                <input type="text" class="form-control" id="Pergunta" name="Pergunta" maxlength="100" placeholder="ex: Nome do primeiro pet?" required>
                            </div>
                        </div>

                        <!-- Resposta Secreta -->
                        <div class="col-md-6 mb-3">
                            <label for="Resposta" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('security_answer')) ?></label>
                            <div class="input-group game-input-group">
                                <span class="input-group-text"><i class="bi bi-chat-left-dots text-primary"></i></span>
                                <input type="text" class="form-control" id="Resposta" name="Resposta" maxlength="120" placeholder="Sua resposta">
                            </div>
                        </div>
                    </div>

                    <!-- Código de Indicação -->
                    <div class="mb-4">
                        <label for="Referrer_Code" class="form-label text-secondary small fw-semibold text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><?= htmlspecialchars(t('referral_code')) ?></label>
                        <div class="input-group game-input-group">
                            <span class="input-group-text"><i class="bi bi-ticket-perforated text-warning"></i></span>
                            <input type="text" class="form-control" id="Referrer_Code" name="Referrer_Code" maxlength="25" value="<?= htmlspecialchars($referrerFromUrl) ?>" placeholder="Opcional">
                        </div>
                        <div class="form-text text-secondary small mt-1"><i class="bi bi-info-circle me-1"></i><?= htmlspecialchars(t('referral_help')) ?></div>
                    </div>

                    <!-- Botão Cadastrar -->
                    <button type="submit" class="btn btn-primary btn-game-register w-100 fw-bold py-2.5 rounded-3 d-flex align-items-center justify-content-center gap-2 text-uppercase">
                        <span><?= htmlspecialchars(t('create_account')) ?></span>
                        <i class="bi bi-arrow-right-circle-fill"></i>
                    </button>
                </form>

                <!-- Divisor -->
                <div class="d-flex align-items-center my-4">
                    <hr class="flex-grow-1 border-secondary opacity-25 m-0">
                    <span class="px-3 text-secondary small" style="font-size: 0.75rem;">OU</span>
                    <hr class="flex-grow-1 border-secondary opacity-25 m-0">
                </div>

                <p class="mb-0 text-center text-secondary small">
                    Já possui uma conta?
                    <a href="login.php" class="text-primary text-decoration-none fw-bold ms-1">
                        <?= htmlspecialchars(t('login')) ?>
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
