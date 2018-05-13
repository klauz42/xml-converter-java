package ru.klauz42.xmlvalidator;


import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FilesMonitor {

    private Map<Path, FileAction> actionMap = new HashMap<>();

    private enum FileAction {  DELETED, MOVE, TRYAGAIN, STOP, OK, SOBIG, CONVERTED}

    private static final Logger LOGGER =
            Logger.getLogger(FilesMonitor.class.getName());

    private Path rqDir;
    private Path rsDir;
    private Path dlqDir;

    FilesMonitor(Path rqDir, Path rsDir, Path dlqDir){
        this.rqDir = rqDir;
        this.rsDir = rsDir;
        this.dlqDir = dlqDir;
    }

    private FileAction createAction(Path inFile) {
        //try в try, т.к. при исключении приложение должно закрыться,
        //файл перемещается в dlq, это действие обрабатывается во втором try-блоке
        String fileName = inFile.getFileName().toString();
        FileAction action = null;
        if (!checkFileUpload(inFile, 4, 10, 500)) return FileAction.SOBIG;
        else try {
            ParserXML parserXML = new ParserXML(inFile.toAbsolutePath().toString());
            parserXML.convertXML(rsDir.toAbsolutePath() + File.separator + fileName);
//          ESBParser.ESBObj o = new ESBParser(inFile.toAbsolutePath().toString()).parseESBXMLToESBObj();

//           CRMWriter writer = new CRMWriter(o,
//                    rsDir.toAbsolutePath() + File.separator + fileName.substring(0,
//                            fileName.length() - 4) + "Rs.xml");
//           writer.WriteXML();
            LOGGER.fine(fileName + " has been successfully saved");
            return FileAction.CONVERTED;
        } catch (SAXException e) {
            LOGGER.log(Level.SEVERE, "Uploaded file is not XML, SAXException occurred", e);
            return FileAction.MOVE;
        } catch (ParseException |
                IOException |
                ParserConfigurationException |
                TransformerException |
                ParserXML.FormatException |
                XPathExpressionException e) {
            LOGGER.log(Level.SEVERE, e.getClass().getName() + " occurred", e);
            return FileAction.MOVE;
        }
    }

    private void actionHandle () {
        Map<Path, FileAction> tmp = new HashMap<>();

        for (Map.Entry<Path, FileAction> entry : actionMap.entrySet()) {
            FileAction value = entry.getValue();
            switch (value) {
                case CONVERTED:
                case SOBIG:
                    FileAction convAction = deleteFile(entry.getKey());
                    tmp.put(entry.getKey(), convAction);
                break;
                case MOVE:
                    FileAction moveAction = moveFileToDlq(entry.getKey());
                    tmp.put(entry.getKey(), moveAction);
                break;
                case OK:
                case DELETED:
                    actionMap.remove(entry.getKey());
                break;
            }
        }
        actionMap.putAll(tmp);
    }
    //переместить файл в dlq
    private FileAction moveFileToDlq(Path inFile) {
        Path dlqFile = Paths.get(dlqDir.toAbsolutePath() + File.separator + inFile.getFileName());
        try {
            Files.move(inFile, dlqFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info(inFile.getFileName() + " has been moved to dlq");
            return FileAction.OK;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException occurred while moving " + inFile.getFileName(), e);
            return FileAction.MOVE;
        }
    }

    private FileAction deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
            LOGGER.info(file.getFileName() + " has been deleted");
            return FileAction.DELETED;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException occurred while deleting " + file.getFileName(), e);
            return FileAction.OK;
        }
    }

    /*
    needAtempts - сколько циклов необходимо не изменяться размеру файла, чтобы он считался заагруженным
    maxLoops - сколько раз файл может менять размер за проверку (не очень подходящая мера для больших файлов)
    sleepTime - время между проверками в мс
    !!!максимальное время выполнения проверки больше, чем (needAttempts-1)*maxLooops*sleepTime/1000 c
     */
    private boolean checkFileUpload(Path pathToFile, int attempCount, int maxLoops, long sleepTime) {
        File file = new File(pathToFile.toAbsolutePath().toString());
        if(!file.exists()) return false;
        for (int i = 0; i < maxLoops; i++) {
            int successAttempts = 0;
            long size = file.length();
            for (int j = 0; j < attempCount; j++){
                if(size == file.length()) {
                    successAttempts++;
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        LOGGER.warning("InterruptedException while checking file");
                    }
                }
                else break;
            }
            if (successAttempts == attempCount) return true;
        }
        return false;
    }


    public void monitor() {
        //Мониторинг папки с запросами
        WatchService watchService = null;
        try {
            watchService = rqDir.getFileSystem().newWatchService();
            rqDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"IOException while creating of watcher occurred", e);
            System.exit(1);
        }
        while (true) {

            actionHandle();

            WatchKey key = null;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE,"InterruptedException while taking of key from watcher occurred", e);
                continue;
            }
            // Итерации для каждого события
            for (WatchEvent event : key.pollEvents()) {
                switch (event.kind().name()) {
                    case "OVERFLOW":
                        LOGGER.info("Watcher lost some events");
                        break;
                    case "ENTRY_CREATE":
                        LOGGER.info(event.context() + " is uploaded");
                        String fileName = event.context().toString();
                        String absolutePathToFile = rqDir.toAbsolutePath().toString() +
                                File.separator + fileName;
                        Path createdFilePath = Paths.get(absolutePathToFile); //что за файл
                        FileAction createFileAction = createAction(createdFilePath);
                        actionMap.put(createdFilePath, createFileAction);

                        break;
                    case "ENTRY_DELETE":
                        LOGGER.info(event.context().toString() +
                                " has been successfully deleted");
                        break;
                }
            }
            key.reset();
        }
    }
}
