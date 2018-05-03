package ru.klauz42.xmlvalidator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class ParserXML {

    private enum XMLFormat { ESB, CRMORG }

    private XMLFormat format;
    private final Document document;
    private ESBObjectOut esbOut = new ESBObjectOut();

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, FormatException, XPathExpressionException, ParseException, TransformerException {
        ParserXML ParserXML = new ParserXML("GetLegalClientProfileRq(esb).xml");
        ParserXML.convertXML("test18.xml");
    }

    public static final Logger LOGGER =
            Logger.getLogger(ParserXML.class.getName());


    public void convertXML(String pathToSave) throws XPathExpressionException, ParseException, FileNotFoundException, TransformerException, ParserConfigurationException {
        switch (format){
            case ESB:
                writeCRMObjToXML(parseESBXMLToCRMObject(), pathToSave);
                break;
            case CRMORG:
                writeESBObjToXML(parseCRMXMLToESBObject(), pathToSave);
                break;
        }

    }

    ParserXML(String inputXML) throws ParserConfigurationException, IOException, SAXException, FormatException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        this.document = builder.parse("file:" + inputXML);
        Node documentRoot = document.getDocumentElement();

        switch (documentRoot.getNodeName()) {
           case "GetLegalClientProfileRq":
               this.format = XMLFormat.ESB;
               break;
           case "GetLegalClientProfileRs":
               this.format = XMLFormat.CRMORG;
               break;
           default:
               throw new FormatException("Wrong XML Fromat");
        }
    }

    private CRMObjectOut parseESBXMLToCRMObject() throws XPathExpressionException, ParseException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        CRMObjectOut crmOut = new CRMObjectOut();
        crmOut.RqUID = (String) (xPath.compile("//RqUID/text()").evaluate(document, XPathConstants.STRING));
        Date esbTime =
                esbTimeFormat.parse(
                        (String) xPath.compile("//RqTm/text()").evaluate(document, XPathConstants.STRING));
        crmOut.RqTm = crmTimeFormat.format(esbTime);
        crmOut.SPName = (String) xPath.compile("//SPName/text()").evaluate(document, XPathConstants.STRING);
        crmOut.SystemId = (String) xPath.compile("//SystemId/text()").evaluate(document, XPathConstants.STRING);
        crmOut.Method = (String) xPath.compile("//Method/text()").evaluate(document, XPathConstants.STRING);
        crmOut.Login = (String) xPath.compile("//UserLogin/text()").evaluate(document, XPathConstants.STRING);
        crmOut.AccountId = (String) xPath.compile("//OrgNum/text()").evaluate(document, XPathConstants.STRING);

        return crmOut;
    }

    private void writeCRMObjToXML(CRMObjectOut crmOut, String pathToSave) throws ParserConfigurationException, TransformerException, FileNotFoundException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc=builder.newDocument();
        Element RootElement=doc.createElement("GetLegalClientProfileRq");

        Element RqUIDElement=doc.createElement("RqUID");
        RqUIDElement.appendChild(doc.createTextNode(crmOut.RqUID));
        RootElement.appendChild(RqUIDElement);

        Element RqTmElement=doc.createElement("RqTm");
        RqTmElement.appendChild(doc.createTextNode(crmOut.RqTm));
        RootElement.appendChild(RqTmElement);

        Element SPNameElement=doc.createElement("SPName");
        SPNameElement.appendChild(doc.createTextNode(crmOut.SPName));
        RootElement.appendChild(SPNameElement);

        Element SystemIdElement=doc.createElement("SystemId");
        SystemIdElement.appendChild(doc.createTextNode(crmOut.SystemId));
        RootElement.appendChild(SystemIdElement);

        Element MethodNameElement=doc.createElement("Method");
        MethodNameElement.appendChild(doc.createTextNode(crmOut.Method));
        RootElement.appendChild(SystemIdElement);

            Element MessageElement=doc.createElement("Message");

            Element LoginElement=doc.createElement("Login");
            LoginElement.appendChild(doc.createTextNode(crmOut.Login));
            MessageElement.appendChild(LoginElement);

            Element AccountInfoElement = doc.createElement("AccountInfo");

            Element AccountIdElement = doc.createElement("AccountId");
            AccountIdElement.appendChild(doc.createTextNode(crmOut.AccountId));
            AccountInfoElement.appendChild(AccountIdElement);

            MessageElement.appendChild(AccountInfoElement);
        RootElement.appendChild(MessageElement);

        doc.appendChild(RootElement);

        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        LOGGER.info("File has been successfully converted");
        t.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(pathToSave)));
    }

    private ESBObjectOut parseCRMXMLToESBObject() {
        return null;
    }

    private void writeESBObjToXML(ESBObjectOut esbOut, String pathToSave) {

    }

    private static final SimpleDateFormat esbTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
    private static final SimpleDateFormat crmTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");

    class CRMObjectIn {

    }

    class ESBObjectOut {

    }

    private class CRMObjectOut {
        String RqUID;
        String RqTm;
        String SPName;
        String SystemId;
        String Method;
        //Message
        String Login;
        ////AccountInfo
        String AccountId;
    }

    class FormatException extends Exception {
        FormatException(String text){
            super(text);
        }
    }

}
