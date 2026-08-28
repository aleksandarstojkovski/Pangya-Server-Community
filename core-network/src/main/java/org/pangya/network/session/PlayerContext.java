package org.pangya.network.session;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Per-session player / child-server fields filled after login or Auth register. */
public final class PlayerContext {

    public volatile long uid;
    public volatile String id = "";
    public volatile String nickname = "";
    public volatile int capability;
    /**
     * C# {@code GMInfo.visible} / {@code state_flag.visible}. Non-GM starts at 1;
     * GM starts at 0 (invisible until {@code CCG_VISIBLE}).
     */
    public volatile int gmVisible = 1;
    public volatile int level;
    public volatile long idState;
    public volatile int blockTime = -1;
    public volatile int loginState;
    public volatile int tipo;
    public volatile String authKeyLogin = "";
    public volatile int channelId = -1;
    public volatile int roomNumber = -1;
    public volatile boolean inPractice;
    public volatile boolean inLobby;
    /** C# {@code PlayerInfo.place}: 0 idle, 70 invited. */
    public volatile int place;
    /** C# {@code PlayerInfo.whisper}: 1 on (default), 0 blocks incoming PM. */
    public volatile int whisper = 1;
    /** C# {@code GMInfo.whisper} / {@code state_flag.whisper}. Default 1 (C# ctor). */
    public volatile int gmWhisper = 1;
    /** C# {@code GMInfo.channel} / {@code state_flag.channel}. Ctor sets 0. */
    public volatile int gmChannel;
    /**
     * C# {@code GMInfo.map_open} UIDs. Open/close whisper-list is intended add/remove
     * (C# {@code openPlayerWhisper} is inverted and never inserts).
     */
    public final Set<Long> gmWhisperPlayers = ConcurrentHashMap.newKeySet();
    /** C# {@code PlayerInfo.location} lounge xz r. */
    public volatile float locX;
    public volatile float locZ;
    public volatile float locR;
    /** C# {@code PlayerInfo.state} lounge pose. */
    public volatile int loungeState;
    /** C# {@code PlayerInfo.state_lounge} icon. */
    public volatile int stateLounge;
    /** C# {@code PlayerRoomInfo.state_flag.away} / lobby bit 0. */
    public volatile int away;
    /** C# {@code DailyQuestInfoUser.current_date} unix seconds. */
    public volatile long dailyCurrentDate;
    /** C# {@code DailyQuestInfoUser.accept_date} unix seconds. */
    public volatile long dailyAcceptDate;
    /** C# {@code DailyQuestInfoUser.count}. */
    public volatile int dailyCount;
    /** C# {@code DailyQuestInfoUser._typeid[3]}. */
    public final int[] dailyQuestTypeids = new int[3];
    /** C# {@code DolfiniLocker.pass}. Seed empty. */
    public volatile String dolfiniPass = "";
    /** C# {@code TutorialInfo.rookie}. */
    public volatile int tutoRookie;
    /** C# {@code TutorialInfo.beginner}. */
    public volatile int tutoBeginner;
    /** C# {@code TutorialInfo.advancer}. */
    public volatile int tutoAdvancer;
    /**
     * C# {@code PlayerInfo.assist_flag}. Room-wait toggle sets this; login
     * does not restore it (only {@link #assistId} from warehouse).
     */
    public volatile boolean assistFlag;
    /**
     * C# {@code PlayerInfo.Assistent.id}. Login restores from warehouse
     * assist typeid {@code 0x1BE00016}.
     */
    public volatile int assistId;
}
