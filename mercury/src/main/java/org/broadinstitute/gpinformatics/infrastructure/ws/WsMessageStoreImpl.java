package org.broadinstitute.gpinformatics.infrastructure.ws;

import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.infrastructure.deckmsgs.DeckMessagesConfig;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.xml.bind.UnmarshalException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.Format;
import java.util.Date;

/**
 * Handles server-side storage of the text of messages from liquid handling decks.  If the database persistence fails,
 * we must have the text of the message, so we can troubleshoot and resubmit.
 */
@Dependent
@Default
public class WsMessageStoreImpl implements WsMessageStore {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private DeckMessagesConfig deckMessagesConfig;

    /** Name of the inbox directory.  There will be one directory per day below this, each containing message files. */
    public static final String INBOX_DIR = "inbox";

    /** Name of the error directory, where messages with exceptions are stored */
    public static final String ERROR_DIR = "error";

    /** Name of the ignore error directory, where messages with ignored exceptions are stored */
    public static final String IGNORE_DIR = "ignore";

    /** One inbox directory per day */
    private final Format directoryFormat = FastDateFormat.getInstance("yyyyMMdd");

    /** Part of the file name.  Millisecond resolution should be enough to avoid collisions, BettaLIMS has worked like this for years */
    private final Format fileFormat = FastDateFormat.getInstance("yyyyMMdd_HHmmssSSS");

    /** For CDI */
    WsMessageStoreImpl() {
    }

    public WsMessageStoreImpl(DeckMessagesConfig deckMessagesConfig) {
        this.deckMessagesConfig = deckMessagesConfig;
    }

    /**
     * Stores the message; even if database persistence fails, we have the text of the message.
     *
     * @param resourceType e.g bettalims, allows message for different web services to have their own directory structure
     * @param message text of the message
     * @param receivedDate when the message was received
     */
    @Override
    public void store(String resourceType, String message, Date receivedDate) {
        String directoryTime = directoryFormat.format(receivedDate);
        String directoryName = deckMessagesConfig.getMessageStoreDirRoot() + File.separator + resourceType +
                               File.separator + INBOX_DIR + File.separator + directoryTime;
        File directory = new File(directoryName);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                // mkdirs can fail if two threads attempt it simultaneously, so try again
                if (!directory.mkdirs()) {
                    throw new RuntimeException("Failed to create day directory");
                }
            }
        }

        String fileTime = fileFormat.format(receivedDate);
        File messageFile = new File(directory, fileTime + ".xml");
        try {
            // todo jmt handle file name collisions?
            try (PrintWriter printWriter = new PrintWriter(new FileOutputStream(messageFile))) {
                printWriter.write(message);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method is called when an exception occurs during processing or persistence of the message.
     *
     * @param resourceType e.g bettalims, allows message for different web services to have their own directory
     *                     structure
     * @param message      text of the message
     * @param receivedDate when the message was received
     * @param exception    the exception to log
     */
    @Override
    public void recordError(String resourceType, String message, Date receivedDate, Exception exception) {
        String directoryName;
        if (exception instanceof UnmarshalException || message.contains("DetectorPlateLoaded")) {
            // Mercury doesn't currently handle Coral messages or detector plates.
            directoryName = IGNORE_DIR;
        } else {
            directoryName = ERROR_DIR;
        }
        File errorDirectory =
                new File(deckMessagesConfig.getMessageStoreDirRoot() + File.separator + resourceType, directoryName);
        if (!errorDirectory.exists()) {
            if (!errorDirectory.mkdirs()) {
                // mkdirs can fail if two threads attempt it simultaneously, so try again
                if (!errorDirectory.mkdirs()) {
                    throw new RuntimeException("Failed to create error directory for " + resourceType);
                }
            }
        }

        String fileTime = fileFormat.format(receivedDate);
        File messageFile = new File(errorDirectory, fileTime + ".xml");
        try {
            try (PrintWriter printWriter = new PrintWriter(new FileOutputStream(messageFile))) {
                printWriter.write(message);
                printWriter.println();
                exception.printStackTrace(printWriter);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
