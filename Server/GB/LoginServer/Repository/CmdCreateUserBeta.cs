using System;
using PangyaAPI.SQL;
using PangyaAPI.Utilities;

namespace LoginServer.Repository
{
    public class CmdCreateUserBeta : Pangya_DB
    {
        private string m_id = string.Empty;
        private string m_pass = string.Empty;
        private string m_ip = string.Empty;
        private uint m_server_uid;
        private uint m_uid;

        public CmdCreateUserBeta(uint uid, string _id, string _pass, string _ip, uint _server_uid)
        {
            m_id = _id;
            m_pass = _pass;
            m_ip = _ip;
            m_server_uid = _server_uid;
            m_uid = uid;
        }

        public string ID
        {
            get => m_id;
            set => m_id = value;
        }

        public string PASS
        {
            get => m_pass;
            set => m_pass = value;
        }

        public string IP
        {
            get => m_ip;
            set => m_ip = value;
        }

        public uint ServerUID
        {
            get => m_server_uid;
            set => m_server_uid = value;
        }

        public uint UID
        {
            get => m_uid;
            set => m_uid = value;
        }

        protected override void lineResult(ctx_res _result, uint _index_result)
        {
            // Apenas INSERT, não há retorno de linhas
        }

        protected override Response prepareConsulta()
        {
            if (string.IsNullOrEmpty(m_id) || string.IsNullOrEmpty(m_pass) || string.IsNullOrEmpty(m_ip))
            {
                throw new exception(
                    $"[CmdCreateUserBeta::prepareConsulta][Error] Argumentos inválidos. [ID={m_id}, PASSWORD={m_pass}, IP={m_ip}]",
                    ExceptionError.STDA_MAKE_ERROR_TYPE(STDA_ERROR_TYPE.PANGYA_DB, 4, 0)
                );
            }

            // Atribui à propriedade m_szConsulta da classe base em vez de declarar uma variável local
           var m_szConsulta = $@"
                INSERT INTO [pangya].[contas_beta] (
                    [UID], 
                    [NomeCompleto],
                    [Birthday],
                    [Email],
                    [Sexo],
                    [Pergunta],
                    [Resposta],
                    [LoginID],
                    [Senha],
                    [ip_register],
                    [referrer_code],
                    [Inviter_UID],
                    [Invited],
                    [status_referal]
                )
                VALUES (
                    {m_uid}, 
                    {makeText("test name full")}, 
                    {makeText(DateTime.Now.ToShortDateString())}, 
                    {makeText("test@live.com")}, 
                    0, 
                    {makeText("test")}, 
                    {makeText("test")}, 
                    {makeText(m_id)}, 
                    {makeText(m_pass)}, 
                    {makeText(m_ip)},
                    {makeText("test_referrer")},
                    0,
                    0,
                    '0'
                );";

            return consulta(m_szConsulta);
        }
    }
}