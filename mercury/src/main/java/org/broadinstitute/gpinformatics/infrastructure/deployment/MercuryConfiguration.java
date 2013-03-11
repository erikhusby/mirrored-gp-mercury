package org.broadinstitute.gpinformatics.infrastructure.deployment;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.datawh.EtlConfig;
import org.broadinstitute.gpinformatics.infrastructure.deckmsgs.DeckMessagesConfig;
import org.broadinstitute.gpinformatics.infrastructure.gap.GAPConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.monitoring.HipChatConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteConfig;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Core class of Mercury configuration.  The two Maps in this class contain
 * <p/>
 * <ol>
 * <p/>
 * <li>Descriptions of external deployments.  These correspond to all stanzas except the "mercury" stanza in the
 * configuration files.</li>
 * <p/>
 * <li>Descriptions of how Mercury deployments connect to those external deployments.  This corresponds to the "mercury"
 * stanza in the configuration file(s).</li>
 * </ol>
 */
public class MercuryConfiguration {
    // Hopefully we can do something with portable extensions and @Observes ProcessAnnotatedType<T> to find these
    // automatically, and maybe something really sneaky to create qualified bean instances of these types to
    // support @TestInstance-style qualifier injection with producer classes.  But not in this version.
    @SuppressWarnings("unchecked")
    private static final Class<? extends AbstractConfig>[] CONFIG_CLASSES = array(
            MercuryConfig.class,
            SquidConfig.class,
            BSPConfig.class,
            JiraConfig.class,
            QuoteConfig.class,
            ThriftConfig.class,
            GAPConfig.class,
            DeckMessagesConfig.class,
            EtlConfig.class,
            BettalimsConfig.class,
            TableauConfig.class,
            HipChatConfig.class);


    private static final String MERCURY_CONFIG = "/mercury-config.yaml";

    private static final String MERCURY_CONFIG_LOCAL = "/mercury-config-local.yaml";

    private static MercuryConfiguration instance;

    private static String MERCURY_BUILD_INFO;

    /**
     * Workaround for Java language limitations regarding creation of generic arrays, prevents classes not extending
     * {@link AbstractConfig} from being entered into the array of configuration classes.
     *
     * @param classes Varargs list of {@link AbstractConfig} classes.
     *
     * @return Arguments are simply passed through this method unmodified.
     */
    private static Class<? extends AbstractConfig> [] array(Class<? extends AbstractConfig>... classes) {
        return classes;
    }


    private class ExternalSystems {
        // Map of system key ("bsp", "squid", "thrift") to a Map of external system Deployments (TEST, QA, PROD) to
        // AbstractConfigs describing those deployments.
        private Map<String, Map<Deployment, AbstractConfig>> map =
                new HashMap<String, Map<Deployment, AbstractConfig>>();

