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

$account        = null;
$stats          = null;
$characters     = [];
$caddies        = [];
$mascots        = [];
$warehouseItems = [];
$itemCount      = 0;
$loadError      = '';

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
    // 6. Itens do armazém, agrupados por TypeId (cada linha do banco é
    //    uma unidade do item; agrupar dá a "quantidade" sem precisar de
    //    uma coluna de quantidade, que essa tabela não tem).
    // -------------------------------------------------------------
    $stmt = $pdo->prepare(
        'SELECT [typeid], COUNT(*) AS qty
         FROM pangya.pangya_item_warehouse
         WHERE [UID] = ? AND [valid] = 1
         GROUP BY [typeid]
         ORDER BY [typeid]'
    );
    $stmt->execute([$uid]);
    $warehouseItems = $stmt->fetchAll();
    $itemCount = array_sum(array_column($warehouseItems, 'qty'));

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

<?php if ($account): ?>
<!-- ==========================================================
     Modal de edição do perfil (Nick / Sexo)
     Envia para update_profile.php, que faz o UPDATE no banco.
     ========================================================== -->
<div class="modal fade" id="editProfileModal" tabindex="-1" aria-labelledby="editProfileModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content bg-dark text-light" action="update_profile.php" method="post" accept-charset="UTF-8">
            <div class="modal-header">
                <h5 class="modal-title" id="editProfileModalLabel"><i class="bi bi-pencil-square me-2"></i>Editar Perfil</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(csrfToken()) ?>">

                <div class="mb-3">
                    <label for="editNick" class="form-label">Nick</label>
                    <input
                        type="text"
                        class="form-control"
                        id="editNick"
                        name="nick"
                        maxlength="20"
                        required
                        value="<?= htmlspecialchars((string)$account['NICK']) ?>"
                    >
                    <div class="form-text">Até 20 caracteres. Acentos e caracteres de outros idiomas (ex.: japonês/coreano) são aceitos.</div>
                </div>

                <div class="mb-1">
                    <label for="editSexo" class="form-label">Sexo</label>
                    <select class="form-select" id="editSexo" name="sexo" required>
                        <option value="1" <?= (int)$account['Sex'] === 1 ? 'selected' : '' ?>>Masculino</option>
                        <option value="2" <?= (int)$account['Sex'] === 2 ? 'selected' : '' ?>>Feminino</option>
                    </select>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-light" data-bs-dismiss="modal">Cancelar</button>
                <button type="submit" class="btn btn-primary">Salvar alterações</button>
            </div>
        </form>
    </div>
</div>

<!-- ==========================================================
     Modal de edição de Personagem (Mastery / cabelo / roupa)
     Um único modal, reaproveitado para qualquer linha da lista —
     o JS no fim da página preenche os campos ao clicar em "Editar".
     ========================================================== -->
<div class="modal fade" id="editCharacterModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content bg-dark text-light" action="update_collection.php" method="post" accept-charset="UTF-8">
            <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-pencil-square me-2"></i>Editar Personagem <span id="charTypeIdLabel"></span></h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(csrfToken()) ?>">
                <input type="hidden" name="type" value="character">
                <input type="hidden" name="typeid" id="charTypeId">

                <div class="mb-3">
                    <label for="charMastery" class="form-label">Mastery</label>
                    <input type="number" min="0" class="form-control" id="charMastery" name="mastery" required>
                </div>
                <div class="row g-3">
                    <div class="col-6">
                        <label for="charHair" class="form-label">Cabelo (default_hair)</label>
                        <input type="number" min="0" class="form-control" id="charHair" name="default_hair" required>
                    </div>
                    <div class="col-6">
                        <label for="charShirts" class="form-label">Roupa (default_shirts)</label>
                        <input type="number" min="0" class="form-control" id="charShirts" name="default_shirts" required>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-light" data-bs-dismiss="modal">Cancelar</button>
                <button type="submit" class="btn btn-primary">Salvar alterações</button>
            </div>
        </form>
    </div>
</div>

<!-- ==========================================================
     Modal de edição de Caddie (nível / exp / aluguel)
     ========================================================== -->
