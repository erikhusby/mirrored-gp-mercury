package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.hibernate.envers.RevisionType;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class EtlTestUtilities {

    /** Deletes all the files written by these tests including .dat, isReady, and lastEtlRun files. */
    public static void deleteEtlFiles(String dir) {
        // Uses current year month day to determine whether to delete a file.
        final String yyyymmdd = (new SimpleDateFormat("yyyyMMdd")).format(new Date());
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dirname, String filename) {
                return (filename.startsWith(yyyymmdd) && filename.endsWith(".dat")
                        || filename.startsWith(yyyymmdd) && filename.endsWith(ExtractTransform.READY_FILE_SUFFIX))
                        || filename.equals(ExtractTransform.LAST_ETL_FILE);
            }
        };
        for (File file : new File(dir).listFiles(filter)) {
            FileUtils.deleteQuietly(file);
        }
    }

    /** Returns all files in the given directory, having filename timestamp in the given range. */
    public static File[] getDirFiles(String directoryName, long msecStart, long msecEnd) {
        final long yyyymmddHHMMSSstart = Long.parseLong(ExtractTransform.secTimestampFormat.format(new Date(msecStart)));
        final long yyyymmddHHMMSSend = Long.parseLong(ExtractTransform.secTimestampFormat.format(new Date(msecEnd)));
        File dir = new File (directoryName);
        File[] list = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dirname, String filename) {
                try {
                    // Only cares about files named <dateTime>_<*>
                    String s = filename.split("_")[0];
                    long timestamp = Long.parseLong(s);
                    return (timestamp >= yyyymmddHHMMSSstart && timestamp <= yyyymmddHHMMSSend);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
        return list;
    }

    /** Entities returned by Audit Reader */
    private List<Object> etlEntities = new ArrayList<Object>();
    private long revNumber;
    private Date revDate;

    /** Sets the list of entities returned by Audit Reader */
    public void setEntities(List<Object> list) {
	etlEntities = list;
    }

    /** Gets the list of entities returned by Audit Reader */
    public List<Object> getEtlEntities() {
        return etlEntities;
    }

    /** Sets the current rev number returned by Audit Reader */
    public void setRevNumber(long num) {
	revNumber = num;
    }

    /** Gets the current rev number returned by Audit Reader */
    public long getRevNumber() {
	return revNumber;
    }

    /** Sets the current rev date returned by Audit Reader */
    public void setRevDate(Date date) {
	revDate = date;
    }

    /** Gets the current rev date returned by Audit Reader */
    public Date getRevDate() {
	return revDate;
    }

    public AuditReaderDao getAuditReaderDao() {
        return new AuditReaderDao() {
            public long currentRevNumber(Date etlDate) {
                return getRevNumber();
            }

            public Date dateForRevNumber(long revNumber) {
                return getRevDate();
            }

            /** Each returned Object[] has 3 elements: [0] is the entity, [1] is the RevInfo, [2] is the RevisionType */
            public List<Object[]> fetchDataChanges(long lastRev, long etlRev, Class entityClass) {
                List<Object[]> dataChanges = new ArrayList<Object[]>();
		for (Object etlEntity : getEtlEntities()) {
		    Object[] dataChange = new Object[3];

		    dataChange[0] = etlEntity;
		    RevInfo revInfo = new RevInfo();
		    revInfo.setRevDate(new Date());
		    dataChange[1] = revInfo;
		    dataChange[2] = RevisionType.ADD;
		}
                return dataChanges;
            }

            public Collection<Object> findAll(Class clazz, long id, long id2) {
                return getEtlEntities();
            }
        };
    }

}