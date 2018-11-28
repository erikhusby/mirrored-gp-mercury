package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.hibernate.jpa.AvailableSettings;

import javax.persistence.Persistence;
import java.util.Properties;

public class EnversSchemaExport {

    public static void main(String[] args) {
        execute(args[0], args[1]);
        System.exit(0);
    }

    /**
     * See persistence.xml commented out settings to generate DDL using the JPA 2.1 standard for all providers
     *
     */
    public static void execute(String persistenceUnitName, String destination) {
        System.out.println("Generating DDL create script to : " + destination);

        final Properties persistenceProperties = new Properties();

        // XXX force persistence properties : remove database target
        persistenceProperties.setProperty(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "");
        persistenceProperties.setProperty(AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "none");

        persistenceProperties.setProperty(AvailableSettings.SCHEMA_GEN_SCRIPTS_ACTION, "create");
        persistenceProperties.setProperty(AvailableSettings.SCHEMA_GEN_CREATE_SOURCE, "metadata");
        persistenceProperties.setProperty(AvailableSettings.SCHEMA_GEN_SCRIPTS_CREATE_TARGET, destination);
        persistenceProperties.setProperty(AvailableSettings.SCHEMA_GEN_DB_NAME, "Oracle");
        persistenceProperties.setProperty(AvailableSettings.SCHEMA_GEN_DB_MAJOR_VERSION, "12");
        persistenceProperties.setProperty(AvailableSettings.SCHEMA_GEN_DB_MINOR_VERSION, "1");

        // todo jmt this doesn't work, because it requires a database connection, which is provided by JBoss
        Persistence.generateSchema(persistenceUnitName, persistenceProperties);
    }

}