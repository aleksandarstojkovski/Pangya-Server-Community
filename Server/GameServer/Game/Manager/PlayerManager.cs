using Pangya_GameServer.Repository;
using Pangya_GameServer.Game.System;
using Pangya_GameServer.Models;
using Pangya_GameServer.PacketFunc;
using PangyaAPI.IFF.JP.Extensions;
using PangyaAPI.IFF.JP.Models.Flags;
using PangyaAPI.Network.PangyaSession;
using PangyaAPI.SQL;
using PangyaAPI.Utilities;
using PangyaAPI.Utilities.BinaryModels;
using PangyaAPI.Utilities.Log;
using System;
using System.Collections.Generic;
using System.Linq;
using static Pangya_GameServer.Models.DefineConstants;

namespace Pangya_GameServer.Game.Manager
{
    public class PlayerManager : SessionManager
    {
        class uIndexOID
        {

            public byte ucFlag;
            public struct stFlag
            {
                public byte busy;
                public bool block;
            }
            public stFlag flag;

            public byte getFlag()
            { return ucFlag; }
        }

        SortedList<int, uIndexOID> m_indexes;       // Index de OID


        public PlayerManager()
        {
            if (m_max_session != 0)
            {
                m_indexes = new SortedList<int, uIndexOID>();

                for (var i = 0; i < m_max_session; ++i)
                    m_sessions.Add(i, new Player());
            }
            else
            {
                throw new exception("fail to class");
            }
        }



        public override void Clear()
        {
            base.Clear();
            if (m_indexes != null && m_indexes.Count > 0)
                m_indexes.Clear();
        }

        public Player findPlayer(uint? _uid, bool _oid = true)
        {

            foreach (var el in m_sessions.Values)
            {
                if ((_oid ? el.getUID() : (uint)el.m_oid) == _uid)
                {
                    return (Player)el;
                }
            }


            return null;
        }

        public Player FindPlayer(uint uid, bool oid)
        {
            Player p = null;
            foreach (var el in m_sessions.Values)
            {
                if (el.m_sock != null && ((!oid) ? el.getUID() : (uint)el.m_oid) == uid)
                {
                    p = (Player)el;
                    break;
                }
            }

            return p;
        }


        public override Session FindSessionByOid(uint oid)
        {
            return base.FindSessionByOid(oid);
        }

        public override Session findSessionByUID(uint uid)
        {
            return base.findSessionByUID(uid);
        }

        public override List<Session> FindAllSessionByUid(uint uid)
        {
            return base.FindAllSessionByUid(uid);
        }

        public override Session FindSessionByNickname(string nickname)
        {
            return base.FindSessionByNickname(nickname);
        }
        // Override methods
        public override bool DeleteSession(Session _session)
        {

            if (_session == null)
                throw new exception("[player_manager::deleteSession][ERR_SESSION] _session is null.");



            // Block Session 
            int tmp_oid = _session.m_oid;

            bool ret = false;
            if (tmp_oid > -1 && (ret = _session.clear()))
            {
                m_sessions[tmp_oid] = _session;//reseta na lista
                // Libera OID
                freeOID((uint)tmp_oid/*_session.m_oid*/);

                m_count--;
            }
            return ret;
        }

        public void checkPlayersItens()
        {
            try
            {

                // !@ WARNING tem que ter o thread safe aqui, pode testar um player, e ele não está online mais
                foreach (var s in m_sessions.Values)
                {

                    if (s.isCreated())
                    {
                        // Item Buff
                        checkItemBuff((Player)s);

                        // Card Equipped Special
                        checkCardSpecial((Player)s);

                        // Caddie
                        checkCaddie((Player)s);

                        // Mascot
                        checkMascot((Player)s);

                        // Warehouse
                        checkWarehouse((Player)s);
                        // Warehouse item limit
                        checkItemLimited((Player)s);
                    }
                }
            }
            catch (exception e)
            {
                _smp.message_pool.getInstance().push(new message("[player_manager::checkPlayersItens][ErrorSystem] " + e.getFullMessageError(), 0));
            }
        }

        public void blockOID(int _oid)
        {
            var it = m_indexes.Where(c => c.Key == _oid).FirstOrDefault();

            if (it.Value != null)
                it.Value.flag.block = true;	// Block

        }

