using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Pangya_GameServer.Game.GameModes;
using Pangya_GameServer.Models;
using Pangya_GameServer.PacketFunc;
using Pangya_GameServer.UTIL;
using PangyaAPI.IFF.JP.Extensions;
using PangyaAPI.Utilities;
using PangyaAPI.Utilities.BinaryModels;
using PangyaAPI.Utilities.Log;
namespace Pangya_GameServer.Game.Manager
{
    public class RoomGrandZodiacEvent : Room
    {


        public class m_cs_instancia : Singleton<CriticalSectionInstancia>
        {
        }

        public class m_instancias : Singleton<List<RoomGrandZodiacEventCtx>>
        {
        }

        protected stStateRoomGrandZodiacEventSync m_state_rbge = new stStateRoomGrandZodiacEventSync();

        protected SYSTEMTIME m_now; // Data que a sala foi criada
        protected SYSTEMTIME m_start; // Data que a sala foi criada

        private CancellationTokenSource m_cancel_token_source;
        private Task m_task_chk_time_wait;
        protected PangyaSyncTimer m_timer_count_down;
        protected EventWaitHandle m_hEvent_wait_start;
        protected EventWaitHandle m_hEvent_wait_start_pulse;
        private bool m_game_running;

        public enum eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC : byte
        {
            WAIT_TIME_START,
            WAIT_10_SECONDS_START,
            WAIT_END_GAME
        }

        public class stStateRoomGrandZodiacEventSync
        {
            public stStateRoomGrandZodiacEventSync()
            {
                this.m_state = eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC.WAIT_TIME_START;
            }

            public void @lock()
            {
            }

            public void unlock()
            {
            }

            public eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC getState()
            {
                return m_state;
            }

            public void setState(eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC _state)
            {

                m_state = _state;
            }

            public void setStateWithLock(eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC _state)
            {
                m_state = _state;
            }


            protected eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC m_state;
        }

        // Static Instance vector strunct
        public class RoomGrandZodiacEventCtx
        {
            public enum eSTATE : byte
            {
                GOOD,
                DESTROYING,
                DESTROYED
            }

            public RoomGrandZodiacEventCtx(RoomGrandZodiacEvent _rbge, eSTATE _state)
            {
                this.m_rbge = _rbge;
                this.m_state = _state;
            }

            public RoomGrandZodiacEvent m_rbge { get; set; }
            public eSTATE m_state { get; set; }
        }

        public class CriticalSectionInstancia
        {
            public CriticalSectionInstancia()
            {
                this.m_state = false;
                this.m_lock = false;

                init();

            }

            public void init()
            {

                m_state = true;
            }

            public void @lock()
            {
                m_lock = true;

            }

            public void unlock()
            {
                // Desbloquea
                m_lock = false;
            }
            public bool m_state { get; set; }
            public bool m_lock { get; set; }
        }

        // Construtor e destrutor
        public RoomGrandZodiacEvent(byte channel_owner, RoomInfoEx ri) : base(channel_owner, ri)
        {

            this.m_state_rbge = new stStateRoomGrandZodiacEventSync();
            this.m_timer_count_down = null;

            // Coloca a instância no vetor estático
            push_instancia(this);

            m_start = new SYSTEMTIME(DateTime.Now.AddSeconds(10));//adiciona mais 10 minutos
            m_now = new SYSTEMTIME(DateTime.Now.AddSeconds(-10)); //remove 10 minutos e aguarda um novo tempo no count time
            // "Zera" a data colocando valores válidos
            m_now.Year = 2000;
            m_now.Month = 1;
            m_now.Day = 1;
            m_now.DayOfWeek = 0; // pode deixar assim, não é usado em DateTime constructor


            try
            {
                // Cria o evento que a thread de sincronização do tempo vai esperar
                m_hEvent_wait_start = new EventWaitHandle(false, EventResetMode.ManualReset);

                // Cria o evento que vai pulsar a thread para responder mais rápido a mudanças de jogadores
                m_hEvent_wait_start_pulse = new EventWaitHandle(false, EventResetMode.AutoReset);
            }
            catch (Exception ex)
            {
                throw new Exception("[RoomGrandZodiacEvent::RoomGrandZodiacEvent][Error] Falha ao criar eventos de sincronização do Grand Zodiac Event.", ex);
            }

            // Cria a thread que vai sincronizar o tempo de início do Grand Zodiac Event 
            StartGameLoop();
        }

