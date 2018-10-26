package ICTCLAS.I3S.AC;

import gate.Gate;

public class ICTCLAS50 {
    public native boolean ICTCLAS_Init(byte[] sPath);

    public native boolean ICTCLAS_Exit();

    public native int ICTCLAS_ImportUserDictFile(byte[] sPath, int eCodeType);

    public native int ICTCLAS_SaveTheUsrDic();

    public native int ICTCLAS_SetPOSmap(int nPOSmap);

    public native boolean ICTCLAS_FileProcess(byte[] sSrcFilename, int eCodeType, int bPOSTagged, byte[] sDestFilename);

    public native byte[] ICTCLAS_ParagraphProcess(byte[] sSrc, int eCodeType, int bPOSTagged);

    public native byte[] nativeProcAPara(byte[] sSrc, int eCodeType, int bPOStagged);

    /* Use static intializer */
    static {
        //get user dir and load ictclas dll from plugins
        String path = System.getProperty("user.dir");
        String dllpath = "";
        // For Unix
        if (Gate.runningOnUnix() == true) {
            dllpath = path + "/plugins/ICTCLASChineseTokenizer/lib/libICTCLAS50.so";
        }
        // For Windows
        else {
            dllpath = path + "\\plugins\\ICTCLASChineseTokenizer\\lib\\ICTCLAS50.dll";
        }

        System.load(dllpath);
    }
}

