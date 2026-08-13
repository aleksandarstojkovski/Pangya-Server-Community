@echo off
setlocal EnableDelayedExpansion

:: ==============================================================================
:: VERIFICAÇÃO DE SISTEMA OPERACIONAL (WINDOWS / WINDOWS SERVER)
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
:: CONFIGURAÇÃO DAS VARIÁVEIS DO BANCO DE DADOS (AUTENTICAÇÃO WINDOWS)
:: ==============================================================================
set "DRIVER=ODBC Driver 17 for SQL Server"
set "DSN_NAME=pangya"
set "DB_NAME=pangya"
set "SERVER=WIN-FMKGDN43P2D\SQLEXPRESS"

:: ==============================================================================
:: INÍCIO DA EXECUÇÃO
:: ==============================================================================
if "%LANG%"=="PT" (
    echo Configurando Fontes de Dados ODBC (System DSN - Autenticacao Windows)...
) else (
    echo Configuring ODBC Data Sources (System DSN - Windows Authentication)...
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

set "KEY_64=HKLM\SOFTWARE\ODBC\ODBC.INI\%DSN_NAME%"

reg add "%KEY_64%" /v "Driver" /t REG_SZ /d "C:\WINDOWS\system32\msodbcsql17.dll" /f >nul 2>&1
reg add "%KEY_64%" /v "Server" /t REG_SZ /d "%SERVER%" /f >nul 2>&1
reg add "%KEY_64%" /v "Database" /t REG_SZ /d "%DB_NAME%" /f >nul 2>&1
reg add "%KEY_64%" /v "Trusted_Connection" /t REG_SZ /d "Yes" /f >nul 2>&1
reg add "HKLM\SOFTWARE\ODBC\ODBC.INI\ODBC Data Sources" /v "%DSN_NAME%" /t REG_SZ /d "%DRIVER%" /f >nul 2>&1

if %errorlevel% equ 0 (
    if "%LANG%"=="PT" (
        echo [OK] DSN System 64-bits criada com sucesso.
    ) else (
        echo [OK] 64-bit System DSN created successfully.
    )
) else (
    if "%LANG%"=="PT" (
        echo [ERRO] Falha ao criar DSN 64-bits no Registro.
    ) else (
        echo [ERROR] Failed to create 64-bit DSN in Registry.
    )
)

:: ------------------------------------------------------------------------------
:: 2. Criar DSN de Sistema - 32-bits (SysWOW64 em SO x64)
:: ------------------------------------------------------------------------------
if "%LANG%"=="PT" (
    echo [2/2] Criando DSN System 32-bits...
) else (
    echo [2/2] Creating System DSN 32-bit...
)

if exist %windir%\SysWOW64 (
    set "KEY_32=HKLM\SOFTWARE\WOW6432Node\ODBC\ODBC.INI\%DSN_NAME%"

    reg add "!KEY_32!" /v "Driver" /t REG_SZ /d "C:\WINDOWS\SysWOW64\msodbcsql17.dll" /f >nul 2>&1
    reg add "!KEY_32!" /v "Server" /t REG_SZ /d "%SERVER%" /f >nul 2>&1
    reg add "!KEY_32!" /v "Database" /t REG_SZ /d "%DB_NAME%" /f >nul 2>&1
    reg add "!KEY_32!" /v "Trusted_Connection" /t REG_SZ /d "Yes" /f >nul 2>&1
    reg add "HKLM\SOFTWARE\WOW6432Node\ODBC\ODBC.INI\ODBC Data Sources" /v "%DSN_NAME%" /t REG_SZ /d "%DRIVER%" /f >nul 2>&1

    if !errorlevel! equ 0 (
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