        public void StartGameLoop()
        {
            if (m_task_chk_time_wait != null && !m_task_chk_time_wait.IsCompleted)
                return; // Já tá rodando

            m_cancel_token_source = new CancellationTokenSource();

            m_game_running = true;

            m_task_chk_time_wait = Task.Run(() => GameLoop(m_cancel_token_source.Token));
        }

        public void StopGameLoop()
        {
            try
            {
                if (m_task_chk_time_wait == null)
                    return;

                if (m_cancel_token_source != null && !m_cancel_token_source.IsCancellationRequested)
                {
                    m_cancel_token_source.Cancel(); // pede para parar
                }

                // Aguarda a task terminar de forma elegante
                m_task_chk_time_wait.Wait();

                // Limpa variáveis
                m_task_chk_time_wait = null;
                m_cancel_token_source.Dispose();
                m_cancel_token_source = null;

                m_game_running = false;

                _smp.message_pool.getInstance().push(
                    new message("[RoomBotGMEvent::StopGameLoop][Log] GameLoop finalizado com sucesso!", type_msg.CL_FILE_LOG_AND_CONSOLE)
                );
            }
            catch (Exception ex)
            {
                _smp.message_pool.getInstance().push(
                    new message($"[RoomBotGMEvent::StopGameLoop][ErrorSystem] {ex.Message}\n{ex.StackTrace}", type_msg.CL_FILE_LOG_AND_CONSOLE)
                );
            }
        }


        private async Task GameLoop(CancellationToken token)
        {
            try
            {
                _smp.message_pool.getInstance().push(new message("[RoomGrandZodiacEvent::GameLoop][Log] Loop iniciou corretamente.", type_msg.CL_FILE_LOG_AND_CONSOLE));

                while (!token.IsCancellationRequested)
                {
                    try
                    {
                        waitTimeStart();
                    }
                    catch (Exception ex)
                    {
                        _smp.message_pool.getInstance().push(new message($"[GameLoop][Error] {ex}", type_msg.CL_FILE_LOG_AND_CONSOLE));
                    }

                    await Task.Delay(100, token);
                }
            }
            catch (OperationCanceledException)
            {
                _smp.message_pool.getInstance().push(new message("[BotGMEvent::GameLoop][Error] Loop cancelado com sucesso.", type_msg.CL_FILE_LOG_AND_CONSOLE));
                await Task.Delay(100); // sem token
            }
            catch (Exception ex)
            {
                _smp.message_pool.getInstance().push(new message($"[GameLoop][Error] {ex}", type_msg.CL_FILE_LOG_AND_CONSOLE));
                await Task.Delay(100); // sem token
            }
        }
        public override bool isAllReady()
        {

            // é sempre true porque quem começa o jogo nessa sala é sempre o server
            // O cliente da erro na hora de começar se tiver convidado na sala
            // então verifica se não tem nenhum convidado na sala
            return !_haveInvited();
        }

        public bool startGame()
        {


            var p = new PangyaBinaryWriter();

            bool ret = true;

            try
            {

                // Verifica se j  tem um jogo inicializado e lan a error se tiver, para o cliente receber uma resposta
                if (m_pGame != null)
                {
                    throw new exception("[RoomGrandZodiacEvent::startGame][Error] Server tentou comecar o jogo na sala[NUMERO=" + Convert.ToString(m_ri.numero) + "], mas ja tem um jogo inicializado. Hacker ou Bug", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.ROOM_BOT_GM_EVENT,
                        8, 0x5900202));
                }

                // Verifica se todos est o prontos se n o da erro
                if (!isAllReady())
                {
                    throw new exception("[RoomGrandZodiacEvent::startGame][Error] Server tentou comecar o jogo na sala[NUMERO=" + Convert.ToString(m_ri.numero) + ", MASTER=" + Convert.ToString(m_ri.master) + "], mas nem todos jogadores estao prontos. Hacker ou Bug.", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.ROOM_BOT_GM_EVENT,
                    8, 0x5900202));
                }

