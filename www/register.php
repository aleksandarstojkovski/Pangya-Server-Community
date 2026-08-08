<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/functions.php';

redirectIfLoggedIn();

$referrerFromUrl = clean($_GET['ref'] ?? '');
$pageTitle = t('create_title');

require __DIR__ . '/includes/header.php';
?>

<div class="row justify-content-center">
    <div class="col-lg-7">
        <div class="card p-4 p-md-5">
            <h2 class="mb-4"><?= htmlspecialchars(t('create_title')) ?></h2>

            <form action="process_register.php" method="post" novalidate>
                <div class="mb-3">
                    <label for="NomeCompleto" class="form-label"><?= htmlspecialchars(t('full_name')) ?> *</label>
                    <input type="text" class="form-control" id="NomeCompleto" name="NomeCompleto" maxlength="100" required>
                </div>

                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label for="Birthday" class="form-label"><?= htmlspecialchars(t('birth_date')) ?></label>
                        <input type="date" class="form-control" id="Birthday" name="Birthday">
                    </div>

                    <div class="col-md-6 mb-3">
                        <label for="Sexo" class="form-label"><?= htmlspecialchars(t('gender')) ?> *</label>
                        <select class="form-select" id="Sexo" name="Sexo" required>
                            <option value="" selected disabled><?= htmlspecialchars(t('select')) ?></option>
                            <option value="1"><?= htmlspecialchars(t('male')) ?></option>
                            <option value="2"><?= htmlspecialchars(t('female')) ?></option>
                        </select>
                    </div>
                </div>

                <div class="mb-3">
                    <label for="Pergunta" class="form-label"><?= htmlspecialchars(t('security_question')) ?> *</label>
                    <input type="text" class="form-control" id="Pergunta" name="Pergunta" maxlength="100" required>
                </div>

                <div class="mb-3">
                    <label for="Resposta" class="form-label"><?= htmlspecialchars(t('security_answer')) ?></label>
                    <input type="text" class="form-control" id="Resposta" name="Resposta" maxlength="120">
                </div>

                <div class="mb-3">
                    <label for="email_in" class="form-label"><?= htmlspecialchars(t('email')) ?> *</label>
                    <input type="email" class="form-control" id="email_in" name="email_in" maxlength="100" required>
                </div>

                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label for="id_in" class="form-label"><?= htmlspecialchars(t('username')) ?> *</label>
                        <input type="text" class="form-control" id="id_in" name="id_in" maxlength="25" required pattern="[A-Za-z0-9_]{3,25}">
                    </div>

                    <div class="col-md-6 mb-3">
                        <label for="pass_in" class="form-label"><?= htmlspecialchars(t('password')) ?> *</label>
                        <input type="password" class="form-control" id="pass_in" name="pass_in" maxlength="40" required minlength="4">
                    </div>
                </div>

                <div class="mb-4">
                    <label for="Referrer_Code" class="form-label"><?= htmlspecialchars(t('referral_code')) ?></label>
                    <input type="text" class="form-control" id="Referrer_Code" name="Referrer_Code" maxlength="25" value="<?= htmlspecialchars($referrerFromUrl) ?>">
                    <div class="form-text"><?= htmlspecialchars(t('referral_help')) ?></div>
                </div>

                <button type="submit" class="btn btn-primary w-100"><?= htmlspecialchars(t('create_account')) ?></button>
            </form>
        </div>
    </div>
</div>

<?php require __DIR__ . '/includes/footer.php'; ?>