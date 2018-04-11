package ru.klauz42.xmlvalidator;

import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;


public class ESBParser {

    public static final Logger LOGGER =
            Logger.getLogger(ESBParser.class.getName());


    private Node esbRoot;

    private static final SimpleDateFormat csbTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

    private static final byte RQUID_INDEX = 1;
    private static final byte RQTM_INDEX = 3;
    private static final byte SPNAME_INDEX = 5;
    private static final byte SYSTEMID_INDEX = 7;
    private static final byte METHOD_INDEX = 9;
    private static final byte USERLOGIN_INDEX = 11;
    private static final byte GETCLIENTPROFILE_INDEX = 13;
    private static final byte ORGREC_INDEX = 1;
    private static final byte ORGID_INDEX = 1;
    private static final short ORGTYPE_INDEX = 1;
    private static final short ORGNUM_INDEX = 3;

    ESBParser(String esbFormatXML) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse("file:" + esbFormatXML);                 //todo: spaces and cyrillic symbols in path
        esbRoot = document.getDocumentElement();
    }

    public ESBObj parseESBXMLToESBObj () throws ParseException, ESBFormatException{
        NodeList nodes = esbRoot.getChildNodes();
        ESBObj result = null;
        try {
            result = new ESBObj(nodes.item(RQUID_INDEX).getTextContent(),
                    csbTimeFormat.parse(nodes.item(RQTM_INDEX).getTextContent()),  //using of SimpleDateFormat
                    nodes.item(SPNAME_INDEX).getTextContent(),
                    nodes.item(SYSTEMID_INDEX).getTextContent(),
                    nodes.item(METHOD_INDEX).getTextContent(),
                    nodes.item(USERLOGIN_INDEX).getTextContent(),
                    nodes.item(GETCLIENTPROFILE_INDEX).getChildNodes().item(ORGREC_INDEX).
                            getChildNodes().item(ORGID_INDEX).getChildNodes().item(ORGTYPE_INDEX).getTextContent(),
                    nodes.item(GETCLIENTPROFILE_INDEX).getChildNodes().item(ORGREC_INDEX).
                            getChildNodes().item(ORGID_INDEX).getChildNodes().item(ORGNUM_INDEX).getTextContent());
        } catch (NullPointerException e) {
            throw new ESBFormatException("Uploaded file has not ESB format");
        }

        return result;
    }

    class ESBObj {                  //RqObject
        public String getRqUID() {
            return RqUID;
        }

        public Date getRqTm() {
            return RqTm;
        }

        public String getSPName() {
            return SPName;
        }

        public String getSystemId() {
            return SystemId;
        }

        public String getMethod() {
            return Method;
        }

        public String getUserLogin() {
            return UserLogin;
        }

        public String getOrgType() {
            return OrgType;
        }

        public String getOrgNum() {
            return OrgNum;
        }

        private String RqUID;
        private Date RqTm;
        private String SPName;
        private String SystemId;
        private String Method;
        private String UserLogin;
        //GetClientProfile
        ////OrgRec
        //////OrgID
        private String OrgType;
        private String OrgNum;


        ESBObj(String RqUID, Date RqTm, String SPName,
               String SystemId, String Method,
               String UserLogin, String OrgType, String OrgNum){
            this.RqUID = RqUID;
            this.RqTm = RqTm;
            this.SPName = SPName;
            this.SystemId = SystemId;
            this.Method = Method;
            this.UserLogin = UserLogin;
            this.OrgType = OrgType;
            this.OrgNum = OrgNum;
        }

        @Override
        public String toString() {
            return  "RqUID: " + RqUID + "\n" +
                    "RqTm: " + RqTm + "\n" +
                    "SPName: " + SPName + "\n" +
                    "SystemId: " + SystemId + "\n" +
                    "Method" + Method + "\n" +
                    "UserLogin: " + UserLogin + "\n" +
                    "OrgType: " + OrgType + "\n" +
                    "OrgNum: " + OrgNum;
        }
    }

    class ESBFormatException extends Exception {
        ESBFormatException(String text){
            super(text);
        }
    }
}

