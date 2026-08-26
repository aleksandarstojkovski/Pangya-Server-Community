using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using PangyaAPI.Network.PangyaPacket;
using PangyaAPI.Utilities;

namespace PangyaAPI.Network.PangyaSession
{
    public class SessionManager
    {
        public uint m_max_session;
        public readonly Dictionary<int, Session> m_sessions = new Dictionary<int, Session>();
        private readonly List<Session> m_session_del = new List<Session>();
        private uint m_ttl;
        public uint m_count = 0;
        private static bool m_is_init = false;
        protected IniHandle m_reader_ini;
        public readonly object _lockObject = new object();
        public SessionManager()
        {
            m_max_session = 0u;
            m_ttl = 0u;
            // Carrega as config do arquivo server.ini
            config_init();
            m_is_init = true;
        }

        public void config_init()
        {
            try
            {
                m_reader_ini = new IniHandle("server.ini");
                //read file
                m_max_session = m_reader_ini.ReadUInt32("SERVERINFO", "MAXUSER");
                m_ttl = m_reader_ini.ReadUInt32("OPTION", "TTL", 0);
            }
            catch (Exception)
            {
                throw;
            }
        }

        public virtual void Clear()
        {
            lock (_lockObject)
            {
                m_session_del.Clear();
                foreach (var session in m_sessions)
                {
                    session.Value.clear();
                }
                m_sessions.Clear();
            }
        }

        public Session AddSession(pangya_packet_handle _Packet_Handle, TcpClient socket, IPEndPoint address, byte key)
        {
            if (socket == null || !socket.Connected)
            {
                throw new InvalidOperationException("[SessionManager::AddSession] m_client is invalid.");
            }

            Session pSession = null;
            lock (_lockObject)
            {
                int index = findSessionFree();
                if (index == -1)
                {
                    throw new exception("[SessionManager::AddSession] Already reached Session limit.");
                }                                   
                pSession = m_sessions[index];
                pSession._Packet_Handle_Base = _Packet_Handle;//trata e lida com jogador 
                pSession.m_client = socket;
                pSession.Stream = socket.GetStream();
                pSession.m_addr = address;
                pSession.m_key = key;
                pSession.m_oid = index;
                pSession.m_time_start = pSession.m_tick = Environment.TickCount;        
                pSession.setState(true);
                pSession.setConnected(true);

                m_count++;
            }

            return pSession;
        }

        public virtual bool DeleteSession(Session session)
        {
            if (session == null)
            {
                throw new ArgumentNullException(nameof(session), "[SessionManager::DeleteSession] Session is null.");
            }

            bool ret = true;
            lock (_lockObject)
            {
                if (!session.getState())
                {
                    return true;
                }

                session.@lock();
                var oid = session.m_oid;
                if (ret = session.clear())
                    m_count--;

                m_sessions[oid] = session;//reseta na lista

                session.unlock();
            }
            return ret;
        }


        public List<Session> findAllGM()
        {
            List<Session> v_gm = new List<Session>();

            foreach (Session el in m_sessions.Values)
            {
                if ((el.getCapability() & 4) != 0 || (el.getCapability() & 128) != 0)    // GM
                    v_gm.Add(el);
            }

            return v_gm;
        }
        public virtual Session FindSessionByOid(uint oid)
        {
            Session session = null;
            lock (_lockObject)
            {
                foreach (var el in m_sessions.Values.Where(el => el.m_client != null))
                {
                    if (el.m_oid == oid)
                        session = el;
                }
            }
            return session;
        }

        public virtual Session findSessionByUID(uint uid)
        {
            Session session = null;
            lock (_lockObject)
            {
                session = m_sessions.Values.FirstOrDefault(el => el.m_client != null && el.getUID() == uid);
            }
            return session;
        }

        public virtual List<Session> FindAllSessionByUid(uint uid)
        {
            List<Session> sessions = new List<Session>();
            lock (_lockObject)
            {
                sessions = m_sessions.Values.Where(el => el.m_client != null && el.getUID() == uid).ToList();
            }
            return sessions;
        }

        public virtual Session FindSessionByNickname(string nickname)
        {
            Session session = null;
            lock (_lockObject)
            {
                session = m_sessions.Values.FirstOrDefault(el => el.m_client != null && el.getNickname() == nickname);
            }
            return session;
        }

        public Session GetSessionToDelete(int timeoutMs)
        {
            Session session = null;
            try
            {
                session = m_session_del.FirstOrDefault(); // Simulate the `get` functionality here with a simple select.
            }
            catch (Exception e)
            {
                if (e is TimeoutException)
                {
                    session = null;
                }
            }
            if (session != null && m_session_del.Remove(session))
                return session;
            else
                return null;
        }

        public bool isFull()
        {
            bool isFull;
            lock (_lockObject)
            {
                isFull = m_sessions.Values.Count(session => session.m_client != null) == m_sessions.Count;
            }
            return isFull;
        }

        public uint NumSessionConnected()
        {
            uint currOnline;
            lock (_lockObject)
            {
                currOnline = m_count;
            }
            return currOnline;
        }

        public bool IsInit()
        {
            return m_is_init;
        }

        public virtual int findSessionFree()
        {
            int i = 0;
            foreach (var _session in m_sessions.Values)
            {
                if (_session.m_oid < 0)
                {
                    return i;
                }
                i++;
            }
            return -1;
        }

        public bool HasSessionWithIP(string ip)
        {
            return m_sessions.Values.Any(s => s.isConnected() && s.getIP() == ip);
        }

        public Session findSessionByIP(string ip)
        {
            return m_sessions.Values.FirstOrDefault(s => s.isConnected() && s.getIP() == ip);
        }

        public List<Session> findAllSessionByIP(string ip)
        {
            return m_sessions.Values.Where(s => s.isConnected() && s.getIP() == ip).ToList();
        }

    }
}
