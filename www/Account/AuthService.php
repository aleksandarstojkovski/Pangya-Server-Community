<?php

final class AuthService
{
    public function login(string $login, string $password): ?array
    {
        $statement = getConnection()->prepare(
            'SELECT [UID], [ID], ISNULL([capability], 0) AS [capability]
             FROM pangya.account WHERE [ID] = ? AND [PASSWORD] = ?'
        );
        $statement->execute([$login, hashPassword($password)]);

        return $statement->fetch() ?: null;
    }
}
