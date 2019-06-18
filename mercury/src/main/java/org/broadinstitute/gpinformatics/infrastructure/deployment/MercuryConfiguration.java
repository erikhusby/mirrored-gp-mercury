package org.broadinstitute.gpinformatics.infrastructure.deployment;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.presentation.filters.CacheFilter;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;
import org.scannotation.WarUrlFinder;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * Singleton core class of Mercury configuration based from YAML files.  The two {@link Map}s in this class contain:
 * <ol>
 * <li>Descriptions of external deployments.  These correspond to all stanzas except the "mercury" stanza in the
 * configuration files.</li>
 * <li>Descriptions of how Mercury deployments connect to those external deployments.  This corresponds to the "mercury"
 * stanza in the configuration file(s).</li>
 * </ol>
 */
public class MercuryConfiguration {
    private static final String MERCURY_CONFIG = "/mercury-config.yaml";

    private static final String MERCURY_CONFIG_LOCAL = "/mercury-config-local.yaml";

    private static final String MERCURY_STANZA = "mercury";

    private static MercuryConfiguration instance = new MercuryConfiguration();

    private static String MERCURY_BUILD_INFO;

    private static Map<String, Class<? extends AbstractConfig>> configKeyToClassMap;

    private static class ExternalSystems {
        // Map of system key ("bsp", "squid", "thrift") to a Map of external system Deployments (TEST, QA, PROD) to
        // AbstractConfigs describing those deployments.
        private final Map<String, Map<Deployment, AbstractConfig>> map = new HashMap<>();

        public void set(String systemKey, Deployment deployment, AbstractConfig config) {
            if (!map.containsKey(systemKey)) {
                map.put(systemKey, new EnumMap<>(Deployment.class));
            }

            map.get(systemKey).put(deployment, config);
        }

        AbstractConfig getConfig(String systemKey, Deployment deployment) {
            if (!map.containsKey(systemKey)) {
                return null;
            }

            if (!map.get(systemKey).containsKey(deployment)) {
                return null;
            }

            return map.get(systemKey).get(deployment);
        }
    }

    private static class ApplicationConnections {
        // Map of system key ("bsp", "squid", "thrift") to a Map of *Mercury* Deployments to the corresponding external
        // system Deployment.
        private final Map<String, Map<Deployment, Deployment>> map = new HashMap<>();

        public boolean isInitialized() {
            return !map.isEmpty();
        }

        public void set(String systemKey, Deployment mercuryDeployment, Deployment externalDeployment) {
            if (!map.containsKey(systemKey)) {
                map.put(systemKey, new EnumMap<>(Deployment.class));
            }

            map.get(systemKey).put(mercuryDeployment, externalDeployment);
        }

        Deployment getExternalDeployment(String systemKey, Deployment mercuryDeployment) {
            if (!map.containsKey(systemKey)) {
                return null;
            }

            if (!map.get(systemKey).containsKey(mercuryDeployment)) {
                return null;
            }

            return map.get(systemKey).get(mercuryDeployment);
        }

        public void clear() {
            map.clear();
        }
    }

    // Map of system key ("bsp", "squid", "thrift") to external system Deployments (TEST, QA, PROD) to
    // AbstractConfigs describing those deployments.
    private ExternalSystems externalSystems = new ExternalSystems();

    // Map of system key ("bsp", "squid", "thrift") to *Mercury* Deployments to the corresponding external
    // system Deployment.
    private final ApplicationConnections applicationConnections = new ApplicationConnections();

    private String getConfigKey(Class<? extends AbstractConfig> configClass) {

        // Try directly on config class
        ConfigKey annotation = configClass.getAnnotation(ConfigKey.class);

        if (annotation == null) {
            // Config class may be a Weld proxy, the only annotation is @ApplicationScoped, try on superclass
            // TODO: JMS Blech! Is this really the best way to do this?
            // The config classes should be ApplicationScoped and initialized once and only once.
            Class<?> superClass = configClass.getSuperclass();
            if( superClass != null ) {
                annotation = superClass.getAnnotation(ConfigKey.class);
            }
        }
        if (annotation == null) {
            // Give it up
            throw new RuntimeException("Failed to get config key for " + configClass.getName());
        }
        return annotation.value();
    }