        public void unblockOID(int _oid)
        {
            var it = m_indexes.Where(c => c.Key == _oid).FirstOrDefault();

            if (it.Value != null)
                it.Value.flag.block = true;	// unblock
        }

        public static void checkItemBuff(Player _session)
        {
            // Item Buff
            // Card Equipped Special
            var cards = _session.m_pi.v_ib.ToList();
            foreach (var it in cards /*Incrementa em baixo*/)
            {

                // Acabou o tempo
                if (DateTime.Now > it.end_date.ConvertTime()){

                    // Log
                    _smp.message_pool.getInstance().push(new message("[player_manager::checkItemBuff][Log] PLAYER[UID=" + (_session.m_pi.uid)
                            + "] Acabou o Tempo do Item Buff[TYPEID=" + (it._typeid) + ", ID=" + (it.id)
                            + ", EFEITO=" + (it.efeito) + ", EFEITO_QNTD=" + (it.efeito_qntd) + ", END_DATE="
                            + (it.end_date.ConvertTime()) + "] excluindo ele do vector.", 0)); 

                    // Delete Item Buff from player vector 
                    _session.m_pi.v_ib.Remove(it);

                }
            }
        }
        public static void checkCardSpecial(Player _session)
        { // Card Equipped Special
            var cards = _session.m_pi.v_cei.ToList();
            foreach (var it in cards /*Incrementa em baixo*/)
            {

                // Acabou o tempo
                if (it.tipo == (uint)CARD_SUB_TYPE.T_SPECIAL &&  DateTime.Now > it.end_date.ConvertTime())
                {

                    // Log
                    _smp.message_pool.getInstance().push(new message("[player_manager::checkCardSpecial][Log] PLAYER[UID=" + (_session.m_pi.uid)
                            + "] Acabou o Tempo do Card Equiped Special[TYPEID=" + (it._typeid) + ", ID=" + (it.id)
                            + ", EFEITO=" + (it.efeito) + ", EFEITO_QNTD=" + (it.efeito_qntd) + ", END_DATE="
                            + (it.end_date.ConvertTime()) + "] excluindo ele do vector.", 0));

                    // Delete Item Buff from player vector
                    _session.m_pi.v_cei.Remove(it);

                }
            }
        }
        public static void checkCaddie(Player _session)
        {
            // Caddie
            foreach (var el in _session.m_pi.mp_ci.Values)
            {
                // Caddie por tempo
                if (el.rent_flag == 2 && DateTime.Now > el.end_date.ConvertTime())
                {

                    // Put Update Item on vector update item of player
                        if (_session.m_pi.findUpdateItemByTypeidAndType((uint)el._typeid, UpdateItem.UI_TYPE.CADDIE).Count == 0)

                        {
                            _session.m_pi.mp_ui.Add(new PlayerInfo.stIdentifyKey(el._typeid, el.id), new UpdateItem(UpdateItem.UI_TYPE.CADDIE, el._typeid, el.id));



                        // Verifica se o Caddie está equipado e desequipa
                        if ((_session.m_pi.ei.cad_info != null && _session.m_pi.ei.cad_info.id == el.id) || _session.m_pi.ue.caddie_id == el.id)
                        {

                            _session.m_pi.ei.cad_info = null;
                            _session.m_pi.ue.caddie_id = 0;
                        }
                    }
                }

                // Parts Caddie End Date
                if (el.parts_typeid != 0 && !el.end_parts_date.IsEmpty && UtilTime.GetLocalDateDiffDESC(el.end_parts_date) <= 0)
                {

                    // Put Update Item on vector update item of player
                    if (_session.m_pi.findUpdateItemByTypeidAndType((uint)el.parts_typeid, UpdateItem.UI_TYPE.CADDIE_PARTS).Count == 0)
                    {

                        _session.m_pi.mp_ui.Add(new PlayerInfo.stIdentifyKey(el._typeid, el.id), new UpdateItem(UpdateItem.UI_TYPE.CADDIE_PARTS, el._typeid, el.id));

                        el.parts_typeid = 0;
                        el.parts_end_date_unix = 0;
                        el.end_parts_date = new SYSTEMTIME();

                        snmdb.NormalManagerDB.getInstance().add(1, new CmdUpdateCaddieInfo(_session.m_pi.uid, el), SQLDBResponse, null);
                    }
                }
            }

        }
        public static void checkMascot(Player _session)
        {
            // Mascot
            foreach (var el in _session.m_pi.mp_mi)
            {

                // Mascot por Tempo
                if (el.Value.tipo == 1 && DateTime.Now > el.Value.data.ConvertTime())
                {

                    // Put Update Item on vector update item of player
                    if (_session.m_pi.findUpdateItemByTypeidAndType((uint)el.Value._typeid, UpdateItem.UI_TYPE.MASCOT).Count == 0)
                    {

                        _session.m_pi.mp_ui.insert(new PlayerInfo.stIdentifyKey(el.Value._typeid, el.Value.id), new UpdateItem(UpdateItem.UI_TYPE.MASCOT, el.Value._typeid, el.Value.id));

                        // Log
                        _smp.message_pool.getInstance().push(new message("[player_manager::checkMascot][Log] PLAYER[UID=" + (_session.m_pi.uid)
                                + "] Mascout[TYPEID=" + (el.Value._typeid) + ", ID=" + (el.Value.id)
                                + ", END_DATE=" + (el.Value.data.ConvertTime()) + "] acabou o tempo dele, coloca no vector de update itens.", 0));

                        stItem item = new stItem();

                        item.type = 2;
                        item.id = el.Value.id;
                        item._typeid = el.Value._typeid;
                        item.qntd = 1;
                        item.STDA_C_ITEM_QNTD = (short)(item.qntd * -1);
                        var p = new PangyaBinaryWriter();
                        // Atualiza o item no Jogo
                        p.init_plain(0x216);

                        p.WriteUInt32((uint)UtilTime.GetSystemTimeAsUnix());
                        p.WriteUInt32(1);//count item 
                        p.WriteByte(item.type);
                        p.WriteUInt32(item._typeid);
                        p.WriteInt32(item.id);
                        p.WriteUInt32(0);//flag time
                        p.WriteBytes(item.stat.ToArray());
                        p.WriteInt32(item.STDA_C_ITEM_QNTD32);
                        p.WriteZero(25);

                        packet_func.session_send(p,
                            _session, 1);

                        // Verifica se o Mascot está equipado e desequipa
                        if ((_session.m_pi.ei.mascot_info != null && _session.m_pi.ei.mascot_info.id == el.Value.id) || _session.m_pi.ue.mascot_id == el.Value.id)
                        {

                            _session.m_pi.ei.mascot_info = null;
                            _session.m_pi.ue.mascot_id = 0;

                            _smp.message_pool.getInstance().push(new message("[player_manager::checkMascot][Log] PLAYER[UID=" + (_session.m_pi.uid)
                                    + "] Desequipando Mascot[TYPEID=" + (el.Value._typeid) + ", ID=" + (el.Value.id)
                                    + "].", 0));
                        }
                    }
                }
            }
        }

