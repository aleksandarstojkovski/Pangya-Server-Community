using Pangya_GameServer.Models;
using PangyaAPI.SQL;
using System;

namespace Pangya_GameServer.Repository
{
    public class CmdWorldTourConfigEvent : Pangya_DB
    {
        private world_tour_config m_config = new world_tour_config();

        protected override void lineResult(ctx_res _result, uint _index_result)
        {
            try
            {
                // Supondo que sua tabela tem 4 colunas: ID, StartDate, EndDate, Active
                checkColumnNumber(4, _result.cols);

                m_config.Id = _result.GetInt32(0);
                m_config.StartDate = _result.GetDateTime(1);
                m_config.EndDate = _result.GetDateTime(2);
                m_config.Active = _result.GetBoolean(3);
            }
            catch (Exception ex)
            {
                Console.WriteLine("[CmdEventWorldTourConfig::lineResult][Error] " + ex.Message);
            }
        }

        protected override Response prepareConsulta()
        {
            // Cria a consulta. Pode ser SELECT direto ou procedure.
            var r = consulta("SELECT TOP 1 [EventID] as Id, StartDate, EndDate, [IsActive] as Active FROM pangya.pangya_world_tour_config WHERE IsActive = 1" +
                " AND GETDATE() BETWEEN StartDate AND EndDate" +
                "\r\nORDER BY StartDate DESC");
            checkResponse(r, "Não conseguiu pegar a configuração do World Tour Event.");
            return r;
        }

        public world_tour_config GetConfig()
        {
            return m_config;
        }
    } 
}
