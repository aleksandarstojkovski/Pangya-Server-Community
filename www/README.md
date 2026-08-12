# PangYa Community — Site PHP + SQL Server (System DSN)

## Organização e implantação

- `Account/`: autenticação e regras da conta.
- `Shop/`: catálogo, moedas e marketplace.
- `Server/`: métricas públicas do servidor.
- `Config/`: políticas de autorização.
- `Includes/`: componentes, auditoria e utilitários.
- `database/web_features.sql`: migração obrigatória para auditoria e marketplace.

Antes de disponibilizar o marketplace, execute `database/web_features.sql` no banco `pangya`. A migração também confirma que `pangya.pangya_item_warehouse` possui `item_id`, usado como identificador único em operações de alteração, remoção e transferência. As permissões web respeitam os bits `GameMaster` (4), `WebAdminEdit` (8) e `BlockItemSpawnGM` (16), definidos em `Config/permissions.php`.

<div align="center">

### Versão Final da Tela Inicial
<img src="https://raw.githubusercontent.com/luismk/Pangya-Server-Community/main/Documentation/home-final.png" alt="Home Final" width="100%">

<br><br>

### Evolução do Layout
<img src="https://raw.githubusercontent.com/luismk/Pangya-Server-Community/main/Documentation/home.png" alt="Home Base" width="48%"> <img src="https://raw.githubusercontent.com/luismk/Pangya-Server-Community/main/Documentation/home-2.png" alt="Home Alt" width="48%">

<br><br>

### Teste de Conexão com SQL Server
<img src="https://raw.githubusercontent.com/luismk/Pangya-Server-Community/main/Documentation/test-connection.png" alt="Teste de Conexão" width="70%">

</div>

## Estrutura de arquivos

```
pangya-site/
├── config.php              # Conexão PDO ODBC via System DSN + helpers
├── index.php                # Página inicial
├── register.php              # Formulário de cadastro
├── process_register.php      # Processa cadastro: ProcMakeUserBeta + ProcAutoItem
├── login.php                 # Login do usuário
├── logout.php                # Encerra sessão
├── dashboard.php              # Painel do usuário autenticado
├── downloads.php              # Lista de downloads
├── .htaccess                  # Bloqueia acesso a includes/ e config.php
├── includes/
│   ├── header.php             # <head> + menu de navegação
│   ├── footer.php             # Rodapé
│   └── functions.php          # clean(), isLoggedIn(), requireLogin(), flash messages
└── assets/
    └── css/style.css          # Tema escuro sobre o Bootstrap
```

## 1. Pré-requisitos no servidor

- PHP 8+ com a extensão **`pdo_odbc`** habilitada (`extension=pdo_odbc` no `php.ini`).
- Driver ODBC do SQL Server instalado (ex.: **ODBC Driver 17/18 for SQL Server**).
- Um **System DSN** já configurado apontando para o banco `pangya`.

### Criando o System DSN (Windows Server)

1. Abra **Ferramentas Administrativas > Fontes de Dados ODBC (64 bits)**.
2. Aba **DSN de Sistema** → **Adicionar...**
3. Selecione o driver `ODBC Driver 17/18 for SQL Server`.
4. Dê um nome ao DSN (ex.: `PangyaCommunityDSN`) e aponte para a instância do SQL Server.
5. Configure a autenticação (SQL Server Auth ou Windows Auth) e o banco padrão `pangya`.
6. Teste a conexão e finalize.

## 2. Configuração do projeto

Edite `config.php`:

```php
define('DSN_NAME', 'PangyaCommunityDSN'); // nome exato do DSN criado acima
define('DB_USER', 'usuario_sql');       // deixe em branco se o DSN já define usuário
define('DB_PASS', 'senha_sql');         // deixe em branco se o DSN já define senha
```

## 3. Ajustes que dependem do seu schema real

Este projeto foi montado com base nas procedures que você enviou
(`ProcMakeUserBeta`, `ProcAutoItem`). Duas telas fazem suposições sobre
nomes de colunas de `pangya.account` que talvez precisem de ajuste:

- **`login.php`**: assume que a coluna de senha se chama `PWD`. Se o nome
  real for outro (ex.: `Password`, `Passwd`), ajuste a query no arquivo.
- **`dashboard.php`**: assume colunas `Pang`, `Cookie`, `level` (vistas em
  `ProcAutoItem.sql`). Adicione/remova campos conforme sua tabela.

## 4. Fluxo de cadastro (register.php → process_register.php)

1. Valida os campos obrigatórios em PHP.
2. Verifica antecipadamente se o `ID` já existe em `pangya.account`
   (evita rodar `ProcAutoItem` duas vezes para a mesma conta, já que
   `ProcMakeUserBeta` é idempotente e retornaria o UID existente sem
   criar uma conta nova).
3. Gera a senha como `MD5` maiúsculo (`hashPassword()` em `config.php`).
4. Captura o IP do cliente (`getClientIp()`).
5. Executa `{CALL pangya.ProcMakeUserBeta (?,?,?,?,?,?,?,?,?,?)}` e lê o
   `UID` retornado via `fetchColumn()`.
6. Se o UID for válido, executa `{CALL pangya.ProcAutoItem (?)}` passando
   o UID.
7. Loga o usuário automaticamente e redireciona para `dashboard.php`.

## 5. Segurança

- Todas as queries usam **prepared statements** (proteção contra SQL Injection).
- Senhas nunca são armazenadas em texto puro (hash MD5 maiúsculo, conforme
  regra de negócio definida — considere migrar para `password_hash()`
  com bcrypt/argon2 em uma versão futura, se a procedure do lado do
  servidor de jogo permitir).
- `.htaccess` bloqueia acesso direto a `includes/` e a `config.php`.
- Sessões PHP nativas controlam o login (`$_SESSION['uid']`).

## 6. Rodando localmente para teste rápido

```bash
php -S localhost:8000
```

Acesse `http://localhost:8000/index.php`. (A conexão com o banco só
funcionará em um ambiente com o driver ODBC e o DSN configurados —
tipicamente Windows Server com IIS ou XAMPP com o driver ODBC do SQL
Server instalado.)
