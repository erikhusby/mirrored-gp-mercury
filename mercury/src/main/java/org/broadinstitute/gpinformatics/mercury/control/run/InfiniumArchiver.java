package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Archives Infinium idats and other files at some interval (e.g. 10 days) after the pipeline starter has been called.
 */
public class InfiniumArchiver {

    private static final Log log = LogFactory.getLog(InfiniumArchiver.class);
    private static final String DECODE_DATA_NAME = "Decode_Data";
    private static final String ARCHIVED_DIR_NAME = "Archived";

    /*
     How to test this?
     Could create vessel and files
     Check vessel is found
     Check directory is archived
     */

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;


    /**
     * Finds chips that have LabEventType.INFINIUM_AUTOCALL_ALL_STARTED events that are old enough that the chips are
     * likely to have been analyzed.
     * @return list of chip barcodes (not entities, because this method clears the session periodically)
     */
    public List<String> findChipsToArchive() {
        List<LabVessel> infiniumChips = labVesselDao.findAllWithEventButMissingAnother(
                LabEventType.INFINIUM_AUTOCALL_SOME_STARTED,
                LabEventType.INFINIUM_ARCHIVED);
        Date tenDaysAgo = new Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 10L);
        List<String> barcodes = new ArrayList<>();
        for (LabVessel labVessel : infiniumChips) {
            barcodes.add(labVessel.getLabel());
        }

        // To avoid running out of memory, break the list into small chunks, and clear the Hibernate session.
        List<Collection<String>> split = BaseSplitter.split(barcodes, 10);
        List<String> chipsToArchive = new ArrayList<>();
        for (Collection<String> strings : split) {
            List<LabVessel> chips = labVesselDao.findByListIdentifiers(new ArrayList<>(strings));
            for (LabVessel chip : chips) {
                for (LabEvent labEvent : chip.getInPlaceLabEvents()) {
                    if (labEvent.getLabEventType() == LabEventType.INFINIUM_AUTOCALL_ALL_STARTED) {
                        String forwardToGap = LabEventFactory.determineForwardToGap(labEvent, chip, productEjb,
                                attributeArchetypeDao);
                        if (Objects.equals(forwardToGap, "N")) {
                            if (labEvent.getEventDate().before(tenDaysAgo)) {
                                chipsToArchive.add(chip.getLabel());
                            }
                        }
                        break;
                    }
                }
            }
            labVesselDao.clear();
        }

