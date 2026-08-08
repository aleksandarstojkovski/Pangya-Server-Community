<?php
/**
 * dashboard.php
 * -----------------------------------------------------------------------
 * Painel do usuário autenticado. Busca dados em:
 *   - pangya.account                    -> perfil / dados de login
 *   - pangya.user_info                  -> estatísticas de jogo (nível, Pang, Cookie, ranking)
 *   - pangya.pangya_character_information -> personagens possuídos
 *   - pangya.pangya_item_warehouse       -> itens no armazém (contagem)
 *   - pangya.pangya_caddie_information   -> caddies possuídos
 *   - pangya.pangya_mascot_info          -> mascotes possuídos
 * -----------------------------------------------------------------------
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/functions.php';

requireLogin();

$uid = (int)$_SESSION['uid'];

$account       = null;
$stats         = null;
$characters    = [];
$caddies       = [];
$mascots       = [];
$itemCount     = 0;
$loadError     = '';

try {
    $pdo = getConnection();

    // -------------------------------------------------------------
    // 1. Perfil da conta
    // -------------------------------------------------------------
    $stmt = $pdo->prepare(
        'SELECT [UID], [ID], [NICK], [Sex], [RegDate], [LastLogonTime],
                [LogonCount], [Guild_UID], [MannerFlag], [Invited]
         FROM pangya.account
         WHERE [UID] = ?'
    );
    $stmt->execute([$uid]);
    $account = $stmt->fetch();

    // -------------------------------------------------------------
    // 2. Estatísticas de jogo (pode não existir ainda para contas novas)
    // -------------------------------------------------------------
    $stmt = $pdo->prepare(
        'SELECT [level], [Xp], [Pang], [Cookie], [Media_score],
                [LadderPoint], [LadderWin], [LadderLose], [LadderDraw],
                [Holes], [HIO], [Holein], [total_pang_win_game]
         FROM pangya.user_info
         WHERE [UID] = ?'
    );
    $stmt->execute([$uid]);
    $stats = $stmt->fetch();

    // -------------------------------------------------------------
    // 3. Personagens possuídos
    // -------------------------------------------------------------
    $stmt = $pdo->prepare(
        'SELECT [typeid], [Mastery], [default_hair], [default_shirts]
         FROM pangya.pangya_character_information
         WHERE [UID] = ?
         ORDER BY [typeid]'
    );
    $stmt->execute([$uid]);
    $characters = $stmt->fetchAll();

    // -------------------------------------------------------------
    // 4. Caddies possuídos
    // -------------------------------------------------------------
    $stmt = $pdo->prepare(
        'SELECT [typeid], [cLevel], [Exp], [RentFlag], [Valid]
         FROM pangya.pangya_caddie_information
         WHERE [UID] = ? AND [Valid] = 1
         ORDER BY [typeid]'
    );
    $stmt->execute([$uid]);
    $caddies = $stmt->fetchAll();

    // -------------------------------------------------------------
    // 5. Mascotes possuídos
    // -------------------------------------------------------------
    $stmt = $pdo->prepare(
        'SELECT [typeid], [mLevel], [mExp], [Valid]
         FROM pangya.pangya_mascot_info
         WHERE [UID] = ? AND [Valid] = 1
         ORDER BY [typeid]'
    );
    $stmt->execute([$uid]);
    $mascots = $stmt->fetchAll();

    // -------------------------------------------------------------
    // 6. Contagem de itens válidos no armazém
    // -------------------------------------------------------------
    $stmt = $pdo->prepare(
        'SELECT COUNT(*) FROM pangya.pangya_item_warehouse WHERE [UID] = ? AND [valid] = 1'
    );
    $stmt->execute([$uid]);
    $itemCount = (int)$stmt->fetchColumn();

} catch (PDOException $e) {
    error_log('Erro ao carregar painel: ' . $e->getMessage());
    $loadError = 'Não foi possível carregar seus dados no momento.';
}

/**
 * Converte o código de sexo (smallint) em texto legível.
 * Ajuste os valores caso seu servidor use uma convenção diferente.
 */
function sexoLabel($sexo): string
{
    return match ((int)$sexo) {
        1 => 'Masculino',
        2 => 'Feminino',
        default => '-',
    };
}

function formatDate($value): string
{
    if (empty($value)) {
        return '-';
    }
    try {
        return (new DateTime($value))->format('d/m/Y H:i');
    } catch (Exception $e) {
        return (string)$value;
    }
}

