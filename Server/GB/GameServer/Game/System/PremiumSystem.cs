using System;
using System.Collections.Generic;
using Pangya_GameServer.Game.Manager;
using Pangya_GameServer.Models;
using Pangya_GameServer.PacketFunc;

using PangyaAPI.Utilities;
using PangyaAPI.Utilities.BinaryModels;
using PangyaAPI.Utilities.Log;
using static Pangya_GameServer.Models.DefineConstants;
namespace Pangya_GameServer.Game.System
{
    public class PremiumSystem
    {
        public void checkEndTimeTicket(Player _session)
        {

            try
            {

                if (isPremium(_session.m_pi.pt._typeid)
                    && _session.m_pi.pt.id != 0
                    && _session.m_pi.pt.unix_sec_date <= 0)
                {

                    WarehouseItemEx ticket = new WarehouseItemEx();

                    var it = _session.m_pi.findWarehouseItemByTypeid(_session.m_pi.pt._typeid);

                    if (it == null)
                    {

                        ticket = ItemManager._ownerItem(_session.m_pi.uid, _session.m_pi.pt._typeid);

                        if (ticket.id <= 0)
                        {
                            _smp.message_pool.getInstance().push(new message("[PremiumSystem::checkEndTimeTicket][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] nao tem o item Ticket Premium. Bug", type_msg.CL_FILE_LOG_AND_CONSOLE));

                            return;
                        }

                        // Add o Ticket Premium User para o map do player, para poder excluir ele
                        _session.m_pi.mp_wi.insert(Tuple.Create(ticket.id, ticket));

                    }
                    else
                    {
                        ticket = it;
                    }

                    stItem item = new stItem();

                    item.type = 2;
                    item.id = (int)ticket.id;
                    item._typeid = ticket._typeid;
                    item.qntd = (int)ticket.c[0];
                    item.c[0] = (short)(item.qntd * -1 <= 0 ? short.MaxValue : item.qntd * -1);

                    // UPDATE ON SERVER AND DB
                    if (ItemManager.removeItem(item, _session) <= 0)
                    {
                        throw new exception("[PremiumSystem::checkEndTimeTicket][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] tentou excluir ticket premium user, mas nao conseguiu deletar ele. Bug", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.PREMIUM_SYSTEM,
                            10000, 0));
                    }

                    //Log
                    _smp.message_pool.getInstance().push(new message("[PremiumSystem::checkEndTimeTicket][Log] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "].\tExcluiu ticket premium do player.", type_msg.CL_ONLY_FILE_LOG));

                    var p = new PangyaBinaryWriter();

                    //// UPDATE ON GAME 
                    packet_func.session_send(packet_func.pacote26D(_session.m_pi.pt.unix_end_date),
                        _session, 0);

                    // Zera o Premium User Ticket que ele j� n�o tem mais
                    _session.m_pi.pt.clear();
                }

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[PremiumSystem::checkEndTimeTicket][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }

        public void addPremiumUser(Player _session,
            WarehouseItemEx _ticket,
            uint _time)
        {
            try
            {

                // Inicializa o PremiumTicket estrutura do player
                _session.m_pi.pt.id = _ticket.id;
                _session.m_pi.pt._typeid = _ticket._typeid;
                _session.m_pi.pt.unix_end_date = (int)_ticket.end_date_unix_local;
                _session.m_pi.pt.unix_sec_date = (int)(_ticket.end_date_unix_local - (int)UtilTime.GetLocalTimeAsUnix()); // Difer�ncia em segundo, quanto tempo ainda tem para acabar o ticket premium

                // add Comet para o player
                _smp.message_pool.getInstance().push(new message("[PremiumSystem::addPremiumUser][Log] Add Comet Premium e set Capability do PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "]", type_msg.CL_FILE_LOG_AND_CONSOLE));
                _smp.message_pool.getInstance().push(new message("[PremiumSystem::addPremiumUser][Log] Agora o PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] eh um Premium User por (" + Convert.ToString(_time) + ") Dias", type_msg.CL_FILE_LOG_AND_CONSOLE));

                // Add comet e outros itens e atualizar no SERVER, DB e GAME
                List<stItem> add_itens = new List<stItem>();

                // Flag Premium User
                _session.m_pi.m_cap.premium_user = true;

                // Add Ball
                var new_ball = addPremiumBall(_session);

                if (new_ball._typeid != 0u)
                    // Add Ball para o jogo
                    add_itens.Add(new_ball);

                uint clubset = getPremiumClubSetByTicket(_session.m_pi.pt._typeid);
                uint mascots = getPremiumMascotByTicket(_session.m_pi.pt._typeid);


                if (isPremium(_session.m_pi.pt._typeid) && !_session.m_pi.ItemExist(clubset) && !_session.m_pi.ItemExist(mascots))
                {
                    // Add ClubSet
                    addPremiumClubSet(_session, _time);
                    // Add Mascot
                    var new_mascot = addPremiumMascot(_session, _time);

                    // Add Mascot para o jogo
                    if (new_mascot._typeid != 0)
                        add_itens.Add(new_mascot);

                    // o ClubSet atualiza com o pacote073
                    packet_func.session_send(_session.m_pi.mp_wi.Build(), _session, 1);
                }

                // Atualiza Capability do player 
                packet_func.session_send(packet_func.pacote09A(_session.m_pi.m_cap.ulCapability), _session, 1);

                if (add_itens.Count != 0)
                {
                    var p = new PangyaBinaryWriter();
                    p.init_plain(0x216);

                    p.WriteUInt32((uint)UtilTime.GetSystemTimeAsUnix());
                    p.WriteUInt32((uint)add_itens.Count); // Count

                    foreach (var el in add_itens)
                    {

                        p.WriteByte(el.type);
                        p.WriteUInt32(el._typeid);
                        p.WriteInt32(el.id);
                        p.WriteUInt32(el.flag_time);
                        p.WriteBytes(el.stat.ToArray());
                        p.WriteUInt32((el.STDA_C_ITEM_TIME == 0) ? el.STDA_C_ITEM_QNTD : el.STDA_C_ITEM_TIME);
                        p.WriteZeroByte(25);
                    }

                    packet_func.session_send(p,
                        _session, 1);
                }

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[PremiumSystem::addPremiumUser][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }

        public void removePremiumUser(Player _session)
        {

            try
            {
                // Remove Premium Ball
                removePremiumBall(_session);

                // Tira capacidade de premium user do player
                _session.m_pi.m_cap.premium_user = false;


                packet_func.session_send(packet_func.pacote09A(_session.m_pi.m_cap.ulCapability),
                    _session, 1);

                // UPDATE ON GAME - Mostra a mensagem que acabou o tempo do ticket premium

                packet_func.session_send(packet_func.pacote26D(_session.m_pi.pt.unix_end_date),
                    _session, 0);

                // Zera o Premium User Ticket que ele j� n�o tem mais
                _session.m_pi.pt.clear();


                _smp.message_pool.getInstance().push(new message("[PremiumSystem::removePremiumUser][Log] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] removeu o Premium User do Player, acabou o tempo do ticket, tirando a capacidade e a Comet(Ball)", type_msg.CL_FILE_LOG_AND_CONSOLE));

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[PremiumSystem::removePremiumUser][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }

        public stItem addPremiumBall(Player _session)
        {

            stItem item = new stItem() { id = 0 };

            try
            {

                uint ball = getPremiumBallByTicket(_session.m_pi.pt._typeid);

                // Add Ball
                WarehouseItemEx new_wi = new WarehouseItemEx();
                new_wi.id = -1;
                new_wi.ano = -1;
                new_wi._typeid = ball; // Premium Ball
                new_wi.c[0] = 1;
                new_wi.type = 0x6A; // Item time Premium
                new_wi.clubset_workshop.level = -1;

                var it = _session.m_pi.mp_wi.insert(Tuple.Create(new_wi.id, new_wi));

                // Coloca a premium ball nos itens equipados
                _session.m_pi.ue.ball_typeid = ball;

                // Warehouse Item on Map Player
                _session.m_pi.ei.comet = it.Value;

                // Initialize Item
                item.type = 2;

                item.id = new_wi.id;
                item._typeid = new_wi._typeid;
                item.flag_time = (byte)new_wi.type;
                item.stat.qntd_ant = 0;
                item.stat.qntd_dep = 1;
                item.qntd = 1;
                item.STDA_C_ITEM_QNTD = (short)item.qntd;

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[PremiumSystem::addPremiumBall][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
            return item;
        }

        public stItem addPremiumClubSet(Player _session, uint _time)
        {

            stItem item = new stItem() { id = 0 };

            try
            {

                uint clubset = getPremiumClubSetByTicket(_session.m_pi.pt._typeid);

                // Add ClubSet
                // Aqui add com o item_manager::addItem
                BuyItem bi = new BuyItem() { id = 0 };

                bi.id = -1;

                bi._typeid = clubset;
                bi.qntd = 1;
                bi.time = (short)_time;

                ItemManager.initItemFromBuyItem(_session.m_pi,
                    item, bi, false, 0, 0, 1);

                if (item._typeid == 0u)
                {
                    throw new exception("[PremiumSystem::addPremiumClubSet][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] nao conseguiu inicializar o item[TYPEID=" + Convert.ToString(bi._typeid) + "]", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.PREMIUM_SYSTEM,
                        400, 0));
                }

                if (_session.m_pi.ItemExist(item._typeid))
                    return item;

                if (ItemManager.addItem(item,
                    _session, 0, 0) < 0)
                {
                    throw new exception("[PremiumSystem::addPremiumClubSet][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] nao conseguiu adicionar o item[TYPEID=" + Convert.ToString(item._typeid) + "]", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.PREMIUM_SYSTEM,
                        401, 0));
                }

                var new_wi = _session.m_pi.findWarehouseItemById(item.id);

                new_wi.c[3] = 0;

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[PremiumSystem::addPremiumClubSet][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }

            return item;

        }

        public stItem addPremiumMascot(Player _session, uint _time)
        {

            stItem item = new stItem() { id = 0 };

            try
            {

                uint mascot = getPremiumMascotByTicket(_session.m_pi.pt._typeid);

                // Add Mascot
                // Aqui add com o item_manager::addItem
                BuyItem bi = new BuyItem() { id = 0 };

                bi.id = -1;

                bi._typeid = mascot;
                bi.qntd = 1;
                bi.time = (short)_time;

                ItemManager.initItemFromBuyItem(_session.m_pi,
                    item, bi, false, 0, 0, 1);

                if (item._typeid == 0u)
                {
                    throw new exception("[PremiumSystem::addPremiumMascot][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] nao conseguiu inicializar o item[TYPEID=" + Convert.ToString(bi._typeid) + "]", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.PREMIUM_SYSTEM,
                        400, 0));
                }

                if (_session.m_pi.ItemExist(item._typeid))
                    return item;

                if (ItemManager.addItem(item,
                    _session, 0, 0) < 0)
                {
                    throw new exception("[PremiumSystem::addPremiumMascot][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] nao conseguiu adicionar o item[TYPEID=" + Convert.ToString(item._typeid) + "]", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.PREMIUM_SYSTEM,
                        401, 0));
                }

                var new_wi = _session.m_pi.findMascotById(item.id);

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[PremiumSystem::addPremiumMascot][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }


            return item;

        }

        public void removePremiumBall(Player _session)
        {

            try
            {

                uint ball = getPremiumBallByTicket(_session.m_pi.pt._typeid);

                // Tira primeiro a Ball
                var pWi = _session.m_pi.findWarehouseItemByTypeid(ball);

                // Delete Premium Ball
                if (pWi != null)
                {

                    stItem item = new stItem();

                    item.type = 2;
                    item.id = pWi.id;
                    item._typeid = pWi._typeid;
                    item.qntd = 1;
                    item.STDA_C_ITEM_QNTD= (short)(item.qntd * -1);
                    item.stat.qntd_ant = 1;
                    item.stat.qntd_dep = 0;
                    item.flag_time = 0x6A; // PREMIUM ITEM, /*0x6A expired time*/

                    // Remove do Server, que esse item n�o tem no DB,
                    // � s� do server um item que ganha quando � premium user quando loga
                    // !@ Aqui pode d� erro por que ent� rodando no loob o map mp_wi, e aqui est� excluindo um iterator do map
                    _session.m_pi.mp_wi.Remove(pWi.id);

                    var p = new PangyaBinaryWriter((ushort)0x216);

                    p.WriteUInt32((uint)UtilTime.GetSystemTimeAsUnix());
                    p.WriteUInt32(1); // Count

                    p.WriteByte(item.type);
                    p.WriteUInt32(item._typeid);
                    p.WriteInt32(item.id);
                    p.WriteUInt32(item.flag_time);
                    p.WriteBytes(item.stat.ToArray());
                    p.WriteUInt32((item.STDA_C_ITEM_TIME == 0) ? item.STDA_C_ITEM_QNTD : item.STDA_C_ITEM_TIME);
                    p.WriteZeroByte(25);

                    packet_func.session_send(p,
                        _session, 1);
                }

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[PremiumSystem::removePremiumBall][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }

        public void updatePremiumUser(Player _session)
        {

            try
            {

                List<stItem> add_itens = new List<stItem>();

                //  Flag Premium User
                _session.m_pi.mi.capability.premium_user = true;

                // Add Ball
                var new_ball = addPremiumBall(_session);

                if (new_ball._typeid == 0u)
                {
                    throw new exception("[PremiumSystem::updatePremiumUser][Error] PLAYER[UID=" + Convert.ToString(_session.m_pi.uid) + "] nao conseguiu adicionar a Premium Ball.", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.PREMIUM_SYSTEM,
                        300, 0));
                }

                add_itens.Add(new_ball);

                //Atualiza Capability do player 
                packet_func.session_send(packet_func.pacote09A(_session.m_pi.m_cap.ulCapability), _session);

                if (add_itens.Count > 0)
                {
                    var p = new PangyaBinaryWriter();
                    p.init_plain(0x216);

                    p.WriteUInt32((uint)UtilTime.GetSystemTimeAsUnix());
                    p.WriteUInt32((uint)add_itens.Count); // Count

                    foreach (var item in add_itens)
                    {

                        p.WriteByte(item.type);
                        p.WriteUInt32(item._typeid);
                        p.WriteInt32(item.id);
                        p.WriteUInt32(item.flag_time);
                        p.WriteBytes(item.stat.ToArray());
                        p.WriteUInt32((item.STDA_C_ITEM_TIME == 0) ? item.STDA_C_ITEM_QNTD : item.STDA_C_ITEM_TIME);
                        p.WriteZeroByte(25);
                    }

                    packet_func.session_send(p,
                        _session, 1);
                }

            }
            catch (exception e)
            {

                _smp.message_pool.getInstance().push(new message("[PremiumSystem::updatePremiumUser][ErrorSystem] " + e.getFullMessageError(), type_msg.CL_FILE_LOG_AND_CONSOLE));
            }
        }

        public uint getPremiumBallByTicket(uint _typeid)
        {

            if (_typeid == PREMIUM_TICKET_TYPEID)
                return PREMIUM_BALL_TYPEID;

            if (_typeid == PREMIUM_2_TICKET_TYPEID)
                return PREMIUM_2_BALL_TYPEID;

            return 0u;
        }

        public uint getPremiumClubSetByTicket(uint _typeid)
        {

            if (_typeid == PREMIUM_TICKET_TYPEID)
                return PREMIUM_CLUBSET_TYPEID;

            if (_typeid == PREMIUM_2_TICKET_TYPEID)
                return PREMIUM_2_CLUBSET_TYPEID;

            return 0u;
        }

        public uint getPremiumMascotByTicket(uint _typeid)
        {

            if (_typeid == PREMIUM_TICKET_TYPEID)
                return PREMIUM_MASCOT_TYPEID;

            if (_typeid == PREMIUM_2_TICKET_TYPEID)
                return PREMIUM_MASCOT_TYPEID;

            return 0u;
        }

        public uint getPremiumTitleByTicket(uint _typeid)
        {

            if (_typeid == PREMIUM_TICKET_TYPEID || _typeid == PREMIUM_2_TICKET_TYPEID)
                return PREMIUM_TITLE_TYPEID;
            return 0u;
        }

        public uint getPremiumBoxByTicket(uint _typeid)
        {

            if (_typeid == PREMIUM_TICKET_TYPEID || _typeid == PREMIUM_2_TICKET_TYPEID)
                return PREMIUM_BOX_TYPEID;
            return 0u;
        }

        public uint getExpPangRateByTicket(uint _typeid)
        {

            if (_typeid == PREMIUM_TICKET_TYPEID)
                return 10u;

            if (_typeid == PREMIUM_2_TICKET_TYPEID)
                return 12u;

            return 0u;
        }

        public uint getBoxQntdByTicket(uint _typeid)
        {

            if (_typeid == PREMIUM_TICKET_TYPEID)
                return 4;

            if (_typeid == PREMIUM_2_TICKET_TYPEID)
                return 8;

            return 0u;
        }

        public bool isPremium(uint _typeid)
        {
            return _typeid == PREMIUM_TICKET_TYPEID || _typeid == PREMIUM_2_TICKET_TYPEID;
        }

    }
    public class sPremiumSystem : Singleton<PremiumSystem>
    {
    }
}