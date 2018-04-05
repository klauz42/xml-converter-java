package ru.klauz42.xmlvalidator;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import ru.klauz42.xmlvalidator.ESBParser.ESBObj;

import java.io.FileOutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.logging.Logger;


public class CRMWriter {

    public static final Logger LOGGER =
            Logger.getLogger(LogDemo.class.getName());


    private static final SimpleDateFormat crmTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");

    private DocumentBuilder builder;
    private ESBObj esbObj;
    private String pathtoSave;

    public CRMWriter(ESBObj esbObj, String pathToSave) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
        this.esbObj = esbObj;
        this.pathtoSave = pathToSave;
    }

    public void WriteXML() throws TransformerException, IOException {
        Document doc=builder.newDocument();
        Element RootElement=doc.createElement("GetLegalClientProfileRs");

        Element RqUIDElement=doc.createElement("RqUID");
        RqUIDElement.appendChild(doc.createTextNode(esbObj.getRqUID()));
        RootElement.appendChild(RqUIDElement);

        Element RqTmElement=doc.createElement("RqTm");
        RqTmElement.appendChild(doc.createTextNode(crmTimeFormat.format(esbObj.getRqTm())));
        RootElement.appendChild(RqTmElement);

        Element SPNameElement=doc.createElement("SPName");
        SPNameElement.appendChild(doc.createTextNode(esbObj.getSystemId()));
        RootElement.appendChild(SPNameElement);

        Element SystemIdElement=doc.createElement("SystemId");
        SystemIdElement.appendChild(doc.createTextNode(esbObj.getSPName()));
        RootElement.appendChild(SystemIdElement);

        Element MethodNameElement=doc.createElement("Method");
        MethodNameElement.appendChild(doc.createTextNode(esbObj.getMethod() + "Response"));
        RootElement.appendChild(SystemIdElement);

        Element StatusElement=doc.createElement("Status");
        RootElement.appendChild(StatusElement);

        Element MessageElement=doc.createElement("Message");
        RootElement.appendChild(MessageElement);

        doc.appendChild(RootElement);

        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        LOGGER.info("File has been successfully converted");
        t.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(pathtoSave)));

    }

}




