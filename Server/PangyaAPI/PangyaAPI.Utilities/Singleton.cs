using System;
namespace PangyaAPI.Utilities
{
    public class Singleton<_ST> where _ST : class, new()
    {
        private static _ST myInstance = default;

        public static _ST getInstance()
        {
            try
            {
                if (myInstance == null)
                    myInstance = new _ST();

                return myInstance;
            }
            catch (Exception e)
            {

                throw e;
            }
        }

        protected Singleton()
        {
        }
    }
}
