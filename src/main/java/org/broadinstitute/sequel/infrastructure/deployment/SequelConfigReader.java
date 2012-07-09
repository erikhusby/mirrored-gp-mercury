package org.broadinstitute.sequel.infrastructure.deployment;

import org.apache.commons.beanutils.BeanUtils;
import org.broadinstitute.sequel.infrastructure.bsp.BSPConfig;
import org.broadinstitute.sequel.infrastructure.jira.JiraConfig;
import org.broadinstitute.sequel.infrastructure.pmbridge.PMBridgeConfig;
import org.broadinstitute.sequel.infrastructure.quote.QuoteConfig;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfig;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class SequelConfigReader {

    // can hopefully do something with portable extensions and @Observes ProcessAnnotatedType<T> to find these
    // automatically, and maybe something really sneaky to create qualified bean instances of these types to
    // support @TestInstance-style qualifier injection with producer classes.  But not in this version.
    private static final Class [] CONFIG_CLASSES = new Class [] {
        SquidConfig.class,
        BSPConfig.class,
        PMBridgeConfig.class,
        JiraConfig.class,
        QuoteConfig.class,
        ThriftConfig.class
    };


    private static final String SEQUEL_CONFIG = "/sequel-config.yaml";

    private static final String SEQUEL_CONFIG_LOCAL = "/sequel-config-local.yaml";

    private static SequelConfigReader instance;


    private Map<String, Map<Deployment, AbstractConfig>> externalSystemsMap =
            new HashMap<String, Map<Deployment, AbstractConfig>>();


    private Map<String, Map<Deployment, Deployment>> sequelConnectionsMap =
            new HashMap<String, Map<Deployment, Deployment>>();



    private String getConfigKey(Class<? extends AbstractConfig> configClass) {
        return configClass.getAnnotation(ConfigKey.class).value();
    }


    private Class<? extends AbstractConfig> getConfigClass(String configKey) {

        for ( Class<? extends AbstractConfig> clazz : CONFIG_CLASSES )
            if ( getConfigKey(clazz).equals(configKey) )
                return clazz;

        return null;
    }




    /**
     * Private to route creation through {@link #getInstance()}
     */
    private SequelConfigReader() {}


    public static SequelConfigReader getInstance() {
        if ( instance == null )
            instance = new SequelConfigReader();

        return instance;
    }




    private void loadExternalSystems(Map<String, Map> doc) {


        for (Map.Entry<String, Map> section :  doc.entrySet()) {

            String systemKey = section.getKey();
            if ( getConfigClass(systemKey) == null && ! "sequel".equals(systemKey) )
                throw new RuntimeException("Unrecognized top-level key: '" + systemKey +"'");

            // this method doesn't deal with sequel connections
            if ( "sequel".equals(systemKey) )
                continue;


            final Class<? extends AbstractConfig> configClass = getConfigClass(systemKey);

            // iterate the deployments for this external system
            for (Map.Entry<String, Map> deploymentEntry : ((Map<String, Map>) section.getValue()).entrySet()) {

                String deploymentString = deploymentEntry.getKey();

                if ( Deployment.valueOf(deploymentString) == null)
                    throw new RuntimeException("Unrecognized deployment key '" + deploymentString + "'");


                Deployment deployment = Deployment.valueOf(deploymentString);

                AbstractConfig config = newConfig(configClass);
                config.setExternalDeployment(deployment);

                setPropertiesIntoConfig(deploymentEntry.getValue(), config);

                if ( ! externalSystemsMap.containsKey(systemKey) )
                    externalSystemsMap.put(systemKey, new HashMap<Deployment, AbstractConfig>());

                externalSystemsMap.get(systemKey).put(deployment, config);

            }

        }
    }


    private void loadSequelConnections(Map<String, Map> doc, boolean globalConfig) {

        if ( ! doc.containsKey("sequel") ) {
            if ( globalConfig )
                throw new RuntimeException("'sequel' key not found in global configuration file!");
            // for local config, there is nothing to do if there's no 'sequel' key
            return;
        }


        Map<String, Map> deploymentsMap = doc.get("sequel");

        for (Map.Entry<String, Map> deployments : deploymentsMap.entrySet()) {

            String sequelDeploymentString = deployments.getKey();
            if ( Deployment.valueOf( sequelDeploymentString ) == null )
                throw new RuntimeException("Unrecognized deployment '" + sequelDeploymentString + "'");


            Deployment sequelDeployment = Deployment.valueOf(sequelDeploymentString);
            Map<String, String> systemsMappings = ( Map<String, String >) deployments.getValue();

            for (Map.Entry<String, String> systemsMapping : systemsMappings.entrySet()) {


                String externalDeploymentString = systemsMapping.getValue();
                // This must point to a known external deployment for this system
                if ( Deployment.valueOf(externalDeploymentString) == null )
                    throw new RuntimeException("Unrecognized deployment '" + externalDeploymentString + "'");

                Deployment externalDeployment = Deployment.valueOf(externalDeploymentString);


                String systemKey = systemsMapping.getKey();

                if ( ! externalSystemsMap.containsKey(systemKey) )
                    throw new RuntimeException("Unrecognized external system in sequel connections: '" + systemKey + "'");

                if ( ! sequelConnectionsMap.containsKey(systemKey) )
                    sequelConnectionsMap.put(systemKey, new HashMap<Deployment, Deployment>());

                sequelConnectionsMap.get(systemKey).put(sequelDeployment, externalDeployment);

            }


        }
    }


    /* package */ void loadGlobal(InputStream is) {

        Yaml yaml = new Yaml();
        final Map<String, Map> doc = (Map<String, Map>) yaml.load(is);

        loadExternalSystems(doc);
        loadSequelConnections(doc, true);

    }




    public AbstractConfig getConfig(Class<? extends AbstractConfig> clazz, Deployment deployment) {

        if (sequelConnectionsMap.size() == 0) {

            InputStream is;

            is = getClass().getResourceAsStream(SEQUEL_CONFIG);

            if ( is ==  null )
                throw new RuntimeException("Cannot find global config file '" + SEQUEL_CONFIG + "'");

            Yaml yaml = new Yaml();
            final Map<String, Map> globalConfigDoc = (Map<String, Map>) yaml.load(is);

            // take local overrides if any
            Map<String, Map> localConfigDoc = null;
            is = getClass().getResourceAsStream(SEQUEL_CONFIG_LOCAL);

            if ( is != null )
                localConfigDoc = (Map<String, Map>) yaml.load(is);


            // load up external systems and overrides
            loadExternalSystems(globalConfigDoc);
            loadExternalSystems(localConfigDoc);

            // now process the sequel connections to those systems
            // second parameter indicates whether global or not.  global config must have "sequel" section.
            loadSequelConnections(globalConfigDoc, true);
            loadSequelConnections(localConfigDoc, false);

        }

        String systemKey = getConfigKey(clazz);

        // Find the external deployment for this system key and SequeL deployment
        Deployment externalDeployment = sequelConnectionsMap.get(systemKey).get(deployment);


        // Look up the config for this system
        return externalSystemsMap.get(systemKey).get(externalDeployment);
    }


    /**
     * Utility method to check for existence of properties on an {@link AbstractConfig}-derived bean and wrap a slew of
     * reflection-related checked exceptions.
     *
     * @param propertyMap
     * @param config
     */
    private void setPropertiesIntoConfig(Map<String, String> propertyMap, AbstractConfig config) {

        try {

            // Find the list of gettable properties on the bean to sanity check whether the specified property exists
            // I would really like to validate settable properties too since this system doesn't work without setters
            final Set<String> properties = BeanUtils.describe(config).keySet();


            for (Map.Entry<String, String> property : propertyMap.entrySet()) {

                if (!properties.contains(property.getKey()))
                    throw new RuntimeException(
                            "Cannot set property '" + property.getKey() + "' into Config class '" + config.getClass() + "': no such property");

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
     * @param clazz
     * @return
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
