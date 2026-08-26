using System.Runtime.InteropServices;
using PangyaAPI.IFF.GB.Models.General;
namespace PangyaAPI.IFF.GB.Models.Data
{
    #region Struct Skin.iff
    [StructLayout(LayoutKind.Sequential, Pack = 4)]
    public class Skin : IFFCommon
    {
        [field: MarshalAs(UnmanagedType.ByValTStr, SizeConst = 40)]
        public string MPet { get; set; }
        public byte horizontal_scroll { get; set; }    // By TH S4 - (HScroll)    
        public byte vertical_scroll { get; set; }      // By TH S4 - (VScroll) 256 efeito de rolagem vertical
        [field: MarshalAs(UnmanagedType.ByValArray, SizeConst = 5)]
        public ushort[] price { get; set; }

    }
    #endregion        
}
