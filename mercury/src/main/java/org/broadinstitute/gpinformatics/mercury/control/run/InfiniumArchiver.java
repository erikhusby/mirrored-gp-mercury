package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.run.InfiniumRunFinder;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Archives Infinium idats and other files at some interval (e.g. 10 days) after the pipeline starter has been called.
 */
@Stateless
@Dependent
@TransactionManagement(TransactionManagementType.BEAN)
public class InfiniumArchiver {

    private static final Log log = LogFactory.getLog(InfiniumArchiver.class);
    private static final String DECODE_DATA_NAME = "Decode_Data";
    private static final String ARCHIVED_DIR_NAME = "Archived";

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private UserBean userBean;

    @Resource
    private EJBContext ejbContext;

    private static final AtomicBoolean busy = new AtomicBoolean(false);

    public void archive() {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        try {
            userBean.login("seqsystem");
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.add(Calendar.DAY_OF_YEAR, -10);
            List<Pair<String, Boolean>> chipsToArchive = findChipsToArchive(
                    infiniumStarterConfig.getNumChipsPerArchivePeriod(), gregorianCalendar.getTime());
            for (Pair<String, Boolean> stringBooleanPair : chipsToArchive) {
                UserTransaction utx = ejbContext.getUserTransaction();
                try {
                    boolean archived = true;
                    if (stringBooleanPair.getRight()) {
                        archived = archiveChip(stringBooleanPair.getLeft(), infiniumStarterConfig);
                    }
                    // else assume GAP has archived it
                    if (archived) {
                        utx.begin();
                        LabVessel chip = labVesselDao.findByIdentifier(stringBooleanPair.getLeft());
                        chip.addInPlaceEvent(new LabEvent(LabEventType.INFINIUM_ARCHIVED, new Date(), LabEvent.UI_EVENT_LOCATION,
                                1L, bspUserList.getByUsername("seqsystem").getUserId(), LabEvent.UI_PROGRAM_NAME));
                        // The commit doesn't cause a flush (not clear why), so we must do it explicitly.
                        labVesselDao.flush();
                        utx.commit();
                    }
                } catch (Throwable e) {
                    try {
                        utx.rollback();
                    } catch (Throwable e1) {
                        log.error("Error rolling back", e1);
                    }
                    log.error("Failed to process chip " + stringBooleanPair.getLeft(), e);
                    // todo jmt email?
                    throw new RuntimeException(e);
                }
            }
        } finally {
            busy.set(false);
        }
    }

    /**
     * Finds chips that have INFINIUM_AUTOCALL_SOME_STARTED and INFINIUM_AUTOCALL_ALL_STARTED events that are old
     * enough that the chips are likely to have been analyzed.  We're checking for both events to avoid chips that
     * were messaged through GAP only.
     * @return list of chip barcodes (not entities, because this method clears the session periodically) and true
     * if Mercury (rather than GAP) is responsible for archiving.
     */
    public List<Pair<String, Boolean>> findChipsToArchive(int limit, Date archiveDate) {
        List<LabVessel> infiniumChips = labVesselDao.findAllWithEventButMissingAnother(
                InfiniumRunFinder.PIPELINE_TRIGGER_EVENT_TYPES,
                LabEventType.INFINIUM_ARCHIVED);
        List<String> barcodes = new ArrayList<>();
        for (LabVessel labVessel : infiniumChips) {
            barcodes.add(labVessel.getLabel());
        }
        labVesselDao.clear();

        // To avoid running out of memory, break the list into small chunks, and clear the Hibernate session.
        List<Collection<String>> split = BaseSplitter.split(barcodes, 10);
        List<Pair<String, Boolean>> chipsToArchive = new ArrayList<>();
        int i = 0;
        for (Collection<String> strings : split) {
            List<LabVessel> chips = labVesselDao.findByListIdentifiers(new ArrayList<>(strings));
            for (LabVessel chip : chips) {
                for (LabEvent labEvent : chip.getInPlaceLabEvents()) {
                    if (labEvent.getLabEventType() == LabEventType.INFINIUM_AUTOCALL_ALL_STARTED) {
                        if (labEvent.getEventDate().before(archiveDate)) {
                            String forwardToGap = LabEventFactory.determineForwardToGap(labEvent, chip, productEjb,
                                    attributeArchetypeDao);
                            chipsToArchive.add(new ImmutablePair<>(chip.getLabel(), Objects.equals(forwardToGap, "N")));
                            i++;
                            if (i %100 == 0) {
                                log.info("Found " + i + " chips");
                            }
                        }
                        break;
                    }
                }
                if (chipsToArchive.size() >= limit) {
                    return chipsToArchive;
                }
            }
            labVesselDao.clear();
        }

        return chipsToArchive;
    }

    /**
     * This code is mostly copied from GAP's InfiniumArchiveBean.archiveChip: delete the jpg files; copy the
     * decode_data files (downloaded from Illumina for each chip position) into the idat directory; zip the
     * idats and decode_data into a temp zip file; copy the zip to the archive directory; delete the temp zip;
     * rename the idats directory (due to locks?), then delete it.
     */
    static boolean archiveChip(String barcode, InfiniumStarterConfig infiniumStarterConfig) {
        log.info("Archiving Chip Barcode " + barcode);
        boolean isSuccessful = true;
        File baseDataDir = new File(ConcordanceCalculator.convertFilePaths(infiniumStarterConfig.getDataPath()));
        File dataDir = new File(baseDataDir, barcode);

        String baseDecodeDataDir = infiniumStarterConfig.getDecodeDataPath();
        File decodeDataDir = new File(baseDecodeDataDir, barcode);
        File newDecodeDataDir = new File(dataDir, DECODE_DATA_NAME);

        if (!dataDir.exists()) {
            if (infiniumStarterConfig.getDeploymentConfig() == Deployment.PROD) {
                log.error("Data directory doesn't exist : " + dataDir.getAbsolutePath());
                return false;
            } else {
                // Assume this is a production chip for which idats don't exist in the dev directory
                return isSuccessful;
            }
        }
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
                    // Directory for chip barcode
                    File newNewDecodeDataDir = new File(newDecodeDataDir, barcode);
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

        // rename then delete dir
        if (isSuccessful) {
            // If here, everything worked.  Time for clean up...
            if (dataDir.exists()) {
                File archivedDataDir = new File(new File(baseDataDir, ARCHIVED_DIR_NAME), barcode);
                if (dataDir.renameTo(archivedDataDir)) {
                    // TODO - I am doing it this way (move and then delete) as I was not able to delete the root directoy...
                    deleteDirectory(archivedDataDir);
                } else {
                    log.error("Failed to rename " + dataDir + " to " + archivedDataDir);
                }
            }

            if (decodeDataDir.exists()) {
                deleteDirectory(decodeDataDir);
            }
        } else {
            // If here, something failed - do clean up...
            if (newDecodeDataDir.exists()) {
                deleteDirectory(newDecodeDataDir);
            }
        }
        return isSuccessful;
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

    private static boolean deleteDirectory(File dir) {
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
