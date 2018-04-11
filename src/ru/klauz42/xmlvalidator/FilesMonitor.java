package ru.klauz42.xmlvalidator;


import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FilesMonitor {

    public static final Logger LOGGER =
            Logger.getLogger(FilesMonitor.class.getName());

    private Path rqDir;
    private Path rsDir;
    private Path dlqDir;

    FilesMonitor(Path rqDir, Path rsDir, Path dlqDir){
        this.rqDir = rqDir;
        this.rsDir = rsDir;
        this.dlqDir = dlqDir;
    }

    private void createAction(WatchEvent event, WatchService watchService) {
        String fileName = event.context().toString();
        String absolutePathToFile = rqDir.toAbsolutePath().toString() +
                File.separator + fileName;
        //try в try, т.к. при исключении приложение должно закрыться,
        //файл перемещается в dlq, это действие обрабатывается во втором try-блоке
        try {
            try {
                ESBParser.ESBObj o = new ESBParser(absolutePathToFile).parseESBXMLToESBObj();
                CRMWriter writer = new CRMWriter(o,
                        rsDir.toAbsolutePath() + File.separator + fileName.substring(0,
                                fileName.length() - 4) + "Rs.xml");
                writer.WriteXML();
                LOGGER.fine(fileName + " has been successfully saved");
            } catch (SAXException e) {
                LOGGER.log(Level.SEVERE, "Uploaded file is not XML, SAXException occurred", e);
                throw e;
            } catch (ParseException |
                    IOException |
                    ParserConfigurationException |
                    TransformerException |
                    ESBParser.ESBFormatException e) {
                LOGGER.log(Level.SEVERE, e.getClass().getName() + " occurred", e);
                throw e;
            }
        } catch (Exception exc) {
            Path inFile = Paths.get(absolutePathToFile);
            moveFileToDlq(absolutePathToFile, watchService);
        }
    }

    //переместить файл в dlq и закрыть приложение
    private void moveFileToDlq(String absolutePathToFile, WatchService watchService) {
        Path inFile = Paths.get(absolutePathToFile);
        Path dlqFile = Paths.get(dlqDir.toAbsolutePath() + File.separator + inFile.getFileName());
        try {
            Files.move(inFile, dlqFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info(inFile.getFileName() + " has been moved to dlq");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException occurred while moving " + inFile.getFileName(), e);
        } finally {
            try {
                watchService.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "IOException occurred while moving " + inFile.getFileName(), e);
            } finally {
                LOGGER.info("Stopping of program");
                System.exit(1);
            }
        }
    }

    /*
    needAtempts - сколько циклов необходимо не изменяться размеру файла, чтобы он считался заагруженным
    maxLoops - сколько раз файл может менять размер за проверку (не очень подходящая мера для больших файлов)
    sleepTime - время между проверками в мс
    !!!максимальное время выполнения проверки больше, чем (needAttempts-1)*maxLooops*sleepTime/1000 c
     */
    private boolean checkFileUpload(String pathToFile, int needAttemps, int maxLoops, long sleepTime) {
        File file = new File(pathToFile);
        int successAttempts;
        if(!file.exists()) return false;
        for (int i = 0; i < maxLoops; i++) {
            successAttempts = 0;
            long size = file.length();
            for (int j = 0; j < needAttemps; j++){
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
            if (successAttempts == needAttemps) return true;
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
                        LOGGER.info(event.context() + " is uploaded");

                        String fileName = event.context().toString();
                        String absolutePathToFile = rqDir.toAbsolutePath().toString() +
                                File.separator + fileName;
                        System.out.println(Paths.get(absolutePathToFile).getFileName());

                        if(checkFileUpload(absolutePathToFile, 4, 10, 500)) {
                            createAction(event, watchService);
                            try {
                                Files.delete(Paths.get(absolutePathToFile));
                            } catch (IOException e) {
                                LOGGER.log(Level.SEVERE, "Can't delete file");
                            }
                        }
                        else moveFileToDlq(absolutePathToFile, watchService);

                        break;
                    case "ENTRY_DELETE":
                        LOGGER.info(event.context().toString() +
                                " has been successfully deleted after conversion");
                        break;
                }
            }
            key.reset();
        }
    }
}
