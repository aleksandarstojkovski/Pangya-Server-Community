using System;
using PangyaAPI.Network.Models;
using PangyaAPI.SQL;
using PangyaAPI.Utilities;

namespace Pangya_GameServer.Repository
{
    public class CmdUpdateChatMacroUser : Pangya_DB
    {
        public CmdUpdateChatMacroUser(uint _uid,
                chat_macro_user _cmu)
        {
            this.m_uid = _uid;
            this.m_cmu = _cmu;
        }


        public uint getUID()
        {
            return m_uid;
        }

        public void setUID(uint _uid)
        {
            m_uid = _uid;
        }

        public chat_macro_user getInfo()
        {
            return m_cmu;
        }

        public void setInfo(chat_macro_user _cmu)
        {
            m_cmu = _cmu;
        }

        protected override void lineResult(ctx_res _result, uint _index_result)
        {

            // N�o usa por que � um UPDATE
            return;
        }

        protected override Response prepareConsulta()
        {

            if (m_uid == 0)
            {
                throw new exception("[CmdUpdateChatMacroUser::prepareConsulta][Error] m_uid is invalid(zero)", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.PANGYA_DB,
                    4, 0));
            }
 
var m0 = (m_cmu.macro[0]);
	var m1 = (m_cmu.macro[1]);
	var m2 = (m_cmu.macro[2]);
	var m3 = (m_cmu.macro[3]);
	var m4 = (m_cmu.macro[4]);
	var m5 = (m_cmu.macro[5]);
	var m6 = (m_cmu.macro[6]);
	var m7 = (m_cmu.macro[7]);
	var m8 = (m_cmu.macro[8]);

	var r = procedure(m_szConsulta, Convert.ToString(m_uid) + ", " + makeText(m0) + ", " + makeText(m1) + ", " + makeText(m2) + ", " 
				+ makeText(m3) + ", " + makeText(m4) + ", " + makeText(m5) + ", " + makeText(m6) + ", " + makeText(m7) + ", " + makeText(m8)
	);

            checkResponse(r, "nao conseguiu atualizar Chat Macro[M1=" + m0 + ", M2=" + m1 + ", M3=" + m2 + ", M4=" + m3 + ", M5=" + m4 + ", M6=" + m5 + ", M7=" + m6 + ", M8=" + m7 + ", M9=" + m8 + "] do PLAYER[UID=" + Convert.ToString(m_uid) + "]");

            return r;
        }
        private uint m_uid = new uint();
        private chat_macro_user m_cmu = new chat_macro_user();

        private const string m_szConsulta = "pangya.ProcUpdateChatMacroUser";
    }
}