                if (m_ri.course >= RoomInfo.eCOURSE.UNK)
                {

                    // Special Shuffle Course
                    if (m_ri.getTipo() == RoomInfo.TIPO.SPECIAL_SHUFFLE_COURSE && m_ri.getModo() == RoomInfo.eMODO.M_SHUFFLE_COURSE)
                    {

                        m_ri.course = (RoomInfo.eCOURSE)(0x80 | (byte)RoomInfo.eCOURSE.CHRONICLE_1_CHAOS);

                    }
                    else
                    { // Random normal

                        Lottery lottery = new Lottery();

                        foreach (var el in sIff.getInstance().getCourse())
                        {

                            var course_id = sIff.getInstance().getItemIdentify(el.ID);

                            if (course_id != 17 && course_id != 0x40)
                            {
                                lottery.Push(100, course_id);
                            }
                        }

                        var lc = lottery.SpinRoleta();

                        if (lc != null)
                        {
                            m_ri.course = (RoomInfo.eCOURSE)(0x80u | Convert.ToByte(lc.Value));
                        }
                    }
                }

                RateValue rv = new RateValue
                {
                    exp = m_ri.rate_exp = (uint)sgs.gs.getInstance().getInfo().rate.exp,
                    pang = m_ri.rate_pang = (uint)sgs.gs.getInstance().getInfo().rate.pang
                };

                // Angel Event
                m_ri.angel_event = sgs.gs.getInstance().getInfo().rate.angel_event == 1;

                rv.clubset = (uint)sgs.gs.getInstance().getInfo().rate.club_mastery;
                rv.rain = (uint)sgs.gs.getInstance().getInfo().rate.chuva;
                rv.treasure = (uint)sgs.gs.getInstance().getInfo().rate.treasure;

                rv.persist_rain = 0; // Persist rain flag isso   feito na classe game

                switch (m_ri.getTipo())
                {
                    case RoomInfo.TIPO.GRAND_ZODIAC_INT:
                    case RoomInfo.TIPO.GRAND_ZODIAC_ADV:
                        m_pGame = new GrandZodiac(v_sessions,
                            m_ri, rv, m_ri.channel_rookie);
                        break;
                    default:
                        throw new exception("[RoomGrandZodiacEvent::startGame][Error] Server tentou comecar o jogo na sala[NUMERO=" + Convert.ToString(m_ri.numero) + ", MASTER=" + Convert.ToString(m_ri.master) + "], mas o tipo da sala nao eh Tourney. Hacker ou Bug", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.ROOM_BOT_GM_EVENT,
                            9, 0x5900202));
                }

                // Update Room State
                m_ri.state = 0; // IN GAME

                p.init_plain(0x230);

                packet_func.room_broadcast(this,
                    p, 1);

                p.init_plain(0x231);

                packet_func.room_broadcast(this,
                    p, 1);

                uint rate_pang = (uint)sgs.gs.getInstance().getInfo().rate.pang;

                p.init_plain(0x77);

                p.WriteUInt32(rate_pang); // Rate Pang

                packet_func.room_broadcast(this,
                    p, 1);

                m_room_log.roomId = Guid.Empty;//seta toda vez que inicia sala
                //insert dados do player
                foreach (var _sessions in v_sessions)
                {
                    CreateRoomLogSql(_sessions);//criar de todos

                    _sessions.m_pGame = m_pGame;//gera a sala
                }

