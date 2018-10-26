/**
    * Chinese ChineseTokenizer for GATE, using ICTClas.
    * @author  whuwy
    * @help http://gatechinese.com
    */

package gate.creole.ChineseTokenizer;

import java.net.URL;

import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ExecutionInterruptedException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.event.ProgressListener;
import gate.event.StatusListener;
import gate.util.Err;
import gate.util.InvalidOffsetException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.*;

/**
 *  * A chinese ChineseTokenizer using ICTClas Chinese ChineseTokenizer
 *  * output.
 *  
 */


@CreoleResource(name = "ICTCLAS Chinese Tokenizer", icon = "shefTokeniser")
public class ICTCLASChineseTokenizer extends AbstractLanguageAnalyser {

    public static final String
            DEF_TOK_DOCUMENT_PARAMETER_NAME = "document";
    public static final String
            DEF_TOK_ANNOT_SET_PARAMETER_NAME = "annotationSetName";
    public static final String
            DEF_TOK_TOKRULES_URL_PARAMETER_NAME = "tokenizerRulesURL";
    public static final String
            DEF_TOK_ENCODING_PARAMETER_NAME = "encoding";

    public ICTCLASChineseTokenizer() {
    }

    /**
     * Initialise this resource, and return it.
     */
    public Resource init() throws ResourceInstantiationException {
        try {
            //init super object
            super.init();
            // check the input parameters
            checkParameters();
            //create all the componets
            if (tokenizer == null) {
                tokenizer = new ICTClasTokeniser();
            }
            if (!tokenizer.init(encoding, userDict, nusrDictPosType))
                throw new ResourceInstantiationException("Could not create ICTCLAS,please check ICTCLAS config file");
            fireProgressChanged(100);
            fireProcessFinished();

        } catch (ResourceInstantiationException rie) {
            throw rie;
        } catch (Exception e) {
            throw new ResourceInstantiationException(e);
        }
        return this;
    }

    public void cleanup() {
        tokenizer.cleanup();
    }

    /**
     *    * Prepares this Processing resource for a new run.
     *  
     */
    public void reset() {
        document = null;
    }

    /**
     *    * Notifies all the PRs in this controller that they should stop their
     *    * execution as soon as possible.
     *  
     */
    public synchronized void interrupt() {
        interrupted = true;
    }

    public void checkParameters() throws ExecutionInterruptedException {

        try {
            nposType = Integer.parseInt(posType);
            nusrDictPosType = Integer.parseInt(usrDictPosType);
        } catch (Exception e) {
            throw new ExecutionInterruptedException(
                    "The type of the posType must be integer!");
        }
        if (nposType > 3 || nposType < 0 || nusrDictPosType > 3 || nusrDictPosType < 0) {
            throw new ExecutionInterruptedException(
                    "The value of the posType must between 0 and 3!");
        }
    }

    /**
     * the ICTCLAS ChineseTokenizer
     */
    protected ICTClasTokeniser tokenizer;

    private String encoding;
    private String annotationSetName;

    protected String posType;
    protected String usrDictPosType;
    protected int nposType;
    protected int nusrDictPosType;

    protected URL userDict;

    private int paragramflagLength = 1;


    /**
     * take ICTClas to ChineseTokenizer, add POS to TOKEN Annotation
     */
    public void execute() throws ExecutionException {
        interrupted = false;
        try {
            fireProgressChanged(0);
            //ChineseTokenizer
            //check the input
            if (document == null) {
                throw new ExecutionException(
                        "No document to tokenise!"
                );
            }
            fireStatusChanged(
                    "Tokenising " + document.getName() + "...");

            String content = document.getContent().toString();
            String[] paragrams = getParagrams(content);
            int paragramcount = paragrams.length;
            int paragramlengh = 0;
            for (int index = 0; index < paragramcount; index++) {
                String paragram = paragrams[index];

                String tokenswithpos = tokenizer.Process(paragram, encoding, nposType);
                SetTokenFeatures(paragramlengh, tokenswithpos);
                SetSplitFeatures(paragramlengh, paragram);

                paragramlengh += paragram.length() + paragramflagLength;
            }

        } catch (Exception rie) {
            throw new ExecutionException(rie);
        }

        fireProgressChanged(5);
        ProgressListener pListener = new IntervalProgressListener(5, 50);
        StatusListener sListener = new StatusListener() {
            public void statusChanged(String text) {
                fireStatusChanged(text);
            }
        };

        //ChineseTokenizer
        if (isInterrupted()) throw new ExecutionInterruptedException(
                "The execution of the \"" + getName() +
                        "\" ChineseTokenizer has been abruptly interrupted!");

    }

    /**
     * Parses return token from content.
     *   *
     *   * @param content the source of document
     *  
     */
    private String[] getParagrams(String content) {
        String sentenceSplit = "";
        // if the paragram flag is \r\n
        if (content.contains("\r\n")) {
            sentenceSplit = "\r\n";
            paragramflagLength = 2;
        } else {
            sentenceSplit = "\n";
            paragramflagLength = 1;
        }
        Pattern externalSplitsPattern = Pattern.compile(sentenceSplit);
        String[] paragrams = externalSplitsPattern.split(content);
        return paragrams;

    }

