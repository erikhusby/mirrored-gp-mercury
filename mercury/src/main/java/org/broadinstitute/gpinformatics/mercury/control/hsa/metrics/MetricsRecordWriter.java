package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import com.opencsv.CSVWriter;
import com.opencsv.bean.MappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.infrastructure.datawh.EtlConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Dependent
public class MetricsRecordWriter {

    @Inject
    private EtlConfig etlConfig;

    public <T> void writeBeanRecord(List<T> beans, File outputFile, MappingStrategy<T> mappingStrategy)
            throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException, IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                            .withMappingStrategy(mappingStrategy)
                            .withSeparator(',')
                            .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                            .build();
            beanToCsv.write(beans);
        }
    }

    public void setEtlConfig(EtlConfig etlConfig) {
        this.etlConfig = etlConfig;
    }
}
