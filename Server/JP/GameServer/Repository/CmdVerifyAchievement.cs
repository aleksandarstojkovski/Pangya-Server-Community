using PangyaAPI.SQL;
using System;

namespace Pangya_GameServer.Repository
{
    public class CmdVerifyAchievementInfo : Pangya_DB
    {
        /// <summary>
        /// Indica se a tabela contém pelo menos um registro.
        /// </summary>
        public bool HasData { get; private set; }

        public CmdVerifyAchievementInfo()
        {
            HasData = false;
        }

        protected override Response prepareConsulta()
        {
            // Consulta eficiente para verificar a existência de dados sem carregar a tabela inteira
           var m_szConsulta = "IF EXISTS (SELECT TOP 1 1 FROM pangya.achievements) SELECT 1 AS HasData ELSE SELECT 0 AS HasData;";

            var response = consulta(m_szConsulta);

            checkResponse(response, "Failed to verify achievement table data");

            return response;
        }

        protected override void lineResult(ctx_res _result, uint _index_result)
        {
            // Lê o retorno da consulta (1 para com dados, 0 para vazia)
            if (_result != null)
            {
                HasData = Convert.ToBoolean(_result.GetInt32(0));
            }
        } 
    }
}