using System;
using System.Data;
using Pangya_GameServer.Models;
using PangyaAPI.SQL;

namespace Pangya_GameServer.Repository
{
    public class CmdInsertOrUpdateRoomLog : Pangya_DB
    {
        RoomInfoLog m_log;
        TYPE m_type;
        int m_state;
        public CmdInsertOrUpdateRoomLog(RoomInfoLog _log, TYPE _type = TYPE.INSERT, bool _waiter = false) : base(_waiter)
        {
            m_log = _log;
            m_type = _type;
        }

        protected override void lineResult(ctx_res _result, uint _index_result)
        {
            checkColumnNumber(1);

            switch (m_type)
            {
                case TYPE.INSERT:
                    // Verifica se a coluna possui dados válidos
                    if (is_valid_c_string(_result.data[0]))
                    {
                        var guid_cstr = (_result.GetString(0)).ToUpper();

                        guid_cstr.Replace("{", "");
                        guid_cstr.Replace("}", "");
                    }
                    break;
                case TYPE.UPDATE:
                    {
                        m_state = (int)IFNULL(_result.data[0]);
                    }
                    break;
                default:
                    break;
            }
        }

        public RoomInfoLog getRoom()
        {
            return m_log;
        }

        public void setRoomLog(RoomInfoLog _log)
        {
            m_log = _log;
        }

        public TYPE getType()
        {
            return m_type;
        }

        public void setType(TYPE _type)
        {
            m_type = _type;
        }

        public int getState()
        {
            return m_state;
        }

        protected override Response prepareConsulta()
        {
            var query = m_szConsulta[(int)m_type];
            //para adicionar salas com string em japones!
            var r = procedureWithParams(
                query,
                new string[] {
            "@NAME", "@PLAYERS", "@MAX_PLAYERS", "@TIPO_EX", "@UID",
            "@ROOMID", "@CHARACTER", "@CADDIE", "@MASCOT", "@CLUB",
            "@TIPO", "@MODO", "@QNTD_HOLE", "@COURSE", "@HOLE",
            "@SCORE", "@EXP", "@PANG", "@BONUS_PANG", "@TACADA_NUM",
            "@TOTAL_TACADA_NUM", "@GIVEUP", "@TIMEOUT", "@ENTER_AFTER_STARTED", "@FINISH_GAME",
            "@ASSIST_FLAG", "@TROFEU", "@Master", "@Is_ShotGame", "@Is_Natural",
            "@hit_hio", "@hit_alba", "@hit_eagle", "@hit_birdie", "@hit_par",
            "@hit_bogey", "@hit_double_bogey", "@hit_triple_bogey"
                },
                new SqlDbType[] {
            SqlDbType.NVarChar,   // @NAME
            SqlDbType.Int,        // @PLAYERS
            SqlDbType.Int,        // @MAX_PLAYERS
            SqlDbType.Int,        // @TIPO_EX
            SqlDbType.Int,        // @UID
            SqlDbType.UniqueIdentifier, // @ROOMID
            SqlDbType.Int,        // @CHARACTER
            SqlDbType.Int,        // @CADDIE
            SqlDbType.Int,        // @MASCOT
            SqlDbType.Int,        // @CLUB
            SqlDbType.Int,        // @TIPO
            SqlDbType.Int,        // @MODO
            SqlDbType.Int,        // @QNTD_HOLE
            SqlDbType.Int,        // @COURSE
            SqlDbType.Int,        // @HOLE
            SqlDbType.Decimal,    // @SCORE
            SqlDbType.Decimal,    // @EXP
            SqlDbType.BigInt,     // @PANG
            SqlDbType.BigInt,     // @BONUS_PANG
            SqlDbType.Decimal,    // @TACADA_NUM
            SqlDbType.Decimal,    // @TOTAL_TACADA_NUM
            SqlDbType.Decimal,    // @GIVEUP
            SqlDbType.Decimal,    // @TIMEOUT
            SqlDbType.Decimal,    // @ENTER_AFTER_STARTED
            SqlDbType.Decimal,    // @FINISH_GAME
            SqlDbType.Decimal,    // @ASSIST_FLAG
            SqlDbType.Decimal,    // @TROFEU
            SqlDbType.Decimal,    // @Master
            SqlDbType.Decimal,    // @Is_ShotGame
            SqlDbType.Decimal,    // @Is_Natural
            SqlDbType.Int,        // @hit_hio
            SqlDbType.Int,        // @hit_alba
            SqlDbType.Int,        // @hit_eagle
            SqlDbType.Int,        // @hit_birdie
            SqlDbType.Int,        // @hit_par
            SqlDbType.Int,        // @hit_bogey
            SqlDbType.Int,        // @hit_double_bogey
            SqlDbType.Int         // @hit_triple_bogey
                },
                new object[] {
            m_log.nome,
            m_log.num_player.ToString(),
            m_log.max_player.ToString(),
            m_log.tipo_ex.ToString(),
            m_log.uid.ToString(),
            m_log.roomId,// deu erro -> Conversão inválida de 'System.String' em 'System.Guid'.
            m_log.character.ToString(),
            m_log.caddie.ToString(),
            m_log.mascot.ToString(),
            m_log.club.ToString(),
            m_log.tipo.ToString(),
            m_log.modo.ToString(),
            m_log.qntd_hole.ToString(),
            Convert.ToInt32(m_log.course).ToString(),
            (m_log.hole == 0? 1: m_log.hole).ToString(),//o primeiro hole é zero né
            m_log.score.ToString(),
            m_log.exp.ToString(),
            m_log.pang.ToString(),
            m_log.bonus_pang.ToString(),
            m_log.tacada_num.ToString(),
            m_log.total_tacada_num.ToString(),
            m_log.giveup.ToString(),
            m_log.timeout.ToString(),
            m_log.enter_after_started.ToString(),
            m_log.finish_game.ToString(),
            m_log.assist_flag.ToString(),
            m_log.Win_trofeu.ToString(),
            m_log.master.ToString(),
            m_log.Is_short_game.ToString(),
            m_log.Is_natural.ToString(),
            m_log.HitHio.ToString(),
            m_log.HitAlba.ToString(),
            m_log.HitEagle.ToString(),
            m_log.HitBirdie.ToString(),
            m_log.HitPar.ToString(),
            m_log.HitBogey.ToString(),
            m_log.Hit_x2_Bogey.ToString(),
            m_log.Hit_x3_Bogey.ToString()
                },
     ParameterDirection.Input // <- só Input
            );

            checkResponse(r, $"Não foi possível fazer {m_type} log game!");

            return r;
        }


        public enum TYPE
        {
            INSERT,
            UPDATE,
        }

        string[] m_szConsulta = { "pangya.ProcInsertRoomLog", "pangya.ProcUpdateRoomLog" };
    }
}
