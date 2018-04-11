package ru.klauz42.xmlvalidator;



import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static final Logger LOGGER =
            Logger.getLogger(Main.class.getName());

    private static void createDirectories(Path ... paths) {
        for (Path path: paths) {
            if(!Files.isDirectory(path)) {
                try {
                    Files.createDirectory(path);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE,"IOException while creating of " + path.getFileName(), e);
                    LOGGER.info("Stopping of program");
                    System.exit(1);
                }
            }
        }
    }

    public static void main(String[] args) {
        LOGGER.info("Starting of program");
        Path rqDir = Paths.get("data" + File.separator + "in");
        Path dlqDir = Paths.get("data" + File.separator + "dlq");
        Path rsDir = Paths.get("data" + File.separator + "out");
        //если директории не существует - создать
        createDirectories(rqDir, rsDir, dlqDir);

        FilesMonitor filesMonitor = new FilesMonitor(rqDir, rsDir, dlqDir);
        filesMonitor.monitor();
    }

}

