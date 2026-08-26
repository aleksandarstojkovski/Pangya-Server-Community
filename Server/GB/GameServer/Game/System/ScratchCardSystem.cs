using System;
using System.Collections.Generic;
using System.Linq;
using Pangya_GameServer.Repository;
using Pangya_GameServer.Models;
using Pangya_GameServer.UTIL;
using PangyaAPI.IFF.GB.Extensions;
using PangyaAPI.SQL;
using PangyaAPI.Utilities;
using PangyaAPI.Utilities.Log;

namespace Pangya_GameServer.Game.System
{
    public class ScratchCardSystem
    {
        public int SCRATCH_CARD_ITEM_MIN_QNTD = 1;
        public int SCRATCH_CARD_ITEM_MAX_QNTD = 2;

        public List<ctx_scratch_card_item> m_ctx_psi = new List<ctx_scratch_card_item>();

        public Dictionary<uint, ctx_scratch_card_coupon> m_ctx_psc = new Dictionary<uint, ctx_scratch_card_coupon>();

        public Dictionary<SCRATCH_CARD_TYPE, ctx_scratch_card_rate> m_rate = new Dictionary<SCRATCH_CARD_TYPE, ctx_scratch_card_rate>();

        public ctx_scratch_card m_ctx_ps = new ctx_scratch_card();


        public bool m_load;

        private readonly object m_cs = new object(); // substitute for CRITICAL_SECTION / pthread_mutex

        readonly uint[] scartch_card_coupon_typeid = new uint[] { 436207779, 436207664, 436207667, 436207668 };

        public ScratchCardSystem()
        {
            m_load = false;
            m_ctx_ps = new ctx_scratch_card();
            m_ctx_psi = new List<ctx_scratch_card_item>();
            m_ctx_psc = new Dictionary<uint, ctx_scratch_card_coupon>();
            m_rate = new Dictionary<SCRATCH_CARD_TYPE, ctx_scratch_card_rate>();
            // no explicit critical section initialization needed in C#
        }

        ~ScratchCardSystem()
        {
            // destructor equivalent, but in C# you usually don't manage unmanaged critical sections here.
            clear();
        }

        // public API (mantive a assinatura e nomes)

        public void load()
        {
            if (isLoad())
                clear();
            initialize();
        }


        public bool isLoad()
        {
            bool isLoad = false;
            lock (m_cs)
            {
                isLoad = (m_load && m_ctx_psi != null && m_ctx_psi.Count > 0 && m_ctx_psc != null && m_ctx_psc.Count > 0);
            }
            return isLoad;
        }


        public WarehouseItemEx hasCoupon(Player _session)
        {
            WarehouseItemEx pWi = null;

            lock (m_cs)
            {
                foreach (var el in m_ctx_psc)
                {
                    if (el.Value.active)
                    {
                        pWi = _session.m_pi.findWarehouseItemByTypeid(el.Value._typeid);
                        if (pWi != null)
                        {
                            return pWi;
                        }
                    }
                }
            }
            return null;
        }

