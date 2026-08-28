package org.pangya.network.session;

/** Per-session player / child-server fields filled after login or Auth register. */
public final class PlayerContext {

    public volatile long uid;
    public volatile String id = "";
    public volatile String nickname = "";
    public volatile int capability;
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
}
