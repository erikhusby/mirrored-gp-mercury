package org.broadinstitute.gpinformatics.infrastructure.deployment;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationFilter;
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
 * Core class of Mercury configuration.  The two Maps in this class contain
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

    private static MercuryConfiguration instance;

    private static String MERCURY_BUILD_INFO;

    private static Map<String, Class<? extends AbstractConfig>> configKeyToClassMap;


    private static class ExternalSystems {
        // Map of system key ("bsp", "squid", "thrift") to a Map of external system Deployments (TEST, QA, PROD) to
        // AbstractConfigs describing those deployments.
        private final Map<String, Map<Deployment, AbstractConfig>> map =
                new HashMap<String, Map<Deployment, AbstractConfig>>();

        public void set(String systemKey, Deployment deployment, AbstractConfig config) {
            if (!map.containsKey(systemKey)) {
                map.put(systemKey, new EnumMap<Deployment, AbstractConfig>(Deployment.class));
            }

            map.get(systemKey).put(deployment, config);
        }

        public AbstractConfig getConfig(String systemKey, Deployment deployment) {
            if (!map.containsKey(systemKey)) {
                return null;
            }

            if (!map.get(systemKey).containsKey(deployment)) {
                return null;
            }

            return map.get(systemKey).get(deployment);
        }
    }


    private static class MercuryConnections {
        // Map of system key ("bsp", "squid", "thrift") to a Map of *Mercury* Deployments to the corresponding external
        // system Deployment.
        private final Map<String, Map<Deployment, Deployment>> map =
                new HashMap<String, Map<Deployment, Deployment>>();

        public boolean isInitialized() {
            return !map.isEmpty();
        }

        public void set(String systemKey, Deployment mercuryDeployment, Deployment externalDeployment) {
            if (!map.containsKey(systemKey)) {
                map.put(systemKey, new EnumMap<Deployment, Deployment>(Deployment.class));
            }

            map.get(systemKey).put(mercuryDeployment, externalDeployment);
        }

        public Deployment getExternalDeployment(String systemKey, Deployment mercuryDeployment) {
            if (!map.containsKey(systemKey)) {
                return null;
            }

            if (!map.get(systemKey).containsKey(mercuryDeployment)) {
                return null;
            }

            return map.get(systemKey).get(mercuryDeployment);
        }
    }

    // Map of system key ("bsp", "squid", "thrift") to external system Deployments (TEST, QA, PROD) to
    // AbstractConfigs describing those deployments.
    private ExternalSystems externalSystems = new ExternalSystems();

    // Map of system key ("bsp", "squid", "thrift") to *Mercury* Deployments to the corresponding external
    // system Deployment.
    private MercuryConnections mercuryConnections = new MercuryConnections();

    private static String getConfigKey(Class<? extends AbstractConfig> configClass) {
        ConfigKey annotation = configClass.getAnnotation(ConfigKey.class);
        if (annotation == null) {
            throw new RuntimeException("Failed to get config key for " + configClass.getName());
        }
        return annotation.value();
    }

    /**
     * Abstract away getting the ServletContext.  Currently the {@link AuthorizationFilter} class has been
     * leveraged to capture the ServletContext during its {@link AuthorizationFilter#init}, hopefully we can find a
     * cleaner way of doing this if we still need the ServletContext.
     *
     * @return the ServletContext.
     */
    private static ServletContext getServletContext() {
        return AuthorizationFilter.getServletContext();
    }

    private static Class<? extends AbstractConfig> getConfigClass(String configKey) {

        if (configKeyToClassMap == null) {
            configKeyToClassMap = new HashMap<String, Class<? extends AbstractConfig>>();

            ServletContext servletContext = getServletContext();

            // Check if we have a ServletContext to determine if running inside the container.
            URL classPathUrl = (servletContext == null) ?
                // Handle calls when running outside the container.
                ClasspathUrlFinder.findClassBase(AbstractConfig.class) :
                // Handle calls when running inside the container.
                WarUrlFinder.findWebInfClassesPath(servletContext);

            AnnotationDB annotationDB = new AnnotationDB();

            try {
                annotationDB.scanArchives(classPathUrl);
                Set<String> annotatedClassNames = annotationDB.getAnnotationIndex().get(ConfigKey.class.getCanonicalName());

                if (CollectionUtils.isEmpty(annotatedClassNames)) {
                    throw new RuntimeException("No @ConfigKey annotated class names found!");
                }
                // Add any found config classes to our Map.
                for (String annotatedClassName : annotatedClassNames) {
                    @SuppressWarnings("unchecked")
                    Class<? extends AbstractConfig> annotatedClass = (Class<? extends AbstractConfig>) Class.forName(annotatedClassName);
                    configKeyToClassMap.put(getConfigKey(annotatedClass), annotatedClass);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
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
        if (instance == null) {
            instance = new MercuryConfiguration();
        }

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

                    MERCURY_BUILD_INFO += " built on " + new SimpleDateFormat("yyyy/MM/dd hh:mm a").format(date);
                } else {
                    return "Version unknown.  Are we in a sandbox?";
                }
            } catch (IOException ioe) {
                MERCURY_BUILD_INFO = "Unknown build - problematic " + versionFilename;
                throw new RuntimeException("Problem reading version file " + versionFilename, ioe);
            } catch (ParseException e) {
                // Problem parsing the maven build date, use its default one.
                if ((buildDate != null) && !buildDate.isEmpty()) {
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
     * @param doc The top level YAML document.
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
            String mercuryDeploymentString = deployments.getKey();
            if (Deployment.valueOf(mercuryDeploymentString) == null) {
                throw new RuntimeException("Unrecognized deployment '" + mercuryDeploymentString + "'.");
            }

            Deployment mercuryDeployment = Deployment.valueOf(mercuryDeploymentString);
            Map<String, String> systemsMappings = deployments.getValue();

            for (Map.Entry<String, String> systemsMapping : systemsMappings.entrySet()) {
                String externalDeploymentString = systemsMapping.getValue();

                // This must point to a known external deployment for this system.
                if (Deployment.valueOf(externalDeploymentString) == null) {
                    throw new RuntimeException("Unrecognized deployment '" + externalDeploymentString + "'.");
                }

                Deployment externalDeployment = Deployment.valueOf(externalDeploymentString);

                String systemKey = systemsMapping.getKey();

                AbstractConfig config = externalSystems.getConfig(systemKey, externalDeployment);

                if (config == null) {
                    throw new RuntimeException("Unrecognized external system in mercury connections: '" + systemKey + "'.");
                }

                mercuryConnections.set(systemKey, mercuryDeployment, externalDeployment);
            }
        }
    }


    /**
     * Intended solely for test code to clear out mappings.
     */
    /* package */
    void clear() {
        externalSystems = new ExternalSystems();
        mercuryConnections = new MercuryConnections();
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

     public AbstractConfig getConfig(Class<? extends AbstractConfig> clazz, Deployment deployment) {
        InputStream is = null;
        try {
            if (!mercuryConnections.isInitialized()) {
                synchronized (this) {
                    if (!mercuryConnections.isInitialized()) {

                        is = getClass().getResourceAsStream(MERCURY_CONFIG);

                        if (is == null) {
                            throw new RuntimeException("Cannot find global config file '" + MERCURY_CONFIG + "'.");
                        }

                        Yaml yaml = new Yaml();

                        @SuppressWarnings("unchecked")
                        Map<String, Map<String, Map<String, String>>> globalConfigDoc =
                                (Map<String, Map<String, Map<String, String>>>) yaml.load(is);

                        // take local overrides if any
                        Map<String, Map<String, Map<String, String>>> localConfigDoc = null;
                        is = getClass().getResourceAsStream(MERCURY_CONFIG_LOCAL);

                        if (is != null) {
                            //noinspection unchecked
                            localConfigDoc = (Map<String, Map<String, Map<String, String>>>) yaml.load(is);
                        }

                        load(globalConfigDoc, localConfigDoc);
                    }
                }
            }

            String systemKey = getConfigKey(clazz);

            // Find the external deployment for this system key and Mercury deployment.
            Deployment externalDeployment = mercuryConnections.getExternalDeployment(systemKey, deployment);

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
     * @param config The configuration object that will receive the specified configuration property settings.
     */
    private static void setPropertiesIntoConfig(Map<String, String> propertyMap, AbstractConfig config) {
        try {
            // Find the list of gettable properties on the bean to sanity check whether the specified property exists.
            // We should really validate settable properties too since this system doesn't work without setters.
            //noinspection unchecked
            Set<String> properties = BeanUtils.describe(config).keySet();


            for (Map.Entry<String, String> property : propertyMap.entrySet()) {

                if (!properties.contains(property.getKey())) {
                    throw new RuntimeException(
                            "Cannot set property '" + property.getKey() + "' into Config class '" + config.getClass() + "': no such property");
                }

                BeanUtils.setProperty(config, property.getKey(), property.getValue());
            }

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Utility method to create a new instance of the specified {@link AbstractConfig}-derived class.
     *
     * @param clazz The class extending {@link AbstractConfig} of which this method should create a new instance.
     * @return The new instance.
     */
    private static AbstractConfig newConfig(Class<? extends AbstractConfig> clazz) {
        try {
            // This will throw NoSuchMethodException if the constructor does not exist, so no need to null check.
            Constructor<? extends AbstractConfig> constructor = clazz.getConstructor(Deployment.class);
            return constructor.newInstance(Deployment.STUBBY);

        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
