using System;
using PangyaAPI.Utilities.Log;
namespace Pangya_RankingServer
{
    public class RankingServer
    {
        static void Main(string[] args)
        {
			try
			{
                srs.rs.getInstance().Start();
                for (; ; )
                {
                    var comando = Console.ReadLine().Split(new char[] { ' ' }, 2);
                    if (srs.rs.getInstance().CheckCommand(new System.Collections.Generic.Queue<string>(comando)))
                        _smp.message_pool.getInstance().push(new message("[login_server::CheckCommand][Log] Command executed.", type_msg.CL_FILE_LOG_AND_CONSOLE));
                    else
                        _smp.message_pool.getInstance().push(new message("[login_server::CheckCommand][Log] Command no executed.", type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }
			catch (Exception)
			{ 
				throw;
			}
        }
    }
}
