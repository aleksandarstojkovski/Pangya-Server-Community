using System;
using System.Diagnostics;
using System.Net.Sockets;
using PangyaAPI.Network.Cryptor;
using PangyaAPI.Network.PangyaSession;
using PangyaAPI.Network.PangyaUnit;
using PangyaAPI.Utilities;

namespace PangyaAPI.Network.PangyaPacket
{
    public abstract class pangya_packet_handle : IUnitAuthServer
    {

        public PangyaSyncTimerManager m_timer_mgr = new PangyaSyncTimerManager();

        public PacketBuffer ToServerBuffer = new PacketBuffer();
        public ToClientBuffer ToClientBuffer = new ToClientBuffer();

        //decript packet client->server
        protected abstract void dispach_packet_same_thread(Session _session, packet _packet);
        //decript packet server->client
        public abstract void dispach_packet_sv_same_thread(Session _session, packet _packet);
        //implement desconnect
        public abstract bool DisconnectSession(Session _session);

        protected bool recv_server_new(Session _session)
        {
            try
            {
                if (!_session.isConnected() || !_session.m_sock.Connected)
                    return false;//falso pq deu errado

                var result = _session.m_sock.Read();

                if (result.check)
                {
                    if (_session.isCreated() && ToServerBuffer.check_packet(result._buffer))
                    {
                        _session.m_sock.ReceiveTimeout = 0;

                        var decryptedPackets = ToServerBuffer.getPackets(result._buffer, _session.m_key); //interpreta packets
                        if (decryptedPackets.Count > 0)
                        {
                            foreach (var _packet in decryptedPackets)
                                dispach_packet_same_thread(_session, _packet);//ler e cuida com packets

                            ToServerBuffer.clear();//necessario limpar, pois pode ficar residos de dados anteriores....
                            return true; //true se caso deu certo
                        }
                        else
                        {
                            ToServerBuffer.clear();//necessario limpar, pois pode ficar residos de dados anteriores....
                            return false; //true se caso deu certo
                        }
                    }
                    else
                    {
                        Debug.WriteLine("[pangya_packet_handle::recv_new][MY] [Log] " + result.len);
                        return false;//falso pq deu errado
                    }
                }
                else
                {
                    Debug.WriteLine("[pangya_packet_handle::recv_new][MY2] [Log] " + result.len);
                    return false;//falso pq deu errado
                }
            }
            catch (SocketException se)
            {
                Debug.WriteLine("[pangya_packet_handle::recv_new] SocketException: " + se.Message);
                DisconnectSession(_session);
            }
            catch (ObjectDisposedException ode)
            {
                Debug.WriteLine("[pangya_packet_handle::recv_new] Socket fechado: " + ode.Message);
                DisconnectSession(_session);
            }
            catch (Exception e)
            {
                Debug.WriteLine("[pangya_packet_handle::recv_new] Exception: " + e.Message);
                // DisconnectSession(_session);
            }
            return false;//falso pq deu errado
        }

        protected bool recv_client_new(Session session, bool raw)
        {
            try
            {
                if (!session.isConnected() || !session.m_sock.Connected)
                    return false;//falso pq deu errado

                var result = session.m_sock.Read();

                if (result.check)
                {
                    if (session.isCreated() && result.len >= 5)
                    {
                        if (raw)
                        {
                            dispach_packet_same_thread(session, new packet(result._buffer, raw));//ler e cuida com packets
                        }
                        else
                        {
                            var decryptedPacket = ToClientBuffer.getPackets(result._buffer, session.m_key);
                            foreach (var packet in decryptedPacket)
                                dispach_packet_same_thread(session, packet);//ler e cuida com packets 
                        }
                        return true; //true se caso deu certo
                    }
                    else
                    {
                        Debug.WriteLine("[pangya_packet_handle::recv_new][MY] [Log] " + result.len);
                        return false;//falso pq deu errado
                    }
                }
                else
                {
                    Debug.WriteLine("[pangya_packet_handle::recv_new] [Log] " + result.len);
                    DisconnectSession(session);//desconecta pq deu errado
                }
            }
            catch (SocketException se)
            {
                Debug.WriteLine("[pangya_packet_handle::recv_new] SocketException: " + se.Message);
                DisconnectSession(session);
            }
            catch (ObjectDisposedException ode)
            {
                Debug.WriteLine("[pangya_packet_handle::recv_new] Socket fechado: " + ode.Message);
                DisconnectSession(session);
            }
            catch (Exception e)
            {
                Debug.WriteLine("[pangya_packet_handle::recv_new] Exception: " + e.Message);
                DisconnectSession(session);
            }
            return false;//falso pq deu errado
        }

        protected bool recv_client_new(Session _session)
        {
            try
            {
                if (!_session.isConnected() || !_session.m_sock.Connected)
                    return false;//falso pq deu errado

                var result = _session.m_sock.Read();

                if (result.check)
                {
                    if (_session.isCreated() && ToServerBuffer.check_packet(result._buffer))
                    {
                        _session.m_sock.ReceiveTimeout = 0;

                        var decryptedPackets = ToServerBuffer.getPackets(result._buffer, _session.m_key); //interpreta packets
                        if (decryptedPackets.Count > 0)
                        {
                            foreach (var _packet in decryptedPackets)
                                dispach_packet_same_thread(_session, _packet);//ler e cuida com packets

                            ToServerBuffer.clear();//necessario limpar, pois pode ficar residos de dados anteriores....
                            return true; //true se caso deu certo
                        }
                        else
                        {
                            ToServerBuffer.clear();//necessario limpar, pois pode ficar residos de dados anteriores....
                            return false; //true se caso deu certo
                        }
                    }
                    else
                    {
                        Debug.WriteLine("[pangya_packet_handle::recv_new][MY] [Log] " + result.len);
                        return false;//falso pq deu errado
                    }
                }
                else
                {
                    Debug.WriteLine("[pangya_packet_handle::recv_new][MY2] [Log] " + result.len);
                    return false;//falso pq deu errado
                }
            }
            catch (SocketException se)
            {
                Debug.WriteLine("[pangya_packet_handle::recv_new] SocketException: " + se.Message);
                DisconnectSession(_session);
            }
            catch (ObjectDisposedException ode)
            {
                Debug.WriteLine("[pangya_packet_handle::recv_new] Socket fechado: " + ode.Message);
                DisconnectSession(_session);
            }
            catch (Exception e)
            {
                Debug.WriteLine("[pangya_packet_handle::recv_new] Exception: " + e.Message);
            }
            return false;//falso pq deu errado
        }

    }
}