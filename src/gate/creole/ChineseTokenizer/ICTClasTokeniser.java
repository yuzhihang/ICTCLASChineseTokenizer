/**
 *    * ICTCLAS tokeniser for GATE
 *    * @author whuwy
 *    * @help http://gatechinese.com
 *  
 */

package gate.creole.ChineseTokenizer;

import gate.creole.ExecutionException;
import gate.creole.ExecutionInterruptedException;
import gate.creole.ResourceInstantiationException;
import ICTCLAS.I3S.AC.ICTCLAS50;
import gate.Gate;

import java.io.*;
import java.net.URL;


public class ICTClasTokeniser {
    private ICTCLAS50 ICTCLAS50Tokeniser = null;

    /**
     * @param encoding the encoding of tokeniser
     * @param usrdict  the URL of user dictionary
     * @throws ResourceInstantiationException
     * @throws ExecutionInterruptedException
     */
    public Boolean init(String encoding, URL usrdict, int nusrDictPosType) throws ResourceInstantiationException, ExecutionInterruptedException {
        ICTCLAS50Tokeniser = new ICTCLAS50();
        // Set Resource Path
        String argu = System.getProperty("user.dir");
        if (Gate.runningOnUnix() == true) {
            argu = argu + "/plugins/ICTCLASChineseTokenizer/lib";
        }
        // For Windows
        else {
            argu = argu + "\\plugins\\ICTCLASChineseTokenizer\\lib";
        }
        //Try Init
        try {
            if (ICTCLAS50Tokeniser.ICTCLAS_Init(argu.getBytes(encoding)) == false) {
                System.out.println("ICTCLAS Init Fail!!!!");
                return false;
            }

            if (usrdict != null) {
                String dictpath = usrdict.getPath();
                String usrdirstring = dictpath.substring(1, dictpath.length());
                // convert string to bytes
                byte[] usrdirb = usrdirstring.getBytes();
                //import user dictionary
                ICTCLAS50Tokeniser.ICTCLAS_ImportUserDictFile(usrdirb, nusrDictPosType);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void cleanup() {
        ICTCLAS50Tokeniser.ICTCLAS_Exit();
    }

    /**
     * output token and POS pair, seperated by " "
     * for example:
     * input : 中国是世界上人口最多的国家。
     * output: 中国/ns 是/v 世界/n 上/f 人口/n 最/d 多/a 的/u 国家/n 。/w
     *
     * @param input    String input
     * @param Encoding input's encoding
     * @param PosType  POS Type
     * @throws ExecutionException
     */
    public String Process(String input, String Encoding, int PosType) throws ExecutionException {
        try {
            // FIXME:北大二级标注集合有BUG,需要使用GB2312编码才能解析
            if (PosType == 2) {
                Encoding = "GB2312";
            }
            byte[] tokens = ICTCLAS50Tokeniser.ICTCLAS_ParagraphProcess(input.getBytes(Encoding), PosType, 1);
            String tokenstring = new String(tokens, 0, tokens.length, Encoding);
            return tokenstring;
        } catch (Exception ex) {
            return null;
        }
    }
}


