using System.Collections.Generic;
using System.Linq;
using Pangya_GameServer.Repository;
using Pangya_GameServer.Models;
using PangyaAPI.Utilities;
using PangyaAPI.Utilities.Log;
namespace Pangya_GameServer.Game.System
{
    public class GrandZodiacEvent
    {
        List<range_time> m_rt;      // Times to make room event
        List<stReward> m_rewards;
        bool m_load;
        SYSTEMTIME m_st;                            // Usando para n�o ficar criando direto na fun��o de check

        public GrandZodiacEvent()
        {

            this.m_rt = new List<range_time>();
            this.m_rewards = new List<stReward>();
            this.m_load = false;
            this.m_st = new SYSTEMTIME();
            // Inicializa
            initialize();
        }

        public void clear()
        {

            if (!m_rt.empty())
                m_rt.Clear();

            if (!m_rewards.empty())
                m_rewards.Clear();

            m_load = false;
        }

        public void load()
        {
            if (isLoad())
                clear();

            initialize();
        }

        public bool isLoad()
        {
            return m_load;
        }

        public void initialize()
        {
            CmdGrandZodiacEventInfo cmd_bgei = new CmdGrandZodiacEventInfo(); // Waiter

            snmdb.NormalManagerDB.getInstance().add(0,
                cmd_bgei, null, null);

            if (cmd_bgei.getException().getCodeError() != 0)
            {
                throw cmd_bgei.getException();
            }

            snmdb.NormalManagerDB.getInstance().add(0,
                cmd_bgei, null, null);

            if (cmd_bgei.getException().getCodeError() != 0)
            {
                throw cmd_bgei.getException();
            }

            m_rt = cmd_bgei.getInfo();

            //var r = (range_time)m_rt[0].Clone();

            // //r.m_start.Hour = (ushort)DateTime.Now.Hour;
            // //r.m_start.Minute = (ushort)(DateTime.Now.Minute + 2);
            // ////
            // //r.m_end.Hour = (ushort)(DateTime.Now.Hour + 1);
            // //r.m_end.Minute = (ushort)DateTime.Now.Minute;

            // m_rt.Add(r);
            // Log  
            if (m_rt.Count == 0)
                _smp.message_pool.getInstance().push(new message("[GrandZodiacEvent::initialize][Warning] Not Loaded!", type_msg.CL_FILE_LOG_AND_CONSOLE));

            m_load = true;

        }
        public bool checkTimeToMakeRoom()
        {
            if (!isLoad())
            {
                _smp.message_pool.getInstance().push(new message("[GrandZodiacEvent::checkTimeToMakeRoom][Error] GrandZodiac Event not have initialized, please call init function first.", type_msg.CL_FILE_LOG_AND_CONSOLE));
                return false;
            }

            m_st.CreateTime();

            var valid_times = m_rt.Where(_el => _el.isBetweenTime(m_st)).ToList();

            return valid_times.Count > 0;
        }

        public void setSendedMessage()
        {

            if (!isLoad())
            {

                _smp.message_pool.getInstance().push(new message("[GrandZodiacEvent::setSendedMessage][Error] GrandZodiac Event not have initialized, please call init function first.", type_msg.CL_FILE_LOG_AND_CONSOLE));

                return;
            }



            m_st.CreateTime();

            for (int i = 0; i < m_rt.Count; i++)
            {
                var _el = m_rt[i];
                if (_el.isBetweenTime(m_st))
                {
                    _el.m_sended_message = true;
                }
                else
                {
                    _el.m_sended_message = false;
                }

                m_rt[i] = _el;
            }
        }
        public range_time getInterval()
        {

            if (!isLoad())
            {

                _smp.message_pool.getInstance().push(new message("[GrandZodiacEvent::getInterval][Error] GrandZodiac Event not have initialized, please call init function first.", type_msg.CL_FILE_LOG_AND_CONSOLE));

                return null;
            }

            range_time rt = null;

            m_st.CreateTime();

            var it = m_rt.Where(_el =>
            {
                return _el.isBetweenTime(m_st) && !_el.m_room_created; // pega somente os que nao foram criados!
            }).ToList();

            if (it.Any())
            {
                rt = it.First();
            }

            return rt;
        }

        public void setInterval(range_time rt)
        {
            var index = m_rt.FindIndex(c => c.RoomID == rt.RoomID);
            if (index > 0)
                m_rt[index] = rt;
        }
    }

    public class sGrandZodiacEvent : Singleton<GrandZodiacEvent>
    {
    }
}
