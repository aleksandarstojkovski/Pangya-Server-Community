using System;
using System.Text;
namespace MessengerServer
{
    public class MessengerServer
    {
        static void Main()
        {
            Console.InputEncoding = Encoding.GetEncoding("Shift_JIS"); 
            try
            { 
                sms.ms.getInstance().Start();
                for (; ; )
                {
                    var comando = Console.ReadLine().Split(new char[] { ' ' }, 2);
                     sms.ms.getInstance().CheckCommand(new System.Collections.Generic.Queue<string>(comando));
                }
            }
			catch (Exception e)
			{

				throw e;
			}
        }
    }
}