<div class="modal fade" id="editCaddieModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content bg-dark text-light" action="update_collection.php" method="post" accept-charset="UTF-8">
            <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-pencil-square me-2"></i>Editar Caddie <span id="caddieTypeIdLabel"></span></h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(csrfToken()) ?>">
                <input type="hidden" name="type" value="caddie">
                <input type="hidden" name="typeid" id="caddieTypeId">

                <div class="row g-3 mb-3">
                    <div class="col-6">
                        <label for="caddieLevel" class="form-label">Nível</label>
                        <input type="number" min="0" class="form-control" id="caddieLevel" name="clevel" required>
                    </div>
                    <div class="col-6">
                        <label for="caddieExp" class="form-label">Exp</label>
                        <input type="number" min="0" class="form-control" id="caddieExp" name="exp" required>
                    </div>
                </div>
                <div class="form-check">
                    <input type="checkbox" class="form-check-input" id="caddieRent" name="rentflag" value="1">
                    <label class="form-check-label" for="caddieRent">Alugado (RentFlag)</label>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-light" data-bs-dismiss="modal">Cancelar</button>
                <button type="submit" class="btn btn-primary">Salvar alterações</button>
            </div>
        </form>
    </div>
</div>

<!-- ==========================================================
     Modal de edição de Mascote (nível / exp)
     ========================================================== -->
<div class="modal fade" id="editMascotModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content bg-dark text-light" action="update_collection.php" method="post" accept-charset="UTF-8">
            <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-pencil-square me-2"></i>Editar Mascote <span id="mascotTypeIdLabel"></span></h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(csrfToken()) ?>">
                <input type="hidden" name="type" value="mascot">
                <input type="hidden" name="typeid" id="mascotTypeId">

                <div class="row g-3">
                    <div class="col-6">
                        <label for="mascotLevel" class="form-label">Nível</label>
                        <input type="number" min="0" class="form-control" id="mascotLevel" name="mlevel" required>
                    </div>
                    <div class="col-6">
                        <label for="mascotExp" class="form-label">Exp</label>
                        <input type="number" min="0" class="form-control" id="mascotExp" name="exp" required>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-light" data-bs-dismiss="modal">Cancelar</button>
                <button type="submit" class="btn btn-primary">Salvar alterações</button>
            </div>
        </form>
    </div>
</div>
<?php endif; ?>

<?php if ($loadError): ?>
    <div class="alert alert-danger"><?= htmlspecialchars($loadError) ?></div>
<?php elseif (!$account): ?>
    <div class="alert alert-warning">Nenhum dado de conta encontrado para este usuário.</div>
