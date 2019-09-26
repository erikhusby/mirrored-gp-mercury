package org.broadinstitute.gpinformatics.infrastructure.parsers.csv;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanFilter;
import com.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * Util class to parse CSV data to objects
 */
public class CsvParser {
    /**
    * Parses a csv file into a list of beans.
    *
    * @param <T> the type of the bean
    * @param inputStream the inputStream of the csv file to parse
    * @param fieldDelimiter the field delimiter
    * @param beanClass the bean class to map csv records to
    * @param columnNameToBeanMap the mapping from Column Name to field name of the beanClass
    * @param filter the filter to apply to a row if any
    * @return the list of beans or an empty list there are none
    */
    public static <T> List<T> parseCsvStreamToBeanByMapping(InputStream inputStream,
                                                             char fieldDelimiter,
                                                             Class<T> beanClass,
                                                             @Nullable Map<String, String> columnNameToBeanMap,
                                                             CsvToBeanFilter filter,
                                                             int skipLines) {
        CSVReader reader = null;
        try {
            reader = new CSVReader(new BufferedReader(new InputStreamReader(inputStream)),
                    fieldDelimiter, '\'', skipLines);
            HeaderColumnNameTranslateMappingStrategy<T> strategy =
                    new HeaderColumnNameTranslateMappingStrategy<>();
            strategy.setColumnMapping(columnNameToBeanMap);
            strategy.setType(beanClass);
            CsvToBean<T> csv = new CsvToBean<>();
            return csv.parse(strategy, reader, filter);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Returns the csv file contents, including header and any blank lines.
     */
    public static List<String[]> parseToCellGrid(InputStream inputStream) throws IOException {
        CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(inputStream)));
        return reader.readAll();
    }
}
