package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.tool.EnversSchemaGenerator;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;

/**
 * hibernate3-maven-plugin and EnversHibernateToolTask don't support Hibernate 4, so this class is necessary to
 * generate DDL that includes the envers auditing tables.  This class is intended to be called from exec-maven-plugin.
 * This code is based on http://doingenterprise.blogspot.com/2012/05/schema-generation-with-hibernate-4-jpa.html
 */

@SuppressWarnings("deprecation")
public class EnversSchemaExport {

    public static final String DELIMITER = "--delimiter=";
    public static final String OUTPUT = "--output=";

    public static void main(String[] args) {
        boolean drop = false;
        boolean create = false;
        String outFile = null;
        String delimiter = "";
        String unitName = null;

        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (arg.equals("--drop")) {
                    drop = true;
                } else if (arg.equals("--create")) {
                    create = true;
                } else if (arg.startsWith(OUTPUT)) {
                    outFile = arg.substring(OUTPUT.length());
                } else if (arg.startsWith(DELIMITER)) {
                    delimiter = arg.substring(DELIMITER.length());
                }
            } else {
                unitName = arg;
            }
        }

        Formatter formatter = FormatStyle.DDL.getFormatter();

        Ejb3Configuration jpaConfiguration = new Ejb3Configuration().configure(unitName, null);
        Configuration hibernateConfiguration = jpaConfiguration.getHibernateConfiguration();
        EnversSchemaGenerator enversSchemaGenerator = new EnversSchemaGenerator(hibernateConfiguration);
        SchemaExport schemaExport = enversSchemaGenerator.export();
        PrintWriter writer = null;
        try {
            // We only want file output, but the SchemaExport class insists on additionally writing to standard output
            // or to a database.  To avoid writing many hundreds of lines of DDL to the build log, we grab the DDL
            // from private fields.  This is fragile, but a lesser evil than polluting the log for every build.
            Field createSQLField = SchemaExport.class.getDeclaredField("createSQL");
            createSQLField.setAccessible(true);
            String[] createSQL = (String[]) createSQLField.get(schemaExport);

            Field dropSQLField = SchemaExport.class.getDeclaredField("dropSQL");
            dropSQLField.setAccessible(true);
            String[] dropSQL = (String[]) dropSQLField.get(schemaExport);

            String directoryName = new File(outFile).getParent();
            File directory = new File(directoryName);
            if(!directory.exists()) {
                if(!directory.mkdirs()) {
                    throw new RuntimeException("Failed to create directory " + directoryName);
                }
            }

            writer = new PrintWriter(outFile);
            if (drop) {
                export(writer, delimiter, formatter, dropSQL);
            }
            if (create) {
                export(writer, delimiter, formatter, createSQL);
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        schemaExport.setOutputFile(outFile);

    }

    private static void export(PrintWriter writer, String delimiter, Formatter formatter, String[] createSQL) {
        for (String line : createSQL) {
            writer.print(formatter.format(line) + delimiter + "\n");
        }
    }
}