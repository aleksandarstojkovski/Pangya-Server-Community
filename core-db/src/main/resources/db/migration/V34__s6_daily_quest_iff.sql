-- SQL stand-ins for C# IFF QuestStuff counter mapping and QuestItem rewards.
CREATE TABLE pangya.iff_daily_quest_stuff (
    quest_typeid INTEGER PRIMARY KEY,
    counter_typeid INTEGER NOT NULL
);

CREATE TABLE pangya.iff_daily_quest_reward (
    achievement_typeid INTEGER NOT NULL,
    seq INTEGER NOT NULL,
    reward_typeid INTEGER NOT NULL,
    qntd INTEGER NOT NULL,
    time INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (achievement_typeid, seq)
);
