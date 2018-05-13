package ru.klauz42.xmlvalidator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import java.util.*;
import java.util.logging.Logger;

public class ParserXML {

    private enum XMLFormat { ESB, CRMORG }

    private XMLFormat format;
    private final Document document;
    //private ESBObjectOut esbOut = new ESBObjectOut();

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, FormatException, XPathExpressionException, ParseException, TransformerException {
        ParserXML ParserXML = new ParserXML("GetLegalClientProfileRs(crmorg).xml");
        ParserXML.convertXML("test.xml");
    }

    private static final Logger LOGGER =
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

    public ParserXML(String inputXML) throws ParserConfigurationException, IOException, SAXException, FormatException {
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

    //Методы для парсинга XML в выходные объекты
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

    private ESBObjectOut parseCRMXMLToESBObject() throws XPathExpressionException, ParseException {

        XPath xPath = XPathFactory.newInstance().newXPath();
        ESBObjectOut esbOut = new ESBObjectOut();
        esbOut.RqUID = (String) (xPath.compile("//RqUID/text()").evaluate(document, XPathConstants.STRING));
        Date esbTime =
                crmTimeFormat.parse(
                        (String) xPath.compile("//RqTm/text()").evaluate(document, XPathConstants.STRING));
        esbOut.RqTm = esbTimeFormat.format(esbTime);
        esbOut.SPName = (String) xPath.compile("//SPName/text()").evaluate(document, XPathConstants.STRING);
        esbOut.SystemId = (String) xPath.compile("//SystemId/text()").evaluate(document, XPathConstants.STRING);
        esbOut.Method = (String) xPath.compile("//Method/text()").evaluate(document, XPathConstants.STRING);

        //Status
        esbOut.StatusCode = (String) xPath.compile("//Status/StatusCode/text()").evaluate(document, XPathConstants.STRING);
        esbOut.ServerStatusCode = (String) xPath.compile("//ServerStatusCode/text()").evaluate(document, XPathConstants.STRING);
        esbOut.Severity = (String) xPath.compile("//Severity/text()").evaluate(document, XPathConstants.STRING);
        String tmp;

        if (!"".equals(tmp = (String) xPath.compile("//AccountInfo/Id/text()").evaluate(document, XPathConstants.STRING))) {
            esbOut.listOrgNum.add(tmp);
            esbOut.listOrgType.add("AccountCRMId");
            tmp = null;
        } else {
            LOGGER.info("No AccountCRMId");
        }

        if (!"".equals(tmp = (String) xPath.compile("//AccountInfo/MDMId/text()").evaluate(document, XPathConstants.STRING))) {
            esbOut.listOrgNum.add(tmp);
            esbOut.listOrgType.add("MDMId");
            tmp = null;
        } else {
            LOGGER.info("No MDMId");
        }

        esbOut.IndustNum = (String) xPath.compile("//AccountInfo/SBRFIndustryLIC/text()").evaluate(document, XPathConstants.STRING);
        esbOut.SubIndustrNum = (String) xPath.compile("//AccountInfo/SBRFKindActivity/text()").evaluate(document, XPathConstants.STRING);
        esbOut.Name = (String) xPath.compile("//AccountInfo/Name/text()").evaluate(document, XPathConstants.STRING);
        esbOut.LegalFullName = (String) xPath.compile("//AccountInfo/FullName/text()").evaluate(document, XPathConstants.STRING);
        esbOut.LicenseNum = (String) xPath.compile("//AccountInfo/LicenseNumber/text()").evaluate(document, XPathConstants.STRING);
        esbOut.LicenseIssueDt = (String) xPath.compile("//AccountInfo/LicenseIssueDate/text()").evaluate(document, XPathConstants.STRING);
        esbOut.TaxId_TIN = (String) xPath.compile("//AccountInfo/SBRFINN/text()").evaluate(document, XPathConstants.STRING);
        esbOut.TaxId_KIO = (String) xPath.compile("//AccountInfo/SBRFKIO/text()").evaluate(document, XPathConstants.STRING);
        esbOut.EmployerCode = (String) xPath.compile("//AccountInfo/TypeLIC/text()").evaluate(document, XPathConstants.STRING);
        esbOut.EmployerCodeOPF = (String) xPath.compile("//AccountInfo/SBRFOPF/text()").evaluate(document, XPathConstants.STRING);
        esbOut.TaxId_KPPInfo = (String) xPath.compile("//AccountInfo/SBRFKPP/text()").evaluate(document, XPathConstants.STRING);
        esbOut.TradeMark = (String) xPath.compile("//AccountInfo/TradeMark/text()").evaluate(document, XPathConstants.STRING);
        esbOut.TaxOfficeCode = (String) xPath.compile("//AccountInfo/IFNS/text()").evaluate(document, XPathConstants.STRING);
        esbOut.StateRegPrimeNum = (String) xPath.compile("//AccountInfo/OGRN/text()").evaluate(document, XPathConstants.STRING);
        //23 page
        esbOut.StateRegPrimeDate = (String) xPath.compile("//AccountInfo/RegistrationDate/text()").evaluate(document, XPathConstants.STRING);
        esbOut.StateRegPrimePlace = (String) xPath.compile("//AccountInfo/RegisteredBy/text()").evaluate(document, XPathConstants.STRING);
        esbOut.FullName = (String) xPath.compile("//AccountInfo/VKO/text()").evaluate(document, XPathConstants.STRING);
        esbOut.PhoneNum = (String) xPath.compile("//AccountInfo/VKOPhone/text()").evaluate(document, XPathConstants.STRING);
        esbOut.EmailAddr = (String) xPath.compile("//AccountInfo/VKOEmail/text()").evaluate(document, XPathConstants.STRING);
        esbOut.Segment = (String) xPath.compile("//AccountInfo/SBRFSegmentLIC/text()").evaluate(document, XPathConstants.STRING);
        esbOut.SubSegment = (String) xPath.compile("//AccountInfo/SBRFSubSegment/text()").evaluate(document, XPathConstants.STRING);
        esbOut.CustStatusCodeCRM = (String) xPath.compile("//AccountInfo/Status/text()").evaluate(document, XPathConstants.STRING);
        esbOut.ResidentExtStatus = (String) xPath.compile("//AccountInfo/Resident/text()").evaluate(document, XPathConstants.STRING);
        //24 page
        esbOut.StopListFlag = (String) xPath.compile("//AccountInfo/StopListFlag/text()").evaluate(document, XPathConstants.STRING); //todo: Boolean?
        esbOut.ParentAccountId = (String) xPath.compile("//AccountInfo/ParentAccountId/text()").evaluate(document, XPathConstants.STRING);
        esbOut.ParentAccount = (String) xPath.compile("//AccountInfo/ParentAccount/text()").evaluate(document, XPathConstants.STRING);
        esbOut.Partnership = (String) xPath.compile("//AccountInfo/SBRFPartnership/text()").evaluate(document, XPathConstants.STRING);
        esbOut.Channel = (String) xPath.compile("//AccountInfo/AttrChannel/text()").evaluate(document, XPathConstants.STRING);
        esbOut.ReceiveDelevery = (String) xPath.compile("//AccountInfo/ReceiveMail/text()").evaluate(document, XPathConstants.STRING);

        NodeList listPhoneInfo = (NodeList) xPath.compile("//PhoneInfo/text()").evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < listPhoneInfo.getLength(); i++) {
            ESBObjectOut.PhoneInfo piTmp = esbOut.new PhoneInfo();
            piTmp.PhoneId = getElementByTagName(listPhoneInfo, i, "PhoneId");
            piTmp.PhoneType = (getElementByTagName(listPhoneInfo, i, "PhoneType"));
            piTmp.Phone = (getElementByTagName(listPhoneInfo, i, "Phone"));
            piTmp.PhoneComment = (getElementByTagName(listPhoneInfo, i, "PhoneComment"));

            esbOut.PhoneInfoList.add(piTmp);
        }

        NodeList listEmailInfo = (NodeList) xPath.compile("//ListOfEmail/EmailInfo/text()").evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < listEmailInfo.getLength(); i++) {
            ESBObjectOut.EmailInfo eiTmp = esbOut.new EmailInfo();
            eiTmp.EmailId = getElementByTagName(listEmailInfo, i, "EmailId");
            eiTmp.EmailType = (getElementByTagName(listEmailInfo, i, "EmailType"));
            eiTmp.MailAddr = (getElementByTagName(listEmailInfo, i, "Email"));
            eiTmp.EmailComment = (getElementByTagName(listEmailInfo, i, "Comment"));

            esbOut.EmailInfoList.add(eiTmp);
        }

