<?php
/**
 * test_connection.php
 */
require_once __DIR__ . '/config.php';

$steps = []; // cada item: ['label' => string, 'ok' => bool, 'detail' => string]

function addStep(array &$steps, string $label, bool $ok, string $detail = ''): void
{
    $steps[] = ['label' => $label, 'ok' => $ok, 'detail' => $detail];
}

// -------------------------------------------------------------------
// 1. Extensão PDO_ODBC carregada?
// -------------------------------------------------------------------
$pdoOdbcLoaded = extension_loaded('pdo_odbc');
addStep(
    $steps,
    'Extensão PHP pdo_odbc habilitada',
    $pdoOdbcLoaded,
    $pdoOdbcLoaded
        ? 'OK — versão PHP ' . PHP_VERSION
        : 'Não encontrada. Habilite "extension=pdo_odbc" no php.ini e reinicie o servidor web.'
);

// -------------------------------------------------------------------
// 2. Drivers PDO disponíveis (deve conter "odbc")
// -------------------------------------------------------------------
$drivers = PDO::getAvailableDrivers();
$hasOdbcDriver = in_array('odbc', $drivers, true);
addStep(
    $steps,
    'Driver "odbc" disponível no PDO',
    $hasOdbcDriver,
    'Drivers PDO instalados: ' . (empty($drivers) ? '(nenhum)' : implode(', ', $drivers))
);

// -------------------------------------------------------------------
// 3. Tenta abrir a conexão via getConnection() (config.php)
// -------------------------------------------------------------------
$pdo = null;
$connectionOk = false;
$connectionDetail = '';

if ($pdoOdbcLoaded && $hasOdbcDriver) {
    try {
        $pdo = getConnection();
        $connectionOk = true;
        $connectionDetail = 'Conexão aberta com sucesso usando o DSN "' . DSN_NAME . '".';
    } catch (PDOException $e) {
        $connectionDetail = 'Falha ao conectar: ' . $e->getMessage();
    }
} else {
    $connectionDetail = 'Etapa pulada — pré-requisitos acima não atendidos.';
}

addStep($steps, 'Conexão PDO ODBC com o System DSN "' . DSN_NAME . '"', $connectionOk, $connectionDetail);

// -------------------------------------------------------------------
// 4. Query simples de sanidade (SELECT 1)
// -------------------------------------------------------------------
$pingOk = false;
$pingDetail = '';

if ($connectionOk) {
    try {
        $result = $pdo->query('SELECT 1 AS ping')->fetch();
        $pingOk = isset($result['ping']) && (int)$result['ping'] === 1;
        $pingDetail = $pingOk ? 'SELECT 1 executado com sucesso.' : 'Resposta inesperada do banco.';
    } catch (PDOException $e) {
        $pingDetail = 'Erro ao executar SELECT 1: ' . $e->getMessage();
    }
} else {
    $pingDetail = 'Etapa pulada — conexão não estabelecida.';
}

addStep($steps, 'Consulta de sanidade (SELECT 1)', $pingOk, $pingDetail);

// -------------------------------------------------------------------
// 5. Identifica o servidor/banco atual e a versão do SQL Server
// -------------------------------------------------------------------
$serverInfoOk = false;
$serverInfoDetail = '';

if ($pingOk) {
    try {
        $row = $pdo->query('SELECT DB_NAME() AS db_name, @@SERVERNAME AS server_name, @@VERSION AS version')->fetch();
        $serverInfoOk = true;
        $serverInfoDetail = sprintf(
            'Banco: %s | Servidor: %s | Versão: %s',
            $row['db_name'] ?? '?',
            $row['server_name'] ?? '?',
            isset($row['version']) ? strtok($row['version'], "\n") : '?'
        );
    } catch (PDOException $e) {
        $serverInfoDetail = 'Erro ao obter informações do servidor: ' . $e->getMessage();
    }
} else {
    $serverInfoDetail = 'Etapa pulada — consulta de sanidade falhou.';
}

addStep($steps, 'Informações do servidor SQL Server', $serverInfoOk, $serverInfoDetail);

// -------------------------------------------------------------------
// 6. Verifica se as tabelas/procedures-chave do projeto existem
// -------------------------------------------------------------------
$objectsOk = false;
$objectsDetail = '';
$expectedObjects = [
    'pangya.account'                        => 'U',  // tabela
    'pangya.contas_beta'                    => 'U',  // tabela
    'pangya.pangya_item_warehouse'           => 'U',  // tabela
    'pangya.ProcMakeUserBeta'                => 'P',  // procedure
    'pangya.ProcAutoItem'                    => 'P',  // procedure
];

if ($serverInfoOk) {
    try {
        $found = [];
        $stmt = $pdo->prepare("SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(?) AND type = ?");
        foreach ($expectedObjects as $objName => $type) {
            $stmt->execute([$objName, $type]);
            $found[$objName] = (bool)$stmt->fetchColumn();
        }
        $objectsOk = !in_array(false, $found, true);
        $lines = [];
        foreach ($found as $objName => $exists) {
            $lines[] = ($exists ? '✔ ' : '✘ ') . $objName;
        }
        $objectsDetail = implode(' | ', $lines);
    } catch (PDOException $e) {
        $objectsDetail = 'Erro ao verificar objetos: ' . $e->getMessage();
    }
} else {
    $objectsDetail = 'Etapa pulada — informações do servidor indisponíveis.';
}

addStep($steps, 'Tabelas e procedures esperadas existem no banco', $objectsOk, $objectsDetail);

$pageTitle = 'Teste de Conexão';
require __DIR__ . '/includes/header.php';
?>

<div class="row justify-content-center">
    <div class="col-lg-9">
        <div class="card p-4 p-md-5">
            <h2 class="mb-1">Teste de Conexão PHP + SQL Server</h2>
            <p class="text-secondary mb-4">Diagnóstico via PDO_ODBC / System DSN.</p>

            <div class="alert <?= $connectionOk ? 'alert-success' : 'alert-danger' ?>">
                <strong><?= $connectionOk ? '✅ Conexão funcionando' : '❌ Conexão com falha' ?></strong>
                — verifique os detalhes de cada etapa abaixo.
            </div>

            <table class="table table-dark align-middle">
                <thead>
                    <tr>
                        <th style="width:40px;">#</th>
                        <th>Etapa</th>
                        <th style="width:90px;">Status</th>
                        <th>Detalhes</th>
                    </tr>
                </thead>
                <tbody>
                <?php foreach ($steps as $i => $step): ?>
                    <tr>
                        <td><?= $i + 1 ?></td>
                        <td><?= htmlspecialchars($step['label']) ?></td>
                        <td>
                            <?php if ($step['ok']): ?>
                                <span class="badge bg-success">OK</span>
                            <?php else: ?>
                                <span class="badge bg-danger">FALHA</span>
                            <?php endif; ?>
                        </td>
                        <td class="small"><?= htmlspecialchars($step['detail']) ?></td>
                    </tr>
                <?php endforeach; ?>
                </tbody>
            </table>

            <div class="alert alert-warning mt-4 mb-0">
                ⚠️ Este arquivo expõe detalhes técnicos do servidor. Apague-o ou
                bloqueie o acesso (ex.: <code>.htaccess</code>) assim que terminar os testes.
            </div>
        </div>
    </div>
</div>

<?php require __DIR__ . '/includes/footer.php'; ?>
