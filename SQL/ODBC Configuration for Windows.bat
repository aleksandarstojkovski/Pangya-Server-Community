@echo off
setlocal EnableDelayedExpansion

:: ==============================================================================
:: VERIFICAÇÃO DE SISTEMA OPERACIONAL (WINDOWS)
:: ==============================================================================
if "%OS%" neq "Windows_NT" (
    echo Este script deve ser executado exclusivamente em um sistema operacional Windows.
    echo This script must be executed exclusively on a Windows operating system.
    pause
    exit /b
)

:: ==============================================================================
:: DETECÇÃO DO IDIOMA DO SISTEMA (PORTUGUÊS / INGLÊS)
:: ==============================================================================
set "LANG=EN"
for /f "tokens=3" %%a in ('reg query "HKCU\Control Panel\International" /v "LocaleName" 2^>nul') do (
    set "LOCALE=%%a"
)
if /i "!LOCALE:~0,2!"=="pt" (
    set "LANG=PT"
)

:: ==============================================================================
:: VERIFICAÇÃO DE PRIVILÉGIOS DE ADMINISTRADOR
:: ==============================================================================
net session >nul 2>&1
if %errorlevel% neq 0 (
    if "%LANG%"=="PT" (
        echo [ERRO] Este script precisa ser executado como Administrador!
        echo Clique com o botao direito no arquivo .bat e selecione "Executar como administrador".
    ) else (
        echo [ERROR] This script must be run as Administrator!
        echo Right-click the .bat file and select "Run as administrator".
    )
    pause
    exit /b
)

:: ==============================================================================
:: CONFIGURAÇÃO DAS VARIÁVEIS DO BANCO DE DADOS E CONEXÃO
:: ==============================================================================

:: [NOME DO PROVEDOR / DRIVER ODBC]
set "DRIVER=ODBC Driver 17 for SQL Server"

:: [NOME DA DSN E DO BANCO DE DADOS]
set "DSN_NAME=pangya"
set "DB_NAME=pangya"

:: [NOME DA MÁQUINA / SERVIDOR SQL] -> Altere aqui o nome do servidor ou IP
set "SERVER=WIN-FMKGDN43P2D\SQLEXPRESS"

:: [NOME DO USUÁRIO] -> Altere aqui o usuário do banco
set "USER=sa"

:: [SENHA DO BANCO DE DADOS] -> Altere aqui a senha correspondente
set "PASS=+lesSnTJB68rjq"

:: ==============================================================================
:: INÍCIO DA EXECUÇÃO
:: ==============================================================================
if "%LANG%"=="PT" (
    echo Configurando Fontes de Dados ODBC (System DSN)...
) else (
    echo Configuring ODBC Data Sources (System DSN)...
)
echo.

:: ------------------------------------------------------------------------------
:: 1. Criar DSN de Sistema - 64-bits (x64)
:: ------------------------------------------------------------------------------
if "%LANG%"=="PT" (
    echo [1/2] Criando DSN System 64-bits...
) else (
    echo [1/2] Creating System DSN 64-bit...
)

%windir%\system32\odbcconf.exe /a {CONFIGSYSDSN "%DRIVER%" "DSN=%DSN_NAME%|Server=%SERVER%|Database=%DB_NAME%|Trusted_Connection=No"} >nul 2>&1

if %errorlevel% equ 0 (
    if "%LANG%"=="PT" (
        echo [OK] DSN System 64-bits criada com sucesso.
    ) else (
        echo [OK] 64-bit System DSN created successfully.
    )
) else (
    if "%LANG%"=="PT" (
        echo [ERRO] Falha ao criar DSN 64-bits. Verifique se o driver "%DRIVER%" esta instalado.
    ) else (
        echo [ERROR] Failed to create 64-bit DSN. Verify if driver "%DRIVER%" is installed.
    )
)

:: ------------------------------------------------------------------------------
:: 2. Criar DSN de Sistema - 32-bits (x86 em SO x64)
:: ------------------------------------------------------------------------------
if "%LANG%"=="PT" (
    echo [2/2] Criando DSN System 32-bits...
) else (
    echo [2/2] Creating System DSN 32-bit...
)

if exist %windir%\SysWOW64\odbcconf.exe (
    %windir%\SysWOW64\odbcconf.exe /a {CONFIGSYSDSN "%DRIVER%" "DSN=%DSN_NAME%|Server=%SERVER%|Database=%DB_NAME%|Trusted_Connection=No"} >nul 2>&1
    
    if %errorlevel% equ 0 (
        if "%LANG%"=="PT" (
            echo [OK] DSN System 32-bits criada com sucesso.
        ) else (
            echo [OK] 32-bit System DSN created successfully.
        )
    ) else (
        if "%LANG%"=="PT" (
            echo [ERRO] Falha ao criar DSN 32-bits.
        ) else (
            echo [ERROR] Failed to create 32-bit DSN.
        )
    )
) else (
    if "%LANG%"=="PT" (
        echo [INFO] Sistema operacional 32-bits detectado. A DSN 64-bits ja cobre todo o sistema.
    ) else (
        echo [INFO] 32-bit operating system detected. The 64-bit DSN covers the entire system.
    )
)

echo.
echo ==============================================================================
if "%LANG%"=="PT" (
    echo Processo concluido!
) else (
    echo Process completed!
)
echo ==============================================================================
pause