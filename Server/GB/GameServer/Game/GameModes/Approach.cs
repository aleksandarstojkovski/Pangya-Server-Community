using Pangya_GameServer.Game.Base;
using Pangya_GameServer.Game.System;
using Pangya_GameServer.Models;
using Pangya_GameServer.PacketFunc;
using PangyaAPI.IFF.GB.Extensions;
using PangyaAPI.Network.PangyaPacket;
using PangyaAPI.Network.PangyaSession;
using PangyaAPI.Utilities;
using PangyaAPI.Utilities.BinaryModels;
using PangyaAPI.Utilities.Log;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using static Pangya_GameServer.Models.DefineConstants;
namespace Pangya_GameServer.Game.GameModes
{
    public class Approach : TourneyBase, IDisposable
    {
        private CancellationTokenSource m_cancel_token_source;
        private Task m_thread_sync_hole;

        private ManualResetEvent m_hEvent_sync_hole = new ManualResetEvent(false);
        private ManualResetEvent m_hEvent_sync_hole_pulse = new ManualResetEvent(false);
        // Approach State Sync
        protected stStateApproachSync m_state_app = new stStateApproachSync();

        // Current mission
        protected mission_approach_ex m_mission = new mission_approach_ex();

        protected bool m_approach_state;

        protected int m_timeout; // Tempo do hole acabou
        protected object m_cs_sync_shot = new object();

        protected enum STATE_APPROACH_SYNC : byte
        {
            LOAD_HOLE,
            LOAD_CHAR_INTRO,
            END_SHOT,
            WAIT_END_GAME
        }

        protected class stStateApproachSync
        {
            public stStateApproachSync()
            {
                this.m_state = STATE_APPROACH_SYNC.LOAD_HOLE;
                m_cs = new object();
            }

            public void @lock()
            {
                Monitor.Enter(m_cs);
            }

            public void unlock()
            {
                Monitor.Exit(m_cs);
            }

            public STATE_APPROACH_SYNC getState()
            {
                return m_state;
            }

            public void setState(STATE_APPROACH_SYNC _state)
            {

                m_state = _state;
            }

            public void setStateWithLock(STATE_APPROACH_SYNC _state)
            {

                @lock();

                m_state = _state;

                unlock();
            }


            protected STATE_APPROACH_SYNC m_state;

            protected object m_cs = new object();
        }


        public Approach(List<Player> _players, RoomInfoEx _ri, RateValue _rv, bool _channel_rookie) : base(_players, _ri, _rv, _channel_rookie)