        public void set(String systemKey, Deployment deployment, AbstractConfig config) {
            if (!map.containsKey(systemKey)) {
                map.put(systemKey, new HashMap<Deployment, AbstractConfig>());
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


    private class MercuryConnections {
        // Map of system key ("bsp", "squid", "thrift") to a Map of *Mercury* Deployments to the corresponding external
        // system Deployment.
        private Map<String, Map<Deployment, Deployment>> map =
                new HashMap<String, Map<Deployment, Deployment>>();

        public boolean isInitialized() {
            return map.size() != 0;
        }

        public void set(String systemKey, Deployment mercuryDeployment, Deployment externalDeployment) {
            if (!map.containsKey(systemKey)) {
                map.put(systemKey, new HashMap<Deployment, Deployment>());
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
    // AbstractConfigs describing those deployments
    private ExternalSystems externalSystems = new ExternalSystems();

    // Map of system key ("bsp", "squid", "thrift") to *Mercury* Deployments to the corresponding external
    // system Deployment
    private MercuryConnections mercuryConnections = new MercuryConnections();

    private String getConfigKey(Class<? extends AbstractConfig> configClass) {
        ConfigKey annotation = configClass.getAnnotation(ConfigKey.class);
        if (annotation == null) {
            throw new RuntimeException("Failed to get config key for " + configClass.getName());
        }
        return annotation.value();
    }

    private Class<? extends AbstractConfig> getConfigClass(String configKey) {
        for (Class<? extends AbstractConfig> clazz : CONFIG_CLASSES) {
            if (getConfigKey(clazz).equals(configKey)) {
                return clazz;
            }
        }

        return null;
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
     * @param doc Top-level YAML document
     */
    private void loadExternalSystems(Map<String, Map> doc) {
        for (Map.Entry<String, Map> section : doc.entrySet()) {
            String systemKey = section.getKey();

            // this method doesn't deal with mercury connections
            if ("mercury".equals(systemKey)) {
                continue;
            }

            final Class<? extends AbstractConfig> configClass = getConfigClass(systemKey);

            if (configClass == null) {
                throw new RuntimeException("Unrecognized top-level key: '" + systemKey + "'");
            }

            // iterate the deployments for this external system
            //noinspection unchecked
            for (Map.Entry<String, Map> deploymentEntry : ((Map<String, Map>) section.getValue()).entrySet()) {
                String deploymentString = deploymentEntry.getKey();

                if (Deployment.valueOf(deploymentString) == null) {
                    throw new RuntimeException("Unrecognized deployment key '" + deploymentString + "'");
                }

                Deployment deployment = Deployment.valueOf(deploymentString);

                AbstractConfig config = externalSystems.getConfig(systemKey, deployment);

                if (config == null) {
                    config = newConfig(configClass);
                }
                // else roll with the preexisting config

                config.setExternalDeployment(deployment);

                //noinspection unchecked
                setPropertiesIntoConfig(deploymentEntry.getValue(), config);

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

        // only do this once if it's not defined
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
                // problem parsing the maven build date, use it's default one
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
     * Load the Mercury connections to external system deployments
     *
     * @param doc The top level YAML document.
     * @param globalConfig Whether this invocation represents the parsing of the global configuration file
     *                     (mercury-config.yaml) or the local overrides file (mercury-config-local.yaml).
     */
    private void loadMercuryConnections(Map<String, Map> doc, boolean globalConfig) {
        final String APP_KEY = "mercury";

        if (!doc.containsKey(APP_KEY)) {
            if (globalConfig) {
                throw new RuntimeException("'" + APP_KEY + "' key not found in global configuration file!");
            }
            // for local config, there is nothing to do if there's no 'mercury' key
            return;
        }

        //noinspection unchecked
        Map<String, Map> deploymentsMap = doc.get(APP_KEY);

        for (Map.Entry<String, Map> deployments : deploymentsMap.entrySet()) {
            String mercuryDeploymentString = deployments.getKey();
            if (Deployment.valueOf(mercuryDeploymentString) == null) {
                throw new RuntimeException("Unrecognized deployment '" + mercuryDeploymentString + "'");
            }

            Deployment mercuryDeployment = Deployment.valueOf(mercuryDeploymentString);
            //noinspection unchecked
            Map<String, String> systemsMappings = (Map<String, String>) deployments.getValue();

            for (Map.Entry<String, String> systemsMapping : systemsMappings.entrySet()) {
                String externalDeploymentString = systemsMapping.getValue();

                // This must point to a known external deployment for this system
                if (Deployment.valueOf(externalDeploymentString) == null) {
                    throw new RuntimeException("Unrecognized deployment '" + externalDeploymentString + "'");
                }

                Deployment externalDeployment = Deployment.valueOf(externalDeploymentString);

                String systemKey = systemsMapping.getKey();

                final AbstractConfig config = externalSystems.getConfig(systemKey, externalDeployment);

                if (config == null) {
                    throw new RuntimeException("Unrecognized external system in mercury connections: '" + systemKey + "'");
                }

                mercuryConnections.set(systemKey, mercuryDeployment, externalDeployment);
            }
        }
    }


    /**
     * Intended solely for test code to clear out mappings
     */
    /* package */
    void clear() {
        externalSystems = new ExternalSystems();
        mercuryConnections = new MercuryConnections();
    }

    /* package */
    void load(Map<String, Map> globalConfigDoc, Map<String, Map> localConfigDoc) {
        // load up external systems and overrides
        loadExternalSystems(globalConfigDoc);

        if (localConfigDoc != null) {
            loadExternalSystems(localConfigDoc);
        }

        // now process the mercury connections to those systems
        // second parameter indicates whether global or not.  global config must have "mercury" section.
        loadMercuryConnections(globalConfigDoc, true);

        if (localConfigDoc != null) {
            loadMercuryConnections(localConfigDoc, false);
        }
    }

    public AbstractConfig getConfig(Class<? extends AbstractConfig> clazz, Deployment deployment) {
        if (!mercuryConnections.isInitialized()) {
            synchronized (this) {
                if (!mercuryConnections.isInitialized()) {
                    InputStream is;

                    is = getClass().getResourceAsStream(MERCURY_CONFIG);

                    if (is == null) {
                        throw new RuntimeException("Cannot find global config file '" + MERCURY_CONFIG + "'");
                    }

                    Yaml yaml = new Yaml();
                    //noinspection unchecked
                    final Map<String, Map> globalConfigDoc = (Map<String, Map>) yaml.load(is);

                    // take local overrides if any
                    Map<String, Map> localConfigDoc = null;
                    is = getClass().getResourceAsStream(MERCURY_CONFIG_LOCAL);

                    if (is != null) {
                        //noinspection unchecked
                        localConfigDoc = (Map<String, Map>) yaml.load(is);
                    }

                    load(globalConfigDoc, localConfigDoc);
                }
            }
        }

        String systemKey = getConfigKey(clazz);

        // Find the external deployment for this system key and Mercury deployment
        Deployment externalDeployment = mercuryConnections.getExternalDeployment(systemKey, deployment);

        // Look up the config for this system
        return externalSystems.getConfig(systemKey, externalDeployment);
    }


    /**
     * Utility method to check for existence of properties on an {@link AbstractConfig}-derived bean and wrap a slew of
     * reflection-related checked exceptions.
     *
     * @param propertyMap Map of property keys to property values.
     * @param config The configuration object that will receive the specified configuration property settings.
     */
    private void setPropertiesIntoConfig(Map<String, String> propertyMap, AbstractConfig config) {
        try {
            // Find the list of gettable properties on the bean to sanity check whether the specified property exists
            // I would really like to validate settable properties too since this system doesn't work without setters
            //noinspection unchecked
            final Set<String> properties = BeanUtils.describe(config).keySet();


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
     * Utility method to create a new instance of the specified {@link AbstractConfig}-derived class
     *
     * @param clazz The class extending {@link AbstractConfig} of which this method should create a new instance.
     * @return The new instance.
     */
    private AbstractConfig newConfig(Class<? extends AbstractConfig> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
