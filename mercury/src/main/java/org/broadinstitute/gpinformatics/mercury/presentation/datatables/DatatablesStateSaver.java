/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2017 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.datatables;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceEjb;
import org.broadinstitute.gpinformatics.athena.entity.preference.NameValueDefinitionValue;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatatablesStateSaver {
    private static final ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    private PreferenceEjb preferenceEjb;
    @Inject
    private PreferenceDao preferenceDao;

    public static final String SAVE_SEARCH_DATA = "saveSearchData";
    public static final String TABLE_STATE_KEY = "tableState";

    private static final Log log = LogFactory.getLog(DatatablesStateSaver.class);
    private UserBean userBean;
    private PreferenceType preferenceType;
    private State state;
    private Map<String, Boolean> columnVisibilityMap = new HashMap<>();
    private String tableState = "{}";

    public DatatablesStateSaver() {
    }

    public DatatablesStateSaver(PreferenceType preferenceType) {
        this.preferenceType = preferenceType;
    }
    public DatatablesStateSaver(PreferenceType preferenceType, PreferenceDao preferenceDao, PreferenceEjb preferenceEjb) {
        this.preferenceType = preferenceType;
        this.preferenceDao = preferenceDao;
        this.preferenceEjb = preferenceEjb;
    }

    public void saveTableData(String tableState) throws Exception {
        this.tableState=tableState;
        NameValueDefinitionValue definitionValue = loadSearchData();
        state = objectMapper.readValue(tableState, State.class);
        if (state.getColumns().isEmpty()) {
            state = new State();
        }
        definitionValue.put(TABLE_STATE_KEY, Collections.singletonList(objectMapper.writeValueAsString(state)));

        preferenceEjb.add(userBean.getBspUser().getUserId(), preferenceType, definitionValue);
    }


    private NameValueDefinitionValue loadSearchData() throws Exception {
        List<Preference> preferences =
                preferenceDao.getPreferences(userBean.getBspUser().getUserId(), preferenceType);
        if (!preferences.isEmpty()) {
            Preference preference = preferences.iterator().next();
            try {
                NameValueDefinitionValue definitionValue =
                        (NameValueDefinitionValue) preference.getPreferenceDefinition().getDefinitionValue();

                return definitionValue;
            } catch (Throwable t) {
                log.error("Could not read search preference");
            }
        }
        return new NameValueDefinitionValue();
    }

    private void loadTableState() {
        try {
            NameValueDefinitionValue nameValueDefinitionValue = loadSearchData();
            List<String> tableStatePreferenceValue = nameValueDefinitionValue.getDataMap().get(TABLE_STATE_KEY);
            if (CollectionUtils.isNotEmpty(tableStatePreferenceValue)) {
                String tableState = tableStatePreferenceValue.iterator().next();
                state = objectMapper.readValue(tableState, State.class);
                buildColumnVisibilityMap();
            }
        } catch (Exception e) {
            log.error("Load table state preference", e);
        }
        if (state == null) {
            state = new State();
        }
    }


    private void buildColumnVisibilityMap() throws Exception {
        State state = getTableState();
        if (tableState != null) {
            for (Column column : state.getColumns()) {
                boolean visible = true;
                if (column!=null){
                    visible = column.isVisible();
                }
                String headerName = column.getHeaderName();
                if (StringUtils.isNotBlank(headerName)) {
                    columnVisibilityMap.put(headerName, visible);
                }
            }
        }
    }


    public boolean showColumn(String columnName) {
        return columnVisibilityMap.isEmpty() ||
               (columnVisibilityMap.get(columnName) != null && columnVisibilityMap.get(columnName));
    }

    public String getTableStateJson() throws Exception {
        JSONObject jsonObject = new JSONObject(getTableState());
        return jsonObject.toString();
    }

    private void setState(State state) {
        this.state = state;
    }

    public State getTableState() {
        if (state == null) {
            loadTableState();
        }

        return state;
    }

    private void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    @Override
    public String toString() {
        String tableStateJson="{}";
        try {
            tableStateJson = getTableStateJson();
            return tableStateJson;
        } catch (Exception e) {
            log.error("Could not build json value from persisted state", e);
        }
        return tableStateJson;
    }
}
