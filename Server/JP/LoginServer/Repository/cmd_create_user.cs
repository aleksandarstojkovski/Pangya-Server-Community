using PangyaAPI.SQL;
using PangyaAPI.Utilities;
using System;

namespace Pangya_LoginServer.Repository
{
    public class CmdCreateUser : Pangya_DB
    {
        public CmdCreateUser(string _id,
            string _pass, string _ip,
            uint _server_uid)
        {
            this.m_id = _id;
            this.m_pass = _pass;
            this.m_ip = _ip;
            this.m_server_uid = _server_uid;
            this.m_uid = 0;
        }


        public string getID()
        {
            return m_id;
        }

        public void setID(string _id)
        {
            m_id = _id;
        }

        public string getPASS()
        {
            return m_pass;
        }

        public void setPass(string _pass)
        {
            m_pass = _pass;
        }

        public string getIP()
        {
            return m_ip;
        }

        public void setIP(string _ip)
        {
            m_ip = _ip;
        }

        public uint getServerUID()
        {
            return (m_server_uid);
        }

        public void setServerUID(uint _server_uid)
        {
            m_server_uid = _server_uid;
        }

        public uint getUID()
        {
            return (m_uid);
        }

        protected override void lineResult(ctx_res _result, uint _index_result)
        {

            checkColumnNumber(1, _result.cols);

            m_uid = IFNULL(_result.data[0]);
        }

        protected override Response prepareConsulta()
        { 
            if (string.IsNullOrEmpty(m_id) || string.IsNullOrEmpty(m_pass) || string.IsNullOrEmpty(m_ip))
            {
                throw new exception("[CmdCreateUser::prepareConsulta][Error] argumentos invalidos.[ID=" + m_id + ",PASSWORD=" + m_pass + ",IP=" + m_ip + "]", ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.PANGYA_DB,
                    4, 0));
            }
            var r = new Response();

            r = procedure(m_szConsulta, makeText("") + ", " + makeText(DateTime.Now.ToShortDateString()) + ", " + makeText("0") + ", " + makeText("") + ", " + makeText("") + ", " + makeText(m_id) + ", " + makeText(m_pass.Length < 32 ? Tools.MD5Hash(m_pass) : m_pass) + ", " + makeText(m_ip));


            return r;
        }


        private string m_id = "";
        private string m_pass = "";
        private string m_ip = "";
        private uint m_server_uid = 0;
        private uint m_uid = 0;
        private const string m_szConsulta = "pangya.ProcNewUser";
    }
}
