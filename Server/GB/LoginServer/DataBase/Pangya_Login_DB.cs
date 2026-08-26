using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using LoginServer.Repository;
using LoginServer.Models;
namespace LoginServer.DataBase
{
    public class Pangya_Login_DB
    {
        // ============================================================
        //  🔹 CREATE USER
        // ============================================================
        public static uint CreateUser(string id, string pass, string ip, uint serverUid)
        {
            var cmd = new CmdCreateUser(id, pass, ip, serverUid);
            
            snmdb.NormalManagerDB.getInstance().add(_id: 0, cmd, null, null);

            return cmd.getUID();    // Command já executa no construtor
        }

        // ============================================================
        //  🔹 FIRST LOGIN CHECK
        // ============================================================
        public static bool IsFirstLogin(uint uid)
        {
            var cmd = new CmdFirstLoginCheck(uid);
            return cmd.getLastCheck();
        }

        public static void AddFirstLogin(uint uid, byte flag)
        {
            var cmd = new CmdAddFirstLogin(uid, flag);
            cmd.getFLag();
        }

        // ============================================================
        //  🔹 FIRST SET CHECK
        // ============================================================
        public static bool IsFirstSet(uint uid)
        {
            var cmd = new CmdFirstSetCheck(uid);
            return cmd.getLastCheck();
        }

        public static void AddFirstSet(uint uid)
        {
            var cmd = new CmdAddFirstSet(uid);
            cmd.getUID();
        }

        // ============================================================
        //  🔹 PLAYER INFO
        // ============================================================
        public static player_info GetPlayerInfo(uint uid)
        {
            var cmd = new CmdPlayerInfo(uid);
            return cmd.getInfo();
        }

        public static void UpdatePlayerInfo(uint uid, PlayerInfo info)
        {
            var cmd = new CmdPlayerInfo(uid);
            cmd.updateInfo(info);
        }

        // ============================================================
        //  🔹 REGISTER / LOGIN SERVER
        // ============================================================
        public static void RegisterLogonServer(uint uid, uint serverUid)
        {
            var cmd = new CmdRegisterLogonServer(uid, serverUid);
            cmd.getServerUID();
        }

        public static void RegisterPlayerLogin(uint _uid, string _ip, uint _server_uid)
        {
            var cmd = new CmdRegisterPlayerLogin(_uid, _ip, _server_uid);
            cmd.getUID();
        }

        // ============================================================
        //  🔹 VERIFY IP
        // ============================================================
        public static bool VerifyIP(uint uid, string ip)
        {
            var cmd = new CmdVerifyIP(uid, ip);
            return cmd.getIP() == ip;
        }
    }
}