        public bool IsCoupon(uint item)
        {
            lock (m_cs)
            {
                foreach (var el in m_ctx_psc)
                {
                    if (el.Value.active && el.Value._typeid == item)
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        public List<ctx_scratch_card_item_win> Play(Player _session)
        {

            var v_item = new List<ctx_scratch_card_item_win>();
            ctx_scratch_card_item_win ctx_b = new ctx_scratch_card_item_win(0);

            lock (m_cs)
            {
                // Lottery seeded with pointer address in original; use a pseudo-seed
                var lottery = new Lottery();

                // Pega o Rate do Game Server
                var rate_cookie_server = sgs.gs.getInstance().getInfo().rate.papel_shop_cookie_item / 100f;
                var rate_rare_server = sgs.gs.getInstance().getInfo().rate.scratchy / 100f;
                bool event_x2 = (sgs.gs.getInstance().getInfo().rate.scratchy) / 100 >= 2;
                var rate_player_sec = DateTime.Now.Second / 100f;

                foreach (var el in m_ctx_psi)
                {
                    if (el.active && (el.numero == -1 || el.numero == m_ctx_ps.numero))
                    {
                        float factor = 1f + rate_player_sec;
                        if (el.tipo == SCRATCH_CARD_TYPE.SCT_COOKIE)
                            factor = rate_cookie_server + rate_player_sec;
                        else if (el.tipo == SCRATCH_CARD_TYPE.SCT_RARE)
                            factor = rate_rare_server + rate_player_sec;

                        // preserve original calculation: cast to uint weight
                        uint weight = (uint)(el.probabilidade * factor);
                        lottery.Push(weight, el);
                    }
                }

                ushort num = 1;
                if (event_x2)
                {
                    var min = SCRATCH_CARD_ITEM_MIN_QNTD;
                    var max = SCRATCH_CARD_ITEM_MAX_QNTD;
                    num = (ushort)(min + (new Random().Next() % (max - min + 1)));
                }

                if (num == 0) num = (ushort)SCRATCH_CARD_ITEM_MIN_QNTD;

                Lottery.LotteryCtx lc = null;
                do
                {
                    lc = lottery.SpinRoleta();
                    if (lc == null)
                        throw new exception("[ScractCardSystem::Play][Error] nao conseguiu sortear item. Bug viu, procura um programador e resolve isso rapido !");

                    ctx_b.clear();
                    var ctx_psi = (ctx_scratch_card_item)lc.Value;

                    // Player já tem o item, e nao pode ter duplicate, sortea um novo para ele
                    if ((!sIff.getInstance().IsCanOverlapped(ctx_psi._typeid) || sIff.getInstance().getItemGroupIdentify(ctx_psi._typeid) == sIff.getInstance().CAD_ITEM)
                        && _session.m_pi.ownerItem(ctx_psi._typeid))
                    {
                        continue;
                    }

                    if (ctx_psi.tipo == SCRATCH_CARD_TYPE.SCT_RARE)
                    {
                        num = (ushort)SCRATCH_CARD_ITEM_MIN_QNTD;
                    }

                    // Raro Item Sempre é a qntd minima
                    if (ctx_psi.tipo == SCRATCH_CARD_TYPE.SCT_RARE)
                    {
                        if (ctx_psi.qntd == 0)
                        {
                            ctx_b.qntd = 1;
                        }
                    }

                    if (ctx_psi.tipo != SCRATCH_CARD_TYPE.SCT_RARE && ctx_psi.qntd == 0)
                    {
                        ctx_b.qntd = (uint)(new Random().Next() % (3 - 1 + 1));
                    }

                    if (ctx_psi.qntd == 0)
                    {
                        ctx_b.qntd = (uint)(new Random().Next() % (3 - 1 + 1));
                    }

                    ctx_b.qntd = ctx_psi.qntd;
                    ctx_b.ctx_psi = new ctx_scratch_card_item();
                    // copy fields explicitly to be faithful
                    ctx_b.ctx_psi._typeid = ctx_psi._typeid;
                    ctx_b.ctx_psi.probabilidade = ctx_psi.probabilidade;
                    ctx_b.ctx_psi.qntd = ctx_psi.qntd;
                    ctx_b.ctx_psi.numero = ctx_psi.numero;
                    ctx_b.ctx_psi.tipo = ctx_psi.tipo;
                    ctx_b.ctx_psi.active = ctx_psi.active;

                    v_item.Add(ctx_b);

                    num--;
                } while (num > 0);
            }

            return v_item;
        }

        protected void initialize()
        {
            lock (m_cs)
            {
                foreach (var t in scartch_card_coupon_typeid)
                {
                    var ctx_psc = new ctx_scratch_card_coupon
                    {
                        _typeid = t,
                        active = true
                    };
                    m_ctx_psc[ctx_psc._typeid] = ctx_psc;
                }

                // Load Item(s)
                var cmd_psi = new CmdScratchCardItem(true);
                snmdb.NormalManagerDB.getInstance().add(0, cmd_psi, null, null);

                m_ctx_psi = cmd_psi.getInfo();

                // Load Rate(s)
                var cmd_psr = new CmdScratchCardRate(true);
                snmdb.NormalManagerDB.getInstance().add(0, cmd_psr, null, null);
                m_rate = cmd_psr.getInfo();

                if (m_ctx_psi == null || m_ctx_psi.Count <= 0 || m_rate == null || m_rate.Count <= 0)
                {
                    _smp.message_pool.getInstance().push(new message("[ScratchCardSystem::initialize][Log] Scratch Card System nao Carregado com sucesso!", 0));
                }

                m_load = true;
            }
        }

        protected void clear()
        {
            lock (m_cs)
            {
                m_ctx_ps = new ctx_scratch_card();

                if (m_ctx_psi != null && m_ctx_psi.Count > 0)
                {
                    m_ctx_psi.Clear();
                }

                if (m_ctx_psc != null && m_ctx_psc.Count > 0)
                    m_ctx_psc.Clear();

                m_load = false;
            }
        }

        private uint FindRateByType(SCRATCH_CARD_TYPE tipo)
        {
            lock (m_cs)
            {
                foreach (var el in m_rate)
                {
                    if (el.Key == tipo)
                    {
                        return el.Value.Prob;
                    }
                }
            }
            return 1;
        }
    } // end class ScratchCardSystem

    public class sScratchCardSystem : Singleton<ScratchCardSystem>
    { }
}