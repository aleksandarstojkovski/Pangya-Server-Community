using PangyaAPI.SQL;
using System;

namespace Pangya_GameServer.Repository
{
    public class CmdUpdateWorldTourConfigEvent : Pangya_DB
    {
        private readonly int m_id;

        public CmdUpdateWorldTourConfigEvent(int _id) : base(false)
        {
            m_id = _id;
        }

        protected override void lineResult(ctx_res _result, uint _index_result)
        {
            // não precisa processar nada, é só UPDATE
        }

        protected override Response prepareConsulta()
        {
            if (m_id <= 0)
                throw new Exception("Id inválido para atualizar configuração do World Tour Event.");

            // Força o evento para inativo
            var query = $@"
                UPDATE pangya.pangya_world_tour_config
                SET IsActive = 0
                WHERE EventId = {m_id};
            ";

            var r = consulta(query);
            checkResponse(r, $"Não conseguiu desativar o evento World Tour (Id={m_id}).");
            return r;
        }
    }
}
