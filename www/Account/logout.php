<?php
require_once __DIR__ . '/../Config/config.php';

$_SESSION = [];
session_destroy();

header('Location: /index.php');
exit;
