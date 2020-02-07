package org.broadinstitute.gpinformatics.infrastructure.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.mercury.boundary.search.SearchRequestBean;
import org.broadinstitute.gpinformatics.mercury.boundary.search.SearchValueBean;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configurable search definitions for various entities.
 */
public class SearchDefinitionFactory {

    // State of ConfigurableSearchDefinition does not change once created.
    private static Map<String, ConfigurableSearchDefinition> MAP_NAME_TO_DEF = new HashMap<>();

    /**
     * Convenience to allow users to just enter the number or suffix of the batch name.  <br />
     * Note:  Only works if the search term is the exact same as the batch prefix (FCT,ARRAY,SK, etc.) <br />
     * TODO: JMS Handle lists of multiple values (e.g when BETWEEN, IN operators selected)
     */
    private static SearchTerm.Evaluator<Object> batchNameInputConverter = new SearchTerm.Evaluator<Object>() {
        @Override
        public Object evaluate(Object entity, SearchContext context) {
            String prefix = context.getSearchValue().getName();
            String value = context.getSearchValueString();
            if( !value.startsWith( prefix ) && context.getSearchValue().getOperator() != SearchInstance.Operator.LIKE ){
                value = prefix + "-" + value;
            }
            return value;
        }
    };

    /**
     * Convenience to allow users to just enter the number of the PDO.
     */
    private static SearchTerm.Evaluator<Object> pdoConverter = new SearchTerm.Evaluator<Object>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            String value = context.getSearchValueString();
            if( value.matches("[0-9]*")){
                value = "PDO-" + value;
            }
            return value;
        }
    };

    private static SearchTerm.Evaluator<Object> billingSessionConverter = new SearchTerm.Evaluator<Object>() {
        @Override
        public Long evaluate(Object entity, SearchContext context) {
            String value = context.getSearchValueString();

            if(value.startsWith(BillingSession.ID_PREFIX)) {
                value = value.split(BillingSession.ID_PREFIX)[1];
            }
            if (!NumberUtils.isNumber(value)) {
                value = "0";
            }
            return Long.valueOf(value);
        }
    };
    private static SearchTerm.Evaluator<Object> userIdConverter = new SearchTerm.Evaluator<Object>() {
        @Override
        public Long evaluate(Object entity, SearchContext context) {

            String value = context.getSearchValueString();
            final Optional<BspUser> searchBspUser = Optional.ofNullable(context.getBspUserList().getByUsername(value));

            Long userId = 0L;
            if(searchBspUser.isPresent()) {
                userId = searchBspUser.get().getUserId();
            }
            return userId;
        }
    };

    private SearchDefinitionFactory(){}

    static {
        SearchDefinitionFactory fact = new SearchDefinitionFactory();
        fact.buildLabEventSearchDef();
        fact.buildLabVesselSearchDef();
        fact.buildMercurySampleSearchDef();
        fact.buildReagentSearchDef();
        fact.buildLabMetricSearchDef();
        fact.buildLabMetricRunSearchDef();
        fact.buildProductOrderSearchDef();
        fact.buildSampleInstanceEntitySearchDef();
        fact.buildQueueGroupingSearchDef();
    }

    public static ConfigurableSearchDefinition getForEntity(String entity) {
        /* **** Change condition to true during development to rebuild for JVM hot-swap changes **** */
        //noinspection ConstantIfStatement

        // *************************************** REVERT TO FALSE BEFORE MERGE ************** //
        if( false ) {
            SearchDefinitionFactory fact = new SearchDefinitionFactory();
            fact.buildLabEventSearchDef();
            fact.buildLabVesselSearchDef();
            fact.buildMercurySampleSearchDef();
            fact.buildReagentSearchDef();
            fact.buildLabMetricSearchDef();
            fact.buildLabMetricRunSearchDef();
            fact.buildProductOrderSearchDef();
            fact.buildSampleInstanceEntitySearchDef();
            fact.buildQueueGroupingSearchDef();
        }

        return MAP_NAME_TO_DEF.get(entity);
    }

    private void buildLabVesselSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new LabVesselSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_VESSEL.getEntityName(), configurableSearchDefinition);
    }

    private void buildLabEventSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new LabEventSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_EVENT.getEntityName(), configurableSearchDefinition);
    }

    private void buildMercurySampleSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new MercurySampleSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.MERCURY_SAMPLE.getEntityName(), configurableSearchDefinition);
    }

    private void buildReagentSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new ReagentSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.REAGENT.getEntityName(), configurableSearchDefinition);
    }

    private void buildLabMetricSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new LabMetricSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_METRIC.getEntityName(), configurableSearchDefinition);
    }

    private void buildLabMetricRunSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new LabMetricRunSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_METRIC_RUN.getEntityName(), configurableSearchDefinition);
    }

    private void buildProductOrderSearchDef() {
        ConfigurableSearchDefinition productOrderSearchDefinition
                = new ProductOrderSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.PRODUCT_ORDER.getEntityName(), productOrderSearchDefinition);
    }

    private void buildSampleInstanceEntitySearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new SampleInstanceEntitySearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.EXTERNAL_LIBRARY.getEntityName(), configurableSearchDefinition);
    }

    private void buildQueueGroupingSearchDef() {
        ConfigurableSearchDefinition queueGroupingSearchDefinition
                = new QueueGroupingSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.QUEUE_GROUPING.getEntityName(), queueGroupingSearchDefinition);
    }

    /**
     * Prepends batch type from search term name to a numeric entry <br/>
     * e.g converts user input 7786 to ARRAY-7786 for ARRAY term name
     */
    static SearchTerm.Evaluator<Object> getBatchNameInputConverter(){
        return batchNameInputConverter;
    }

    /**
     * Prepends 'PDO-' to a numeric entry for PDO term
     */
    static SearchTerm.Evaluator<Object> getPdoInputConverter(){
        return pdoConverter;
    }

    static SearchTerm.Evaluator<Object> getBillingSessionConverter() {
        return billingSessionConverter;
    }

    static SearchTerm.Evaluator<Object> getUserIdConverter() {
        return userIdConverter;
    }


    /**
     * Shared logic to extract the type of any lab vessel
     */
    static String findVesselType( LabVessel vessel ) {
        String vesselTypeName;
        switch( vessel.getType() ) {
        case STATIC_PLATE:
            StaticPlate p = OrmUtil.proxySafeCast(vessel, StaticPlate.class);
            vesselTypeName = p.getPlateType()==null?"":p.getPlateType().getAutomationName();
            break;
        case TUBE_FORMATION:
            TubeFormation tf = OrmUtil.proxySafeCast(vessel, TubeFormation.class );
            vesselTypeName = tf.getRackType()==null?"":tf.getRackType().getDisplayName();
            break;
        case RACK_OF_TUBES:
            RackOfTubes rot = OrmUtil.proxySafeCast(vessel, RackOfTubes.class );
            vesselTypeName = rot.getRackType()==null?"":rot.getRackType().getDisplayName();
            break;
        default:
            // Not sure of others for in-place vessels
            vesselTypeName = vessel.getType().getName();
        }
        return vesselTypeName;
    }

    /**
     * Builds a link for result cell to facilitate a drill down link to another UDS
     * @param linkText The text to display in the anchor
     * @param entityType The entity type of the targeted search
     * @param selectedSearchName The name of a saved search to use (must exist)
     * @param terms Terms and values as required to match saved search
     * @param context Search instance context values
     * @return Encoded HTML URL link to present in search result column
     */
    public static String buildDrillDownLink( String linkText, ColumnEntity entityType, String selectedSearchName, Map<String,String[]> terms, SearchContext context ) {

        StringBuilder link = new StringBuilder();
        link.append("<a class=\"external\" target=\"new\" href=\"");
        buildDrillDownHref(entityType, selectedSearchName, terms, link, context.getBaseSearchURL());

        link.append("\">")
            .append(StringEscapeUtils.escapeHtml4(linkText))
            .append("</a>");

        return link.toString();
    }

    /**
     * Builds an href for result cell to facilitate a drill down link to another UDS
     * @param baseSearchURL
     * @param entityType The entity type of the targeted search
     * @param selectedSearchName The name of a saved search to use (must exist)
     * @param terms Terms and values as required to match saved search
     * @return Encoded href to present in search result column
     */
    public static void buildDrillDownHref(ColumnEntity entityType, String selectedSearchName,
                                          Map<String, String[]> terms, StringBuilder link, String baseSearchURL) {
        link.append(baseSearchURL);
        if (!baseSearchURL.contains(ConfigurableSearchActionBean.URL_BINDING)) {
            link.append(ConfigurableSearchActionBean.URL_BINDING);
        }
        link.append("?")
            .append(ConfigurableSearchActionBean.DRILL_DOWN_EVENT)
            .append("=&drillDownRequest=");

        List<SearchValueBean> searchValues = new ArrayList<>();
        for( Map.Entry<String,String[]> term : terms.entrySet()){
            searchValues.add( new SearchValueBean( term.getKey(), Arrays.asList( term.getValue() ) ) );
        }
        SearchRequestBean searchRequest = new SearchRequestBean(entityType.getEntityName(), selectedSearchName, searchValues);

        try {
            link.append(URLEncoder.encode(new ObjectMapper().writeValueAsString(searchRequest), "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException("Fail marshalling drill down configuration", e);
        }
    }


    /**
     * Shared value list of all lab event types.
     */
    static class EventTypeValuesExpression extends SearchTerm.Evaluator<List<ConstrainedValue>> {

        public static List<ConstrainedValue> getConstrainedValues() {
            List<ConstrainedValue> constrainedValues = new ArrayList<>();
            for (LabEventType labEventType : LabEventType.values()) {
                constrainedValues.add(new ConstrainedValue(labEventType.toString(), labEventType.getName()));
            }
            Collections.sort(constrainedValues);
            return constrainedValues;
        }

        @Override
        public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
            return getConstrainedValues();
        }
    }

    /**
     * Shared conversion of input String to LabEventType enumeration value
     */
    static class EventTypeValueConversionExpression extends SearchTerm.Evaluator<Object> {
        @Override
        public LabEventType evaluate(Object entity, SearchContext context) {
            return Enum.valueOf(LabEventType.class, context.getSearchValueString());
        }
    }

}