        public static void checkWarehouse(Player _session)
        {
            foreach (var el in _session.m_pi.mp_wi)
            {

                // Item Por tempo
                if ((el.Value.flag & (0x20 | 0x40 | 0x60)) != 0
                        && el.Value.end_date_unix_local > 0)
                {

                    var st = UtilTime.UnixToSystemTime(el.Value.end_date_unix_local);

                    if (DateTime.Now > st.ConvertTime())
                    {
                        var pWi = el.Value;
                        // Put Update Item on vector update item of player
                        if (_session.m_pi.findUpdateItemByTypeidAndType((uint)el.Value._typeid, UpdateItem.UI_TYPE.WAREHOUSE).Count == 0)
                        {

                            _session.m_pi.mp_ui.insert(new PlayerInfo.stIdentifyKey(el.Value._typeid, el.Value.id), new UpdateItem(UpdateItem.UI_TYPE.WAREHOUSE, el.Value._typeid, el.Value.id));

                            // Log
                            _smp.message_pool.getInstance().push(new message("[player_manager::checkWarehouse][Log] PLAYER[UID=" + (_session.m_pi.uid)
                                    + "] Warehouse Item[TYPEID=" + (el.Value._typeid) + ", ID=" + (el.Value.id)
                                    + ", END_DATE=" + UtilTime.UnixToSystemTime(el.Value.end_date_unix_local).ConvertTime() + "] acabou o tempo dele, coloca no vector de update itens.", type_msg.CL_FILE_LOG_AND_CONSOLE));

                            //avisa ao cliente que ele deletou ou acabou o tempo!!!
                            stItem item = new stItem();

                            item.type = 2;
                            item.id = pWi.id;
                            item._typeid = pWi._typeid;
                            item.qntd = 1;
                            item.STDA_C_ITEM_QNTD = (short)(item.qntd * -1);
                            var p = new PangyaBinaryWriter();
                            // Atualiza o item no Jogo
                            p.init_plain(0x216);

                            p.WriteUInt32((uint)UtilTime.GetSystemTimeAsUnix());
                            p.WriteUInt32(1);//count item 
                            p.WriteByte(item.type);
                            p.WriteUInt32(item._typeid);
                            p.WriteInt32(item.id);
                            p.WriteUInt32(0);//flag time
                            p.WriteBytes(item.stat.ToArray());
                            p.WriteInt32(item.STDA_C_ITEM_QNTD32);
                            p.WriteZero(25);

                            packet_func.session_send(p,
                                _session, 1);

                            // Verifica se o item é um PART e se ele está equipado e deseequipa ele
                            if (sIff.getInstance().getItemGroupIdentify(el.Value._typeid) == sIff.getInstance().PART && _session.m_pi.isPartEquiped(el.Value._typeid, el.Value.id))
                            {

                                var ci = _session.m_pi.findCharacterByTypeid((uint)((Convert.ToUInt32(sIff.getInstance().CHARACTER << 26)) | sIff.getInstance().getItemCharIdentify(el.Value._typeid)));

                                if (ci != null)
                                {

                                    var part = sIff.getInstance().findPart(el.Value._typeid);

                                    if (part != null)
                                    {

                                        // Deseequipa o Part do character e coloca os Parts Default do Character no lugar
                                        ci.unequipPart(part);

                                        _smp.message_pool.getInstance().push(new message("[player_manager::checkWarehouse][Log] PLAYER[UID=" + (_session.m_pi.uid)
                                                + "] Desequipando Part[TYPEID=" + (el.Value._typeid) + ", ID=" + (el.Value.id)
                                                + "] do Character[TYPEID=" + (ci._typeid) + "], coloca parts default no lugar do part que estava equipado.", type_msg.CL_FILE_LOG_AND_CONSOLE));

                                    }
                                    else
                                    {

                                        for (var i = 0; i < 24; ++i)
                                        {

                                            if (ci.parts_id[i] == el.Value.id && ci.parts_typeid[i] == el.Value._typeid)
                                            {
                                                ci.parts_typeid[i] = 0;
                                                ci.parts_id[i] = 0;
                                            }
                                        }

                                        _smp.message_pool.getInstance().push(new message("[player_manager::checkWarehouse][Error] PLAYER[UID=" + (_session.m_pi.uid)
                                                + "] nao tem o Part[TYPEID=" + (el.Value._typeid) + "] do Character[TYPEID=" + (ci._typeid) + "], no IFF_STRUCT desequipa ele. Hacker ou Bug", type_msg.CL_FILE_LOG_AND_CONSOLE));
                                    }

                                    // Update no DB
                                    snmdb.NormalManagerDB.getInstance().add(2, new CmdUpdateCharacterAllPartEquiped(_session.m_pi.uid, ci), SQLDBResponse, null);

                                }
                                else
                                    _smp.message_pool.getInstance().push(new message("[player_manager::checkWarehouse][Error][Warning] PLAYER[UID=" + (_session.m_pi.uid)
                                            + "] nao tem o Character[TYPEID=" + ((Convert.ToUInt32(sIff.getInstance().CHARACTER << 26)) | sIff.getInstance().getItemCharIdentify(el.Value._typeid)) + "]. Hacker ou Bug", type_msg.CL_FILE_LOG_AND_CONSOLE));

                            }

                            // Verifica se é ClubSet e desequipa ele, e coloca o CV
                            if (sIff.getInstance().getItemGroupIdentify(el.Value._typeid) == sIff.getInstance().CLUBSET && _session.m_pi.ei.clubset != null
                                    && _session.m_pi.ei.clubset.id == el.Value.id || _session.m_pi.ue.clubset_id == el.Value.id)
                            {

                                var it = _session.m_pi.findWarehouseItemByTypeid(AIR_KNIGHT_SET);

                                if (it != null)
                                {

                                    _session.m_pi.ei.clubset = it;
                                    _session.m_pi.ue.clubset_id = it.id;

                                    // Atualiza o ClubSet Enchant no Equiped Item do Player
                                    _session.m_pi.ei.csi.setValues(it.id, it._typeid, it.c);

                                    var cs = sIff.getInstance().findClubSet(it._typeid);

                                    if (cs != null)
                                        for (var i = 0; i < 5; ++i)
                                            _session.m_pi.ei.csi.enchant_c[i] = (short)(cs.SlotStats.getSlot[i] + it.clubset_workshop.c[i]);
                                }

                                _smp.message_pool.getInstance().push(new message("[player_manager::checkWarehouse][Log] PLAYER[UID=" + (_session.m_pi.uid)
                                        + "] Desequipando ClubSet[TYPEID=" + (el.Value._typeid) + ", ID=" + (el.Value.id)
                                        + "]" + (it != null ? ", e colocando o Air Knight Set[TYPEID=" + (it._typeid) + ", ID="
                                        + (it.id) + "] no lugar." : "."), type_msg.CL_FILE_LOG_AND_CONSOLE));
                            }

                            // Verifica se é um Comet(Ball) e desequipa ele, e coloca a bola padrão
                            if (sIff.getInstance().getItemGroupIdentify(el.Value._typeid) == sIff.getInstance().BALL && _session.m_pi.ei.comet != null
                                    && _session.m_pi.ei.comet.id == el.Value.id || _session.m_pi.ue.ball_typeid == el.Value._typeid)
                            {

                                var it = _session.m_pi.findWarehouseItemByTypeid(DEFAULT_COMET_TYPEID);

                                if (it != null)
                                {

                                    _session.m_pi.ei.comet = it;
                                    _session.m_pi.ue.ball_typeid = DEFAULT_COMET_TYPEID;
                                }

                                _smp.message_pool.getInstance().push(new message("[player_manager::checkWarehouse][Log] PLAYER[UID=" + (_session.m_pi.uid)
                                        + "] Desequipando Ball[TYPEID=" + (el.Value._typeid) + ", ID=" + (el.Value.id)
                                        + "]" + (it != null ? ", e colocando a Ball[TYPEID=" + (it._typeid) + ", ID="
                                        + (it.id) + "] padrao no lugar." : "."), type_msg.CL_FILE_LOG_AND_CONSOLE));
                            }

                            // Verifica se é SKIN, para desequipar ele
                            if (sIff.getInstance().getItemGroupIdentify(el.Value._typeid) == sIff.getInstance().SKIN)
                            {

                                for (var i = 0; i < _session.m_pi.ue.skin_typeid.Length; ++i)
                                {

                                    if (_session.m_pi.ue.skin_typeid[i] == el.Value._typeid && _session.m_pi.ue.skin_id[i] == el.Value.id)
                                    {

                                        _session.m_pi.ue.skin_id[i] = 0;
                                        _session.m_pi.ue.skin_typeid[i] = 0;

                                        _smp.message_pool.getInstance().push(new message("[player_manager::checkWarehouse][Log] PLAYER[UID=" + (_session.m_pi.uid)
                                                + "] Desequipando SKIN[TYPEID=" + (el.Value._typeid) + ", ID=" + (el.Value.id)
                                                + ", SLOT=" + (i) + "]", type_msg.CL_FILE_LOG_AND_CONSOLE));

                                        break;
                                    }
                                }
                            }

                            // Verifica se é o Premium Ticket
                            if (sIff.getInstance().getItemGroupIdentify(el.Value._typeid) == sIff.getInstance().ITEM && sPremiumSystem.getInstance().isPremium(el.Value._typeid))
                            {

                                // Log
                                _smp.message_pool.getInstance().push(new message("[player_manager::checkWarehouse][Log] PLAYER[UID=" + (_session.m_pi.uid)
                                        + "] Tirando o Modo Premium User do Player, acabou o tempo do ticket, tirando a capacidade e a Comet(Ball)", type_msg.CL_FILE_LOG_AND_CONSOLE));

                                sPremiumSystem.getInstance().removePremiumUser(_session);
                            }
                        }
                    }
                }
            }
        }
         