        return chipsToArchive;
    }

    /**
     * This code is inspired by GAP's InfiniumArchiveBean.archiveChip.
     */
    public void archiveChip(String barcode) {
        log.info("Archiving Chip Barcode " + barcode);
        boolean isSuccessful = true;
        File baseDataDir = new File(infiniumStarterConfig.getDataPath());
        File dataDir = new File(baseDataDir, barcode);

        String baseDecodeDataDir = infiniumStarterConfig.getDecodeDataPath();
        File decodeDataDir = new File(baseDecodeDataDir, barcode);
        File newDecodeDataDir = new File(dataDir, DECODE_DATA_NAME);

        // delete jpg files
        String[] extensions = {"jpg"};
        Iterator<File> iter = FileUtils.iterateFiles(dataDir, extensions, false);
        while (iter.hasNext()) {
            File jpgFile = iter.next();
            if (jpgFile.getName().endsWith("jpg")) {
                if (!jpgFile.delete()) {
                    isSuccessful = false;
                }
            }
        }

        if (isSuccessful) {
            if (decodeDataDir.exists()) {
                // Copy the Decode_data for this barcode intoto the data directory
                // NOTE - I am copying, rather than moving, so I can back out if need be...
                if (newDecodeDataDir.mkdir()) {
                    File newNewDecodeDataDir = new File(newDecodeDataDir, barcode); // Oooh another level...
                    if (newNewDecodeDataDir.mkdir()) {
                        try {
                            FileUtils.copyDirectory(decodeDataDir, newNewDecodeDataDir);
                        } catch (IOException ioe) {
                            isSuccessful = false;
                            log.error("Error copying " + decodeDataDir + " to " + newNewDecodeDataDir);
                        }
                    } else {
                        isSuccessful = false;
                        log.error("Failed to make directory: " + newNewDecodeDataDir.getAbsolutePath());
                    }
                } else {
                    isSuccessful = false;
                    log.error("Failed to make directory: " + newDecodeDataDir.getAbsolutePath());
                }
            }
        }

        // Create zip file in tmp
        String tempDir = System.getProperty("java.io.tmpdir");
        String zipFileName = barcode + ".zip";
        File zipFile = new File(tempDir, zipFileName);
        if (isSuccessful) {
            // If here, the contents of the data directory is complete for archiving
            isSuccessful = zipDir(dataDir, zipFile, Boolean.TRUE);
        }

        // Copy archive
        File newZipFileLocn = new File(infiniumStarterConfig.getArchivePath(), zipFile.getName());
        if (isSuccessful) {
            // If here, we have successfully created the zip file in the temp directory....
            // Move the zip file to archive pending..
            if (!newZipFileLocn.exists()) {
                try {
                    FileUtils.copyFile(zipFile, newZipFileLocn);
                } catch (IOException ioe) {
                    isSuccessful = false;
                    log.error("Error copying " + zipFile + " to " + newZipFileLocn);
                }
                if (zipFile.length() == newZipFileLocn.length()) {
                    // TODO - MD5 or some sort of check...
//                isSuccessful = true;
                } else {
                    isSuccessful = false;
                    log.error("Size of '" + newZipFileLocn.getAbsolutePath()
                            + "' Does not agree with previous - error in transfer? - deleting..");
                    newZipFileLocn.delete();
                }
            } else {
                isSuccessful = false;
                log.error(newZipFileLocn + " already exists!");
            }
        }

        // delete tmp
        if (zipFile.exists()) {
            zipFile.delete();
        }
        // todo update status to ARCHIVED?

        // rename dir
        // delete dir
        if (isSuccessful) {
            // If here, everything worked.  Time for clean up... gulp...
            if (dataDir.exists()) {
//                deleteDirectory(dataDir);
                File archivedDataDir = new File(new File(baseDataDir, ARCHIVED_DIR_NAME), barcode);
                if (dataDir.renameTo(archivedDataDir)) {
                    // TODO - I am doing it this way (move and then delete) as I was not able to delete the root directoy...
                    deleteDirectory(archivedDataDir);
                } else {
                    log.error("Failed to rename " + dataDir + " to " + archivedDataDir);
                }
            }

            if (decodeDataDir.exists()) {
                // We only remove the decode data if:
                // we are NOT doing rescan archiving AND the chip is not flagged for rescan
                // or
                // we ARE doing rescan archiving AND the chip has been flagged for rescan..
                // (isn't that an XOR?)
//                    if ((!rescanArchiving && !chipFlaggedForRescan) ||
//                            (rescanArchiving && chipFlaggedForRescan)) {
                    deleteDirectory(decodeDataDir);
//                    File archivedDecodeDataDir = new File(new File(baseDecodeDataDir, ARCHIVED_DIR_NAME), barcode);
//                    if (!decodeDataDir.renameTo(archivedDecodeDataDir)) {
//                        log.error("Failed to rename " + decodeDataDir + " to " + archivedDecodeDataDir);
//                    }
//                    }
            }
        } else {
            // If here, something failed - do clean up...
            // Note that I am NOT deleting the zip file if found on pending.  Not sure why it would be there, but....
//            if (newZipFileLocn.exists()) {
//                newZipFileLocn.delete();
//            }
            if (newDecodeDataDir.exists()) {
                deleteDirectory(newDecodeDataDir);
            }
        }
    }

    private static boolean zipDir(File dir2Zip, File zipFile, boolean useFullPath) {
        boolean isSuccessful = false;

        if (zipFile.exists()) {
            log.error("Zip File " + zipFile.getAbsolutePath() + " already exists!");
            return false;
        }
        if (!dir2Zip.exists()) {
            log.error("Directory " + dir2Zip.getAbsolutePath() + " Does not exist!");
            return false;
        }
        if (!dir2Zip.isDirectory()) {
            log.error("Directory " + dir2Zip.getAbsolutePath() + " Is not a directory!");
            return false;
        }

        try {
            // create a ZipOutputStream to zip the data to
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
            // assuming that there is a directory named inFolder (If there isn't
            // create one) in the same directory as the one the code runs from,
            // call the zipDir method
            isSuccessful = zipDir(dir2Zip.getAbsolutePath(), zos, useFullPath);

            // close the stream
            zos.close();
        } catch (IOException ioe) {
            log.error("IOException creating zip");
            isSuccessful = false;
        } finally {
            if (!isSuccessful) {
                if (zipFile.exists()) {
                    zipFile.delete();
                }
            }
        }
        return isSuccessful;
    }

    private static boolean zipDir(String dir2zip, ZipOutputStream zos, boolean useFullPath) {
        boolean isSuccessful = false;

        try {
            // create a new File object based on the directory we have to zip
            File zipDir = new File(dir2zip);
            // get a listing of the directory content
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn;
            // loop through dirList, and zip the files
            for (String aDirList : dirList) {
                File f = new File(zipDir, aDirList);
                if (f.isDirectory()) {
                    // if the File object is a directory, call this
                    // function again to add its content recursively
                    String filePath = f.getPath();
                    zipDir(filePath, zos, useFullPath);
                    // loop again
                    continue;
                }

                // if we reached here, the File object f was not a directory
                // create a FileInputStream on top of f
                FileInputStream fis = new FileInputStream(f);

                // create a new zip entry
                ZipEntry anEntry;

                if (useFullPath) {
                    anEntry = new ZipEntry(f.getPath());
                } else {
                    anEntry = new ZipEntry(f.getName());
                }
                // place the zip entry in the ZipOutputStream object
                zos.putNextEntry(anEntry);

                // now write the content of the file to the ZipOutputStream
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                // close the Stream
                fis.close();
                isSuccessful = true;
            }
        } catch (IOException ioe) {
            log.error("IO Error zip directory '" + dir2zip + "' " + ioe.getMessage());
        }
        return isSuccessful;
    }

    private boolean deleteDirectory(File dir) {
        boolean isSuccessful = true;
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException ioe) {
            isSuccessful = false;
            log.error("Error deleting directory " + dir);
        }
        return isSuccessful;
    }

}