<?php else: ?>

    <div class="row g-4">
        <!-- Perfil -->
        <div class="col-lg-5">
            <div class="card p-4 h-100">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h5 class="mb-0"><i class="bi bi-person-circle me-2"></i>Perfil</h5>
                    <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#editProfileModal">
                        <i class="bi bi-pencil-square me-1"></i>Editar
                    </button>
                </div>
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
                <h5 class="mb-3"><i class="bi bi-bar-chart-fill me-2"></i>Estatísticas de Jogo</h5>
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
                <h5 class="mb-3"><i class="bi bi-person-badge-fill me-2"></i>Personagens (<?= count($characters) ?>)</h5>
                <?php if (empty($characters)): ?>
                    <p class="mb-0 text-secondary">Nenhum personagem encontrado.</p>
                <?php else: ?>
                    <ul class="list-group list-group-flush">
                        <?php foreach ($characters as $char): ?>
                            <li class="list-group-item bg-transparent text-light collection-row">
                                <?= itemIconTag((int)$char['typeid']) ?>
                                <div class="collection-info">
                                    <div>TypeID <?= htmlspecialchars((string)$char['typeid']) ?></div>
                                    <div class="text-secondary small">Mastery: <?= htmlspecialchars((string)$char['Mastery']) ?></div>
                                </div>
                                <div class="collection-actions">
                                    <button
                                        type="button"
                                        class="btn btn-sm btn-outline-light edit-character-btn"
                                        data-bs-toggle="modal"
                                        data-bs-target="#editCharacterModal"
                                        data-typeid="<?= (int)$char['typeid'] ?>"
                                        data-mastery="<?= (int)$char['Mastery'] ?>"
                                        data-hair="<?= (int)$char['default_hair'] ?>"
                                        data-shirts="<?= (int)$char['default_shirts'] ?>"
                                    ><i class="bi bi-pencil-square"></i></button>
                                </div>
                            </li>
                        <?php endforeach; ?>
                    </ul>
                <?php endif; ?>
            </div>
        </div>

        <!-- Caddies -->
        <div class="col-lg-4">
            <div class="card p-4 h-100">
                <h5 class="mb-3"><i class="bi bi-person-arms-up me-2"></i>Caddies (<?= count($caddies) ?>)</h5>
                <?php if (empty($caddies)): ?>
                    <p class="mb-0 text-secondary">Nenhum caddie encontrado.</p>
                <?php else: ?>
                    <ul class="list-group list-group-flush">
                        <?php foreach ($caddies as $caddie): ?>
                            <li class="list-group-item bg-transparent text-light collection-row">
                                <?= itemIconTag((int)$caddie['typeid']) ?>
                                <div class="collection-info">
                                    <div>TypeID <?= htmlspecialchars((string)$caddie['typeid']) ?></div>
                                    <div class="text-secondary small">Nv. <?= htmlspecialchars((string)$caddie['cLevel']) ?></div>
                                </div>
                                <div class="collection-actions">
                                    <button
                                        type="button"
                                        class="btn btn-sm btn-outline-light edit-caddie-btn"
                                        data-bs-toggle="modal"
                                        data-bs-target="#editCaddieModal"
                                        data-typeid="<?= (int)$caddie['typeid'] ?>"
                                        data-clevel="<?= (int)$caddie['cLevel'] ?>"
                                        data-exp="<?= (int)$caddie['Exp'] ?>"
                                        data-rentflag="<?= (int)$caddie['RentFlag'] ?>"
                                    ><i class="bi bi-pencil-square"></i></button>
                                </div>
                            </li>
                        <?php endforeach; ?>
                    </ul>
                <?php endif; ?>
            </div>
        </div>

        <!-- Mascotes -->
        <div class="col-lg-4">
            <div class="card p-4 h-100">
                <h5 class="mb-3"><i class="bi bi-emoji-heart-eyes-fill me-2"></i>Mascotes (<?= count($mascots) ?>)</h5>
                <?php if (empty($mascots)): ?>
                    <p class="text-secondary">Nenhum mascote encontrado.</p>
                <?php else: ?>
                    <ul class="list-group list-group-flush mb-3">
                        <?php foreach ($mascots as $mascot): ?>
                            <li class="list-group-item bg-transparent text-light collection-row">
                                <?= itemIconTag((int)$mascot['typeid']) ?>
                                <div class="collection-info">
                                    <div>TypeID <?= htmlspecialchars((string)$mascot['typeid']) ?></div>
                                    <div class="text-secondary small">Nv. <?= htmlspecialchars((string)$mascot['mLevel']) ?></div>
                                </div>
                                <div class="collection-actions">
                                    <button
                                        type="button"
                                        class="btn btn-sm btn-outline-light edit-mascot-btn"
                                        data-bs-toggle="modal"
                                        data-bs-target="#editMascotModal"
                                        data-typeid="<?= (int)$mascot['typeid'] ?>"
                                        data-mlevel="<?= (int)$mascot['mLevel'] ?>"
                                        data-mexp="<?= (int)$mascot['mExp'] ?>"
                                    ><i class="bi bi-pencil-square"></i></button>
                                </div>
                            </li>
                        <?php endforeach; ?>
                    </ul>
                <?php endif; ?>
            </div>
        </div>

        <!-- Armazém -->
        <div class="col-lg-12">
            <div class="card p-4">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h5 class="mb-0"><i class="bi bi-archive-fill me-2"></i>Armazém (<?= (int)$itemCount ?> itens · <?= count($warehouseItems) ?> tipos)</h5>
                </div>

                <!-- Adicionar novo item -->
                <form action="update_warehouse.php" method="post" accept-charset="UTF-8" class="row g-2 align-items-end mb-3">
                    <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(csrfToken()) ?>">
                    <input type="hidden" name="action" value="add">
                    <div class="col-auto">
                        <label for="newItemTypeId" class="form-label small mb-1">Adicionar item por TypeId</label>
                        <input type="number" min="1" name="typeid" id="newItemTypeId" class="form-control form-control-sm" style="width:140px" required>
                    </div>
                    <div class="col-auto">
                        <button type="submit" class="btn btn-sm btn-primary"><i class="bi bi-plus-lg me-1"></i>Adicionar</button>
                    </div>
                </form>

                <?php if (empty($warehouseItems)): ?>
                    <p class="mb-0 text-secondary">Nenhum item no armazém.</p>
                <?php else: ?>
                    <div class="row g-2">
                        <?php foreach ($warehouseItems as $item): ?>
                            <div class="col-sm-6 col-md-4 col-lg-3">
                                <div class="collection-row p-2 rounded bg-black bg-opacity-25">
                                    <?= itemIconTag((int)$item['typeid']) ?>
                                    <div class="collection-info">
                                        <div>TypeID <?= htmlspecialchars((string)$item['typeid']) ?></div>
                                        <div class="text-secondary small">Qtd: <?= (int)$item['qty'] ?></div>
                                    </div>
                                    <div class="collection-actions d-flex gap-1">
                                        <form action="update_warehouse.php" method="post" accept-charset="UTF-8">
                                            <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(csrfToken()) ?>">
                                            <input type="hidden" name="action" value="add">
                                            <input type="hidden" name="typeid" value="<?= (int)$item['typeid'] ?>">
                                            <button type="submit" class="btn btn-sm btn-outline-light" title="Adicionar mais um"><i class="bi bi-plus-lg"></i></button>
                                        </form>
                                        <form action="update_warehouse.php" method="post" accept-charset="UTF-8">
                                            <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(csrfToken()) ?>">
                                            <input type="hidden" name="action" value="remove">
                                            <input type="hidden" name="typeid" value="<?= (int)$item['typeid'] ?>">
                                            <button type="submit" class="btn btn-sm btn-outline-danger" title="Remover um"><i class="bi bi-dash-lg"></i></button>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        <?php endforeach; ?>
                    </div>
                <?php endif; ?>
            </div>
        </div>
    </div>

