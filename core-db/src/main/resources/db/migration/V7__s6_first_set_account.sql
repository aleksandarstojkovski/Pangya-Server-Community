-- S6: unfinished first-login account so Login 0x06/0x07/0x08 can be exercised.
-- FIRST_LOGIN=0 / FIRST_SET=0 / empty nick; no character or warehouse yet.

INSERT INTO pangya.account (
    "ID", "UID", "PASSWORD", "IDState", "BlockTime", "Logon", "FIRST_LOGIN",
    "NICK", "FIRST_SET", "Guild_UID", "Sex", "doTutorial", "LogonCount",
    "School", capability, "Event", "MannerFlag", "Event1", "Event2", domainid, "ChannelFlag"
) VALUES (
    'newuser', 10003, 'testpass', 0, 0, 0, 0,
    '', 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0
);

INSERT INTO pangya.user_info (
    "UID", "Tacadas", "Putt", "Tempo", "Tempo tacadas", "Max_distancia", "Acerto_pangya",
    "Bunker", "O.B", "Total_distancia", "Holes", "Holein", "HIO", "Timeout", "Fairway",
    "Albatross", "MaConduta", "Acerto_Putt", "Long-putt", "Chip-in", "Xp", "level", "Pang",
    "Media_score", "BestScore0", "BestScore1", "BestScore2", "BestScore3", "BestScore4",
    "MaxPang0", "maxPang1", "maxPang2", "maxPang3", "maxPang4", "SumPang", "EventFlag",
    "Jogado", "Quitado", "SkinPang", "SkinWin", "SkinLose", "SkinRunHole", "SkinStrikePoint",
    "SkinAllinCount", "Todos_combos", "Combos", "TeamWin", "TeamGames", "Teamhole",
    "LadderPoint", "LadderWin", "LadderLose", "LadderDraw", "LadderHole", "EventValue",
    "NaoSei", "MaxJogoNaoSei", "JogosNaoSei", "GameCountSeason", "Cookie",
    total_pang_win_game, lucky_medal, fast_medal, best_drive_medal, best_chipin_medal,
    best_puttin_medal, best_recovery_medal, "16bit_naosei"
) VALUES (
    10003, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 1, 0,
    0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0,
    0, 0, 0, 0, 0,
    0, 0, 0
);

INSERT INTO pangya.pangya_user_macro (
    "UID", "Macro1", "Macro2", "Macro3", "Macro4", "Macro5",
    "Macro6", "Macro7", "Macro8", "Macro9", "Macro10"
) VALUES (
    10003, 'Nice shot!', '', '', '', '', '', '', '', '', ''
);