        esbOut.RelDepartment = (String) xPath.compile("//AccountInfo/SBRFRelDepartment/text()").evaluate(document, XPathConstants.STRING);
        esbOut.Priorities = (String) xPath.compile("//AccountInfo/SBRFPriorities/text()").evaluate(document, XPathConstants.STRING);

        NodeList listLoanInfo = (NodeList) xPath.compile("//ListOfProduct/ProductInfo/text()").evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < listLoanInfo.getLength(); i++) {
            ESBObjectOut.LoanInfo liTmp = esbOut.new LoanInfo();
            liTmp.ProdType = getElementByTagName(listLoanInfo, i, "ProductId");
            liTmp.ProductMDMId = (getElementByTagName(listLoanInfo, i, "ProductMDMId"));
            liTmp.ProdName = (getElementByTagName(listLoanInfo, i, "ProductName"));
            liTmp.Status = (getElementByTagName(listLoanInfo, i, "Status"));

            esbOut.LoanInfoList.add(liTmp);
        }

        NodeList listPersonInfo_LOC = (NodeList) xPath.compile("//ListOfContacts/ContactInfo").evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < listPersonInfo_LOC.getLength(); i++) {
            ESBObjectOut.PersonInfo_LOC pilocTmp = esbOut.new PersonInfo_LOC();

            pilocTmp.LastName = getElementByTagName(listPersonInfo_LOC, i, "LastName");
            pilocTmp.FirstName = (getElementByTagName(listPersonInfo_LOC, i, "FirstName"));
            pilocTmp.MiddleName = (getElementByTagName(listPersonInfo_LOC, i, "MiddleName"));
            pilocTmp.EmailAddr = (getElementByTagName(listPersonInfo_LOC, i, "Email"));
            pilocTmp.WorkPhoneNum = (getElementByTagName(listPersonInfo_LOC, i, "WorkPhone"));
            pilocTmp.MobilePhoneNum = (getElementByTagName(listPersonInfo_LOC, i, "MobilePhone"));
            pilocTmp.BirthDt = (getElementByTagName(listPersonInfo_LOC, i, "DateOfBirth"));
            pilocTmp.Gender = (getElementByTagName(listPersonInfo_LOC, i, "Gender"));
            pilocTmp.OEDCode = (getElementByTagName(listPersonInfo_LOC, i, "JobTitle"));
            pilocTmp.ContactId = (getElementByTagName(listPersonInfo_LOC, i, "ContactId"));

            esbOut.PersonInfoLOCList.add(pilocTmp);
        }

        NodeList listPersonInfo_LOA = (NodeList) xPath.compile("//ListOfAddress/AddressInfo").evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < listPersonInfo_LOA.getLength(); i++) {
            ESBObjectOut.PersonInfo_LOA tmpObj = esbOut.new PersonInfo_LOA();
            Node tmpNode = listPersonInfo_LOA.item(i);

            tmpObj.City = getElementByTagName(tmpNode, "City");
            tmpObj.StateProv = getElementByTagName(tmpNode, "Province");
            tmpObj.Area = getElementByTagName(tmpNode, "State");
            tmpObj.PostalCode = getElementByTagName(tmpNode, "PostalCode");
            tmpObj.Country = getElementByTagName(tmpNode, "Country");
            tmpObj.AddressId = getElementByTagName(tmpNode, "AddressId");
            tmpObj.AddrType = getElementByTagName(tmpNode, "AddressType");
            tmpObj.Place = getElementByTagName(tmpNode, "SBRFLocality");
            tmpObj.Street = getElementByTagName(tmpNode, "Street");
            tmpObj.House = getElementByTagName(tmpNode, "House");
            tmpObj.Corpus = getElementByTagName(tmpNode, "SBRFHousing");
            tmpObj.Building = getElementByTagName(tmpNode, "Building");
            tmpObj.Office = getElementByTagName(tmpNode, "ApartmentNumber");
            tmpObj.Comments = getElementByTagName(tmpNode, "AddrDescription");

            esbOut.PersonInfoLOAList.add(tmpObj);
        }

        NodeList listIPPInfo = (NodeList) xPath.compile("//IPPInfo").evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < listIPPInfo.getLength(); i++) {
            ESBObjectOut.IPPInfo tmpObj = esbOut.new IPPInfo();
            Node tmpNode = listIPPInfo.item(i);

            tmpObj.IPPId = getElementByTagName(tmpNode, "IPPId");
            tmpObj.ProductNumber = getElementByTagName(tmpNode, "ProductNumber");
            tmpObj.ProdName = getElementByTagName(tmpNode, "ProductName");
            tmpObj.OverallScore = getElementByTagName(tmpNode, "OverallScore");
            tmpObj.ProductOutcome = getElementByTagName(tmpNode, "ProductOutcome");
            tmpObj.ProductComment = getElementByTagName(tmpNode, "ProductComment");
            tmpObj.AvgProductAmount = getElementByTagName(tmpNode, "AvgProductAmount");
            tmpObj.MaxProductAmount = getElementByTagName(tmpNode, "MaxProductAmount");
            tmpObj.ProductAmount = getElementByTagName(tmpNode, "ProductAmount");
            tmpObj.IPPPriority = getElementByTagName(tmpNode, "IPPPriority");
            tmpObj.IPPPDescription = getElementByTagName(tmpNode, "Description");
            tmpObj.LeaderOrgFullName = getElementByTagName(tmpNode, "LeaderOrgFullName");
            tmpObj.Revenue = getElementByTagName(tmpNode, "Revenue");
            tmpObj.SBRevenue = getElementByTagName(tmpNode, "SBRevenue");
            tmpObj.ProfitLoss = getElementByTagName(tmpNode, "ProfitLoss");
            tmpObj.OrgRating = getElementByTagName(tmpNode, "OrgRating");
            tmpObj.FEAIndicator = getElementByTagName(tmpNode, "FEAIndicator");
            tmpObj.CurAdb = getElementByTagName(tmpNode, "CurAdb");
            tmpObj.CurValue = getElementByTagName(tmpNode, "CurValue");
            tmpObj.DelValue = getElementByTagName(tmpNode, "DelValue");
            tmpObj.U1C2 = getElementByTagName(tmpNode, "U1C2");

            esbOut.IPPInfoList.add(tmpObj);
        }

        NodeList listTeamInfo = (NodeList) xPath.compile("//TeamInfo").evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < listTeamInfo.getLength(); i++) {
            ESBObjectOut.TeamInfo tmpObj = esbOut.new TeamInfo();
            Node tmpNode = listTeamInfo.item(i);

            tmpObj.UserLogin = getElementByTagName(tmpNode, "Login");
            tmpObj.PrimaryFlg = getElementByTagName(tmpNode, "PrimaryFlg");
            tmpObj.LastName = getElementByTagName(tmpNode, "LastName");
            tmpObj.FirstName = getElementByTagName(tmpNode, "FirstName");
            tmpObj.MiddleName = getElementByTagName(tmpNode, "MiddleName");
            tmpObj.PhoneNum = getElementByTagName(tmpNode, "Phone");
            tmpObj.EmailAddr = getElementByTagName(tmpNode, "EmailAddr");
            tmpObj.JobTitle = getElementByTagName(tmpNode, "JobTitle");
            tmpObj.SBRFAccountRole = getElementByTagName(tmpNode, "SBRFAccountRole");

            esbOut.TeamInfoList.add(tmpObj);
        }

        return esbOut;
    }

    private String getElementByTagName(NodeList list, int nodeIndex, String tagName) {         //для NodeList с указанием индекса
        return ((Element) list.item(nodeIndex)).getElementsByTagName(tagName).item(0).getTextContent();
    }

    private String getElementByTagName(Node node, String tagName) {
        return ((Element) node).getElementsByTagName(tagName).item(0).getTextContent();
    }

    //Методы для записи в XML, включая "подметоды" для ESB
    ///////////////////////////////////////////////////////////////
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

        Element AccountInfoElement = doc.createElement("AccountInfo"); //todo: ???

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

    private void writeESBObjToXML(ESBObjectOut esbOut, String pathToSave) throws ParserConfigurationException, TransformerException, FileNotFoundException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc=builder.newDocument();
        Element RootElement=doc.createElement("GetLegalClientProfileRs");

        Element RqUIDElement=doc.createElement("RqUID");
        RqUIDElement.appendChild(doc.createTextNode(esbOut.RqUID));
        RootElement.appendChild(RqUIDElement);

        Element RqTmElement=doc.createElement("RqTm");
        RqTmElement.appendChild(doc.createTextNode(esbOut.RqTm));
        RootElement.appendChild(RqTmElement);

        Element SPNameElement=doc.createElement("SPName");
        SPNameElement.appendChild(doc.createTextNode(esbOut.SPName));
        RootElement.appendChild(SPNameElement);

        Element SystemIdElement=doc.createElement("SystemId");
        SystemIdElement.appendChild(doc.createTextNode(esbOut.SystemId));
        RootElement.appendChild(SystemIdElement);

        Element MethodElement=doc.createElement("Method");
        MethodElement.appendChild(doc.createTextNode(esbOut.Method));
        RootElement.appendChild(MethodElement);


        Element StatusElement = createStatus(doc, esbOut);
        RootElement.appendChild(StatusElement);

        Element ListOfClientCard = createListOfClientCard(doc, esbOut);
        RootElement.appendChild(ListOfClientCard);

        doc.appendChild(RootElement);

        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        LOGGER.info("File has been successfully converted");
        t.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(pathToSave)));
    }

    private Element createStatus (Document doc, ESBObjectOut esbOut) {
        Element StatusElement = doc.createElement("Status");

        if(esbOut.StatusCode != null) {
            Element StatusCode = doc.createElement("StatusCode");
            StatusCode.appendChild(doc.createTextNode(esbOut.StatusCode));
            StatusElement.appendChild(StatusCode);

            Element ServerStatusCode = doc.createElement("ServerStatusCode");
            ServerStatusCode.appendChild(doc.createTextNode(esbOut.getServerStatusCode()));
            StatusElement.appendChild(ServerStatusCode);

            Element Severity = doc.createElement("Severity");
            Severity.appendChild(doc.createTextNode(esbOut.Severity));
            StatusElement.appendChild(Severity);

            Element StatusDesc = doc.createElement("StatusDesc");
            StatusDesc.appendChild(doc.createTextNode(esbOut.getStatusDesc()));
            StatusElement.appendChild(StatusDesc);
        }

        return StatusElement;
    }

    private Element createListOfClientCard (Document doc, ESBObjectOut esbOut) {
        Element ListOfClientCardElement = doc.createElement("ListOfClientCard");

        Element ClientCardInfo = createClientCardInfo(doc, esbOut);
        ListOfClientCardElement.appendChild(ClientCardInfo);

        return ListOfClientCardElement;
    }

    private Element createClientCardInfo (Document doc, ESBObjectOut esbOut) {
        Element ClientCardInfo = doc.createElement("ClientCardInfo");

        Element OrgRec = createOrgRec(doc, esbOut);
        ClientCardInfo.appendChild(OrgRec);

        Element Channel = doc.createElement("Channel");
        Channel.appendChild(doc.createTextNode(esbOut.getChannel()));
        ClientCardInfo.appendChild(Channel);

        Element ReceiveDelevery = doc.createElement("ReceiveDelevery");
        ReceiveDelevery.appendChild(doc.createTextNode(esbOut.getReceiveDelevery()));
        ClientCardInfo.appendChild(ReceiveDelevery);

        Element ListOfPhone = createListOfPhone(doc, esbOut);
        ClientCardInfo.appendChild(ListOfPhone);

        Element ListOfEmail = createListOfEmail(doc, esbOut);
        ClientCardInfo.appendChild(ListOfEmail);

        Element RelDepartment = doc.createElement("RelDepartment");
        RelDepartment.appendChild(doc.createTextNode(esbOut.getRelDepartment()));
        ClientCardInfo.appendChild(RelDepartment);

        Element Priorities = doc.createElement("Priorities");
        Priorities.appendChild(doc.createTextNode(esbOut.getPriorities()));
        ClientCardInfo.appendChild(Priorities);

        Element ListOfProduct = createListOfProduct(doc, esbOut);
        ClientCardInfo.appendChild(ListOfProduct);

        Element ListOfContacts = createListOfContacts(doc, esbOut);
        ClientCardInfo.appendChild(ListOfContacts);

        Element ListOfAddress = createListOfAddress(doc, esbOut);
        ClientCardInfo.appendChild(ListOfAddress);

        Element ListOfIPP = createListOfIPP(doc, esbOut);
        ClientCardInfo.appendChild(ListOfIPP);

        Element ListOfTeamInfo = createListOfTeamInfo(doc, esbOut);
        ClientCardInfo.appendChild(ListOfTeamInfo);

        return ClientCardInfo;
    }

    private Element createListOfProduct (Document doc, ESBObjectOut esbOut) {
        Element ListOfProduct = doc.createElement("ListOfProduct");

        for (ESBObjectOut.LoanInfo  loanInfo: esbOut.LoanInfoList) {
            Element ProductInfo = doc.createElement("ProductInfo");
                Element LoanInfo = doc.createElement("LoanInfo");

                    Element ProdType = doc.createElement("ProdType");
                    ProdType.appendChild(doc.createTextNode(loanInfo.getProdName()));
                    LoanInfo.appendChild(ProdType);

                    Element ProductMDMId = doc.createElement("ProductMDMId");
                    ProductMDMId.appendChild(doc.createTextNode(loanInfo.getProductMDMId()));
                    LoanInfo.appendChild(ProductMDMId);

                    Element ProdName = doc.createElement("ProdName");
                    ProdName.appendChild(doc.createTextNode(loanInfo.getProdName()));
                    LoanInfo.appendChild(ProdName);

                    Element Status = doc.createElement("Status");
                    ProdType.appendChild(doc.createTextNode(loanInfo.getProdName()));
                    LoanInfo.appendChild(Status);

                ProductInfo.appendChild(LoanInfo);
                ListOfProduct.appendChild(ProductInfo);
        }

        return ListOfProduct;
    }

    private Element createOrgRec (Document doc, ESBObjectOut esbOut) {
        Element OrgRec = doc.createElement("OrgRec");

        for(int i = 0; i < esbOut.listOrgType.size(); i++){
            Element OrgId = doc.createElement("OrgId");

                Element OrgType = doc.createElement("OrgType");
                OrgType.appendChild(doc.createTextNode(esbOut.listOrgType.get(i)));
                OrgId.appendChild(OrgType);

                Element OrgNum = doc.createElement("OrgNum");
                OrgNum.appendChild(doc.createTextNode(esbOut.listOrgNum.get(i)));
                OrgId.appendChild(OrgNum);

            OrgRec.appendChild(OrgId);
        }

        Element OrgInfo = createOrgInfo(doc, esbOut);
        OrgRec.appendChild(OrgInfo);

        Element OrgStateReg = createOrgStateReg(doc, esbOut);
        OrgRec.appendChild(OrgStateReg);

        Element RelPerson = createRelPerson(doc, esbOut);
        OrgRec.appendChild(RelPerson);

        Element Segment = doc.createElement("Segment");
        Segment.appendChild(doc.createTextNode(esbOut.getSegment()));
        OrgRec.appendChild(Segment);

        Element SubSegment = doc.createElement("SubSegment");
        Segment.appendChild(doc.createTextNode(esbOut.getSubSegment()));
        OrgRec.appendChild(SubSegment);

        Element CustStatusCodeCRM = doc.createElement("CustStatusCodeCRM");
        Segment.appendChild(doc.createTextNode(esbOut.getCustStatusCodeCRM()));
        OrgRec.appendChild(CustStatusCodeCRM);

        Element ResidentExtStatus = doc.createElement("CustStatusCodeCRM");
        Segment.appendChild(doc.createTextNode(esbOut.getResidentExtStatus()));
        OrgRec.appendChild(ResidentExtStatus);

        Element StopListFlag = doc.createElement("StopListFlag");
        Segment.appendChild(doc.createTextNode(esbOut.getStopListFlag()));
        OrgRec.appendChild(StopListFlag);

        Element ParentOrg = createParentOrg(doc, esbOut);
        OrgRec.appendChild(ParentOrg);

        Element Partnership = doc.createElement("Partnership");
        Segment.appendChild(doc.createTextNode(esbOut.getPartnership()));
        OrgRec.appendChild(Partnership);

        return OrgRec;
    }

    private Element createOrgInfo (Document doc, ESBObjectOut esbOut) {
        Element OrgInfo = doc.createElement("OrgInfo");

        Element IndustId = doc.createElement("IndustId");

            Element IndustNum = doc.createElement("IndustNum");
            IndustNum.appendChild(doc.createTextNode(esbOut.getIndustNum()));
            IndustId.appendChild(IndustNum);

            Element SubIndustrNum = doc.createElement("SubIndustrNum");
            IndustNum.appendChild(doc.createTextNode(esbOut.getSubIndustrNum()));
            IndustId.appendChild(SubIndustrNum);

        OrgInfo.appendChild(IndustId);

        Element Name = doc.createElement("Name");
        Name.appendChild(doc.createTextNode(esbOut.getName()));
        OrgInfo.appendChild(Name);

        Element LegalFullName = doc.createElement("LegalFullName");
        LegalFullName.appendChild(doc.createTextNode(esbOut.getLegalFullName()));
        OrgInfo.appendChild(LegalFullName);

        Element Licenses = doc.createElement("Licenses");
            Element LicenseInfo = doc.createElement("LicenseInfo");

                Element LicenseNum = doc.createElement("LicenseNum");
                LicenseNum.appendChild(doc.createTextNode(esbOut.getLicenseNum()));
                LicenseInfo.appendChild(LicenseNum);

                Element LicenseIssueDt = doc.createElement("LicenseIssueDt");
                LicenseIssueDt.appendChild(doc.createTextNode(esbOut.getLicenseIssueDt()));
                LicenseInfo.appendChild(LicenseIssueDt);

            Licenses.appendChild(LicenseInfo);
        OrgInfo.appendChild(Licenses);

        Element TINInfo = doc.createElement("TINInfo");
            Element TaxId_TIN = doc.createElement("TaxId");
            TaxId_TIN.appendChild(doc.createTextNode(esbOut.getTaxId_TIN()));
            TINInfo.appendChild(TaxId_TIN);
        OrgInfo.appendChild(TINInfo);

        Element KIOInfo = doc.createElement("KIOInfo");
            Element TaxId_KIO = doc.createElement("TaxId");
            TaxId_TIN.appendChild(doc.createTextNode(esbOut.getTaxId_KIO()));
            KIOInfo.appendChild(TaxId_KIO);
        OrgInfo.appendChild(KIOInfo);

        Element EmployerCode = doc.createElement("EmployerCode");
        EmployerCode.appendChild(doc.createTextNode(esbOut.getEmployerCode()));
        OrgInfo.appendChild(EmployerCode);

        Element EmployerCodeOPF = doc.createElement("EmployerCodeOPF");
        EmployerCodeOPF.appendChild(doc.createTextNode(esbOut.getEmployerCodeOPF()));
        OrgInfo.appendChild(EmployerCodeOPF);

        Element KPPInfo = doc.createElement("KPPInfo");
            Element TaxId_KPP = doc.createElement("TaxId");
            TaxId_KPP.appendChild(doc.createTextNode(esbOut.getTaxId_KPPInfo()));
            KPPInfo.appendChild(TaxId_KPP);
        OrgInfo.appendChild(KPPInfo);

        Element TradeMark = doc.createElement("TradeMark");
        TradeMark.appendChild(doc.createTextNode(esbOut.getTradeMark()));
        OrgInfo.appendChild(TradeMark);

        Element TaxOfficeCode = doc.createElement("TaxOfficeCode");
        TaxOfficeCode.appendChild(doc.createTextNode(esbOut.getTaxOfficeCode()));
        OrgInfo.appendChild(TaxOfficeCode);

        return OrgInfo;
    }

    private Element createOrgStateReg (Document doc, ESBObjectOut esbOut) {
        Element OrgStateReg = doc.createElement("OrgStateReg");


        Element StateRegPrimeNum = doc.createElement("StateRegPrimeNum");
        StateRegPrimeNum.appendChild(doc.createTextNode(esbOut.getStateRegPrimeNum()));
        OrgStateReg.appendChild(StateRegPrimeNum);

        Element StateRegPrimeDate = doc.createElement("StateRegPrimeDate");
        StateRegPrimeDate.appendChild(doc.createTextNode(esbOut.getStateRegPrimeDate()));
        OrgStateReg.appendChild(StateRegPrimeDate);

        Element StateRegPrimePlace = doc.createElement("StateRegPrimePlace");
        StateRegPrimePlace.appendChild(doc.createTextNode(esbOut.getStateRegPrimePlace()));
        OrgStateReg.appendChild(StateRegPrimePlace);

        return OrgStateReg;
    }

    private Element createRelPerson (Document doc, ESBObjectOut esbOut) {
        Element RelPerson = doc.createElement("RelPerson");
            Element PersonInfo = doc.createElement("PersonInfo");

                Element FullName = doc.createElement("FullName");
                FullName.appendChild(doc.createTextNode(esbOut.getFullName()));
                PersonInfo.appendChild(FullName);

                Element ContactInfo = doc.createElement("ContactInfo");

                    Element PhoneNum = doc.createElement("PhoneNum");
                    PhoneNum.appendChild(doc.createTextNode(esbOut.getPhoneNum()));
                    ContactInfo.appendChild(PhoneNum);

                    Element EmailAddr = doc.createElement("EmailAddr");
                    EmailAddr.appendChild(doc.createTextNode(esbOut.getEmailAddr()));
                    ContactInfo.appendChild(EmailAddr);

                PersonInfo.appendChild(ContactInfo);

        RelPerson.appendChild(PersonInfo);
        return RelPerson;
    }

    private Element createParentOrg (Document doc, ESBObjectOut esbOut) {
        Element ParentOrg = doc.createElement("ParentOrg");

        Element ParentAccountId = doc.createElement("ParentAccountId");
        ParentAccountId.appendChild(doc.createTextNode(esbOut.getParentAccountId()));
        ParentOrg.appendChild(ParentAccountId);

        Element ParentAccount = doc.createElement("ParentAccountId");
        ParentAccountId.appendChild(doc.createTextNode(esbOut.getParentAccount()));
        ParentOrg.appendChild(ParentAccountId);

        return ParentOrg;
    }

    private Element createListOfPhone (Document doc, ESBObjectOut esbOut) {
        Element ListOfPhone = doc.createElement("ListOfPhone");

        for (ESBObjectOut.PhoneInfo phoneInfo : esbOut.PhoneInfoList) {
            Element PhoneInfo = doc.createElement("PhoneInfo");

                Element PhoneId = doc.createElement("PhoneId");
                PhoneId.appendChild(doc.createTextNode(phoneInfo.getPhoneId()));
                PhoneInfo.appendChild(PhoneId);

                Element PhoneType = doc.createElement("PhoneType");
                PhoneType.appendChild(doc.createTextNode(phoneInfo.getPhoneType()));
                PhoneInfo.appendChild(PhoneType);

                Element Phone = doc.createElement("Phone");
                Phone.appendChild(doc.createTextNode(phoneInfo.getPhone()));
                PhoneInfo.appendChild(Phone);

                Element PhoneComment = doc.createElement("PhoneComment");
                PhoneComment.appendChild(doc.createTextNode(phoneInfo.getPhoneComment()));
                PhoneInfo.appendChild(PhoneComment);

            ListOfPhone.appendChild(PhoneInfo);
        }

        return ListOfPhone;
    }

    private Element createListOfEmail (Document doc, ESBObjectOut esbOut) {
        Element ListOfEmail = doc.createElement("ListOfEmail");

        for (ESBObjectOut.EmailInfo emailInfo : esbOut.EmailInfoList) {
            Element EmailInfo = doc.createElement("EmailInfo");

            Element EmailId = doc.createElement("EmailId");
            EmailId.appendChild(doc.createTextNode(emailInfo.getEmailId()));
            EmailInfo.appendChild(EmailId);

            Element EmailType = doc.createElement("EmailType");
            EmailType.appendChild(doc.createTextNode(emailInfo.getEmailType()));
            EmailInfo.appendChild(EmailType);

            Element MailAddr = doc.createElement("MailAddr");
            MailAddr.appendChild(doc.createTextNode(emailInfo.getMailAddr()));
            EmailInfo.appendChild(MailAddr);

            Element EmailComment = doc.createElement("EmailComment");
            EmailComment.appendChild(doc.createTextNode(emailInfo.getEmailComment()));
            EmailInfo.appendChild(EmailComment);

            ListOfEmail.appendChild(EmailInfo);
        }

        return ListOfEmail;
    }

    private Element createListOfContacts  (Document doc, ESBObjectOut esbOut) {
        Element ListOfContacts = doc.createElement("ListOfContacts");

        for (ESBObjectOut.PersonInfo_LOC personInfo_loc : esbOut.PersonInfoLOCList) {
            Element PersonInfo = doc.createElement("PersonInfo");

                Element PersonName = doc.createElement("PersonName");

                    Element LastName = doc.createElement("LastName");
                    LastName.appendChild(doc.createTextNode(personInfo_loc.getLastName()));
                    PersonName.appendChild(LastName);

                    Element FirstName = doc.createElement("FirstName");
                    FirstName.appendChild(doc.createTextNode(personInfo_loc.getFirstName()));
                    PersonName.appendChild(FirstName);

                    Element MiddleName = doc.createElement("MiddleName");
                    MiddleName.appendChild(doc.createTextNode(personInfo_loc.getMiddleName()));
                    PersonName.appendChild(MiddleName);

                PersonInfo.appendChild(PersonName);

                Element ContactInfo = doc.createElement("ContactInfo");

                    Element EmailAddr = doc.createElement("EmailAddr");
                    EmailAddr.appendChild(doc.createTextNode(personInfo_loc.getEmailAddr()));
                    ContactInfo.appendChild(EmailAddr);

                    Element WorkPhoneNum = doc.createElement("WorkPhoneNum");
                    WorkPhoneNum.appendChild(doc.createTextNode(personInfo_loc.getWorkPhoneNum()));
                    ContactInfo.appendChild(WorkPhoneNum);

                    Element MobilePhoneNum = doc.createElement("MobilePhoneNum");
                    MobilePhoneNum.appendChild(doc.createTextNode(personInfo_loc.getMobilePhoneNum()));
                    ContactInfo.appendChild(MobilePhoneNum);

                PersonInfo.appendChild(ContactInfo);

                Element BirthDt = doc.createElement("BirthDt");
                BirthDt.appendChild(doc.createTextNode(personInfo_loc.getBirthDt()));
                PersonInfo.appendChild(BirthDt);

                Element Gender = doc.createElement("Gender");
                Gender.appendChild(doc.createTextNode(personInfo_loc.getGender()));
                PersonInfo.appendChild(Gender);

                Element OEDCode = doc.createElement("OEDCode");
                OEDCode.appendChild(doc.createTextNode(personInfo_loc.getOEDCode()));
                PersonInfo.appendChild(OEDCode);

                Element ContactId = doc.createElement("ContactId");
                ContactId.appendChild(doc.createTextNode(personInfo_loc.getContactId()));
                PersonInfo.appendChild(ContactId);

            ListOfContacts.appendChild(PersonInfo);
        }

        return  ListOfContacts;
    }

    private Element createListOfAddress  (Document doc, ESBObjectOut esbOut) {
        Element ListOfAddress = doc.createElement("ListOfAddress");

        for (ESBObjectOut.PersonInfo_LOA personInfo_loa : esbOut.PersonInfoLOAList) {
            Element PersonInfo = doc.createElement("PersonInfo");

                Element ContactInfo = doc.createElement("ContactInfo");

                    Element PostAddr = doc.createElement("PostAddr");

                        Element City = doc.createElement("City");
                        City.appendChild(doc.createTextNode(personInfo_loa.getCity()));
                        PostAddr.appendChild(City);

                        Element StateProv = doc.createElement("StateProv");
                        StateProv.appendChild(doc.createTextNode(personInfo_loa.getStateProv()));
                        PostAddr.appendChild(StateProv);

                        Element Area = doc.createElement("Area");
                        Area.appendChild(doc.createTextNode(personInfo_loa.getArea()));
                        PostAddr.appendChild(Area);

                        Element PostalCode = doc.createElement("PostalCode");
                        PostalCode.appendChild(doc.createTextNode(personInfo_loa.getPostalCode()));
                        PostAddr.appendChild(PostalCode);

                        Element Country = doc.createElement("Country");
            Country.appendChild(doc.createTextNode(personInfo_loa.getCountry()));
                        PostAddr.appendChild(Country);


                        Element AddressId = doc.createElement("AddressId");
            AddressId.appendChild(doc.createTextNode(personInfo_loa.getAddressId()));
                        PostAddr.appendChild(AddressId);

                        Element AddrType = doc.createElement("AddrType");
            AddrType.appendChild(doc.createTextNode(personInfo_loa.getAddrType()));
                        PostAddr.appendChild(AddrType);

                        Element Place = doc.createElement("Place");
            Place.appendChild(doc.createTextNode(personInfo_loa.getPlace()));
                        PostAddr.appendChild(Place);

                        Element Street = doc.createElement("Street");
            Street.appendChild(doc.createTextNode(personInfo_loa.getStreet()));
                        PostAddr.appendChild(Street);

                        Element House = doc.createElement("House");
            House.appendChild(doc.createTextNode(personInfo_loa.getHouse()));
                        PostAddr.appendChild(House);

                        Element Corpus = doc.createElement("Corpus");
            Corpus.appendChild(doc.createTextNode(personInfo_loa.getCorpus()));
                        PostAddr.appendChild(Corpus);

                        Element Building = doc.createElement("Building");
            Building.appendChild(doc.createTextNode(personInfo_loa.getBuilding()));
                        PostAddr.appendChild(Building);

                        Element Office = doc.createElement("Office");
            Office.appendChild(doc.createTextNode(personInfo_loa.getOffice()));
                        PostAddr.appendChild(Office);

                        Element Comments = doc.createElement("Comments");
            Comments.appendChild(doc.createTextNode(personInfo_loa.getComments()));
                        PostAddr.appendChild(Comments);

                    ContactInfo.appendChild(PostAddr);

                PersonInfo.appendChild(ContactInfo);

            ListOfAddress.appendChild(PersonInfo);
        }

        return  ListOfAddress;
    }

    private Element createListOfIPP  (Document doc, ESBObjectOut esbOut) {
        Element ListOfIPP = doc.createElement("ListOfAddress");

        for (ESBObjectOut.IPPInfo ippInfo : esbOut.IPPInfoList) {
            Element IPPInfo = doc.createElement("IPPInfo");

                Element IPPId = doc.createElement("IPPId");
                IPPId.appendChild(doc.createTextNode(ippInfo.getIPPId()));
                IPPInfo.appendChild(IPPId);

                Element ProductNumber = doc.createElement("ProductNumber");
            ProductNumber.appendChild(doc.createTextNode(ippInfo.getProductNumber()));
                IPPInfo.appendChild(ProductNumber);

                Element ProdName = doc.createElement("ProdName");
            ProdName.appendChild(doc.createTextNode(ippInfo.getProdName()));
                IPPInfo.appendChild(ProdName);

                Element OverallScore = doc.createElement("OverallScore");
            OverallScore.appendChild(doc.createTextNode(ippInfo.getOverallScore()));
                IPPInfo.appendChild(OverallScore);

                Element ProductOutcome = doc.createElement("ProductOutcome");
            ProductOutcome.appendChild(doc.createTextNode(ippInfo.getProductOutcome()));
                IPPInfo.appendChild(ProductOutcome);

                Element ProductComment = doc.createElement("ProductComment");
            ProductComment.appendChild(doc.createTextNode(ippInfo.getProductComment()));
                IPPInfo.appendChild(ProductComment);

                Element AvgProductAmount = doc.createElement("AvgProductAmount");
            AvgProductAmount.appendChild(doc.createTextNode(ippInfo.getAvgProductAmount()));
                IPPInfo.appendChild(AvgProductAmount);

                Element MaxProductAmount = doc.createElement("MaxProductAmount");
            MaxProductAmount.appendChild(doc.createTextNode(ippInfo.getMaxProductAmount()));
                IPPInfo.appendChild(MaxProductAmount);

                Element ProductAmount = doc.createElement("ProductAmount");
            ProductAmount.appendChild(doc.createTextNode(ippInfo.getProductAmount()));
                IPPInfo.appendChild(ProductAmount);

                Element IPPPriority = doc.createElement("IPPPriority");
            IPPPriority.appendChild(doc.createTextNode(ippInfo.getIPPPriority()));
                IPPInfo.appendChild(IPPPriority);

                Element IPPPDescription = doc.createElement("IPPPDescription");
            IPPPDescription.appendChild(doc.createTextNode(ippInfo.getIPPPDescription()));
                IPPInfo.appendChild(IPPPDescription);

                Element LeaderOrgFullName = doc.createElement("LeaderOrgFullName");
            LeaderOrgFullName.appendChild(doc.createTextNode(ippInfo.getLeaderOrgFullName()));
                IPPInfo.appendChild(LeaderOrgFullName);

                Element Revenue = doc.createElement("Revenue");
            Revenue.appendChild(doc.createTextNode(ippInfo.getRevenue()));
                IPPInfo.appendChild(Revenue);

                Element SBRevenue = doc.createElement("SBRevenue");
            SBRevenue.appendChild(doc.createTextNode(ippInfo.getSBRevenue()));
                IPPInfo.appendChild(SBRevenue);

                Element ProfitLoss = doc.createElement("ProfitLoss");
            ProfitLoss.appendChild(doc.createTextNode(ippInfo.getProfitLoss()));
                IPPInfo.appendChild(ProfitLoss);

                Element OrgRating = doc.createElement("OrgRating");
            OrgRating.appendChild(doc.createTextNode(ippInfo.getOrgRating()));
                IPPInfo.appendChild(OrgRating);

                Element FEAIndicator = doc.createElement("FEAIndicator");
            FEAIndicator.appendChild(doc.createTextNode(ippInfo.getFEAIndicator()));
                IPPInfo.appendChild(FEAIndicator);

                Element CurAdb = doc.createElement("CurAdb");
            CurAdb.appendChild(doc.createTextNode(ippInfo.getCurAdb()));
                IPPInfo.appendChild(CurAdb);

                Element CurValue = doc.createElement("CurValue");
            CurValue.appendChild(doc.createTextNode(ippInfo.getCurValue()));
                IPPInfo.appendChild(CurValue);

                Element DelValue = doc.createElement("DelValue");
            DelValue.appendChild(doc.createTextNode(ippInfo.getDelValue()));
                IPPInfo.appendChild(DelValue);


                Element U1C2 = doc.createElement("U1C2");
            U1C2.appendChild(doc.createTextNode(ippInfo.getU1C2()));
                IPPInfo.appendChild(U1C2);

            ListOfIPP.appendChild(IPPInfo);
        }

        return  ListOfIPP;
    }

    private Element createListOfTeamInfo  (Document doc, ESBObjectOut esbOut) {
        Element ListOfTeamInfo = doc.createElement("ListOfTeamInfo");

            for (ESBObjectOut.TeamInfo teamInfo : esbOut.TeamInfoList) {
                Element TeamInfo = doc.createElement("TeamInfo");


                    Element UserLogin = doc.createElement("UserLogin");
                    UserLogin.appendChild(doc.createTextNode(teamInfo.getUserLogin()));
                    TeamInfo.appendChild(UserLogin);

                    Element PrimaryFlg = doc.createElement("PrimaryFlg");
                    PrimaryFlg.appendChild(doc.createTextNode(teamInfo.getPrimaryFlg()));
                    TeamInfo.appendChild(PrimaryFlg);

                Element PersonInfo = doc.createElement("PersonInfo");
                    Element PersonName = doc.createElement("PersonName");

                    Element LastName = doc.createElement("LastName");
                    LastName.appendChild(doc.createTextNode(teamInfo.getLastName()));
                    PersonName.appendChild(LastName);

                    Element FirstName = doc.createElement("FirstName");
                    FirstName.appendChild(doc.createTextNode(teamInfo.getFirstName()));
                    PersonName.appendChild(FirstName);

                    Element MiddleName = doc.createElement("MiddleName");
                    MiddleName.appendChild(doc.createTextNode(teamInfo.getMiddleName()));
                    PersonName.appendChild(MiddleName);

                PersonInfo.appendChild(PersonName);

                Element ContactInfo = doc.createElement("ContactInfo");

                    Element PhoneNum = doc.createElement("PhoneNum");
                    PhoneNum.appendChild(doc.createTextNode(teamInfo.getPhoneNum()));
                    ContactInfo.appendChild(PhoneNum);

                    Element EmailAddr = doc.createElement("EmailAddr");
                    EmailAddr.appendChild(doc.createTextNode(teamInfo.getEmailAddr()));
                    ContactInfo.appendChild(EmailAddr);

                PersonInfo.appendChild(ContactInfo);

                Element EmploymentHistory = doc.createElement("EmploymentHistory");
                    Element JobTitle = doc.createElement("JobTitle");
                    JobTitle.appendChild(doc.createTextNode(teamInfo.getJobTitle()));
                    EmploymentHistory.appendChild(JobTitle);


                PersonName.appendChild(EmploymentHistory);

                PersonInfo.appendChild(PersonName);
                TeamInfo.appendChild(PersonInfo);

                Element SBRFAccountRole = doc.createElement("SBRFAccountRole");
                SBRFAccountRole.appendChild(doc.createTextNode(teamInfo.getSBRFAccountRole()));
                TeamInfo.appendChild(SBRFAccountRole);


                ListOfTeamInfo.appendChild(TeamInfo);
            }

        return ListOfTeamInfo;
    }
    ///////////////////////////////////////////////////////////

    //Форматы времени
    private static final SimpleDateFormat esbTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
    private static final SimpleDateFormat crmTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");

    //Классы выходных объектов
    private class ESBObjectOut {
        //GetLegalClientProfile>
        String RqUID;
        String RqTm;
        String SPName;
        String SystemId;
        String Method;
        String StatusCode;
        public String getRqTm() {
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

        public String getStatusCode() {
            return StatusCode;
        }

        String ServerStatusCode;
        String getServerStatusCode() {
            return notNullStringReturner(ServerStatusCode);
        }

        String Severity;
        public String getSeverity() {
            return Severity;
        }


        String StatusDesc;
        String getStatusDesc() {
           return notNullStringReturner(StatusDesc);
        }

        List<String> listOrgType = new ArrayList<>(2);
        List<String> listOrgNum = new ArrayList<>(2);

        String IndustNum;
        String getIndustNum() {
            return notNullStringReturner(IndustNum);
        }

        String SubIndustrNum;
        String getSubIndustrNum() {
            return notNullStringReturner(SubIndustrNum);
        }
        String Name;
        public String getName() {
            return Name;
        }
        String LegalFullName;
        String getLegalFullName() {
            return notNullStringReturner(LegalFullName);
        }
        String LicenseNum;
        String getLicenseNum() {
            return notNullStringReturner(LicenseNum);
        }
        String LicenseIssueDt;
        String getLicenseIssueDt() {
            return notNullStringReturner(LicenseIssueDt);
        }
        String TaxId_TIN;
        String getTaxId_TIN() {
            return notNullStringReturner(TaxId_TIN);
        }
        String TaxId_KIO;
        String getTaxId_KIO() {
            return notNullStringReturner(TaxId_KIO);
        }
        String EmployerCode;
        String getEmployerCode() {
            return notNullStringReturner(LegalFullName);
        }
        String EmployerCodeOPF;
        String getEmployerCodeOPF() {
            return notNullStringReturner(EmployerCodeOPF);
        }
        String TaxId_KPPInfo;
        String getTaxId_KPPInfo() {
            return notNullStringReturner(TaxId_KPPInfo);
        }
        String TradeMark;
        String getTradeMark() {
            return notNullStringReturner(TradeMark);
        }
        String TaxOfficeCode;
        String getTaxOfficeCode() {
            return notNullStringReturner(TaxOfficeCode);
        }
        String StateRegPrimeNum;
        String getStateRegPrimeNum() {
            return notNullStringReturner(StateRegPrimeNum);
        }
        String StateRegPrimeDate;
        String getStateRegPrimeDate() {
            return notNullStringReturner(StateRegPrimeDate);
        }
        String StateRegPrimePlace;
        String getStateRegPrimePlace() {
            return notNullStringReturner(StateRegPrimePlace);
        }

        public String getFullName() {
            return FullName;
        }

        String FullName;

        String PhoneNum;
        String getPhoneNum() {
            return notNullStringReturner(PhoneNum);
        }
        String EmailAddr;
        String getEmailAddr() {
            return notNullStringReturner(EmailAddr);
        }
        String Segment;
        String getSegment() {
            return notNullStringReturner(Segment);
        }
        String SubSegment;
        String getSubSegment() {
            return notNullStringReturner(SubSegment);
        }
        String CustStatusCodeCRM;
        String getCustStatusCodeCRM() {
            return notNullStringReturner(CustStatusCodeCRM);
        }
        String ResidentExtStatus;
        String getResidentExtStatus() {
            return notNullStringReturner(ResidentExtStatus);
        }
        String  StopListFlag;    //todo: Boolean?
        String getStopListFlag() {
            return notNullStringReturner(StopListFlag);
        }
        String ParentAccountId;
        String getParentAccountId() {
            return notNullStringReturner(ParentAccountId);
        }
        String ParentAccount;
        String getParentAccount() {
            return notNullStringReturner(ParentAccount);
        }
        String Partnership;
        String getPartnership() {
            return notNullStringReturner(Partnership);
        }
        String Channel;
        String getChannel() {
            return notNullStringReturner(Channel);
        }
        String ReceiveDelevery;
        String getReceiveDelevery() {
            return notNullStringReturner(ReceiveDelevery);
        }
        List<PhoneInfo> PhoneInfoList = new ArrayList<>();
        List<EmailInfo> EmailInfoList = new ArrayList<>();

        String RelDepartment;
        String getRelDepartment() {
            return notNullStringReturner(LegalFullName);
        }
        String Priorities;
        String getPriorities() {
            return notNullStringReturner(Priorities);
        }

        List<LoanInfo> LoanInfoList = new ArrayList<>();
        List<PersonInfo_LOC> PersonInfoLOCList = new ArrayList<>();
        List<PersonInfo_LOA> PersonInfoLOAList = new ArrayList<>();
        List<IPPInfo> IPPInfoList = new ArrayList<>();
        List<TeamInfo> TeamInfoList = new ArrayList<>();
        private class PhoneInfo {
            String PhoneId;
            String PhoneType;
            String Phone;

            public String getPhoneId() {
                return PhoneId;
            }

            public String getPhoneType() {
                return PhoneType;
            }

            public String getPhone() {
                return Phone;
            }

            String PhoneComment;
            String getPhoneComment() {
                return notNullStringReturner(PhoneComment);
            }
        }
        private class EmailInfo {
            String EmailId;
            String EmailType;

            public String getEmailId() {
                return EmailId;
            }

            public String getEmailType() {
                return EmailType;
            }

            public String getMailAddr() {
                return MailAddr;
            }

            String MailAddr;
            String EmailComment;
            String getEmailComment() {
                return notNullStringReturner(EmailComment);
            }
        }
        private class LoanInfo {
            String ProdType;

            public String getProdType() {
                return ProdType;
            }

            String ProductMDMId;
            String getProductMDMId() {
                return notNullStringReturner(ProductMDMId);
            }
            String ProdName;
            String Status;

            public String getProdName() {
                return ProdName;
            }

            String getStatus() {
                return notNullStringReturner(Status);
            }
        }
        private class PersonInfo_LOC {
            //class PersonName {
            String LastName;
            String FirstName;
            String MiddleName;
            String getMiddleName() {
                return notNullStringReturner(MiddleName);
            }
            //}
            //class ContactInfo {
            String EmailAddr;
            String getEmailAddr() {
                return notNullStringReturner(EmailAddr);
            }
            String WorkPhoneNum;
            String getWorkPhoneNum() {
                return notNullStringReturner(WorkPhoneNum);
            }
            String MobilePhoneNum;
            String getMobilePhoneNum() {
                return notNullStringReturner(MobilePhoneNum);
            }
            // }
            String BirthDt;
            String getBirthDt() {
                return notNullStringReturner(BirthDt);
            }
            String Gender;
            String getGender() {
                return notNullStringReturner(Gender);
            }

            public String getLastName() {
                return LastName;
            }

            public String getFirstName() {
                return FirstName;
            }

            public String getContactId() {
                return ContactId;
            }

            String OEDCode;
            String getOEDCode() {
                return notNullStringReturner(OEDCode);
            }
            String ContactId;
        }//26 page
        private class PersonInfo_LOA {
            String City;
            String StateProv;
            String getStateProv() {
                return notNullStringReturner(StateProv);
            }
            String Area;
            String getArea() {
                return notNullStringReturner(Area);
            }
            String PostalCode;
            String getPostalCode() {
                return notNullStringReturner(PostalCode);
            }
            String Country;

            public String getCountry() {
                return Country;
            }

            String AddressId;
            String getAddressId() {
                return notNullStringReturner(AddressId);
            }
            String AddrType;
            String Place;
            String getPlace() {
                return notNullStringReturner(Place);
            }
            String Street;

            public String getAddrType() {
                return AddrType;
            }

            public String getStreet() {
                return Street;
            }

            String House;
            String getHouse() {
                return notNullStringReturner(House);
            }
            String Corpus;
            String getCorpus() {
                return notNullStringReturner(Corpus);
            }
            String Building;
            String getBuilding() {
                return notNullStringReturner(Building);
            }
            String Office;

            public String getCity() {
                return City;
            }

            String getOffice() {
                return notNullStringReturner(Office);
            }
            String Comments;
            String getComments() {
                return notNullStringReturner(Comments);
            }
        }
        private class IPPInfo {
            String IPPId;
            String getIPPId() {
                return notNullStringReturner(IPPId);
            }
            String ProductNumber;
            String ProdName;
            String OverallScore;
            String getOverallScore() {
                return notNullStringReturner(OverallScore);
            }
            String ProductOutcome;

            public String getProdName() {
                return ProdName;
            }

            String getProductOutcome() {
                return notNullStringReturner(ProductOutcome);
            }
            String ProductComment;
            String getProductComment() {
                return notNullStringReturner(ProductComment);
            }
            //27 page
            String AvgProductAmount;
            String getAvgProductAmount() {
                return notNullStringReturner(AvgProductAmount);
            }
            String MaxProductAmount;
            String getMaxProductAmount() {
                return notNullStringReturner(MaxProductAmount);
            }
            String ProductAmount;
            String getProductAmount() {
                return notNullStringReturner(ProductAmount);
            }
            String IPPPriority;
            String getIPPPriority() {
                return notNullStringReturner(IPPPriority);
            }
            String IPPPDescription; //todo опечатка?
            String getIPPPDescription() {
                return notNullStringReturner(IPPPDescription);
            }
            String LeaderOrgFullName;
            String getLeaderOrgFullName() {
                return notNullStringReturner(LeaderOrgFullName);
            }
            String Revenue;
            String getRevenue() {
                return notNullStringReturner(Revenue);
            }
            String SBRevenue;
            String getSBRevenue() {
                return notNullStringReturner(SBRevenue);
            }
            String ProfitLoss;
            String getProfitLoss() {
                return notNullStringReturner(ProfitLoss);
            }
            String OrgRating;
            String getOrgRating() {
                return notNullStringReturner(OrgRating);
            }
            String FEAIndicator;
            String getFEAIndicator() {
                return notNullStringReturner(FEAIndicator);
            }
            String CurAdb;

            public String getProductNumber() {
                return ProductNumber;
            }

            String getCurAdb() {

                return notNullStringReturner(CurAdb);
            }
            String CurValue;
            String getCurValue() {
                return notNullStringReturner(CurValue);
            }
            String DelValue;
            String getDelValue() {
                return notNullStringReturner(DelValue);
            }
            String U1C2;
            String getU1C2() {
                return notNullStringReturner(U1C2);
            }
        }
        private class TeamInfo {
            String UserLogin;
            String PrimaryFlg;
            String LastName;
            String FirstName;
            String MiddleName;

            public String getPrimaryFlg() {
                return PrimaryFlg;
            }

            public String getLastName() {
                return LastName;
            }

            public String getFirstName() {
                return FirstName;
            }

            public String getMiddleName() {
                return MiddleName;
            }

            //<ContactInfo>
            String PhoneNum;
            String getPhoneNum() {
                return notNullStringReturner(PhoneNum);
            }
            String EmailAddr;

            public String getUserLogin() {
                return UserLogin;
            }

            String getEmailAddr() {
                return notNullStringReturner(EmailAddr);
            }

            String JobTitle;
            String getJobTitle() {
                return notNullStringReturner(JobTitle);
            }
            String SBRFAccountRole;
            String getSBRFAccountRole() {
                return notNullStringReturner(SBRFAccountRole);
            }
        }

        private String notNullStringReturner(String s) {
            return s != null ? s : "";
        }

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
