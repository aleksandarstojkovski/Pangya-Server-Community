using PangyaAPI.Network.Repository;
using snmdb; 
namespace Pangya_GameServer.Repository
{
    public class CommandDB : PangyaCommandDB
    {
        public static bool VerifyAchievement()
        {
            var cmdVerify = new CmdVerifyAchievementInfo();

            NormalManagerDB.getInstance().add(0, cmdVerify, null, null);

            if (cmdVerify.getException().getCodeError() != 0)
                throw cmdVerify.getException();

            return cmdVerify.HasData; 
        }
    }
}
