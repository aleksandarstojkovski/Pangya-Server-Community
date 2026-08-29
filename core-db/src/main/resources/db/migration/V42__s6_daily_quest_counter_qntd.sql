-- C# IFF QuestStuff counter_item.qntd target for daily quest clear checks.
ALTER TABLE pangya.iff_daily_quest_stuff
    ADD COLUMN IF NOT EXISTS counter_qntd INTEGER NOT NULL DEFAULT 1;