        public static void checkItemLimited(Player _session)
        {
            foreach (var el in _session.m_pi.mp_wi)
            {

                // Item Por tempo
                if (el.Value.STDA_C_ITEM_QNTD >= 20000)
                {
                    // Log
                    _smp.message_pool.getInstance().push(new message("[player_manager::checkItemLimited][Log] PLAYER[UID=" + _session.m_pi.uid + ", ITEMTID=" + el.Value._typeid + ", ITEMID=" + el.Value.id + "]" + "teste do codigo", type_msg.CL_FILE_LOG_AND_CONSOLE));

                }
            }
        }

        // Sem proteção de sincronização, chamar ela em uma função thread safe(thread com seguranção de sincronização)//errado
        public override int findSessionFree()
        {
            for (var i = 0; i < m_sessions.Count; ++i)
            {
                if (m_sessions[i].m_oid < 0)
                {
                    var oid = getNewOID();
                    return oid;
                }
            }
            return int.MaxValue;
        }

        // Sem proteção de sincronização, chamar ela em uma função thread safe(thread com seguranção de sincronização)//errado
        public int getNewOID()
        {
            int oid = 0;

            // Find a index OID FREE
            var it = m_indexes.Where(c => c.Value.ucFlag == 0).FirstOrDefault();

            if (it.Value != null)
            {   // Achei 1 index desocupado

                m_indexes[it.Key].flag.busy = 1; // BUSY OCUPDADO 
                oid = it.Key;
            }
            else
            {   // Add um novo index no mapa de oid 
                oid = m_indexes.Count;//sempre gerar o proximo

                m_indexes.Add(oid, new uIndexOID() { ucFlag = 1 });
            }
            return oid;
        }

