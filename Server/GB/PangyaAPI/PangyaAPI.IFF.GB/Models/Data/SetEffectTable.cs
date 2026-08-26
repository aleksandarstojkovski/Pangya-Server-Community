using System;
using System.Runtime.InteropServices;
using PangyaAPI.Utilities.BinaryModels;
namespace PangyaAPI.IFF.GB.Models.Data
{
    #region Struct SetEffectTable.iff
    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    public class SetEffectTable : ICloneable
    { 

        public uint Index { get; set; }
        [field: MarshalAs(UnmanagedType.Struct)]
        public Effect effect { get; set; }
        [field: MarshalAs(UnmanagedType.Struct)]
        public Item item { get; set; } 
        public byte Slot { get; set; }
        public short Effect_Add_Power { get; set; }   // Força sem penalidade

        public object Clone()
        {
            return MemberwiseClone();
        }

        [StructLayout(LayoutKind.Sequential, Pack = 1)]
        public class Effect
        {
            [field: MarshalAs(UnmanagedType.ByValArray, SizeConst = 3)]
            public uint[] effect { get; set; } // eEFFECT = Effect[0~2] é o da descrição em cima
            [field: MarshalAs(UnmanagedType.ByValArray, SizeConst = 3)]
            public uint[] Type { get; set; }// eEFFECT_TYPE = Type[0~2], 2 Game, 4 Room e 8 Lounge
        }
        [StructLayout(LayoutKind.Sequential, Pack = 1)]
        public class Item
        {
            [field: MarshalAs(UnmanagedType.ByValArray, SizeConst = 5)]
            public uint[] ID { get; set; }
            [field: MarshalAs(UnmanagedType.ByValArray, SizeConst = 5)]
            public byte[] Active { get; set; }

        }


        public uint getID(int idx)
        {
            return item.ID[idx];
        }
        public bool IsActive(int idx)
        {
            return item.Active[idx] > 0;
        }
    }
    #endregion
}
