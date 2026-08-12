<?php

final class ServerMetrics
{
    public function __construct(private PDO $pdo)
    {
    }

    public function snapshot(): array
    {
        $result = [
            'registered' => 0,
            'online' => 0,
            'login_online' => false,
            'game_online' => false,
            'pang_rate' => 0,
            'exp_rate' => 0,
            'peak_online' => 0,
        ];

        try {
            $result['registered'] = (int) $this->pdo->query('SELECT COUNT(*) FROM pangya.account')->fetchColumn();
            $result['online'] = (int) $this->pdo->query('SELECT COUNT(*) FROM pangya.account WHERE [Logon] = 1')->fetchColumn();
            $result['peak_online'] = $result['online'];

            $server = $this->pdo->query(
                'SELECT TOP (1) [State], [CurrUser], [MaxUser], [AppRate], [ScratchRate]
                 FROM pangya.pangya_server_list WHERE [Type] = 1 ORDER BY [UpdateTime] DESC'
            )->fetch();

            if ($server) {
                $result['game_online'] = (int) $server['State'] === 1;
                $result['login_online'] = $result['game_online'];
                $result['online'] = max($result['online'], (int) $server['CurrUser']);
                $result['peak_online'] = max($result['peak_online'], (int) $server['CurrUser']);
                $result['exp_rate'] = (float) $server['AppRate'];
                $result['pang_rate'] = (float) $server['ScratchRate'];
            }
        } catch (PDOException $exception) {
            error_log('Não foi possível obter métricas do servidor: ' . $exception->getMessage());
        }

        return $result;
    }
}
