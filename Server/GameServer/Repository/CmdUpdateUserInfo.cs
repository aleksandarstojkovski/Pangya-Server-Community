using Pangya_GameServer.Models;
using PangyaAPI.SQL;
using PangyaAPI.Utilities;
using System;
using System.Data;

namespace Pangya_GameServer.Repository
{
    public class CmdUpdateUserInfo : Pangya_DB
    {
        public CmdUpdateUserInfo(uint _uid, UserInfoEx _ui)
        {
            this.m_uid = _uid;
            this.m_ui = _ui;
        }
        public uint getUID()
        {
            return (m_uid);
        }

        public void setUID(uint _uid)
        {
            m_uid = _uid;
        }

        public UserInfoEx getInfo()
        {
            return m_ui;
        }

        public void setInfo(UserInfoEx _ui)
        {
            m_ui = _ui;
        }

        protected override void lineResult(ctx_res _result, uint _index_result)
        {

            // N�o usa por que � um UPDATE
            return;
        }

        protected override Response prepareConsulta()
        {

            if (m_uid == 0 || m_uid == uint.MaxValue)
            {
                throw new exception("[CmdUpdateUserInfo::prepareConsulta][Error] m_uid is invalid(zero)", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.PANGYA_DB,
                    4, 0));
            }

            string[] parametros = new string[]
            {
    "@IDUSER", "@BEST_DRIVE", "@BEST_LONG_PUTT", "@BEST_CHIPIN",
        "@COMBO", "@ALL_COMBO", "@TACADA", "@PUTT", "@TEMPO", "@TEMPO_TACADA",
        "@ACERTO_PANGYA", "@TIMEOUT", "@OB", "@TOTAL_DISTANCIA", "@HOLE",
        "@HOLEIN", "@HIO", "@BUNKER", "@FAIRWAY", "@ALBATROSS", "@MAD_CONDUTA",
        "@PUTTIN", "@MEDIA_SCORE", "@BEST_SCORE_0", "@BEST_SCORE_1", "@BEST_SCORE_2",
        "@BEST_SCORE_3", "@BEST_SCORE_4", "@BEST_PANG_0", "@BEST_PANG_1",
        "@BEST_PANG_2", "@BEST_PANG_3", "@BEST_PANG_4", "@SUM_PANG",
        "@EVENT_FLAG", "@JOGADO", "@TEAM_GAME", "@TEAM_WIN", "@TEAM_HOLE",
        "@LADDER_POINT", "@LADDER_HOLE", "@LADDER_WIN", "@LADDER_LOSE", "@LADDER_DRAW",
        "@QUITADO", "@SKIN_PANG", "@SKIN_WIN", "@SKIN_LOSE", "@SKIN_RUN_HOLE",
        "@SKIN_ALL_IN_COUNT", "@NAO_SEI", "@JOGOS_NAO_SEI", "@EVENT_VALUE",
        "@SKIN_STRIKE_POINT", "@MAX_JOGOS_NAO_SEI", "@GAME_COUNT_SEASON",
        "@TOTAL_PANG_WIN_GAME", "@MEDAL_LUCKY", "@MEDAL_FAST", "@MEDAL_BEST_DRIVE",
        "@MEDAL_BEST_CHIPIN", "@MEDAL_BEST_PUTTIN", "@MEDAL_BEST_RECOVERY", "@_16BIT_NAO_SEI"
            };
            SqlDbType[] tipos = new SqlDbType[]
            {
    SqlDbType.Int,        // @IDUSER
    SqlDbType.Float,      // @BEST_DRIVE
    SqlDbType.Float,      // @BEST_LONG_PUTT
    SqlDbType.Float,      // @BEST_CHIPIN
    SqlDbType.Int,        // @COMBO
    SqlDbType.Int,        // @ALL_COMBO
    SqlDbType.Int,        // @TACADA
    SqlDbType.Int,        // @PUTT
    SqlDbType.Int,        // @TEMPO
    SqlDbType.Int,        // @TEMPO_TACADA
    SqlDbType.Int,        // @ACERTO_PANGYA
    SqlDbType.Int,        // @TIMEOUT
    SqlDbType.Int,        // @OB
    SqlDbType.Int,        // @TOTAL_DISTANCIA
    SqlDbType.Int,        // @HOLE
    SqlDbType.Int,        // @HOLEIN
    SqlDbType.Int,        // @HIO
    SqlDbType.SmallInt,   // @BUNKER
    SqlDbType.Int,        // @FAIRWAY
    SqlDbType.Int,        // @ALBATROSS
    SqlDbType.Int,        // @MAD_CONDUTA
    SqlDbType.Int,        // @PUTTIN
    SqlDbType.Int,        // @MEDIA_SCORE
    SqlDbType.TinyInt,    // @BEST_SCORE_0
    SqlDbType.TinyInt,    // @BEST_SCORE_1
    SqlDbType.TinyInt,    // @BEST_SCORE_2
    SqlDbType.TinyInt,    // @BEST_SCORE_3
    SqlDbType.TinyInt,    // @BEST_SCORE_4
    SqlDbType.BigInt,     // @BEST_PANG_0
    SqlDbType.BigInt,     // @BEST_PANG_1
    SqlDbType.BigInt,     // @BEST_PANG_2
    SqlDbType.BigInt,     // @BEST_PANG_3
    SqlDbType.BigInt,     // @BEST_PANG_4
    SqlDbType.BigInt,     // @SUM_PANG
    SqlDbType.TinyInt,    // @EVENT_FLAG
    SqlDbType.Int,        // @JOGADO
    SqlDbType.Int,        // @TEAM_GAME
    SqlDbType.Int,        // @TEAM_WIN
    SqlDbType.Int,        // @TEAM_HOLE
    SqlDbType.Int,        // @LADDER_POINT
    SqlDbType.Int,        // @LADDER_HOLE
    SqlDbType.Int,        // @LADDER_WIN
    SqlDbType.Int,        // @LADDER_LOSE
    SqlDbType.Int,        // @LADDER_DRAW
    SqlDbType.Int,        // @QUITADO
    SqlDbType.BigInt,     // @SKIN_PANG
    SqlDbType.Int,        // @SKIN_WIN
    SqlDbType.Int,        // @SKIN_LOSE
    SqlDbType.Int,        // @SKIN_RUN_HOLE
    SqlDbType.Int,        // @SKIN_ALL_IN_COUNT
    SqlDbType.Int,        // @NAO_SEI
    SqlDbType.Int,        // @JOGOS_NAO_SEI
    SqlDbType.SmallInt,   // @EVENT_VALUE
    SqlDbType.Int,        // @SKIN_STRIKE_POINT
    SqlDbType.Int,        // @MAX_JOGOS_NAO_SEI
    SqlDbType.Int,        // @GAME_COUNT_SEASON
    SqlDbType.Int,        // @TOTAL_PANG_WIN_GAME
    SqlDbType.Int,        // @MEDAL_LUCKY
    SqlDbType.Int,        // @MEDAL_FAST
    SqlDbType.Int,        // @MEDAL_BEST_DRIVE
    SqlDbType.Int,        // @MEDAL_BEST_CHIPIN
    SqlDbType.Int,        // @MEDAL_BEST_PUTTIN
    SqlDbType.Int,        // @MEDAL_BEST_RECOVERY
    SqlDbType.SmallInt    // @_16BIT_NAO_SEI
            };
            object[] valores = new object[]
           {
    (int)m_uid,                     // @IDUSER
    (float)m_ui.best_drive,         // @BEST_DRIVE
    (float)m_ui.best_long_putt,     // @BEST_LONG_PUTT
    (float)m_ui.best_chip_in,       // @BEST_CHIPIN
    (int)m_ui.combo,                // @COMBO
    (int)m_ui.all_combo,            // @ALL_COMBO
    (int)m_ui.tacada,               // @TACADA
    (int)m_ui.putt,                 // @PUTT
    (int)m_ui.tempo,                // @TEMPO
    (int)m_ui.tempo_tacada,         // @TEMPO_TACADA
    (int)m_ui.acerto_pangya,        // @ACERTO_PANGYA
    (int)m_ui.timeout,              // @TIMEOUT
    m_ui.ob,                   // @OB
    (int)m_ui.total_distancia,      // @TOTAL_DISTANCIA
    (int)m_ui.hole,                 // @HOLE
    m_ui.hole_in,              // @HOLEIN
        m_ui.hio,                  // @HIO
    (short)m_ui.bunker,             // @BUNKER
    m_ui.fairway,              // @FAIRWAY
    m_ui.albatross,            // @ALBATROSS
    m_ui.mad_conduta,          // @MAD_CONDUTA
    m_ui.putt_in,              // @PUTTIN
    m_ui.media_score,          // @MEDIA_SCORE
    (byte)m_ui.best_score[0],       // @BEST_SCORE_0
    (byte)m_ui.best_score[1],       // @BEST_SCORE_1
    (byte)m_ui.best_score[2],       // @BEST_SCORE_2
    (byte)m_ui.best_score[3],       // @BEST_SCORE_3
    (byte)m_ui.best_score[4],       // @BEST_SCORE_4
    (long)m_ui.best_pang[0],        // @BEST_PANG_0
    (long)m_ui.best_pang[1],        // @BEST_PANG_1
    (long)m_ui.best_pang[2],        // @BEST_PANG_2
    (long)m_ui.best_pang[3],        // @BEST_PANG_3
    (long)m_ui.best_pang[4],        // @BEST_PANG_4
    (long)m_ui.sum_pang,            // @SUM_PANG
    (byte)m_ui.event_flag,          // @EVENT_FLAG
    m_ui.jogado,               // @JOGADO
    m_ui.team_game,            // @TEAM_GAME
    m_ui.team_win,             // @TEAM_WIN
        m_ui.team_hole,            // @TEAM_HOLE
    m_ui.ladder_point,         // @LADDER_POINT
    m_ui.ladder_hole,          // @LADDER_HOLE
        m_ui.ladder_win,           // @LADDER_WIN
            m_ui.ladder_lose,          // @LADDER_LOSE
    (int)m_ui.ladder_draw,          // @LADDER_DRAW
    (int)m_ui.quitado,              // @QUITADO
    (long)m_ui.skin_pang,           // @SKIN_PANG
    (int)m_ui.skin_win,             // @SKIN_WIN
    (int)m_ui.skin_lose,            // @SKIN_LOSE
    (int)m_ui.skin_run_hole,        // @SKIN_RUN_HOLE
    (int)m_ui.skin_all_in_count,    // @SKIN_ALL_IN_COUNT
    (int)m_ui.disconnect,           // @NAO_SEI
    (int)m_ui.jogados_disconnect,   // @JOGOS_NAO_SEI
    (short)m_ui.event_value,        // @EVENT_VALUE
    (int)m_ui.skin_strike_point,    // @SKIN_STRIKE_POINT
    m_ui.sys_school_serie,     // @MAX_JOGOS_NAO_SEI
    (int)m_ui.game_count_season,    // @GAME_COUNT_SEASON
    (int)m_ui.total_pang_win_game,  // @TOTAL_PANG_WIN_GAME
    (int)m_ui.medal.lucky,          // @MEDAL_LUCKY
    (int)m_ui.medal.fast,           // @MEDAL_FAST
    (int)m_ui.medal.best_drive,     // @MEDAL_BEST_DRIVE
    (int)m_ui.medal.best_chipin,    // @MEDAL_BEST_CHIPIN
    (int)m_ui.medal.best_puttin,    // @MEDAL_BEST_PUTTIN
    (int)m_ui.medal.best_recovery,  // @MEDAL_BEST_RECOVERY
    (short)m_ui._16bit_nao_sei      // @_16BIT_NAO_SEI
           };

            var r = procedureWithParams(m_szConsulta, parametros, tipos, valores);

            checkResponse(r, "nao conseguiu atualizar o User Info do PLAYER[UID=" + Convert.ToString(m_uid) + "]");

            return r;
        }

        private uint m_uid = new uint();
        private UserInfoEx m_ui = new UserInfoEx();

        private const string m_szConsulta = "pangya.ProcUpdateUserInfo";
    }
}
