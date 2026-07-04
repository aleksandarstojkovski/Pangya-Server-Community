using System;
using System.Collections.Generic;
using System.Text;
using PangyaAPI.Utilities;
using PangyaAPI.Utilities.Log;
namespace Pangya_GameServer
{
    public class GameServer
    {
        static void Main()
        {

            Console.InputEncoding = Encoding.GetEncoding("Shift_JIS");
            try
            {
                sgs.gs.getInstance().Start();
                for (; ; )
                {
                    var input = Console.ReadLine();
                    var comando = new Queue<string>(input.Split(' '));
                    if (sgs.gs.getInstance().CheckCommand(comando))
                    {
                        _smp.message_pool.getInstance().push(new message($"[GameServer::CheckCommand][Log] Command Executed-> {input}", type_msg.CL_ONLY_CONSOLE));
                    }
                }
            }
            catch (exception e)
            {
                _smp.message_pool.getInstance().push(new message("[GameServer::Main][Error] " + e.getFullMessageError() + "]", type_msg.CL_FILE_LOG_AND_CONSOLE));

                throw e;
            }
        }                                                    
    }
}