        {
            this.m_approach_state = false;
            this.m_thread_sync_hole = null;
            this.m_state_app = new stStateApproachSync();
            this.m_timeout = 0;
            this.m_mission = new mission_approach_ex();



            if (!sApproachMissionSystem.getInstance().isLoad())
            {
                sApproachMissionSystem.getInstance().load();
            }

            // Inicializa o Treasure Hunter System
            if (!sTreasureHunterSystem.getInstance().isLoad())
            {
                sTreasureHunterSystem.getInstance().load();
            }

            var course = sTreasureHunterSystem.getInstance().findCourse((byte)(m_ri.getMap() & 0x7F));

            if (course == null)
            {
                _smp.message_pool.getInstance().push(new message("[Approach::Approach][Error] tentou pegar o course do Treasure Hunter System, mas o course[COURSE=" + Convert.ToString((ushort)(m_ri.getMap() & 0x7F)) + "] nao existe no sistema", type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
            else
            {
                sTreasureHunterSystem.getInstance().updateCoursePoint(course, -1); // -1 ponto a cada jogo iniciado
            }

            // Aqui tem que inicializar os players info
            initAllPlayerInfo();

            m_state = init_game();

            start_thread_syncHoleTime();
        }

        public int sort_approach_rank_place(approach_dados_ex ad1, approach_dados_ex ad2)
        {
            // Se ad1 está ativo e ad2 não -> ad2 vem primeiro
            if (ad1.state.ucState != 0u && ad2.state.ucState == 0u)
                return 1;

            // Se ad2 está ativo e ad1 não -> ad1 vem primeiro
            if (ad2.state.ucState != 0u && ad1.state.ucState == 0u)
                return -1;

            // Se distâncias são iguais, desempata pelo tempo (menor tempo vem primeiro)
            if (ad1.distance == ad2.distance)
                return ad1.time.CompareTo(ad2.time);

            // Senão, menor distância vem primeiro
            return ad1.distance.CompareTo(ad2.distance);
        }

        public new PlayerApproachInfo INIT_PLAYER_INFO(string _method, string _msg, Player __session)
        {
            var pgi = getPlayerInfo((__session));
            if (pgi == null)
                throw new exception($"[{GetType().Name}::" + _method + "][Error] PLAYER[UID=" + __session.m_pi.uid + "] " + _msg + ", mas o game nao tem o info dele guardado. Bug", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.GAME, 1, 4));

            return (PlayerApproachInfo)pgi;
        }

        public void start_thread_syncHoleTime()
        {
            if (m_thread_sync_hole != null && !m_thread_sync_hole.IsCompleted)
                return; // Já tá rodando

            m_cancel_token_source = new CancellationTokenSource();

            m_thread_sync_hole = Task.Run(() => syncHoleTime());
        }

        public void clearRoom()
        {
            try
            {
                // marca que o Approach não está mais ativo
                m_approach_state = false;

                if (m_game_init_state != 2)
                    finish();

                // espera jogadores finalizarem
                while (!PlayersCompleteGameAndClear())
                    Thread.Sleep(500);

                deleteAllPlayer();

                // finaliza thread de syncHoleTime
                finish_thread_sync_hole();

                // libera o "critical section" (em C# o lock é automático, mas se tivesse Mutex usaria Dispose nele)
                if (m_cs_sync_shot is IDisposable disp)
                    disp.Dispose();

#if DEBUG
                _smp.message_pool.getInstance().push(
                    new message($"[Approach::Dispose][Log] Approach destroyed on Room[Number={m_ri.numero}]", type_msg.CL_FILE_LOG_AND_CONSOLE));
#endif
            }
            catch (AggregateException ae)
            {
                foreach (var ex in ae.InnerExceptions)
                    _smp.message_pool.getInstance().push(new message($"[Dispose][TaskError] {ex}", type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }

        private void finish_thread_sync_hole()
        {
            try
            {
                if (m_cancel_token_source != null)
                {
                    m_cancel_token_source.Cancel();
                    m_cancel_token_source.Dispose();
                    m_cancel_token_source = null;
                }

                if (m_hEvent_sync_hole != null)
                    m_hEvent_sync_hole.Dispose();

                if (m_hEvent_sync_hole_pulse != null)
                    m_hEvent_sync_hole_pulse.Dispose();

                m_thread_sync_hole?.Wait(); // Espera encerrar 

            }
            catch (AggregateException ae)
            {
                foreach (var ex in ae.InnerExceptions)
                    _smp.message_pool.getInstance().push(new message($"[Dispose][TaskError] {ex}", type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
            finally
            {
                m_thread_sync_hole?.Dispose();
                m_thread_sync_hole = null;
            }
            m_hEvent_sync_hole?.Dispose();
            m_hEvent_sync_hole_pulse?.Dispose();
            m_thread_sync_hole = null;
            m_hEvent_sync_hole = null;
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                _smp.message_pool.getInstance().push(new message("[Approach::Dispose][Log] Approach destroyed on Room[Number=" + (m_ri.numero) + "]", type_msg.CL_FILE_LOG_AND_CONSOLE));
                clearRoom();
            }
            base.Dispose(disposing);
        }



        public override bool deletePlayer(Player _session, int _option)
        {

            if (_session == null)
            {
                throw new exception("[Approach::deletePlayer][Error] tentou deletar um player, mas o seu endereco eh nullptr.", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.APPROACH,
                    50, 0));
            }

            bool ret = false;

            try
            {
                var it = m_players.Find(c => c == _session);

                if (it != null)
                {

                    byte opt = 3; // Saiu Quitou

                    PangyaBinaryWriter p = new PangyaBinaryWriter();

                    var sessions = getSessions(it);

                    if (m_game_init_state == 1)
                    {

                        var pgi = INIT_PLAYER_INFO("deletePlayer",
                            "tentou sair do jogo",
                            _session);

                        requestFinishItemUsedGame((it)); // Salva itens usados no Approach

                        requestSaveInfo((it), (_option == 0x800) ? 5 : 1); // Quitou ou tomou DC

                        setGameFlag(pgi, PlayerGameInfo.eFLAG_GAME.QUIT);

                        // Saiu do approach
                        pgi.m_app_dados.setLeftGame();

                        // Resposta Player saiu do Jogo, tira ele do list de score
                        p.init_plain(0x61);

                        p.WriteInt32(it.m_oid);

                        packet_func.vector_send(p,
                            sessions, 1);

                        // Resposta Player saiu do jogo MSG
                        p.init_plain(0x40);

                        p.WriteByte(2); // Player Saiu Msg

                        p.WriteString(it.m_pi.nickname);

                        p.WriteUInt16(0); // size Msg, n o precisa de msg o pangya j  manda na opt 2

                        packet_func.vector_send(p,
                            sessions, 1);

                        // Resposta Player saiu do jogo
                        sendUpdateState(_session, opt);

                        if (AllCompleteGameAndClear())
                        {
                            ret = true; // Termina o Approach
                        }

                        sendUpdateInfoAndMapStatistics(_session, -1);

                    }

                    // Delete Player
                    m_players.Remove(it);
                }
                else
                {
                    _smp.message_pool.getInstance().push(new message("[Approach::deletePlayer][Warning] player ja foi excluido do game.", type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }
            catch (exception e)
            {
                _smp.message_pool.getInstance().push(new message("[Approach::deletePlayer][Error] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }

            return ret;
        }

        public void deleteAllPlayer()
        {
            while (m_players.Any())
            {
                deletePlayer(m_players.FirstOrDefault(), 0);
            }
        }

        public override bool requestFinishLoadHole(Player _session, packet _packet)
        {
            ////REQUEST_BEGIN("FinishLoadHole");

            bool ret = false;

            try
            {

                m_state_app.setStateWithLock(STATE_APPROACH_SYNC.LOAD_HOLE);

                var pgi = INIT_PLAYER_INFO("requestFinishLoadHole",
                    "tentou finalizar carregamento do hole no jogo",
                    _session);

                setLoadHole(pgi);

                _smp.message_pool.getInstance().push(new message("[Approach::requestFinishLoadHole][Log] " + _session.m_pi.id, type_msg.CL_FILE_LOG_AND_CONSOLE));

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[Approach::requestFinishLoadHole][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }

            return ret;
        }

        public override void requestFinishCharIntro(Player _session, packet _packet)
        {
            ////REQUEST_BEGIN("FinishCharIntro");

            try
            {

                var pgi = INIT_PLAYER_INFO("requestFinishCharIntro",
                    "tentou finalizar intro do char no jogo",
                    _session);

                // Zera todas as tacada num dos players
                pgi.data.tacada_num = 0;

                // Giveup Flag
                pgi.data.giveup = 0;


                // Set
                pgi.finish_char_intro = 1;

                _smp.message_pool.getInstance().push(new message("[Approach::requestFinishCharIntro][Log] " + _session.m_pi.id, type_msg.CL_FILE_LOG_AND_CONSOLE));

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[Approach::requestFinishCharIntro][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }
        public override void changeHole(Player _session)
        {

            if (checkEndGame(_session))
            {
                finish_approach(_session, 0);
            }
            else
            {
                // Resposta terminou o hole
                updateFinishHole(_session, 1); // Terminou
            }
        }
        public override void finishHole(Player _session)
        {

            requestFinishHole(_session, 0);

            requestUpdateItemUsedGame(_session);

            delete_all_quiter();
        }
        public override void requestInitShot(Player _session, packet _packet)
        {
            ////REQUEST_BEGIN("InitShot");

            try
            {

                ShotDataEx sd = new ShotDataEx();

                // Power Shot
                #region Read Shot Sync Data
                sd.option = _packet.ReadUInt16();

                if (sd.option == 1)
                {
                    sd.power_shot.option = _packet.ReadByte();
                    sd.power_shot.decrease_power_shot = _packet.ReadInt32();
                    sd.power_shot.increase_power_shot = _packet.ReadInt32();
                }

                //READ SHOTDataBase primeiro, primeiro se lê a classe base, e depois a classe que herda.

                sd.bar_point[0] = _packet.ReadSingle();
                sd.bar_point[1] = _packet.ReadSingle();

                sd.ball_effect[0] = _packet.ReadSingle();
                sd.ball_effect[1] = _packet.ReadSingle();

                sd.acerto_pangya_flag = _packet.ReadByte();
                sd.special_shot.ulSpecialShot = _packet.ReadUInt32();
                sd.time_hole_sync = _packet.ReadUInt32();
                sd.mira = _packet.ReadSingle();

                sd.time_shot = _packet.ReadUInt32();
                sd.bar_point1 = _packet.ReadSingle();
                sd.club = _packet.ReadByte();

                sd.fUnknown[0] = _packet.ReadSingle();
                sd.fUnknown[1] = _packet.ReadSingle();

                sd.impact_zone_pixel = _packet.ReadSingle();

                sd.natural_wind[0] = _packet.ReadInt32();
                sd.natural_wind[1] = _packet.ReadInt32();
                #endregion

                var pgi = INIT_PLAYER_INFO("requestInitShot",
                    "tentou iniciar tacada no jogo",
                    _session);

                pgi.shot_data = sd;

            }
            catch (exception e)
            {
                _smp.message_pool.getInstance().push(new message("[Approach::requestInitShot][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }
        public void finish_approach(Player _session, int _option)
        {

            if (m_players.Count > 0 && m_game_init_state == 1)
            {

                var pgi = INIT_PLAYER_INFO("finish_approach",
                    "tentou terminar o tourney no jogo",
                    _session);

                if (pgi.flag == PlayerGameInfo.eFLAG_GAME.PLAYING)
                {

                    // Calcula os pangs que o player ganhou
                    requestCalculePang(_session);

                    // Atualizar os pang do player se ele estiver com assist ligado, e for maior que beginner E
                    updatePlayerAssist(_session);

                    if (m_game_init_state == 1 && _option == 0)
                    {

                        // Achievement Counter
                        pgi.sys_achieve.incrementCounter(0x6C400004u /*/ Normal game complete /*/);

                    }
                    else if (m_game_init_state == 1 && _option == 1)
                    { // Acabou o Tempo

                        requestFinishHole(_session, 1); // Acabou o Tempo
                    }
                }

                setGameFlag(pgi, (_option == 0) ? PlayerGameInfo.eFLAG_GAME.FINISH : PlayerGameInfo.eFLAG_GAME.END_GAME);

                pgi.time_finish.CreateTime();

                if (AllCompleteGameAndClear() && m_game_init_state == 1)
                {
                    finish(); // Envia os pacotes que termina o jogo Ex: 0xCE, 0x79 e etc
                }
            }
        }
        public override bool requestFinishGame(Player _session, packet _packet)
        {
            ////REQUEST_BEGIN("FinishGame");

            bool ret = false;

            try
            {

                // OID do player que enviou o pacote para terminar o Jogo(Approach)
                uint oid = _packet.ReadUInt32();

                if (oid != _session.m_oid)
                {
                    throw new exception("[Approach::requestFinishGame][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + ", OID=" + Convert.ToString(_session.m_oid) + "] OID(" + Convert.ToString(oid) + ") is wrong", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.APPROACH,
                        410, 0));
                }

                // Packet0CB
                ret = finish_game(_session, 0xCB);

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[Approach::requestFinishGame][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }

            return ret;
        }
        public override void timeIsOver()
        {

            m_timeout = 1;

            short hole = -1;

            if (m_players.Count > 0)
            {

                var pgi = INIT_PLAYER_INFO("timeIsOver",
                    "acabou o tempo do hole, tentou pegar o info do primeiro player do jogo",
                    m_players[0]);

                hole = pgi.hole;
            }

            _smp.message_pool.getInstance().push(new message("[Approach::timeIsOver][Log] Tempo do Hole[" + Convert.ToString(hole) + "] acabou no Approach. na sala[NUMERO=" + Convert.ToString(m_ri.numero) + "]", type_msg.CL_FILE_LOG_AND_CONSOLE));

        }
        protected override bool init_game()
        {

            if (m_players.Count > 0)
            {

                // variavel que salva a data local do sistema
                initGameTime();

                m_game_init_state = 1; // Come ou

                m_approach_state = true;
            }

            return true;
        }

        protected override uint getCountPlayersGame()
        {
            return (uint)m_player_info.Count(_el =>
                _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT ||
                ((PlayerApproachInfo)_el.Value).m_app_dados.state_quit == approach_dados_ex.eSTATE_QUIT.SQ_QUIT_START
            );
        }

        protected override void requestUpdateItemUsedGame(Player _session)
        {

            var pgi = INIT_PLAYER_INFO("requestUpdateItemUsedGame",
                "tentou atualizar itens usado no jogo",
                _session);

            var ui = pgi.used_item;

            ui.club.count += (uint)(TRANSF_SERVER_RATE_VALUE(m_rv.clubset) * TRANSF_SERVER_RATE_VALUE(ui.rate.club));

            // Passive Item exceto Time Booster e Auto Command, que soma o contador por uso, o cliente passa o pacote, dizendo que usou o item
            foreach (var el in ui.v_passive)
            {

                // Passive Item no Approach s  consome os item boost de pang e o Club Mastery Boost,
                // Consome todos os outros menos os de Experi ncia
                if (passive_item_exp.Any(predicate: c => c == el.Value._typeid))
                {
                    if (CHECK_PASSIVE_ITEM(el.Value._typeid)
                    && el.Value._typeid != TIME_BOOSTER_TYPEID/* / Time Booster /*/


                        && el.Value._typeid != AUTO_COMMAND_TYPEID)
                    {
                        el.Value.count++;
                    }
                    else if (sIff.getInstance().getItemGroupIdentify(el.Value._typeid) == sIff.getInstance().BALL || sIff.getInstance().getItemGroupIdentify(el.Value._typeid) == sIff.getInstance().AUX_PART)
                    {

                        el.Value.count++;
                    }
                }
            }
        }
        public void finish()
        {

            m_game_init_state = 2; // Acabou

            requestCalculeRankPlace();

            top_rank_win();

            finishAllDadosApproach();

            foreach (var el in m_players)
            {

                var pgi = INIT_PLAYER_INFO("finish",
                    "tentou finalizar os dados do jogador no jogo",
                    el);

                if (pgi.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                {
                    requestFinishData(el);
                }
            }
        }
        public void requestFinishData(Player _session)
        {

            // Finish Artefact Frozen Flame agora   direto no Finish Item Used Game
            requestFinishItemUsedGame(_session);

            requestDrawTreasureHunterItem(_session);

            // Resposta terminou game - Placar
            sendPlacar(_session);

            // Resposta Treasure Hunter Item Draw
            sendTreasureHunterItemDrawGUI(_session);

            requestSaveInfo(_session, 0);

            // Passa o finaliza o Approach
            PangyaBinaryWriter p = new PangyaBinaryWriter((ushort)0x151);

            packet_func.game_broadcast(this,
                p, 1);

            // Resposta Treasure Hunter Item
            requestSendTreasureHunterItem(_session);
        }
        protected override void requestTranslateSyncShotData(Player _session, ShotSyncData _ssd)
        {

            //CHECK_SESSION_BEGIN("requestTranslateSyncShotData");

            try
            {

                var s = findSessionByOID(_ssd.oid);

                if (s == null)
                {
                    throw new exception("[Approach::requestTranslateSyncShotData][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] tentou sincronizar tacada do PLAYER[OID=" + Convert.ToString(_ssd.oid) + "], mas o player nao existe nessa jogo. Hacker ou Bug", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.APPROACH,
                        200, 0));
                }

                // Update Sync Shot Player
                if (_session.m_pi.uid == s.m_pi.uid)
                {

                    var pgi = INIT_PLAYER_INFO("requestTranslateSyncShotData",
                        "tentou sincronizar a tacada no jogo",
                        _session);

                    pgi.shot_sync = _ssd;

                    // Last Location Player
                    var last_location = pgi.location;

                    // Update Location Player
                    pgi.location.x = _ssd.location.x;
                    pgi.location.z = _ssd.location.z;

                    // Update Pang and Bonus Pang
                    pgi.data.pang = _ssd.pang;
                    pgi.data.bonus_pang = _ssd.bonus_pang;

                    // J  s  na fun  o que come a o tempo do player do turno
                    pgi.data.tacada_num++;

                    if (_ssd.state == ShotSyncData.SHOT_STATE.OUT_OF_BOUNDS || _ssd.state == ShotSyncData.SHOT_STATE.UNPLAYABLE_AREA)
                    {

                        pgi.data.tacada_num++;

                        // Approach
                        pgi.m_app_dados.state.ob_or_water_hazard = 1;
                    }

                    var hole = m_course.findHole(pgi.hole);

                    if (hole == null)
                    {
                        throw new exception("[Approach::requestTranslateSyncShotData][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] tentou sincronizar tacada no hole[NUMERO=" + Convert.ToString((ushort)pgi.hole) + "], mas o numero do hole is invalid. Bug", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.APPROACH,
                            12, 0));
                    }

                    // Conta j  a pr xima tacada, no give up
                    if (!_ssd.state_shot.display.acerto_hole && hole.getPar().total_shot <= (pgi.data.tacada_num + 1))
                    {

                        // +1 que   giveup, s  add se n o passou o n mero de tacadas
                        if (pgi.data.tacada_num < hole.getPar().total_shot)
                        {
                            pgi.data.tacada_num++;
                        }

                        pgi.data.giveup = 1;

                        // Approach
                        pgi.m_app_dados.state.giveup = 1;

                        // Soma +1 no Bad Condute
                        pgi.data.bad_condute++;
                    }

                    // Approach
                    if (_ssd.state == ShotSyncData.SHOT_STATE.INTO_HOLE || Math.Abs(hole.getPinLocation().diffXZ(pgi.location) * MEDIDA_PARA_YARDS) <= 0.08 /*/ *Est dentro do hole chip -@in * /*/)

                    {
                        pgi.m_app_dados.state.chip_in = 1;
                    }

                    // Pega a distancia e o tempo
                    int elapsed_time = (int)(m_ri.time_30s - m_timer.getElapsed());
                    if (elapsed_time <= 0)
                    {
                        pgi.m_app_dados.state.timeout = 1;
                    }

                    if (pgi.m_app_dados.state.ucState == 0u)
                    {

                        pgi.m_app_dados.distance = (uint)(Math.Round(hole.getPinLocation().diffXZ(pgi.location) * MEDIDA_PARA_YARDS * 10));
                        pgi.m_app_dados.time = (uint)elapsed_time;

                    }
                    else
                    {

                        pgi.m_app_dados.distance = (uint)(Math.Round(hole.getPinLocation().diffXZ(last_location) * MEDIDA_PARA_YARDS * 10));
                        pgi.m_app_dados.time = (uint)(elapsed_time <= 0 ? 0 : elapsed_time);
                    }

                    _smp.message_pool.getInstance().push(new message("[Approach::requestTranslateSyncShotData][Log] SyncShot(time_shot: " + Convert.ToString(pgi.shot_sync.tempo_shot) + "), Timer(Elapsed: " + Convert.ToString(elapsed_time) + ")", type_msg.CL_FILE_LOG_AND_CONSOLE));
                }

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[TourneyBase::requestTranslateSyncShotData][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }
        protected override void requestReplySyncShotData(Player _session)
        {
            //CHECK_SESSION_BEGIN("requestReplySyncShotData");

            try
            {

                // Resposta Sync Shot
                sendSyncShot(_session);

                var pgi = INIT_PLAYER_INFO("requestReplySyncShotData",
                    "tentou responder o Sync Shot Data",
                    _session);

                // Set Sync Shot Flag
                setSyncShot(pgi);

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[Approach::requestReplySyncShotData][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }
        public override int checkEndShotOfHole(Player _session)
        {

            var pgi = INIT_PLAYER_INFO("checkEndShotOfHole",
                "tentou verificar o fim do shot no hole",
                _session);

            setFinishShot(pgi);

            return 0;
        }
        public override void updateFinishHole(Player _session, int option)
        {

            var pgi = INIT_PLAYER_INFO("updateFinishHole",
                "tentou terminar o hole no jogo",
                _session);

            _smp.message_pool.getInstance().push(new message("[Approach::updateFinishHole][Log] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] Terminou o hole[NUMERO=" + Convert.ToString(pgi.hole) + "].", type_msg.CL_FILE_LOG_AND_CONSOLE));

            PangyaBinaryWriter p = new PangyaBinaryWriter((ushort)0x65);

            packet_func.game_broadcast(this,
                p, 1);
        }
        public void sendRatesOfApproach()
        {

            try
            {
                // Table Rate Voice And Effect
                TableRateVoiceAndEffect table = new TableRateVoiceAndEffect("W_BIGBONGDARI", TableRateVoiceAndEffect.eTYPE.W_BIGBONGDARI);

                // Rate Table Voice
                var p = new PangyaBinaryWriter((ushort)0x115);

                p.WriteString(table.name);

                p.WriteBytes(table.table, table.table.Length);

                packet_func.game_broadcast(this,
                    p, 1);

                // Table Rate Voice And Effect
                table = new TableRateVoiceAndEffect("R_BIGBONGDARI", TableRateVoiceAndEffect.eTYPE.R_BIGBONGDARI);

                p.init_plain(0x115);

                p.WriteString(table.name);

                p.WriteBytes(table.table, table.table.Length);

                packet_func.game_broadcast(this,
                    p, 1);

                // Table Rate Voice And Effect
                table = new TableRateVoiceAndEffect("VOICE_CLUB", TableRateVoiceAndEffect.eTYPE.VOICE_CLUB);
                p.init_plain(0x115);

                p.WriteString(table.name);

                p.WriteBytes(table.table, table.table.Length);

                packet_func.game_broadcast(this,
                    p, 1);


            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[Approach::sendRatesOfApproach][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }
        protected override void requestFinishHole(Player _session, int option)
        {

            var pgi = INIT_PLAYER_INFO("requestFinishHole",
                "tentou finalizar o dados do hole do player no jogo",
                _session);

            var hole = m_course.findHole(pgi.hole);

            if (hole == null)
            {
                throw new exception("[Approach::finishHole][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] tentou finalizar hole[NUMERO=" + Convert.ToString((ushort)pgi.hole) + "] no jogo, mas o numero do hole is invalid. Bug", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.GAME,
                    20, 0));
            }

            uint time_hole = 0;
            uint distance_hole = 0;

            // Finish Hole Dados
            if (option == 0)
            {

                pgi.m_app_dados.total_distance += pgi.m_app_dados.distance;
                pgi.m_app_dados.total_time += pgi.m_app_dados.time;
                pgi.m_app_dados.total_box += pgi.m_app_dados.box;
                pgi.m_app_dados.total_box += pgi.m_app_dados.rank_box;

                time_hole = pgi.m_app_dados.time;
                distance_hole = pgi.m_app_dados.distance;

                _smp.message_pool.getInstance().push(new message("[Approach::requestFinishHole][Log] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] terminou o hole[COURSE=" + Convert.ToString(hole.getCourse()) + ", NUMERO=" + Convert.ToString(hole.getNumero()) + ", PAR=" + Convert.ToString(hole.getPar().par) + ", DISTANCE=" + Convert.ToString(distance_hole) + ", TIME=" + Convert.ToString(time_hole) + ", BOX=" + Convert.ToString(pgi.m_app_dados.box) + ", RANK_BOX=" + Convert.ToString(pgi.m_app_dados.rank_box) + ", TOTAL_DISTANCE=" + Convert.ToString(pgi.m_app_dados.total_distance) + ", TOTAL_TIME=" + Convert.ToString(pgi.m_app_dados.total_time) + ", TOTAL_BOX=" + Convert.ToString(pgi.m_app_dados.total_box) + "]", type_msg.CL_FILE_LOG_AND_CONSOLE));

                pgi.m_app_dados.distance = 0;
                pgi.m_app_dados.time = 0;
                pgi.m_app_dados.box = 0;
                pgi.m_app_dados.rank_box = 0;
                pgi.m_app_dados.state.ucState = 0;

                // Zera dados
                pgi.data.time_out = 0;

                // Giveup Flag
                pgi.data.giveup = 0;

                // Zera as penalidades do hole
                pgi.data.penalidade = 0;

            }

            // Aqui tem que atualiza o PGI direitinho com outros dados
            pgi.progress.hole = (short)m_course.findHoleSeq(pgi.hole);

            // Dados Game Progress do Player
            if (option == 0)
            {

                if (pgi.progress.hole > 0)
                {

                    if (pgi.shot_sync.state_shot.display.acerto_hole)
                    {
                        pgi.progress.finish_hole[pgi.progress.hole - 1] = 1; // Terminou o hole
                    }

                    pgi.progress.par_hole[pgi.progress.hole - 1] = hole.getPar().par;
                    pgi.progress.score[pgi.progress.hole - 1] = (sbyte)time_hole;
                    pgi.progress.tacada[pgi.progress.hole - 1] = distance_hole;
                }

            }
        }
        protected override void requestSaveInfo(Player _session, int option)
        {

            var pgi = INIT_PLAYER_INFO("requestSaveInfo",
                "tentou salvar o info dele no jogo",
                _session);

            try
            {

                _smp.message_pool.getInstance().push(new message("[Approach::requestSaveInfo][Log] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] UserInfo[" + pgi.ui.ToString() + "]", type_msg.CL_FILE_LOG_AND_CONSOLE));

                // Limpa o User Info por que n o add nada, s  o tempo e os pangs ganhos
                pgi.ui.clear();
                var diff = UtilTime.GetLocalDateDiff(m_start_time);

                pgi.ui.tempo = (int)diff;
                // Pode tirar pangs
                ulong total_pang = (ulong)(pgi.data.pang + pgi.data.bonus_pang);

                // UPDATE ON SERVER AND DB
                _session.m_pi.addUserInfo(pgi.ui, (ulong)total_pang); // add User Info

                if (total_pang > 0)
                {
                    _session.m_pi.addPang(total_pang); // add Pang
                }
                else if (total_pang < 0)
                {
                    _session.m_pi.consomePang(total_pang - 1); // consome Pangs
                }

                // Log
                _smp.message_pool.getInstance().push(new message("[Approach::requestSaveInfo][Log] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] " + (option == 0 ? "Terminou o Approach " : "Saiu do Approach ") + " com [TOTAL_DISTANCE=" + Convert.ToString(pgi.m_app_dados.distance) + ", TOTAL_TIME=" + Convert.ToString(pgi.m_app_dados.time) + ", TOTAL_BOX=" + Convert.ToString(pgi.m_app_dados.box) + ", TOTAL_PANG=" + Convert.ToString(total_pang) + "]", type_msg.CL_FILE_LOG_AND_CONSOLE));

            }
            catch (exception e)
            {
                _smp.message_pool.getInstance().push(new message("[Approach::requestSaveInfo][Error] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }
        public override void requestDrawTreasureHunterItem(Player _session)
        {

            if (!sTreasureHunterSystem.getInstance().isLoad())
            {
                sTreasureHunterSystem.getInstance().load();
            }

            var pgi = INIT_PLAYER_INFO("requestDrawTreasureHunterItem",
                "tentou sortear os item(ns) do Treasure Hunter do jogo",
                _session);

            pgi.thi.v_item = sTreasureHunterSystem.getInstance().drawApproachBox(pgi.m_app_dados.box, (byte)(m_ri.getMap() & 0x7F));
        }
        public override void sendPlacar(Player _session)
        {

            PangyaBinaryWriter p = new PangyaBinaryWriter((ushort)0x14E);

            uint count = getCountPlayersGame();

            p.WriteByte((byte)count);

            foreach (var el in m_player_info)
            {
                if (el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT || ((PlayerApproachInfo)el.Value).m_app_dados.state_quit == approach_dados_ex.eSTATE_QUIT.SQ_QUIT_START)
                {
                    ((PlayerApproachInfo)el.Value).m_app_dados.toPacket(p);
                }
            }

            packet_func.game_broadcast(this,
                p, 1);
        }
        public override void sendSyncShot(Player _session)
        {

            var pgi = INIT_PLAYER_INFO("sendSyncShot",
                "tentou sincronizar a tacada do jogador no jogo",
                _session);

            PangyaBinaryWriter p = new PangyaBinaryWriter((ushort)0x6E);

            p.WriteInt32(pgi.shot_sync.oid);

            p.WriteByte(pgi.hole);

            p.WriteFloat(pgi.location.x);
            p.WriteFloat(pgi.location.z);

            p.WriteUInt32(pgi.shot_sync.state_shot.shot.ulState);

            if (pgi.m_app_dados.state.ucState != 0u)
            {

                p.WriteUInt32((uint)~0u);
                p.WriteUInt32(0u);

            }
            else
            {

                p.WriteUInt32(pgi.m_app_dados.distance);
                p.WriteUInt32(pgi.m_app_dados.time);
            }

            p.WriteUInt16(pgi.shot_sync.tempo_shot);

            packet_func.game_broadcast(this,
                p, 1);
        }
        public override PlayerGameInfo makePlayerInfoObject(Player _session)
        {

            var pai = new PlayerApproachInfo();

            try
            {

                //CHECK_SESSION_BEGIN("makePlayerInfoObject");

                pai.m_app_dados.uid = _session.m_pi.uid;
                pai.m_app_dados.oid = _session.m_oid;

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[Approach::makePlayerInfoObject][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }

            return pai;
        }
        public void setFinishShot(PlayerGameInfo _pgi)
        {

            if (_pgi == null)
            {

                _smp.message_pool.getInstance().push(new message("[Approach::setFinishShot][Error] PlayerGameInfo_pgi is invalid(nullptr).", type_msg.CL_FILE_LOG_AND_CONSOLE));

                return;
            }




            // Set
            _pgi.finish_shot = 1;


        }
        public void clearFinishShot()
        {




            clear_all_finish_shot();



        }
        public void setLoadHole(PlayerGameInfo _pgi)
        {
            if (_pgi == null)
                return;

            _pgi.finish_load_hole = 1;
        }


        public bool checkAllFinishCharIntro()
        {
            uint count = 0;

            foreach (var _el in m_players)
            {
                try
                {
                    var pgi = INIT_PLAYER_INFO("CheckAllFinishCharIntro", "tentou verificar se todos os player terminaram a Intro do Character no jogo", _el);

                    if (pgi.finish_char_intro > 0)
                        count++;
                }
                catch (exception e)
                {
                    _smp.message_pool.getInstance().push(new message("[Approach::CheckAllFinishCharIntro][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }
            ;

            return (count == m_players.Count);
        }

        public bool checkAllFinishShot()
        {
            uint count = 0;

            foreach (var _el in m_players)
            {
                try
                {
                    var pgi = INIT_PLAYER_INFO("CheckAllFinishShot", "tentou verificar se todos os player terminaram a Tacada no jogo", _el);

                    if (pgi.finish_shot > 0)
                        count++;
                }
                catch (exception e)
                {
                    _smp.message_pool.getInstance().push(new message("[Approach::CheckAllFinishShot][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }
            ;

            return (count == m_players.Count);
        }

        public bool checkAllSyncShot()
        {
            uint count = 0;

            foreach (var _el in m_players)
            {
                try
                {
                    var pgi = INIT_PLAYER_INFO("CheckAllSyncShot", "tentou verificar se todos os player sincronizaram a Tacada no jogo", _el);

                    if (pgi.sync_shot_flag > 0)
                        count++;
                }
                catch (exception e)
                {
                    _smp.message_pool.getInstance().push(new message("[Approach::CheckAllSyncShot][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }
            ;

            return (count == m_players.Count);
        }

        public void clearFinishCharIntro()
        {




            clear_all_finish_char_intro();



        }

        public void setSyncShot(PlayerGameInfo _pgi)
        {

            if (_pgi == null)
            {

                _smp.message_pool.getInstance().push(new message("[Approach::setSyncShot[Error] PlayerGameInfo _pgi is invalid(nullptr).", type_msg.CL_FILE_LOG_AND_CONSOLE));

                return;
            }




            // Set
            _pgi.sync_shot_flag = 1;


        }

        public void clearSyncShot()
        {




            clear_all_sync_shot();



        }

        public void clear_all_load_hole()
        {

            foreach (var _el in m_players)
            {
                try
                {
                    var pgi = INIT_PLAYER_INFO("clear_all_load_hole",
                        " tentou limpar all load hole no jogo",
                        _el);
                    pgi.finish_load_hole = 0;
                }
                catch (exception e)
                {
                    _smp.message_pool.getInstance().push(new message("[Approach::clear_all_load_hole][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }
            ;
        }
        public void clear_all_finish_shot()
        {

            foreach (var _el in m_players)
            {
                try
                {
                    var pgi = INIT_PLAYER_INFO("clear_all_finish_shot",
                        " tentou limpar all finish tacada no jogo",
                        _el);
                    pgi.finish_shot = 0;
                    pgi.tick_sync_end_shot.clear();
                }
                catch (exception e)
                {
                    _smp.message_pool.getInstance().push(new message("[Approach::clear_all_finish_shot][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }
            ;
        }
        public void clear_all_finish_char_intro()
        {

            foreach (var _el in m_players)
            {
                try
                {
                    var pgi = INIT_PLAYER_INFO("clear_all_finish_char_intro",
                        " tentou limpar all finish char intro no jogo",
                        _el);
                    pgi.finish_char_intro = 0;
                }
                catch (exception e)
                {
                    _smp.message_pool.getInstance().push(new message("[Approach::clear_all_finish_char_intro][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }
            ;
        }
        public void clear_all_sync_shot()
        {

            foreach (var _el in m_players)
            {
                try
                {
                    var pgi = INIT_PLAYER_INFO("clear_all_sync_shot",
                        " tentou limpar all sync shot do jogo",
                        _el);
                    pgi.sync_shot_flag = 0;
                    pgi.tick_sync_shot.clear();
                }
                catch (exception e)
                {
                    _smp.message_pool.getInstance().push(new message("[Approach::clear_all_sync_shot][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }
            ;
        }

        protected override void requestCalculeRankPlace()
        {

            List<approach_dados_ex> v_ad = new List<approach_dados_ex>();
            approach_dados_ex tmp = new approach_dados_ex(0u);
            PlayerApproachInfo pai = null;

            foreach (var el in m_player_info)
            {

                if (el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                { // menos os que quitaram

                    pai = (PlayerApproachInfo)el.Value;

                    tmp.clear();

                    tmp.uid = pai.m_app_dados.uid;
                    tmp.oid = pai.m_app_dados.oid;

                    tmp.distance = pai.m_app_dados.total_distance;
                    tmp.time = pai.m_app_dados.total_time;

                    v_ad.Add(tmp);
                }
            }

            // ORIGINAL LINE: sort(v_ad.begin(), v_ad.end(), Approach::sort_approach_rank_place);
            v_ad.Sort(sort_approach_rank_place);

            // Set positions
            byte position = 1;

            foreach (var el in v_ad)
            {

                try
                {

                    var pgi = INIT_PLAYER_INFO("requestCalculeRankPlace",
                        "Tentou inicializar a position dos player",
                        findSessionByUID(el.uid));

                    pgi.m_app_dados.position = (sbyte)position++;

                }
                catch (exception e)
                {

                    _smp.message_pool.getInstance().push(new message("[Approach::requestCalculeRankPlace][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }

            if (v_ad.Count > 0)
            {
                v_ad.Clear();
            }
        }
        public void requestCalculeRankPlaceHole()
        {

            List<approach_dados_ex> v_ad = new List<approach_dados_ex>();

            foreach (var el in m_player_info)
            {
                if (el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT) // menos os que quitaram
                {
                    v_ad.Add(((PlayerApproachInfo)el.Value).m_app_dados);
                }
            }

            v_ad.Sort(sort_approach_rank_place);

            // Set positions
            byte position = 1;

            foreach (var el in v_ad)
            {

                try
                {

                    var pgi = INIT_PLAYER_INFO("requestCalculeRankPlaceHole",
                        "Tentou inicializar a position dos player",
                        findSessionByUID(el.uid));

                    pgi.m_app_dados.position = (sbyte)position++;

                }
                catch (exception e)
                {

                    _smp.message_pool.getInstance().push(new message("[Approach::requestCalculeRankPlaceHole][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            }

            if (v_ad.Count > 0)
            {
                v_ad.Clear();

            }
        }
        public void top_rank_win()
        {

            var count = getCountPlayersGame();

            if (count >= 5 && count < 11)
            {

                var it = m_player_info.FirstOrDefault(_el =>
                {
                    return (_el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)_el.Value).m_app_dados.position == 1);
                });

                if (it.Key != null)
                {
                    ((PlayerApproachInfo)it.Value).m_app_dados.rank_box = 1;
                }

            }
            else if (count >= 11 && count < 18)
            {

                foreach (var _el in m_player_info)
                {
                    if (_el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                    {
                        var pai = (PlayerApproachInfo)_el.Value;
                        switch (pai.m_app_dados.position)
                        {
                            case 1:
                                pai.m_app_dados.rank_box = 2;
                                break;
                            case 2:
                                pai.m_app_dados.rank_box = 1;
                                break;
                        }
                    }
                }
                ;

            }
            else if (count >= 18 && count < 26)
            {
                foreach (var _el in m_player_info)
                {
                    if (_el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                    {
                        var pai = (PlayerApproachInfo)_el.Value;
                        switch (pai.m_app_dados.position)
                        {
                            case 1:
                                pai.m_app_dados.rank_box = 3;
                                break;
                            case 2:
                                pai.m_app_dados.rank_box = 2;
                                break;
                            case 3:
                                pai.m_app_dados.rank_box = 1;
                                break;
                        }
                    }
                }
                ;

            }
            else if (count >= 26)
            {

                foreach (var _el in m_player_info)
                {
                    if (_el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                    {
                        var pai = (PlayerApproachInfo)_el.Value;
                        switch (pai.m_app_dados.position)
                        {
                            case 1:
                                pai.m_app_dados.rank_box = 4;
                                break;
                            case 2:
                                pai.m_app_dados.rank_box = 3;
                                break;
                            case 3:
                                pai.m_app_dados.rank_box = 2;
                                break;
                            case 4:
                                pai.m_app_dados.rank_box = 1;
                                break;
                        }
                    }
                }
                ;
            }
        }
        public void finishAllDadosApproach()
        {

            PlayerApproachInfo pai = null;

            foreach (var el in m_player_info)
            {

                pai = (PlayerApproachInfo)el.Value;

                if (pai != null && pai.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                {

                    pai.m_app_dados.box = pai.m_app_dados.total_box + pai.m_app_dados.rank_box;
                    pai.m_app_dados.distance = pai.m_app_dados.total_distance;
                    pai.m_app_dados.time = pai.m_app_dados.total_time;
                    pai.m_app_dados.rank_box = 0;
                }
            }
        }

        public void sendScoreBoard()
        {

            PangyaBinaryWriter p = new PangyaBinaryWriter((ushort)0x150);

            uint count = getCountPlayersGame();

            p.WriteByte((byte)count);

            foreach (var el in m_player_info)
            {
                if (el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT || ((PlayerApproachInfo)el.Value).m_app_dados.state_quit == approach_dados_ex.eSTATE_QUIT.SQ_QUIT_START)
                {
                    ((PlayerApproachInfo)el.Value).m_app_dados.toPacket(p);
                }
            } 
            Debug.WriteLine("sendScoreBoard: " + p.GetBytes.HexDump());

            packet_func.game_broadcast(this,
                p, 1);
        }
        public void init_mission()
        {

            m_mission = sApproachMissionSystem.getInstance().drawMission((uint)m_players.Count);

            if (m_mission.is_player_uid)
            {

                var index_p = m_mission.condition[1];

                m_mission.condition[1] = 0;

                if (m_players[index_p] != null && m_players[index_p].getState())
                {

                    m_mission.condition[0] = (int)m_players[index_p].m_pi.uid;
                    m_mission.nick = m_players[index_p].m_pi.nickname;
                }
            }
        }
        public void mission_win()
        {

            if (m_mission.numero > 0)
            {

                switch (m_mission.numero)
                {
                    case 1: // Par ou  mpar, Primeiro lugar parar com dist ncia Par ou  mpar(mission), todos ganham box
                        {
                            bool win = false;

                            PlayerApproachInfo pai = null;

                            var it = m_player_info.FirstOrDefault(_el =>
                            {
                                return (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)_el.Value).m_app_dados.state.ucState == 0u && ((PlayerApproachInfo)_el.Value).m_app_dados.position == 1);
                            });

                            if (it.Key != null)
                            {

                                pai = (PlayerApproachInfo)it.Value;

                                if (m_mission.condition[0] == 0)
                                { // Par

                                    win = (pai.m_app_dados.distance % 2) == 0;

                                }
                                else
                                { // Impar

                                    win = (pai.m_app_dados.distance % 2) != 0;
                                }
                            }

                            if (win)
                            {

                                foreach (var el in m_player_info)
                                {
                                    if (el.Value != null
                                        && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                        && (pai = (PlayerApproachInfo)el.Value).m_app_dados.state.ucState == 0u)
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }

                            break;
                        }
                    case 3: // Quem fazer chip-in ganha box
                        foreach (var el in m_player_info)
                        {
                            if (el.Value != null
                                && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                && ((PlayerApproachInfo)el.Value).m_app_dados.state.chip_in > 0)
                            {
                                ((PlayerApproachInfo)el.Value).m_app_dados.box = m_mission.box_qntd;
                            }
                        }
                        break;
                    case 6: // Mascot equiped, Primeiro lugar estiver com o mascot da mission, todos ganham box
                        {
                            bool win = false;

                            PlayerApproachInfo pai = null;

                            var it = m_player_info.FirstOrDefault(_el =>
                            {
                                return (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)_el.Value).m_app_dados.state.ucState == 0u && ((PlayerApproachInfo)_el.Value).m_app_dados.position == 1);
                            });

                            if (it.Key != null)
                            {

                                pai = (PlayerApproachInfo)it.Value;

                                switch (m_mission.condition[0])
                                {
                                    case 1:
                                        win = it.Value.mascot_typeid == 0x40000002u; // Cocoa
                                        break;
                                    case 2:
                                        win = it.Value.mascot_typeid == 0x40000001; // Puff
                                        break;
                                    case 3:
                                        win = it.Value.mascot_typeid == 0x40000003u; // Bily
                                        break;
                                    case 4:
                                        win = it.Value.mascot_typeid == 0x40000000; // Lemmy
                                        break;
                                }
                            }

                            if (win)
                            {

                                foreach (var el in m_player_info)
                                {
                                    if (el.Value != null
                                        && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                        && (pai = (PlayerApproachInfo)el.Value).m_app_dados.state.ucState == 0u)
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }

                            break;
                        }
                    case 7: // Rank, O player que ficar no rank da mission ele ganha box, independente se ele fez OUT
                        {
                            var it = m_player_info.FirstOrDefault(_el =>
                            {
                                return (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)_el.Value).m_app_dados.position == m_mission.condition[0]);
                            });

                            if (it.Key != null)
                            {
                                ((PlayerApproachInfo)it.Value).m_app_dados.box = m_mission.box_qntd;
                            }

                            break;
                        }
                    case 10: // Character Equiped, Todos que estiverem com o character da mission e pararem coma dist ncia menor que 10y eles ganham box
                        {

                            foreach (var _el in m_player_info)
                            {
                                if (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                                {
                                    var pai = (PlayerApproachInfo)_el.Value;
                                    if (pai.m_app_dados.state.ucState == 0u
                                        && pai.m_app_dados.distance < 100


                                        && _el.Key.m_pi.ei.char_info != null
                                        && m_mission.condition[0] == ((Player)_el.Key).m_pi.ei.char_info._typeid)

                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }
                            ;

                            break;
                        }

                    case 11: // Mais (Par ou  mpar), Se tiver dist ncias dos players mais (Par ou  mpar)(mission), todos ganham box
                        {
                            bool win = false;

                            uint count_par = 0;
                            uint count_impar = 0;

                            PlayerApproachInfo pai = null;

                            foreach (var _el in m_player_info)
                            {
                                if (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                                {
                                    pai = (PlayerApproachInfo)_el.Value;
                                    if (pai.m_app_dados.state.ucState == 0u)
                                    {
                                        if ((pai.m_app_dados.distance % 2) == 0)
                                        {
                                            count_par++;
                                        }
                                        else
                                        {
                                            count_impar++;
                                        }
                                    }
                                }
                            }
                            ;

                            if (m_mission.condition[0] == 0)
                            { // Mais par

                                win = count_par > count_impar;

                            }
                            else
                            { // Mais Impar

                                win = count_impar > count_par;
                            }

                            if (win)
                            {

                                foreach (var el in m_player_info)
                                {
                                    if (el.Value != null
                                        && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                        && (pai = (PlayerApproachInfo)el.Value).m_app_dados.state.ucState == 0u)
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }

                            break;
                        }
                    case 12: // Chip-in, se a maioria dos players fizer chip-in, todos que chiparam e n o fizeram OUT(timeout, ob) ganham box
                        {

                            PlayerApproachInfo pai = null;

                            uint count_chip = 0;
                            uint count_p = getCountPlayersGame();

                            foreach (var _el in m_player_info)
                            {
                                if (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                                {
                                    pai = (PlayerApproachInfo)_el.Value;
                                    if (pai.m_app_dados.state.chip_in > 0)
                                    {
                                        count_chip++;
                                    }
                                }
                            }
                            ;

                            if (count_chip > (count_p / 2))
                            {

                                foreach (var el in m_player_info)
                                {
                                    if (el.Value != null
                                        && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                        && ((pai = (PlayerApproachInfo)el.Value).m_app_dados.state.ucState == 0u || pai.m_app_dados.state.chip_in > 0))
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }

                            break;
                        }
                    case 14: // Menor ou igual a dist ncia(mission) o player ganha box
                        {
                            PlayerApproachInfo pai = null;

                            foreach (var _el in m_player_info)
                            {
                                if (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                                {
                                    pai = (PlayerApproachInfo)_el.Value;
                                    if (pai.m_app_dados.state.ucState == 0u && pai.m_app_dados.distance <= (m_mission.condition[0]))
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }
                            ;

                            break;
                        }
                    case 15: // Rank dist ncia Par ou  mpar, se o player no rank(mission) a dist ncia for (Par ou  mpar)(mission), todos que n o deram OUT ganham box
                        {
                            bool win = false;

                            PlayerApproachInfo pai = null;

                            var it = m_player_info.FirstOrDefault(_el =>
                            {
                                return (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)_el.Value).m_app_dados.state.ucState == 0u && ((PlayerApproachInfo)_el.Value).m_app_dados.position == m_mission.condition[0]);
                            });

                            if (it.Key != null)
                            {

                                pai = (PlayerApproachInfo)it.Value;

                                if (m_mission.condition[1] == 0)
                                { // Par

                                    win = (pai.m_app_dados.distance % 2) == 0;

                                }
                                else
                                { // Impar

                                    win = (pai.m_app_dados.distance % 2) != 0;
                                }
                            }

                            if (win)
                            {

                                foreach (var el in m_player_info)
                                {
                                    if (el.Value != null
                                        && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                        && (pai = (PlayerApproachInfo)el.Value).m_app_dados.state.ucState == 0u)
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }

                            break;
                        }

                    case 16: // Chip-in, Se 4 ou mais player fazer chip-in, todos que fizeram chip-in e n o deram OUT(timeout, ob) ganham box
                        {
                            PlayerApproachInfo pai = null;

                            uint count_chip = 0;

                            foreach (var _el in m_player_info)
                            {
                                if (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                                {
                                    pai = (PlayerApproachInfo)_el.Value;
                                    if (pai.m_app_dados.state.chip_in > 0)
                                    {
                                        count_chip++;
                                    }
                                }
                            }
                            ;

                            if (count_chip >= 4)
                            {

                                foreach (var el in m_player_info)
                                {
                                    if (el.Value != null
                                        && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                        && ((pai = (PlayerApproachInfo)el.Value).m_app_dados.state.ucState == 0u || pai.m_app_dados.state.chip_in == 1))
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }

                            break;
                        }
                    case 17: // Total dist ncia de todos players n o pode passar da dist ncia da mission, que todos que n o fizeram OUT ganham box
                        {
                            uint total_dist = 0;

                            PlayerApproachInfo pai = null;

                            foreach (var _el in m_player_info)
                            {
                                if (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                                {
                                    pai = (PlayerApproachInfo)_el.Value;
                                    total_dist += pai.m_app_dados.distance;
                                }
                            }
                            ;

                            if (total_dist < (m_mission.condition[1]))
                            {

                                foreach (var el in m_player_info)
                                {
                                    if (el.Value != null
                                        && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                        && (pai = (PlayerApproachInfo)el.Value).m_app_dados.state.ucState == 0u)
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }

                            break;
                        }
                    case 18: // Se um player ficar na dist ncia exata da mission, todos que n o fizeram OUT ganham box
                        {
                            PlayerApproachInfo pai = null;

                            var it = m_player_info.FirstOrDefault(_el =>
                            {
                                return (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)_el.Value).m_app_dados.state.ucState == 0u && ((PlayerApproachInfo)_el.Value).m_app_dados.distance == ((m_mission.condition[0]) + m_mission.condition[1]));
                            });

                            if (it.Key != null)
                            {

                                foreach (var el in m_player_info)
                                {
                                    if (el.Value != null
                                        && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                        && (pai = (PlayerApproachInfo)el.Value).m_app_dados.state.ucState == 0u)
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }
                            break;
                        }

                    case 20: // Player com a maior dist ncia que n o fez OUT ganha box
                        {
                            var it_win = m_player_info.end();

                            foreach (var it in m_player_info)
                            {

                                if ((it.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)it.Value).m_app_dados.state.ucState == 0u) && (it_win.Key == m_player_info.end().Key || ((PlayerApproachInfo)it.Value).m_app_dados.distance > ((PlayerApproachInfo)it_win.Value).m_app_dados.distance))
                                {
                                    it_win = it;
                                }
                            }

                            if (it_win.Key != m_player_info.end().Key)
                            {
                                ((PlayerApproachInfo)it_win.Value).m_app_dados.box = m_mission.box_qntd;
                            }

                            break;
                        }
                    case 22: // Se o tempo do player em primeiro lugar for menor ou igual a 10s(10.000 milliValues), todos que n o fizeram OUT ganham box
                        {
                            PlayerApproachInfo pai = null;

                            var it = m_player_info.FirstOrDefault(_el =>
                            {
                                return (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)_el.Value).m_app_dados.state.ucState == 0u && ((PlayerApproachInfo)_el.Value).m_app_dados.position == 1 && ((PlayerApproachInfo)_el.Value).m_app_dados.time <= 10000);
                            });

                            if (it.Key != null)
                            {

                                foreach (var el in m_player_info)
                                {
                                    if (el.Value != null
                                        && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                        && (pai = (PlayerApproachInfo)el.Value).m_app_dados.state.ucState == 0u)
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }

                            break;
                        }
                    case 23: // Caddie equiped, o Player que tiver o caddie da mission equipado e ficar com a dist ncia menor que 20y ganha box
                        {
                            foreach (var _el in m_player_info)
                            {
                                if (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                                {
                                    var pai = (PlayerApproachInfo)_el.Value;
                                    if (pai.m_app_dados.state.ucState == 0u
                                        && pai.m_app_dados.distance < 200


                                        && _el.Key.m_pi.ei.cad_info != null)

                                    {
                                        switch (m_mission.condition[0])
                                        {
                                            case 0:
                                                if (_el.Key.m_pi.ei.cad_info._typeid == 0x1C000000u)
                                                {
                                                    pai.m_app_dados.box = m_mission.box_qntd;
                                                }
                                                break;
                                            case 1:
                                                if (_el.Key.m_pi.ei.cad_info._typeid == 0x1C000001u || _el.Key.m_pi.ei.cad_info._typeid == 0x1C000010u)
                                                {
                                                    pai.m_app_dados.box = m_mission.box_qntd;
                                                }
                                                break;
                                            case 2:
                                                if (_el.Key.m_pi.ei.cad_info._typeid == 0x1C000002u || _el.Key.m_pi.ei.cad_info._typeid == 0x1C000011u)
                                                {
                                                    pai.m_app_dados.box = m_mission.box_qntd;
                                                }
                                                break;
                                            case 3:
                                                if (_el.Key.m_pi.ei.cad_info._typeid == 0x1C000003u || _el.Key.m_pi.ei.cad_info._typeid == 0x1C000012u)
                                                {
                                                    pai.m_app_dados.box = m_mission.box_qntd;
                                                }
                                                break;
                                            case 4:
                                                if (_el.Key.m_pi.ei.cad_info._typeid == 0x1C000004u || _el.Key.m_pi.ei.cad_info._typeid == 0x1C000013u)
                                                {
                                                    pai.m_app_dados.box = m_mission.box_qntd;
                                                }
                                                break;
                                            case 5:
                                                if (_el.Key.m_pi.ei.cad_info._typeid == 0x1C000005u || _el.Key.m_pi.ei.cad_info._typeid == 0x1C000014u)
                                                {
                                                    pai.m_app_dados.box = m_mission.box_qntd;
                                                }
                                                break;
                                            case 6:
                                                if (_el.Key.m_pi.ei.cad_info._typeid == 0x1C000006u || _el.Key.m_pi.ei.cad_info._typeid == 0x1C000015u)
                                                {
                                                    pai.m_app_dados.box = m_mission.box_qntd;
                                                }
                                                break;
                                            case 7:
                                                if (_el.Key.m_pi.ei.cad_info._typeid == 0x1C000007u || _el.Key.m_pi.ei.cad_info._typeid == 0x1C000016u)
                                                {
                                                    pai.m_app_dados.box = m_mission.box_qntd;
                                                }
                                                break;
                                        }
                                    }
                                }
                            }
                            ;
                            break;
                        }

                    case 24: // Primeiro player a tacar e que n o deu OUT ganha box
                        {
                            var it_win = m_player_info.end();

                            foreach (var it in m_player_info)
                            {

                                if ((it.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)it.Value).m_app_dados.state.ucState == 0u) && (it_win.Key == m_player_info.Last().Key || ((PlayerApproachInfo)it.Value).m_app_dados.time > ((PlayerApproachInfo)it_win.Value).m_app_dados.time))
                                {
                                    it_win = it;
                                }
                            }

                            if (it_win.Key != m_player_info.end().Key)
                            {
                                ((PlayerApproachInfo)it_win.Value).m_app_dados.box = m_mission.box_qntd;
                            }

                            break;
                        }
                    case 25: // Par ou  mpar, quem ficar com a dist ncia (Par ou  mpar)(mission) e n o fizer OUT ganha box
                        {
                            PlayerApproachInfo pai = null;

                            foreach (var _el in m_player_info)
                            {
                                if (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT)
                                {
                                    pai = (PlayerApproachInfo)_el.Value;
                                    if (pai.m_app_dados.state.ucState == 0u)
                                    {
                                        if (m_mission.condition[0] == 0)
                                        {
                                            if ((pai.m_app_dados.distance % 2) == 0)
                                            {
                                                pai.m_app_dados.box = m_mission.box_qntd;
                                            }
                                        }
                                        else
                                        {
                                            if ((pai.m_app_dados.distance % 2) != 0)
                                            {
                                                pai.m_app_dados.box = m_mission.box_qntd;
                                            }
                                        }
                                    }
                                }
                            }
                            ;
                            break;
                        }
                    case 26: // Ultimo player a tacar e que n o deu OUT ganha box
                        {
                            var it_win = m_player_info.end();

                            foreach (var it in m_player_info)
                            {

                                if ((it.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)it.Value).m_app_dados.state.ucState == 0u) && (it_win.Key == m_player_info.end().Key || ((PlayerApproachInfo)it.Value).m_app_dados.time < ((PlayerApproachInfo)it_win.Value).m_app_dados.time))
                                {
                                    it_win = it;
                                }
                            }

                            if (it_win.Key != m_player_info.end().Key)
                            {
                                ((PlayerApproachInfo)it_win.Value).m_app_dados.box = m_mission.box_qntd;
                            }

                            break;
                        }
                    case 29: // Player(mission) que fazer chip-in, todos que n o fizeram OUT ganha box e o player que fez chip-in ganha box
                        {

                            PlayerApproachInfo pai = null;

                            var it = m_player_info.FirstOrDefault(_el =>
                            {
                                return (_el.Value != null && _el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT && ((PlayerApproachInfo)_el.Value).m_app_dados.uid == m_mission.condition[0] && ((PlayerApproachInfo)_el.Value).m_app_dados.state.chip_in == 1);
                            });

                            if (it.Key != null)
                            {

                                foreach (var el in m_player_info)
                                {
                                    if (el.Value != null
                                        && el.Value.flag != PlayerGameInfo.eFLAG_GAME.QUIT
                                        && ((pai = (PlayerApproachInfo)el.Value).m_app_dados.state.ucState == 0u || pai.m_app_dados.uid == m_mission.condition[0]))
                                    {
                                        pai.m_app_dados.box = m_mission.box_qntd;
                                    }
                                }
                            }

                            break;
                        } // End Case 29

                } // End Switch
            }
        }
        public void delete_all_quiter()
        {

            PlayerApproachInfo pai = null;

            foreach (var el in m_player_info)
            {

                pai = (PlayerApproachInfo)el.Value;

                if (pai != null && pai.m_app_dados.state_quit == approach_dados_ex.eSTATE_QUIT.SQ_QUIT_START)
                {
                    pai.m_app_dados.state_quit = approach_dados_ex.eSTATE_QUIT.SQ_QUIT_ENDED;
                }
            }
        }
        public override bool finish_game(Player _session, int option)
        {
            if (_session.m_client != null
                && _session.getState()
                && _session.isConnected()
                && m_players.Count > 0)
            {

                PangyaBinaryWriter p = new PangyaBinaryWriter();

                if (option == 0xCB)
                {

                    if (m_approach_state)
                    {
                        finish_approach(_session, 1); // Termina sem ter acabado de jogar
                    }

                    var pgi = INIT_PLAYER_INFO("finish_game",
                        "tentou terminar o jogo",
                        _session);

                    // Update Info Map Statistics
                    sendUpdateInfoAndMapStatistics(_session, 0);

                    // Update Mascot Info ON GAME, se o player estiver com um mascot equipado
                    if (_session.m_pi.ei.mascot_info != null)
                    {
                        packet_func.session_send(packet_func.pacote06B(_session.m_pi, 8),
                            _session, 1);
                    }

                    // Resposta que tem sempre que acaba um jogo, n o sei o que   ainda, esse s  n o tem no HIO Event
                    p.init_plain(0x244);

                    p.WriteUInt32(0); // OK

                    packet_func.session_send(p,
                        _session, 1);

                    // Esse   novo do JP, tem Tourney, VS, Grand Prix, HIO Event, n o vi talvez tenha nos outros tamb m
                    p.init_plain(0x24F);

                    p.WriteUInt32(0); // OK

                    packet_func.session_send(p,
                        _session, 1);

                    // Resposta Update Pang
                    p.init_plain(0xC8);

                    p.WriteUInt64(_session.m_pi.ui.pang);

                    p.WriteUInt64(0Ul);

                    packet_func.session_send(p,
                        _session, 1);

                    // Colocar o finish_game Para 1 quer dizer que ele acabou o camp
                    pgi.finish_game = 1;

                    // Flag do game que terminou
                    m_game_init_state = 2; // ACABOU

                }
            }

            return (PlayersCompleteGameAndClear() && m_approach_state);
        }

        public object syncHoleTime()
        {
            try
            {

                // Log
                _smp.message_pool.getInstance().push(new message("[Approach::syncHoleTime][Log] syncHoleTime iniciado com sucesso!", type_msg.CL_FILE_LOG_AND_CONSOLE));

                PangyaBinaryWriter p = new PangyaBinaryWriter();

                int retWait = WaitHandle.WaitTimeout;

                WaitHandle[] waitEvents = { m_hEvent_sync_hole, m_hEvent_sync_hole_pulse };

                while ((retWait = WaitHandle.WaitAny(waitEvents, 1000)) == WaitHandle.WaitTimeout || retWait == 1)
                {
                    try
                    {

                        m_state_app.@lock();

                        switch (m_state_app.getState())
                        {
                            case STATE_APPROACH_SYNC.LOAD_HOLE:
                                {
                                    if (checkAllLoadHole())
                                    {
                                        _smp.message_pool.getInstance().push(new message("[Approach::SyncHoleTime][Log] Terminou de carregar o hole", type_msg.CL_FILE_LOG_AND_CONSOLE));

                                        clear_all_load_hole();

                                        init_mission();

                                        foreach (var el in m_players)
                                        {

                                            p.init_plain(0x53);
                                            p.WriteInt32(el.m_oid);

                                            packet_func.session_send(p,
                                                el, 1);
                                        }

                                        sendRatesOfApproach();

                                        // Mission
                                        p.init_plain(0x14F);

                                        m_mission.toPacket(p);

                                        packet_func.game_broadcast(this,
                                            p, 1);


                                        m_state_app.setState(STATE_APPROACH_SYNC.LOAD_CHAR_INTRO);
                                    }

                                    break;
                                }
                            case STATE_APPROACH_SYNC.LOAD_CHAR_INTRO:
                                {
                                    if (checkAllFinishCharIntro())//vai ficar batendo no time ate verificar se o tempo acabou, se sim, ele reinicia o proximo hole
                                    {
                                        _smp.message_pool.getInstance().push(new message("[Approach::SyncHoleTime][Log] Terminou a intro char no hole", type_msg.CL_FILE_LOG_AND_CONSOLE));

                                        clearFinishCharIntro();
                                        m_timeout = 0;
                                        startTime();
                                        p.init_plain(0x90);
                                        packet_func.game_broadcast(this, p, 1);
                                        m_state_app.setState(STATE_APPROACH_SYNC.END_SHOT);

                                    }
                                }
                                break;
                            case STATE_APPROACH_SYNC.END_SHOT:
                                {

                                    if (checkAllSyncShot()
                                        && checkAllFinishShot()
                                        && m_timeout == 1)
                                    {

                                        clearSyncShot();
                                        clearFinishShot();

                                        requestCalculeRankPlaceHole();

                                        mission_win();

                                        top_rank_win();

                                        // Score board Approach
                                        sendScoreBoard(); // -> packet 0x150

                                        foreach (var el in m_players)
                                        { 
                                            finishHole(el);

                                            changeHole(el);
                                        }
                                        //seta aui

                                        _smp.message_pool.getInstance().push(new message("[Approach::SyncHoleTime][Log] hora de trocar o hole", type_msg.CL_FILE_LOG_AND_CONSOLE));

                                    }
                                }
                                break;

                            case STATE_APPROACH_SYNC.WAIT_END_GAME:
                                {
                                    // Faz nada por enquanto
                                    break;
                                }
                        }

                        // Libera
                        m_state_app.unlock();

                    }
                    catch (exception e)
                    {

                        m_state_app.unlock();

                        _smp.message_pool.getInstance().push(new message("[Approach::syncHoleTime][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                    }
                }
            }
            catch (exception e)
            {
                _smp.message_pool.getInstance().push(new message("[Approach::syncHoleTime][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
            _smp.message_pool.getInstance().push(new message("[Approach::syncHoleTime][Log] Saindo de syncHoleTime()...", type_msg.CL_FILE_LOG_AND_CONSOLE));

            return null;
        } 

        public override void startTime()
        {
            try
            {
                if (m_timer != null)
                    stopTime();

                m_timer = sgs.gs.getInstance().MakeTime(m_ri.time_30s, () => end_time(this, null), new List<long>(), PangyaSyncTimer.TIMER_TYPE.NORMAL);

                _smp.message_pool.getInstance().push(new message("[Approach::startTime][Log] Create Timer[Tempo=" + (m_ri.time_30s / 1000) + "Sec, STATE=" + (m_timer.getState()) + "]", type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
            catch (Exception e)
            {
                _smp.message_pool.getInstance().push(new message(
                    $"[Approach::startTime][ErrorSystem] {e.Message}",
                    type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }

        public bool checkAllLoadHole()
        {
            uint count = 0;

            foreach (var _el in m_players)
            {
                try
                {
                    var pgi = INIT_PLAYER_INFO("CheckAllLoadHole", "tentou verificar se todos os player terminaram de carregar o hole no jogo", _el);

                    if (pgi.finish_load_hole > 0)
                        count++;
                }
                catch (exception e)
                {
                    _smp.message_pool.getInstance().push(new message("[Approach::CheckAllLoadHole][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
                }
            } 
            return count == m_players.Count;
        }
    }
}