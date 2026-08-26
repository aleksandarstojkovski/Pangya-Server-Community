using PangyaAPI.SQL;

// Arquivo cmd_update_guild_ranking.cpp
// Criado em 29/12/2019 as 15:47 por Acrisio
// Implementa��o da classe CmdUpdateGuildRanking



// Arquivo cmd_update_guild_ranking.hpp
// Criado em 29/12/2019 as 15:43 por Acrisio
// Defini��o da classe CmdUpdateGuildRanking




namespace AuthServer.Cmd
{
	public class CmdUpdateGuildRanking : Pangya_DB
	{
			public CmdUpdateGuildRanking(bool _waiter = false) : base(_waiter)
			{
			}

			public virtual void Dispose()
			{
			}

			protected override void lineResult(ctx_res _result, uint _index_result)
			{

				// N�o usa por que � um UPDATE
				return;
			}

			protected override Response prepareConsulta()
			{

				var r = procedure(
					m_szConsulta, "");

				checkResponse(r, "Nao conseguiu atualizar Guild Ranking.");

				return r;
			} 

			private string m_szConsulta = "pangya.USP_UPDATE_GUILD_RANKING";
	}
}
