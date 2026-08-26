using System.Runtime.InteropServices;
using PangyaAPI.IFF.GB.Models.General;
namespace PangyaAPI.IFF.GB.Models.Data
{
    #region Struct HairStyle.iff
    [StructLayout(LayoutKind.Sequential, Pack = 4)]
    public class HairStyle : IFFCommon
    {
        public byte Color { get; set; }
        public byte Character { get; set; }
        public ushort Blank { get; set; }
    }
    #endregion
}