    /**
     * Abstract away getting the ServletContext.  Currently the {@link CacheFilter} class has been
     * leveraged to capture the ServletContext during its {@link CacheFilter#init}, hopefully we can find a
     * cleaner way of doing this if we still need the ServletContext.
     *
     * @return the ServletContext.
     */
    private ServletContext getServletContext() {
        return AppInitServlet.getInitServletContext();
    }

    private Class<? extends AbstractConfig> getConfigClass(String configKey) {
        if (configKeyToClassMap == null) {

            ServletContext servletContext = getServletContext();



            // Check if we have a ServletContext to determine if running inside the container.
            URL classPathUrl = servletContext == null ?
                    // Handle calls when running outside the container.
                    ClasspathUrlFinder.findClassBase(AbstractConfig.class) :
                    // Handle calls when running inside the container.
                    WarUrlFinder.findWebInfClassesPath(servletContext);

            AnnotationDB annotationDB = new AnnotationDB();

            try {
                annotationDB.scanArchives(classPathUrl);
                Set<String> annotatedClassNames =
                        annotationDB.getAnnotationIndex().get(ConfigKey.class.getCanonicalName());

                if (CollectionUtils.isEmpty(annotatedClassNames)) {
                    throw new RuntimeException("No @ConfigKey annotated class names found!");
                }
                configKeyToClassMap = new HashMap<>();
                // Add any found config classes to our Map.
                for (String annotatedClassName : annotatedClassNames) {
                    @SuppressWarnings("unchecked")
                    Class<? extends AbstractConfig> annotatedClass =
                            (Class<? extends AbstractConfig>) Class.forName(annotatedClassName);
                    configKeyToClassMap.put(getConfigKey(annotatedClass), annotatedClass);
                }

            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return configKeyToClassMap.get(configKey);
    }

    /**
     * Private to force access through {@link #getInstance()}.
     */
    private MercuryConfiguration() {
    }

    public static MercuryConfiguration getInstance() {
        return instance;
    }

    /**
     * Load the configuration of external systems only, not the Mercury connections to those systems.
     *
     * @param doc Top-level YAML document.
     */
    private void loadExternalSystems(Map<String, Map<String, Map<String, String>>> doc) {
        for (Map.Entry<String, Map<String, Map<String, String>>> section : doc.entrySet()) {
            String systemKey = section.getKey();

            // This method doesn't deal with Mercury connections to external systems.
            if (MERCURY_STANZA.equals(systemKey)) {
                continue;
            }

            Class<? extends AbstractConfig> configClass = getConfigClass(systemKey);

            if (configClass == null) {
                throw new RuntimeException("Unrecognized top-level key: '" + systemKey + "'");
            }

            // Iterate the deployments for this external system.
            Set<Map.Entry<String, Map<String, String>>> entrySet = section.getValue().entrySet();
            for (Map.Entry<String, Map<String, String>> deploymentEntry : entrySet) {
                String deploymentString = deploymentEntry.getKey();

                if (Deployment.valueOf(deploymentString) == null) {
                    throw new RuntimeException("Unrecognized deployment key '" + deploymentString + "'");
                }

                Deployment deployment = Deployment.valueOf(deploymentString);
                // DISABLED is a sentinel value for use only in the 'mercury' stanza, there should not be
                // any external system definitions for this deployment.
                if (deployment == Deployment.DISABLED) {
                    throw new RuntimeException(
                            "Unexpectedly saw DISABLED deployment section for system '" + deploymentEntry.getKey()
                            + "'");
                }

                AbstractConfig config = externalSystems.getConfig(systemKey, deployment);

                // This method is called for both local and global configuration files.  If we're reading the local
                // configuration file, it's quite likely the config will already exist.
                if (config == null) {
                    config = newConfig(configClass);
                }

                config.setExternalDeployment(deployment);

                Map<String, String> deploymentEntryValue = deploymentEntry.getValue();
                setPropertiesIntoConfig(deploymentEntryValue, config);

                externalSystems.set(systemKey, deployment, config);
            }
        }
    }

    /**
     * Get the build information -- version and date -- from the appropriate file (version.properties) that gets
     * generated by maven.
     *
     * @return A string representation of the build number and the build date produced by maven
     */
    public String getBuildInformation() {
        String versionFilename = "build.properties";

        // Only do this once if it's not defined.
        if (MERCURY_BUILD_INFO == null) {
            InputStream in = null;
            Properties props = new Properties();
            String buildDate = "";
            try {
                in = getClass().getResourceAsStream("/" + versionFilename);

                if (in != null) {
                    props.load(in);

                    MERCURY_BUILD_INFO = "Version " + props.getProperty("build.resultKey");

                    buildDate = props.getProperty("build.buildTimeStamp");
                    Date date = new SimpleDateFormat("yyyyMMdd-HHmm").parse(buildDate);

                    MERCURY_BUILD_INFO += " built on " + FastDateFormat.getInstance("yyyy/MM/dd hh:mm a").format(date);
                } else {
                    return "Version unknown.  Are we in a sandbox?";
                }
            } catch (IOException ioe) {
                MERCURY_BUILD_INFO = "Unknown build - problematic " + versionFilename;
                throw new RuntimeException("Problem reading version file " + versionFilename, ioe);
            } catch (ParseException e) {
                // Problem parsing the maven build date, use its default one.
                if (buildDate != null && !buildDate.isEmpty()) {
                    MERCURY_BUILD_INFO += " built on " + buildDate;
                }
            } finally {
                IOUtils.closeQuietly(in);
            }

        }
        return MERCURY_BUILD_INFO;
    }

    /**
     * Load the Mercury connections to external system deployments.
     *
     * @param doc          The top level YAML document.
     * @param globalConfig Whether this invocation represents the parsing of the global configuration file
     *                     (mercury-config.yaml) or the local overrides file (mercury-config-local.yaml).
     */
    private void loadMercuryConnections(Map<String, Map<String, Map<String, String>>> doc, boolean globalConfig) {
        if (!doc.containsKey(MERCURY_STANZA)) {
            if (globalConfig) {
                throw new RuntimeException("'" + MERCURY_STANZA + "' key not found in global configuration file!");
            }
            // For local config, there is nothing to do in this method if there's no 'mercury' key.
            return;
        }

        Map<String, Map<String, String>> deploymentsMap = doc.get(MERCURY_STANZA);

        for (Map.Entry<String, Map<String, String>> deployments : deploymentsMap.entrySet()) {
            // An entry in the map goes from a Mercury deployment to a list of system deployments.
            //
            // mercury:
            //   QA:
            //     jira: DEV
            //     ...
            String mercuryDeploymentString = deployments.getKey();
            if (Deployment.valueOf(mercuryDeploymentString) == null) {
                throw new RuntimeException("Unrecognized deployment '" + mercuryDeploymentString + "'.");
            }

            Deployment mercuryDeployment = Deployment.valueOf(mercuryDeploymentString);
            Map<String, String> systemsMappings = deployments.getValue();

            for (Map.Entry<String, String> systemsMapping : systemsMappings.entrySet()) {
                String externalDeploymentString = systemsMapping.getValue();

                // This corresponds to the system deployment, which in the example above would be 'DEV' for jira.
                // This first test is to see if the value in the YAML file is a recognized member of the Deployment enum.
                Deployment externalDeployment = Deployment.valueOf(externalDeploymentString);

                if (externalDeployment == null) {
                    throw new RuntimeException("Unrecognized deployment '" + externalDeploymentString + "'.");
                }

                // Next check if there is a config for this system for the given system deployment.
                String systemKey = systemsMapping.getKey();

                AbstractConfig config = externalSystems.getConfig(systemKey, externalDeployment);

                // It is okay to see DISABLED and not find a configuration as this is a sentinel value for
                // the absence of a configuration.
                if (config == null && externalDeployment != Deployment.DISABLED) {
                    throw new RuntimeException(
                            "Unrecognized external system in mercury connections: '" + systemKey + "'.");
                }

                applicationConnections.set(systemKey, mercuryDeployment, externalDeployment);
            }
        }
    }


    /**
     * Intended solely for test code to clear out mappings.
     */
    /* package */
    void clear() {
        externalSystems = new ExternalSystems();
        applicationConnections.clear();
    }

    /* package */
    void load(Map<String, Map<String, Map<String, String>>> globalConfigDoc,
              Map<String, Map<String, Map<String, String>>> localConfigDoc) {
        // Load up external systems and overrides.
        loadExternalSystems(globalConfigDoc);

        if (localConfigDoc != null) {
            loadExternalSystems(localConfigDoc);
        }

        // Now process the Mercury connections to those systems.
        // The second parameter indicates whether we're reading the global configuration file or not.  The global
        // configuration file must have a "mercury" section.
        loadMercuryConnections(globalConfigDoc, true);

        if (localConfigDoc != null) {
            loadMercuryConnections(localConfigDoc, false);
        }
    }

    private static String getConfigPath() {
        return MERCURY_CONFIG;
    }

    private static String getLocalConfigPath() {
        return MERCURY_CONFIG_LOCAL;
    }

    public AbstractConfig getConfig(Class<? extends AbstractConfig> clazz, Deployment deployment) {
        InputStream is = null;
        try {
            synchronized (applicationConnections) {
                if (!applicationConnections.isInitialized()) {

                    is = getClass().getResourceAsStream(getConfigPath());

                    if (is == null) {
                        throw new RuntimeException("Cannot find global config file '" + getConfigPath() + "'.");
                    }

                    Yaml yaml = new Yaml();

                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, Map<String, String>>> globalConfigDoc =
                            (Map<String, Map<String, Map<String, String>>>) yaml.load(is);

                    // take local overrides if any
                    Map<String, Map<String, Map<String, String>>> localConfigDoc = null;
                    is = getClass().getResourceAsStream(getLocalConfigPath());

                    if (is != null) {
                        //noinspection unchecked
                        localConfigDoc = (Map<String, Map<String, Map<String, String>>>) yaml.load(is);
                    }

                    load(globalConfigDoc, localConfigDoc);
                }
            }

            String systemKey = getConfigKey(clazz);

            // Find the external deployment for this system key and Mercury deployment.
            Deployment externalDeployment = applicationConnections.getExternalDeployment(systemKey, deployment);

            // Look up the config for this system.
            return externalSystems.getConfig(systemKey, externalDeployment);
        } finally {
            IOUtils.closeQuietly(is);
        }

    }

    /**
     * Utility method to check for existence of properties on an {@link AbstractConfig}-derived bean and wrap a slew of
     * reflection-related checked exceptions.
     *
     * @param propertyMap Map of property keys to property values.
     * @param config      The configuration object that will receive the specified configuration property settings.
     */
    private static void setPropertiesIntoConfig(Map<String, String> propertyMap, AbstractConfig config) {
        try {
            // Find the list of gettable properties on the bean to sanity check whether the specified property exists.
            // We should really validate settable properties too since this system doesn't work without setters.
            //noinspection unchecked
            Set<String> properties = BeanUtils.describe(config).keySet();

            if (propertyMap != null) {
                for (Map.Entry<String, String> property : propertyMap.entrySet()) {
                    if (!properties.contains(property.getKey())) {
                        throw new RuntimeException(
                                "Cannot set property '" + property.getKey() + "' into Config class '" + config.getClass()
                                + "': no such property");
                    }

                    BeanUtils.setProperty(config, property.getKey(), property.getValue());
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Utility method to create a new instance of the specified {@link AbstractConfig}-derived class.
     *
     * @param clazz The class extending {@link AbstractConfig} of which this method should create a new instance.
     *
     * @return The new instance.
     */
    private static AbstractConfig newConfig(Class<? extends AbstractConfig> clazz) {
        try {
            // This will throw NoSuchMethodException if the constructor does not exist, so no need to null check.
            Constructor<? extends AbstractConfig> constructor = clazz.getConstructor(Deployment.class);
            return constructor.newInstance(Deployment.STUBBY);

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}