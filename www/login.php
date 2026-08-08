<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/functions.php';

redirectIfLoggedIn();

$errorMsg = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $id_in = clean($_POST['id_in'] ?? '');
    $pass = (string)($_POST['pass_in'] ?? '');

    if ($id_in === '' || $pass === '') {
        $errorMsg = t('fill_login');
    } else {
        try {
            $stmt = getConnection()->prepare('SELECT [UID], [ID] FROM pangya.account WHERE [ID] = ? AND [PASSWORD] = ?');
            $stmt->execute([$id_in, hashPassword($pass)]);
            $user = $stmt->fetch();

            if ($user) {
                session_regenerate_id(true);
                $_SESSION['uid'] = (int)$user['UID'];
                $_SESSION['login_id'] = $user['ID'];

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
require __DIR__ . '/includes/header.php';
?>

<div class="row justify-content-center">
    <div class="col-lg-5">
        <div class="card p-4 p-md-5">
            <h2 class="mb-4"><?= htmlspecialchars(t('account_panel')) ?></h2>

            <?php if ($errorMsg): ?>
                <div class="alert alert-danger"><?= htmlspecialchars($errorMsg) ?></div>
            <?php endif; ?>

            <form action="login.php" method="post" novalidate>
                <div class="mb-3">
                    <label for="id_in" class="form-label"><?= htmlspecialchars(t('username')) ?></label>
                    <input type="text" class="form-control" id="id_in" name="id_in" required autofocus>
                </div>

                <div class="mb-4">
                    <label for="pass_in" class="form-label"><?= htmlspecialchars(t('password')) ?></label>
                    <input type="password" class="form-control" id="pass_in" name="pass_in" required>
                </div>

                <button type="submit" class="btn btn-primary w-100"><?= htmlspecialchars(t('enter')) ?></button>
            </form>

            <p class="mt-3 mb-0 text-center">
                <?= htmlspecialchars(t('no_account')) ?> 
                <a href="register.php"><?= htmlspecialchars(t('sign_up')) ?></a>
            </p>
        </div>
    </div>
</div>

<?php require __DIR__ . '/includes/footer.php'; ?>