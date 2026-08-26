using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.NetworkInformation;
using System.Net.Sockets; 
namespace PangyaAPI.Utilities
{
    public static class TcpClientEx
    {
        public static void SafeClose(this TcpClient client)
        {
            if (client == null)
                return;

            try
            {
                var socket = client.Client;

                if (socket != null && socket.Connected)
                {
                    try
                    {
                        socket.Shutdown(SocketShutdown.Both);
                    }
                    catch
                    {
                        // normal: socket já morto
                    }
                }
            }
            catch { }

            try
            {
                client.Close(); // Fecha stream + socket
            }
            catch { }
        }



        public static (bool check, byte[] _buffer, int len) Read(this TcpClient client)
        {
            if (client.Connected)
                return client.Client.Read();
            else
                return (false, new byte[0], 0);
        } 

        public static bool Send(this TcpClient client, byte[] buffer, int len = 0)
        {
            try
            {
                return client.GetState() != TcpState.Unknown && client.GetStream().Send(buffer, 0, len);
            }
            catch (Exception e)
            {
                throw e;
            }
        }

        public static bool Send(this NetworkStream stream, byte[] buffer, int offset, int len)
        {
            try
            {
                if (stream.CanWrite)
                {
                    stream.Write(buffer, offset, len);
                    return true;
                }

                return false;
            }
            catch (IOException ioEx)
            {
                Debug.WriteLine($"[Send] Erro de leitura: {ioEx.Message}");
                return false;
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"[Send] Erro inesperado: {ex.Message}");
                return false;
            }
        }

        public static TcpState GetState(this TcpClient tcpClient)
        {
            var foo = IPGlobalProperties.GetIPGlobalProperties()
              .GetActiveTcpConnections()
              .SingleOrDefault(x => x.LocalEndPoint.Equals(tcpClient.Client.LocalEndPoint)
                                 && x.RemoteEndPoint.Equals(tcpClient.Client.RemoteEndPoint)
              );

            return foo != null ? foo.State : TcpState.Unknown;
        }

        public static bool Shutdown(this TcpClient _sock, SocketShutdown how)
        { Shutdown(_sock.Client, how); return true; }

        public static bool Shutdown(this Socket _sock, SocketShutdown how)
        { _sock.Shutdown(how); return true; }
        public static bool SocketConnected(Socket s)
        {
            bool part1 = s.Poll(1000, SelectMode.SelectRead);
            bool part2 = (s.Available == 0);
            if (part1 & part2)
            {//connection is closed
                return false;
            }
            return true;
        }

        public static (bool Success, byte[] Buffer, int Length) Read(this Socket stream)
        {
            if (!stream.Connected)
            {
                Debug.WriteLine("[Read] Socket desconectado.");
                return (false, Array.Empty<byte>(), 0);
            }

            byte[] buffer = new byte[8192]; // 8 KB é suficiente na maioria dos casos
            try
            {
                int bytesRead = stream.Receive(buffer, 0, buffer.Length, SocketFlags.None);

                if (bytesRead == 0)
                {
                    Debug.WriteLine("[Read] Cliente desconectou durante a leitura.");
                    return (false, Array.Empty<byte>(), 0);
                }

                byte[] result = new byte[bytesRead];
                Array.Copy(buffer, result, bytesRead);

                return (true, result, bytesRead);
            }
            catch (SocketException sockEx)
            {
                Debug.WriteLine($"[Read] Socket encerrado pelo cliente: {sockEx.SocketErrorCode} - {sockEx.Message}");
                return (false, Array.Empty<byte>(), 0);
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"[Read] Erro inesperado: {ex.Message}");
                return (false, Array.Empty<byte>(), 0);
            }
        }

        public static (bool Success, byte[] Buffer, int Length) Read(this NetworkStream stream)
        {
            if (stream == null || !stream.CanRead)
            {
                Debug.WriteLine("[Read] Stream nula ou não pode ser lida.");
                return (false, Array.Empty<byte>(), 0);
            }

            byte[] buffer = new byte[8192]; // 8 KB é suficiente na maioria dos casos
            try
            {
                int bytesRead = stream.Read(buffer, 0, buffer.Length);

                if (bytesRead == 0)
                {
                    // Cliente fechou a conexão (EOF)
                    Debug.WriteLine("[Read] Cliente desconectou durante a leitura.");
                    return (false, Array.Empty<byte>(), 0);
                }

                byte[] result = new byte[bytesRead];
                Array.Copy(buffer, result, bytesRead);

                return (true, result, bytesRead);
            }
            catch (IOException ioEx) when (ioEx.InnerException is SocketException sockEx)
            {
                Debug.WriteLine($"[Read] Socket encerrado pelo cliente: {sockEx.SocketErrorCode} - {sockEx.Message}");
                return (false, Array.Empty<byte>(), 0);
            }
            catch (IOException ioEx)
            {
                Debug.WriteLine($"[Read] Erro de leitura de stream: {ioEx.Message}");
                return (false, Array.Empty<byte>(), 0);
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"[Read] Erro inesperado: {ex.Message}");
                return (false, Array.Empty<byte>(), 0);
            }
        }
    }
}
