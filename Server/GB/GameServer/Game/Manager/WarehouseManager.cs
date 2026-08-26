using Pangya_GameServer.Models;
using Pangya_GameServer.PacketFunc;
using PangyaAPI.IFF.GB.Extensions;
using PangyaAPI.Network.PangyaSession;
using PangyaAPI.Utilities;
using PangyaAPI.Utilities.BinaryModels;
using System.Collections.Generic;
using System.Linq;
namespace Pangya_GameServer.Game.Manager
{
    public class WarehouseManager : Dictionary<int/*ID*/, WarehouseItemEx>
    {
        public WarehouseManager()
        {
        }

        protected PangyaBinaryWriter Build(List<WarehouseItemEx> list)
        {
            try
            {
                return packet_func.pacote073(list);
            }
            catch
            {
                return packet_func.pacote073(new List<WarehouseItemEx>());
            }
        }

        public List<PangyaBinaryWriter> Build(uint itensPerPacket = 50)//maximo é 50 agora
        {
            var responses = new List<PangyaBinaryWriter>();
            if (Count * 196 < (1000 - 100))//envio normal
            {
                responses.Add(Build(Values.ToList()));
            }
            else
            {
                var splitList = Values.ToList().Split((int)itensPerPacket);

                //Percorre lista e adiciona ao resultado
                splitList.ForEach(lista => responses.Add(Build(lista)));
            }
            return responses;
        }

        public WarehouseItemEx findWarehouseItemById(int _id)
        {
            return this.Values.FirstOrDefault(c => c.id == _id);
        }

        public WarehouseItemEx findWarehouseItemByTypeid(uint _typeid)
        {
            if (sIff.getInstance().getItemGroupIdentify((_typeid)) == sIff.getInstance().ITEM && sIff.getInstance().getItemSubGroupIdentify24((_typeid)) > 1/*Passive Item*/)
            {
               return Values.Where(c => c._typeid == _typeid)
                                            .OrderByDescending(c => c.STDA_C_ITEM_QNTD)
                                            .FirstOrDefault();//pega sempre o que tem mais quantidade
            }
            else//
                return this.Values.FirstOrDefault(c => c._typeid == _typeid);
        }
         

        public WarehouseItemEx findWarehouseItemByTypeidAndId(uint _typeid, int _id)
        {
            if (sIff.getInstance().getItemGroupIdentify((_typeid)) == sIff.getInstance().ITEM && sIff.getInstance().getItemSubGroupIdentify24((_typeid)) > 1/*Passive Item*/)
            {
                return Values.Where(c => c.id == _id && c._typeid == _typeid)
                                             .OrderByDescending(c => c.STDA_C_ITEM_QNTD)
                                             .FirstOrDefault();//pega sempre o que tem mais quantidade
            }
            else//
                return this.Values.FirstOrDefault(c => c.id == _id && c._typeid == _typeid);
        }
    }
}
