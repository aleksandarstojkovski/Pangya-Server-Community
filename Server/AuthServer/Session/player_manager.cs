using System.Collections.Generic;
using System.Linq;
using PangyaAPI.Network.PangyaSession; 
using PangyaAPI.Utilities;
namespace AuthServer.Session
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

        public List<Player> getAllPlayer()
        {

            List<Player> v_p = new List<Player>();

            foreach (var el in m_sessions.Values) 
            {
                v_p.Add((Player)el);
            }

            return new List<Player>(v_p);
        }
        public Player findPlayer(uint _uid, bool _oid = false)
        {

            Player _Player = null;
 
            foreach (var el in m_sessions.Values)
            {
                if (((!_oid) ? el.getUID() : (uint)el.m_oid) == _uid)
                {
                    _Player = (Player)el;
                    break;
                }
            } 

            return _Player;
        }

        public List<Player> findPlayerByType(uint _type)
        {

            List<Player> v_p = new List<Player>();
            foreach (var el in m_sessions.Values)
            {
                if (el != null && el.getCapability() == _type)
                {
                    v_p.Add((Player)el);
                }
            }

            return new List<Player>(v_p);
        }
        public List<Player> findPlayerByTypeExcludeUID(uint _type, uint _uid)
        {

            List<Player> v_p = new List<Player>();
             
            foreach (var el in m_sessions.Values)
            {
                if (el != null
                    && el.getCapability() == _type
                    && el.getUID() != _uid)
                {
                    v_p.Add((Player)el);
                }
            }  
            return new List<Player>(v_p);
        }


    }
}