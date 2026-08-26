using PangyaAPI.SQL;
using System;

namespace Pangya_GameServer.Repository
{
    public class CmdUpdateWorldTourEvent : Pangya_DB
    {
        private readonly int _uid;
        private readonly int _course;
        private readonly bool _completed;
        private readonly DateTime _finishDate;

        private bool _isEnd;

        public CmdUpdateWorldTourEvent(int uid, int course, bool completed, DateTime? finishDate = null) : base(false)
        {
            _uid = uid;
            _course = course;
            _completed = completed;
            _finishDate = finishDate ?? DateTime.Now;
            _isEnd = false;
        }

        public int getUID() => _uid;
        public bool getIsEnd() => _isEnd;

        protected override void lineResult(ctx_res _result, uint _index_result)
        {
            try
            {
                _isEnd = _result.GetInt32(1) == 1;
            }
            catch (Exception ex)
            {
                Console.WriteLine("[CmdUpdateWorldTourEvent::lineResult] " + ex.Message);
            }
        }

        protected override Response prepareConsulta()
        {
            if (_uid <= 0 || _course <= 0)
                throw new Exception("UID ou Course inválido para atualizar World Tour Event.");

            var r = procedure("pangya.UpdateWorldTourEvent", _uid.ToString() + "," + _course.ToString() + "," +
                (_completed ? "1" : "0") + "," +
                _finishDate.ToString("yyyy-MM-dd HH:mm:ss"));

            checkResponse(r, $"Não conseguiu atualizar o progresso do World Tour Event para UID={_uid}, Course={_course}.");
            return r;
        }
    }
}