<?php endif; ?>

<script>
// Preenche os modais de edição (Personagem / Caddie / Mascote) com os
// dados da linha clicada, usando os atributos data-* de cada botão.
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.edit-character-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            document.getElementById('charTypeId').value = btn.dataset.typeid;
            document.getElementById('charTypeIdLabel').textContent = '#' + btn.dataset.typeid;
            document.getElementById('charMastery').value = btn.dataset.mastery;
            document.getElementById('charHair').value = btn.dataset.hair;
            document.getElementById('charShirts').value = btn.dataset.shirts;
        });
    });

    document.querySelectorAll('.edit-caddie-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            document.getElementById('caddieTypeId').value = btn.dataset.typeid;
            document.getElementById('caddieTypeIdLabel').textContent = '#' + btn.dataset.typeid;
            document.getElementById('caddieLevel').value = btn.dataset.clevel;
            document.getElementById('caddieExp').value = btn.dataset.exp;
            document.getElementById('caddieRent').checked = btn.dataset.rentflag === '1';
        });
    });

    document.querySelectorAll('.edit-mascot-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            document.getElementById('mascotTypeId').value = btn.dataset.typeid;
            document.getElementById('mascotTypeIdLabel').textContent = '#' + btn.dataset.typeid;
            document.getElementById('mascotLevel').value = btn.dataset.mlevel;
            document.getElementById('mascotExp').value = btn.dataset.mexp;
        });
    });
});
</script>

<?php require __DIR__ . '/includes/footer.php'; ?>
