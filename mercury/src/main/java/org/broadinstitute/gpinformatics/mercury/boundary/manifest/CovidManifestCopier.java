package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.opencsv.CSVWriter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.common.MercuryStringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.parsers.GenericTableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleStorageConfig;

import javax.ejb.Stateless;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Copies a manifest file to a a Google bucket. */
@Stateless
@Dependent
public class CovidManifestCopier {
    private static Log logger = LogFactory.getLog(CovidManifestCopier.class);
    private final static Class DEFAULT_CONFIG_CLASS = CovidManifestBucketConfig.class;

    @Inject
    private GoogleBucketDao googleBucketDao;
    @Inject
    private Deployment deployment;

    /**
     * CDI constructor.
     */
    @SuppressWarnings("UnusedDeclaration")
    public CovidManifestCopier() {
    }

   /**
     * Copies a .csv or .xls or .xlst Covid manifest file content to the default Google bucket.
     */
    public void copyContentToBucket(String filename, byte[] content) {
        copyContentToBucket(filename, content, DEFAULT_CONFIG_CLASS);
    }

   /**
     * Copies a .csv or .xls or .xlsx Covid manifest file content to the specified Google bucket.
     */
    public <T extends GoogleStorageConfig> void copyContentToBucket(String filename, byte[] content,
            Class<T> configClass) {
        StringWriter stringWriter = new StringWriter();
        if (filename.toLowerCase().endsWith(".xls") || filename.toLowerCase().endsWith(".xlsx")) {
            // An xls or xlsx spreadsheet is parsed into a generic cell grid, then to csv.
            List<List<String>> cellGrid = new ArrayList<>();
            try {
                GenericTableProcessor processor = new GenericTableProcessor();
                InputStream inputStream = new ByteArrayInputStream(content);
                PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
                cellGrid.addAll(processor.getHeaderAndDataRows().stream().
                        filter(row -> row.size() > 0).
                        collect(Collectors.toList()));
            } catch (Exception e) {
                logger.error("Manifest file " + filename + " cannot be parsed. " + e.getMessage());
            }
            if (cellGrid.size() < 2) {
                // No data rows found.
                return;
            }
            // Cleans up headers and values by removing characters that may present a problem later.
            int maxColumnIndex = -1;
            for (List<String> columns : cellGrid) {
                for (int i = 0; i < columns.size(); ++i) {
                    columns.set(i, MercuryStringUtils.cleanupValue(columns.get(i)));
                    if (StringUtils.isNotBlank(columns.get(i))) {
                        maxColumnIndex = Math.max(maxColumnIndex, i);
                    }
                }
            }
            // Makes a csv file and suitable filename.
            CSVWriter writer = new CSVWriter(stringWriter);
            String[] template = new String[0];
            for (List<String> columns : cellGrid) {
                writer.writeNext(columns.toArray(template));
            }
            IOUtils.closeQuietly(writer);
            filename = FilenameUtils.getBaseName(filename) + ".csv";
        } else {
            // A csv file is cleaned up but not converted. The filename is unchanged.
            Stream.of(StringUtils.split(new String(content), CharUtils.LF)).
                    map(line -> MercuryStringUtils.cleanupValue(line)).
                    forEach(line -> stringWriter.write(line + CharUtils.LF));
        }
        // Writes csv to google bucket.
        if (deployment != null && googleBucketDao != null) {
            T config = (T) MercuryConfiguration.getInstance().getConfig(configClass, deployment);
            googleBucketDao.setConfigGoogleStorageConfig(config);
            MessageCollection messageCollection = new MessageCollection();
            googleBucketDao.upload(filename, stringWriter.toString().getBytes(), messageCollection);
            if (messageCollection.hasErrors()) {
                logger.error(StringUtils.join(messageCollection.getErrors(), "; "));
            } else {
                logger.info("Wrote Covid manifest " + filename + " to Google bucket.");
            }
        }
    }
}
