<?php

function availableLanguages(): array
{
    return [
        'pt-BR' => 'Português',
        'en'    => 'English',
    ];
}

function currentLanguage(): string
{
    if (isset($_GET['lang']) && array_key_exists($_GET['lang'], availableLanguages())) {
        $_SESSION['lang'] = $_GET['lang'];
    }

    return $_SESSION['lang'] ?? 'pt-BR';
}

function t(string $key): string
{
    static $m = [
        'pt-BR' => [
            'home'               => 'Início',
            'register'           => 'Cadastro',
            'downloads'          => 'Downloads',
            'dashboard'          => 'Painel',
            'login'              => 'Entrar',
            'logout'             => 'Sair',
            'welcome'            => 'Bem-vindo ao PangYa Community',
            'hero'               => 'Um servidor privado dedicado a reviver a experiência clássica de PangYa — o golfe mais divertido do mundo. Crie sua conta, baixe o cliente e volte a jogar com a comunidade.',
            'create_account'     => 'Criar minha conta',
            'download_game'      => 'Baixar o jogo',
            'classic'            => 'Gameplay clássico',
            'classic_text'       => 'Personagens, mapas e itens fiéis à era de ouro do PangYa.',
            'community'          => 'Comunidade ativa',
            'community_text'     => 'Sistema de indicação de amigos com recompensas para quem convida.',
            'starter_items'      => 'Itens iniciais',
            'starter_items_text' => 'Toda conta nova recebe automaticamente um kit de itens para começar a jogar.',
            'server'             => 'Servidor PangYa Community',
            'server_desc'        => 'Código-fonte e documentação do servidor.',
            'tools'              => 'Ferramentas PangYa Suite',
            'tools_desc'         => 'Aplicativo e ferramentas complementares para o PangYa.',
            'open_project'       => 'Abrir projeto',
            'account_panel'      => 'Painel do Usuário',
            'username'           => 'ID / Usuário',
            'password'           => 'Senha',
            'no_account'         => 'Não tem conta?',
            'sign_up'            => 'Cadastre-se',
            'enter'              => 'Entrar',
            'invalid_login'      => 'Usuário ou senha inválidos.',
            'fill_login'         => 'Informe usuário e senha.',
            'create_title'       => 'Criar Conta',
            'full_name'          => 'Nome completo',
            'birth_date'         => 'Data de nascimento',
            'gender'             => 'Sexo',
            'select'             => 'Selecione...',
            'male'               => 'Masculino',
            'female'             => 'Feminino',
            'security_question'  => 'Pergunta de segurança',
            'security_answer'    => 'Resposta de segurança',
            'email'              => 'E-mail',
            'referral_code'      => 'Código de indicação',
            'referral_help'      => 'Opcional. Se um amigo te indicou, insira o código dele aqui.',
            'footer'             => 'Projeto de revival de servidor privado. Não afiliado à Ntreev.',
        ],
        'en' => [
            'home'               => 'Home',
            'register'           => 'Register',
            'downloads'          => 'Downloads',
            'dashboard'          => 'Dashboard',
            'login'              => 'Sign in',
            'logout'             => 'Sign out',
            'welcome'            => 'Welcome to PangYa Community',
            'hero'               => 'A private server dedicated to reviving the classic PangYa experience — the most fun golf game in the world. Create your account, download the client, and play again with the community.',
            'create_account'     => 'Create my account',
            'download_game'      => 'Download the game',
            'classic'            => 'Classic gameplay',
            'classic_text'       => 'Characters, courses, and items faithful to PangYa’s golden age.',
            'community'          => 'Active community',
            'community_text'     => 'A friend referral system with rewards for invitations.',
            'starter_items'      => 'Starter items',
            'starter_items_text' => 'Every new account automatically receives a starter item kit.',
            'server'             => 'PangYa Community Server',
            'server_desc'        => 'Server source code and documentation.',
            'tools'              => 'PangYa Suite Tools',
            'tools_desc'         => 'Application and complementary tools for PangYa.',
            'open_project'       => 'Open project',
            'account_panel'      => 'User Dashboard',
            'username'           => 'ID / Username',
            'password'           => 'Password',
            'no_account'         => 'Don’t have an account?',
            'sign_up'            => 'Register',
            'enter'              => 'Sign in',
            'invalid_login'      => 'Invalid username or password.',
            'fill_login'         => 'Enter your username and password.',
            'create_title'       => 'Create Account',
            'full_name'          => 'Full name',
            'birth_date'         => 'Date of birth',
            'gender'             => 'Gender',
            'select'             => 'Select...',
            'male'               => 'Male',
            'female'             => 'Female',
            'security_question'  => 'Security question',
            'security_answer'    => 'Security answer',
            'email'              => 'Email',
            'referral_code'      => 'Referral code',
            'referral_help'      => 'Optional. If a friend referred you, enter their code here.',
            'footer'             => 'Private-server revival project. Not affiliated with Ntreev.',
        ],
    ];

    return $m[currentLanguage()][$key] ?? $key;
}