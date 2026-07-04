using System.Collections.Generic;
using PangyaAPI.Network.PangyaSession;
using Pangya_RankingServer.Models;
using PangyaAPI.Network.PangyaPacket;
using PangyaAPI.Utilities;

namespace Pangya_RankingServer.Session
{
    public class player_manager : SessionManager
    {
        public player_manager()
        {
            if (m_max_session != 0u)
            {
                for (var i = 0; i < m_max_session; ++i)
                    m_sessions.Add(i, new Player());
            }
            else
            {
                throw new exception("fail to class");
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
                if (el.m_sock != null && ((!oid) ? el.getUID() : (uint)el.m_oid) == uid)
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
                if (el.m_sock != null && ((el.getCapability() & 4) != 0 || (el.getCapability() & 128) != 0))
                {
                    gmList.Add((Player)el);
                }
            }

            return gmList;
        }
         
    }
}