                // Coloca para o thread que cria o tempo sspera o jogo acabar
                m_state_rbge.setStateWithLock(eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC.WAIT_END_GAME);

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[RoomGrandZodiacEvent::startGame][Error] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));

                ret = false; // Error ao inicializar o Jogo
            }

            return ret;
        }

        public static void initFirstInstance()
        {

            if (m_cs_instancia.getInstance().m_state && m_instancias.getInstance().empty())
            {
                //_smp.message_pool.getInstance().push(new message("[RoomGrandZodiacEvent::initFirstInstance][Log] Criou primeira instance do Singleton da classe Room Grand Zodiac Event static vector.", type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }

        private void waitTimeStart()
        {
            m_game_running = true;
            try
            {
                _smp.message_pool.getInstance().push(new message("[RoomGrandZodiacEvent::waitTimeStart][Log] waitTimeStart iniciado com sucesso!", type_msg.CL_FILE_LOG_AND_CONSOLE));

                while (m_game_running)
                {
                    try
                    {
                        m_state_rbge.@lock();

                        switch (m_state_rbge.getState())
                        {
                            case eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC.WAIT_TIME_START:
                                if (m_timer_count_down != null)
                                    break;

                                bool passouDoTempo = (DateTime.Now - m_now.ConvertTime()) >= TimeSpan.FromMinutes(2);
                                bool salaCheia = _getRealNumPlayersWithoutInvited() >= m_ri.max_player;

                                if (passouDoTempo || salaCheia)
                                {
                                    if (salaCheia)
                                    {
                                        var p = new PangyaBinaryWriter(0x40);
                                        p.WriteByte(12);
                                        p.WriteUInt16(0);
                                        p.WriteUInt16(0);
                                        p.WriteUInt32(10);
                                        packet_func.room_broadcast(this, p, 1);
                                    }

                                    count_down(10);
                                    m_state_rbge.setState(eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC.WAIT_10_SECONDS_START);
                                }
                                break;
                            case eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC.WAIT_10_SECONDS_START:
                                // Nada a fazer aqui, só aguardar countdown acabar
                                break;

                            case eSTATE_ROOM_GRAND_ZODIAC_EVENT_SYNC.WAIT_END_GAME:
                                // Nada a fazer
                                break;
                        }
                    }
                    catch (Exception ex)
                    {
                        _smp.message_pool.getInstance().push(new message($"[RoomGrandZodiacEvent::waitTimeStart][ErrorSystem] {ex.Message}\n{ex.StackTrace}", type_msg.CL_FILE_LOG_AND_CONSOLE));
                    }
                    finally
                    {
                        m_state_rbge.unlock();
                    }
                    Thread.Sleep(1000); // Ajuste para evitar uso excessivo de CPU
                }
            }
            catch (Exception ex)
            {
                _smp.message_pool.getInstance().push(new message($"[RoomGrandZodiacEvent::waitTimeStart][ErrorSystem] {ex.Message}\n{ex.StackTrace}", type_msg.CL_FILE_LOG_AND_CONSOLE));
            }

            _smp.message_pool.getInstance().push(new message("[RoomGrandZodiacEvent::waitTimeStart][Log] Saindo de waitTimeStart()...", type_msg.CL_FILE_LOG_AND_CONSOLE));
        }

        public int _count_down_time(object _arg1, object _arg2)
        {

            RoomGrandZodiacEvent rbge = (RoomGrandZodiacEvent)(_arg1);
            long sec_to_start = (long)(_arg2);

            try
            {

                if (rbge != null && instancia_valid(rbge))
                {
                    if (rbge.count_down(sec_to_start) == 1)//nessa versao 10 segundos 
                    {
                        sgs.gs.getInstance().destroyRoom(rbge.m_channel_owner, (short)rbge.m_ri.numero); // Destroi a sala, se não tem players, ou não conseguiu inicializar
                    }
                }

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[RoomGrandZodiacEvent::_count_down_time][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }

            return 0;
        }

        public int count_down(long _sec_to_start)
        {
            int ret = 0;

            try
            {
                // Se chegou no zero ou menos, para tudo
                if (_sec_to_start <= 0)
                {   // Come�a o jogo

                    // exclu� o timer se ele ainda existir
                    clear_timer_count_down();

                    // Come�a o jogo se tem pelo menos 1 jogador na sala
                    if (v_sessions.Count() >= 1 && startGame())
                        sgs.gs.getInstance().sendUpdateRoomInfo(this, 3); // Update Room Info
                    else
                        ret = 1; // Destroi a sala

                }
                else
                {

                    uint wait = 0u;

                    ushort interval = 0;
                    float diff = 0.0f;

                    //pega o tempo decorrido, entre o inicio e final = resultado
                    int elapsed_sec = (m_timer_count_down != null) ? (int)m_timer_count_down.getTimeElapsed() : 0;

                    _sec_to_start -= elapsed_sec;//tempo já vem atualizado lá em cima

                    if ((diff = ((_sec_to_start - 10/*10 segundos*/) / 30.0f/* 30 segundos*/)) >= 1.0f)
                    {   // Intervalo de 30 segundos

                        if ((_sec_to_start % 30) == 0)
                        {

                            // Intervalo
                            interval = 30 * 1000;   // 30 segundos

                            wait = (uint)(interval * (int)diff);    // 30 * diff minutos em milisegundos

                        }
                        else
                        {

                            // Corrige o tempo para ficar no intervalo certo
                            wait = interval = (ushort)((_sec_to_start % 30) * 1000);

                        }

                    }
                    else if ((diff = ((_sec_to_start - 1/*1 segundo*/) / 10.0f/*10 segundos*/)) >= 1.0f)
                    {           // Intervalo de 10 segundos

                        if ((_sec_to_start % 10) == 0)
                        {

                            // Intervalo
                            interval = 10 * 1000;   // 10 segundos

                            wait = (uint)(interval * (int)diff);    // 10 * diff segundos em milisegundos

                        }
                        else
                        {

                            // Corrige o tempo para ficar no intervalo certo
                            wait = interval = (ushort)((_sec_to_start % 10) * 1000);
                        }

                    }
                    else
                    {       // Intervalo de 1 segundo

                        diff = (float)Math.Round(_sec_to_start / 1.0f);

                        // Intervalo
                        interval = 1000;    // 1 segundo

                        wait = (uint)(interval * (int)diff);    // 1 * diff segundos em milesegundos

                    }

                    var p = new PangyaBinaryWriter(0x40);
                    p.WriteByte(11);    // msg
                    p.WriteUInt16(0);     // nick vazio
                    p.WriteUInt16(0);     // msg vazio
                    p.WriteUInt32(Convert.ToUInt32(_sec_to_start));    // tempo para iniciar (10s)

                    packet_func.room_broadcast(this, p, 1);

                    // Make Time
                    if (m_timer_count_down == null || m_timer_count_down.getState() == PangyaSyncTimer.TIMER_STATE.STOP || m_timer_count_down.getState() == PangyaSyncTimer.TIMER_STATE.FINISH)
                    {
                        // Se o Shutdown Timer estiver criado descria e cria um novo
                        if (m_timer_count_down != null)
                            clear_timer_count_down();

                        // Timer de 1 segundo

                        m_timer_count_down = sgs.gs.getInstance().MakeTime(wait, new List<long> { interval }, () => _count_down_time(this, _sec_to_start));

                    }
                }
            }
            catch (exception e)
            {
                _smp.message_pool.getInstance().push(new message("[RoomGrandZodiacEvent::count_down][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
            return ret;
        }

        public void clear_timer_count_down()
        {
            try
            {
                if (m_timer_count_down != null)
                    sgs.gs.getInstance().unMakeTime(m_timer_count_down);

            }
            catch (Exception e)
            {
                _smp.message_pool.getInstance().push(new message(
                    "[RoomGrandZodiacEvent::clear_timer_count_down][ErrorSystem] " + e.Message,
                    type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }

        public void push_instancia(RoomGrandZodiacEvent _rbge)
        {

            m_cs_instancia.getInstance().@lock();

            m_instancias.getInstance().Add(new RoomGrandZodiacEventCtx(_rbge, RoomGrandZodiacEventCtx.eSTATE.GOOD));

            m_cs_instancia.getInstance().unlock();
        }

        public void pop_instancia(RoomGrandZodiacEvent _rbge)
        {

            m_cs_instancia.getInstance().@lock();

            var index = get_instancia_index(_rbge);

            if (index >= 0)
            {
                m_instancias.getInstance().RemoveAt(index);
            }

            m_cs_instancia.getInstance().unlock();
        }

        public void set_instancia_state(RoomGrandZodiacEvent _rbge, RoomGrandZodiacEventCtx.eSTATE _state)
        {

            m_cs_instancia.getInstance().@lock();

            var index = get_instancia_index(_rbge);

            if (index >= 0)
            {
                m_instancias.getInstance()[index].m_state = _state;
            }

            m_cs_instancia.getInstance().unlock();
        }

        public int get_instancia_index(RoomGrandZodiacEvent _rbge)
        {

            int index = -1;

            for (var i = 0; i < m_instancias.getInstance().Count; ++i)
            {

                if (m_instancias.getInstance()[i].m_rbge == _rbge)
                {

                    index = (int)i;

                    break;
                }
            }

            return index;
        }

        public bool instancia_valid(RoomGrandZodiacEvent _rbge)
        {

            bool valid = false;

            m_cs_instancia.getInstance().@lock();

            var index = get_instancia_index(_rbge);

            if (index >= 0)
            {
                valid = (m_instancias.getInstance()[index].m_state == RoomGrandZodiacEventCtx.eSTATE.GOOD);
            }

            m_cs_instancia.getInstance().unlock();

            return valid;
        }
    }

}