        public void freeOID(uint _oid)
        {
            var it = m_indexes.Where(c => c.Key == _oid).FirstOrDefault();

            if (it.Value != null)
            {
                it.Value.flag.busy = 0; // WAITING DESOCUPADO LIVRE

                if (it.Value.flag.block)
                    _smp.message_pool.getInstance().push(new message("[player_manager::freeOID][Warning] index[OID=" + (it.Key) + "] esta bloqueado, nao pode liberar ele agora", 0));
            }
            else
                _smp.message_pool.getInstance().push(new message("[player_manager::freeOID][Warning] index[OID=" + (_oid) + "] nao esta no mapa.", 0));

        }

        public static void SQLDBResponse(int _msg_id, Pangya_DB _pangya_db, object _arg)
        {

            if (_arg == null)
            {
                // Static Functions of Class
                _smp.message_pool.getInstance().push(new message("[player_manager::SQLDBResponse]WARNING] _arg is null", 0));
                return;
            }

            // Por Hora só sai, depois faço outro tipo de tratamento se precisar
            if (_pangya_db.getException().getCodeError() != 0)
            {
                _smp.message_pool.getInstance().push(new message("[player_manager::SQLDBResponse][Error] " + _pangya_db.getException().getFullMessageError(), 0));
                return;
            }

            //var pm = reinterpret_cast< player_manager* >(_arg);

            switch (_msg_id)
            {
                case 1: // Update Caddie Info
                    {
                        var cmd_uci = (CmdUpdateCaddieInfo)(_pangya_db);

                        _smp.message_pool.getInstance().push(new message("[player_manager::SQLDBResponse][Debug] PLAYER[UID=" + (cmd_uci.getUID()) + "] Atualizou Caddie Info[TYPEID="
                                 + (cmd_uci.getInfo()._typeid) + ", ID=" + (cmd_uci.getInfo().id) + ", PARTS_TYPEID=" + (cmd_uci.getInfo().parts_typeid)
                                 + ", END_DATE=" + cmd_uci.getInfo().end_date.ConvertTime() + ", PARTS_END_DATE=" + cmd_uci.getInfo().end_parts_date.ConvertTime() + "] com sucesso!", 0));

                        break;
                    }
                case 2: // Update All parts of Character
                    {
                        break;
                    }
                case 0:
                default:
                    break;
            }
        }
    }
}