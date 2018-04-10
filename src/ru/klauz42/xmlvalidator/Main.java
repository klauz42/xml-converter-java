package ru.klauz42.xmlvalidator;


import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static final Logger LOGGER =
            Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        LOGGER.info("Starting of program");
        Path rqDir = Paths.get("data" + File.separator + "in");
        String dlqDir = "data" + File.separator + "dlq" + File.separator;
        String rsDir = "data" + File.separator + "out" + File.separator;
        WatchService watchService = null;
        try {
            watchService = rqDir.getFileSystem().newWatchService();
            rqDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"IOException while creating of watcher occurred", e);
        }
        while (true) {
            WatchKey key = null;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE,"InterruptedException while taking of key from watcher occurred", e);
            }
            // Итерации для каждого события
            for (WatchEvent event : key.pollEvents()) {
                switch (event.kind().name()) {
                    case "OVERFLOW":
                        LOGGER.info("Watcher lost some events");
                        break;
                    case "ENTRY_CREATE":
                        String fileName = event.context().toString();
                        String absolutePathToFile = rqDir.toAbsolutePath().toString() +
                                File.separator + fileName;
                        LOGGER.info(event.context() + " uploaded");
                        try {
                            try {
                                ESBParser.ESBObj o = new ESBParser(absolutePathToFile).parseESBXMLToESBObj();
                                CRMWriter writer = new CRMWriter(o,
                                        rsDir + fileName.substring(0, fileName.length() - 4) + "Rs.xml");
                                writer.WriteXML();
                                LOGGER.fine(fileName + " has been successfully saved");
                            } catch (ParseException e) {
                                LOGGER.log(Level.SEVERE, "ParseException occurred", e);
                                throw e;
                            } catch (IOException e) {
                                LOGGER.log(Level.SEVERE, "IOException occurred", e);
                                throw e;
                            } catch (SAXException e) {
                                LOGGER.log(Level.SEVERE, "Uploaded file is not XML, SAXException occurred", e);
                                throw e;
                            } catch (ParserConfigurationException e) {
                                LOGGER.log(Level.SEVERE, "ParserConfigurationException occurred", e);
                                throw e;
                            } catch (TransformerException e) {
                                LOGGER.log(Level.SEVERE, "TransformerException occurred", e);
                                throw e;
                            } catch (ESBParser.ESBFormatException e) {
                                LOGGER.log(Level.SEVERE, "ESBFormat exception occurred", e);
                                throw e;
                            }
                        } catch (Exception exc) {
                            Path inFile = Paths.get(absolutePathToFile);
                            Path dlqFile = Paths.get(dlqDir + fileName);
                            try {
                                Files.move(inFile, dlqFile, StandardCopyOption.REPLACE_EXISTING);
                                LOGGER.info(fileName + " has been moved to dlq");
                            } catch (IOException e) {
                                LOGGER.log(Level.SEVERE, "IOException occurred while moving " + fileName, e);
                            } finally {
                                try {
                                    watchService.close();
                                } catch (IOException e) {
                                    LOGGER.log(Level.SEVERE, "IOException occurred while moving " + fileName, e);
                                } finally {
                                    LOGGER.info("Stopping of program");
                                    System.exit(1);
                                }
                            }

                        }

                        break;
                }
            }
            key.reset();
        }
    }
}