    /**
     * Parses token and pos, add features to TOKEN annotation.
     *    *
     *    * @param paragramstart the start index of a paragram
     *    * @param tokenstream the string with token and pos
     *  
     */
    private boolean SetTokenFeatures(int paragramstart, String tokenstream) {
        // get document's annotationset
        AnnotationSet annotationSet;
        if (annotationSetName == null ||
                annotationSetName.equals("")) annotationSet = document.getAnnotations();
        else annotationSet = document.getAnnotations(annotationSetName);

        String[] tokens = tokenstream.split(" ");
        int tokencount = tokens.length;
        int charIdx = paragramstart;
        int tokenStart = paragramstart;
        FeatureMap newTokenFm;

        for (int index = 0; index < tokencount; index++) {
            String[] tokenpos = tokens[index].split("/");
            String token = tokenpos[0];
            String POS = "";
            if (tokenpos.length == 2) {
                POS = tokenpos[1];
            }
            //When length>2, means there are two "/"
            //the first one is string "/" ,and the second one is pos flag
            else if (tokenpos.length > 2) {
                token = "/";
                POS = "w";
            }

            //we have a match!
            newTokenFm = Factory.newFeatureMap();
            //got a space!
            if (tokenpos.length == 1) {
                // watch out, not token.length() but 1!
                // 2014-6-16 15:37:07，THERE MAY BE MORE THAN ONE SPACE!
                charIdx = getNoSpaceIdx(tokenStart);
                newTokenFm.put(TOKEN_KIND_FEATURE_NAME, "space");
                newTokenFm.put(TOKEN_STRING_FEATURE_NAME, token);
                newTokenFm.put(TOKEN_LENGTH_FEATURE_NAME,
                        Integer.toString(charIdx - tokenStart));

                try {
                    annotationSet.add(new Long(tokenStart),
                            new Long(charIdx),
                            "SpaceToken", newTokenFm);
                } catch (InvalidOffsetException ioe) {
                    //This REALLY shouldn't happen!
                    ioe.printStackTrace(Err.getPrintWriter());
                }
            } else {
                newTokenFm.put(TOKEN_KIND_FEATURE_NAME, "word");
                newTokenFm.put(TOKEN_STRING_FEATURE_NAME, token);
                newTokenFm.put(TOKEN_LENGTH_FEATURE_NAME,
                        Integer.toString(token.length()));
                newTokenFm.put(TOKEN_CATEGORY_FEATURE_NAME, POS);

                charIdx = tokenStart + token.length();
                try {
                    annotationSet.add(new Long(tokenStart),
                            new Long(charIdx),
                            "Token", newTokenFm);
                } catch (InvalidOffsetException ioe) {
                    //This REALLY shouldn't happen!
                    ioe.printStackTrace(Err.getPrintWriter());
                }
            }
            // then assign the value of charIdx to tokenStart
            tokenStart = charIdx;
        }
        return true;
    }

    /**
     *    * Count the length of space words.
     *  
     */
    protected int getNoSpaceIdx(int startIdx) {
        String content = document.getContent().toString();
        if (startIdx >= content.length() - 1) {
            return 0;
        }
        String spacestring = content.substring(startIdx, startIdx + 1);
        while (spacestring != null && spacestring.equals(" ")) {
            startIdx++;
            spacestring = content.substring(startIdx, startIdx + 1);
        }
        return startIdx;
    }

    private boolean SetSplitFeatures(int paragramstart, String paragram) throws InvalidOffsetException {
        // get document's annotationset
        AnnotationSet annotationSet;
        if (annotationSetName == null ||
                annotationSetName.equals("")) annotationSet = document.getAnnotations();
        else annotationSet = document.getAnnotations(annotationSetName);

        try {
            FeatureMap features = Factory.newFeatureMap();
            features.put("kind", "external");
            annotationSet.add(new Long(paragramstart + paragram.length()), new Long(paragramstart + paragram.length() + paragramflagLength),
                    "Split", features);
        } catch (InvalidOffsetException ioe) {
            return false;
        }

        return true;
    }

    @CreoleParameter(defaultValue = "UTF-8", comment = "The encoding used for reading the definitions")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    @CreoleParameter(defaultValue = "0", comment = "The POS type for tokensier, 0 计算所二级标注集，1 计算所一级标注集，2 北大二级标注集，3 北大一级标注集")
    public void setPosType(String posType) {
        this.posType = posType;
    }

    public String getPosType() {
        return posType;
    }

    @Optional
    @CreoleParameter(comment = "The usr dictionary to add")
    public void setUserDict(URL userDict) {
        this.userDict = userDict;
    }

    public URL getUserDict() {
        return userDict;
    }

    @Optional
    @CreoleParameter(defaultValue = "0", comment = "The POS type for usr dictionary, 0 计算所二级标注集，1 计算所一级标注集，2 北大二级标注集，3 北大一级标注集")
    public void setUsrDictPosType(String usrDictPosType) {
        this.usrDictPosType = usrDictPosType;
    }

    public String getUsrDictPosType() {
        return usrDictPosType;
    }

    @RunTime
    @CreoleParameter(defaultValue = "", comment = "The annotation set to be used for the generated annotations")
    public void setAnnotationSetName(String annotationSetName) {
        this.annotationSetName = annotationSetName;
    }

    public String getAnnotationSetName() {
        return annotationSetName;
    }
}



