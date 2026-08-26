using System.Collections.Generic;
using PangyaAPI.Network.PangyaSession;
using MessengerServer.Models;
using PangyaAPI.Network.PangyaPacket;

namespace MessengerServer.Session
{
    public class player_manager : SessionManager
    {          
        public player_manager()
        {
            if (m_max_session != 0)
            {
                for (var i = 0; i < m_max_session; ++i)
                    m_sessions.Add(i, new Player());
            }
        }

        public new void Clear()
        {
            base.Clear();
        }

        public Player findPlayer(uint? _uid, bool _oid = true)
        {

            foreach (var el in m_sessions.Values)
            {
                if ((_oid ? el.getUID() : (uint)el.m_oid) == _uid)
                {
                    return (Player)el;
                }
            }


            return null;
        }

        public Player FindPlayer(uint uid, bool oid)
        {
            Player p = null;
            foreach (var el in m_sessions.Values)
            {
                if (el.m_client != null && ((!oid) ? el.getUID() : (uint)el.m_oid) == uid)
                {
                    p = (Player)el;
                    break;
                }
            }

            return p;
        }

        public List<Player> FindAllGM()
        {
            var gmList = new List<Player>();

            foreach (var el in m_sessions.Values)
            {
                if (el.m_client != null && ((el.getCapability() & 4) != 0 || (el.getCapability() & 128) != 0))
                {
                    gmList.Add((Player)el);
                }
            }

            return gmList;
        }

        public Dictionary<uint, Player> findAllFriend(List<FriendInfoEx> friends)
        {
            var friendMap = new Dictionary<uint, Player>();

            foreach (var el in friends)
            {
                var player = (Player)findSessionByUID(el.uid);

                if (player != null && !friendMap.ContainsKey(player.m_pi.uid))
                {
                    friendMap[player.m_pi.uid] = player;
                }
            }

            return friendMap;
        }

        public Dictionary<uint, Player> findAllGuildMember(uint guildUid)
        {
            var guildMap = new Dictionary<uint, Player>();

            foreach (var el in m_sessions.Values)
            {
                var player = el as Player;

                if (player != null && player.m_pi.guild_uid > 0 && player.m_pi.guild_uid == guildUid)
                {
                    if (!guildMap.ContainsKey(player.m_pi.uid))
                    {
                        guildMap[player.m_pi.uid] = player;
                    }
                }
            }

            return guildMap;
        }
    }
}