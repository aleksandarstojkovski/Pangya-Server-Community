using System;
namespace PangyaAPI.Utilities
{
    public class Singleton<_ST> where _ST : class, new()
    {
        private static _ST myInstance;

        public static _ST getInstance()
        {
            try
            {
                if (myInstance == null)
                    myInstance = new _ST();

                return myInstance;
            }
            catch (exception e)
            { 
                throw e;
            }
        }

        protected Singleton()
        {
        }
    }
}