$pageTitle = 'Painel do Usuário';
require __DIR__ . '/includes/header.php';
?>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2 class="mb-0">Meu Painel</h2>
    <a href="logout.php" class="btn btn-outline-light btn-sm">Sair</a>
</div>

<?php if ($loadError): ?>
    <div class="alert alert-danger"><?= htmlspecialchars($loadError) ?></div>
<?php elseif (!$account): ?>
    <div class="alert alert-warning">Nenhum dado de conta encontrado para este usuário.</div>
<?php else: ?>

    <div class="row g-4">
        <!-- Perfil -->
        <div class="col-lg-5">
            <div class="card p-4 h-100">
                <h5 class="mb-3">Perfil</h5>
                <table class="table table-dark table-borderless mb-0">
                    <tbody>
                        <tr><th scope="row">UID</th><td><?= htmlspecialchars((string)$account['UID']) ?></td></tr>
                        <tr><th scope="row">Usuário (ID)</th><td><?= htmlspecialchars((string)$account['ID']) ?></td></tr>
                        <tr><th scope="row">Nick</th><td><?= htmlspecialchars((string)$account['NICK']) ?></td></tr>
                        <tr><th scope="row">Sexo</th><td><?= htmlspecialchars(sexoLabel($account['Sex'])) ?></td></tr>
                        <tr><th scope="row">Cadastrado em</th><td><?= htmlspecialchars(formatDate($account['RegDate'])) ?></td></tr>
                        <tr><th scope="row">Último acesso</th><td><?= htmlspecialchars(formatDate($account['LastLogonTime'])) ?></td></tr>
                        <tr><th scope="row">Total de logins</th><td><?= htmlspecialchars((string)$account['LogonCount']) ?></td></tr>
                        <tr><th scope="row">Guild</th>
                            <td><?= ((int)$account['Guild_UID'] > 0) ? htmlspecialchars((string)$account['Guild_UID']) : 'Nenhuma' ?></td>
                        </tr>
                        <tr><th scope="row">Convidado por indicação</th>
                            <td><?= ((int)($account['Invited'] ?? 0) === 1) ? 'Sim' : 'Não' ?></td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Estatísticas de jogo -->
        <div class="col-lg-7">
            <div class="card p-4 h-100">
                <h5 class="mb-3">Estatísticas de Jogo</h5>
                <?php if (!$stats): ?>
                    <p class="mb-0 text-secondary">Ainda não há estatísticas de jogo para esta conta.</p>
                <?php else: ?>
                    <div class="row g-3">
                        <div class="col-6 col-md-4">
                            <div class="p-3 rounded bg-black bg-opacity-25 text-center">
                                <div class="fs-4 fw-bold"><?= htmlspecialchars((string)$stats['level']) ?></div>
                                <div class="small text-secondary">Nível</div>
                            </div>
                        </div>
                        <div class="col-6 col-md-4">
                            <div class="p-3 rounded bg-black bg-opacity-25 text-center">
                                <div class="fs-4 fw-bold"><?= number_format((float)$stats['Pang'], 0, ',', '.') ?></div>
                                <div class="small text-secondary">Pang</div>
                            </div>
                        </div>
                        <div class="col-6 col-md-4">
                            <div class="p-3 rounded bg-black bg-opacity-25 text-center">
                                <div class="fs-4 fw-bold"><?= number_format((float)$stats['Cookie'], 0, ',', '.') ?></div>
                                <div class="small text-secondary">Cookie</div>
                            </div>
                        </div>
                        <div class="col-6 col-md-4">
                            <div class="p-3 rounded bg-black bg-opacity-25 text-center">
                                <div class="fs-4 fw-bold"><?= number_format((float)$stats['Xp'], 0, ',', '.') ?></div>
                                <div class="small text-secondary">Experiência</div>
                            </div>
                        </div>
                        <div class="col-6 col-md-4">
                            <div class="p-3 rounded bg-black bg-opacity-25 text-center">
                                <div class="fs-4 fw-bold"><?= htmlspecialchars((string)$stats['LadderPoint']) ?></div>
                                <div class="small text-secondary">Pontos Ranking</div>
                            </div>
                        </div>
                        <div class="col-6 col-md-4">
                            <div class="p-3 rounded bg-black bg-opacity-25 text-center">
                                <div class="fs-4 fw-bold">
                                    <?= htmlspecialchars((string)$stats['LadderWin']) ?>V
                                    / <?= htmlspecialchars((string)$stats['LadderLose']) ?>D
                                </div>
                                <div class="small text-secondary">Vitórias / Derrotas</div>
                            </div>
                        </div>
                        <div class="col-6 col-md-4">
                            <div class="p-3 rounded bg-black bg-opacity-25 text-center">
                                <div class="fs-4 fw-bold"><?= htmlspecialchars((string)$stats['Holes']) ?></div>
                                <div class="small text-secondary">Buracos jogados</div>
                            </div>
                        </div>
                        <div class="col-6 col-md-4">
                            <div class="p-3 rounded bg-black bg-opacity-25 text-center">
                                <div class="fs-4 fw-bold"><?= htmlspecialchars((string)$stats['HIO']) ?></div>
                                <div class="small text-secondary">Hole-in-One</div>
                            </div>
                        </div>
                        <div class="col-6 col-md-4">
                            <div class="p-3 rounded bg-black bg-opacity-25 text-center">
                                <div class="fs-4 fw-bold"><?= htmlspecialchars((string)$stats['Media_score']) ?></div>
                                <div class="small text-secondary">Média de score</div>
                            </div>
                        </div>
                    </div>
                <?php endif; ?>
            </div>
        </div>

        <!-- Personagens -->
        <div class="col-lg-4">
            <div class="card p-4 h-100">
                <h5 class="mb-3">Personagens (<?= count($characters) ?>)</h5>
                <?php if (empty($characters)): ?>
                    <p class="mb-0 text-secondary">Nenhum personagem encontrado.</p>
                <?php else: ?>
                    <ul class="list-group list-group-flush">
                        <?php foreach ($characters as $char): ?>
                            <li class="list-group-item bg-transparent text-light d-flex justify-content-between">
                                <span>TypeID <?= htmlspecialchars((string)$char['typeid']) ?></span>
                                <span class="text-secondary">Mastery: <?= htmlspecialchars((string)$char['Mastery']) ?></span>
                            </li>
                        <?php endforeach; ?>
                    </ul>
                <?php endif; ?>
            </div>
        </div>

        <!-- Caddies -->
        <div class="col-lg-4">
            <div class="card p-4 h-100">
                <h5 class="mb-3">Caddies (<?= count($caddies) ?>)</h5>
                <?php if (empty($caddies)): ?>
                    <p class="mb-0 text-secondary">Nenhum caddie encontrado.</p>
                <?php else: ?>
                    <ul class="list-group list-group-flush">
                        <?php foreach ($caddies as $caddie): ?>
                            <li class="list-group-item bg-transparent text-light d-flex justify-content-between">
                                <span>TypeID <?= htmlspecialchars((string)$caddie['typeid']) ?></span>
                                <span class="text-secondary">Nv. <?= htmlspecialchars((string)$caddie['cLevel']) ?></span>
                            </li>
                        <?php endforeach; ?>
                    </ul>
                <?php endif; ?>
            </div>
        </div>

        <!-- Mascotes + Armazém -->
        <div class="col-lg-4">
            <div class="card p-4 h-100">
                <h5 class="mb-3">Mascotes (<?= count($mascots) ?>)</h5>
                <?php if (empty($mascots)): ?>
                    <p class="text-secondary">Nenhum mascote encontrado.</p>
                <?php else: ?>
                    <ul class="list-group list-group-flush mb-3">
                        <?php foreach ($mascots as $mascot): ?>
                            <li class="list-group-item bg-transparent text-light d-flex justify-content-between">
                                <span>TypeID <?= htmlspecialchars((string)$mascot['typeid']) ?></span>
                                <span class="text-secondary">Nv. <?= htmlspecialchars((string)$mascot['mLevel']) ?></span>
                            </li>
                        <?php endforeach; ?>
                    </ul>
                <?php endif; ?>

                <div class="p-3 rounded bg-black bg-opacity-25 text-center mt-auto">
                    <div class="fs-4 fw-bold"><?= $itemCount ?></div>
                    <div class="small text-secondary">Itens no armazém</div>
                </div>
            </div>
        </div>
    </div>

<?php endif; ?>

<?php require __DIR__ . '/includes/footer.php'; ?>
