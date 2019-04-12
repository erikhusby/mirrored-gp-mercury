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

import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Dependent
public class DatatablesStateSaver {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private PreferenceEjb preferenceEjb;
    private PreferenceDao preferenceDao;

    public static final String SAVE_SEARCH_DATA = "saveSearchData";
    public static final String TABLE_STATE_KEY = "tableState";

    private static final Log log = LogFactory.getLog(DatatablesStateSaver.class);
    private UserBean userBean;
    private PreferenceType preferenceType;
    private State state;
    private Map<String, Boolean> columnVisibilityMap = new HashMap<>();
    private String tableState;

    public DatatablesStateSaver() {

    }

    @Inject
    public DatatablesStateSaver(UserBean userBean, PreferenceDao preferenceDao, PreferenceEjb preferenceEjb) {
        this.userBean = userBean;
        this.preferenceDao = preferenceDao;
        this.preferenceEjb = preferenceEjb;
    }

    public void saveTableData(String tableState) throws Exception {
        this.tableState = tableState;
        NameValueDefinitionValue definitionValue = new NameValueDefinitionValue();
        definitionValue.put(TABLE_STATE_KEY, Collections.singletonList(tableState));
        preferenceEjb.add(userBean.getBspUser().getUserId(), preferenceType, definitionValue);
    }

    private void loadSearchData() throws Exception {
        List<Preference> preferences =
                preferenceDao.getPreferences(userBean.getBspUser().getUserId(), preferenceType);
        NameValueDefinitionValue definitionValue = null;
        if (!preferences.isEmpty()) {
            Preference preference = preferences.iterator().next();
            definitionValue = (NameValueDefinitionValue) preference.getPreferenceDefinition().getDefinitionValue();

        } else {
            definitionValue = new NameValueDefinitionValue();
            state = new State();
            tableState = objectMapper.writeValueAsString(state);
        }
        List<String> tableStatePreferenceValue = definitionValue.getDataMap().get(TABLE_STATE_KEY);
        if (CollectionUtils.isNotEmpty(tableStatePreferenceValue)) {
            tableState = tableStatePreferenceValue.iterator().next();
        }
    }

    private void loadTableState() {
        if (tableState == null) {
            try {
                loadSearchData();
            } catch (Exception e) {
                log.error("Could not load search preferences.");
            }
        }
        if (StringUtils.isNotBlank(tableState)) {
            try {
                state = objectMapper.readValue(tableState, State.class);
            } catch (IOException e) {
                log.error("Could not load preferences.", e);
            }
        }
    }

    private void buildColumnVisibilityMap() {
        State state = getTableState();
        if (state != null) {
            for (Column column : state.getColumns()) {
                boolean visible = true;
                if (column!=null){
                    visible = column.isVisible();
                }
                String headerName = column.getHeaderName();
                if (StringUtils.isNotBlank(headerName)) {
                    columnVisibilityMap.put(headerName.replaceAll("\\s{2}",""), visible);
                }
            }
        }
    }

    public boolean showColumn(String columnName) {
        try {
            if (columnVisibilityMap.isEmpty()) {
                buildColumnVisibilityMap();
            }
        } catch (Exception e) {
            log.error("Error loading columnVisibilityMap", e);
            return true;
        }
        return columnVisibilityMap.isEmpty() ||
               (columnVisibilityMap.get(columnName) != null && columnVisibilityMap.get(columnName));
    }

    public String getTableStateJson() {
        if (tableState == null) {
            loadTableState();
        }
        return tableState;
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

    public void setPreferenceType(PreferenceType preferenceType) {
        this.preferenceType = preferenceType;
    }

    @Override
    public String toString() {
        return getTableStateJson();
    }

    public Collection<String> visibleColumns() {
        buildColumnVisibilityMap();
        Set<String> visibleColumns = new HashSet<>();
        for (Map.Entry<String, Boolean> columnVisiblityEntry : columnVisibilityMap.entrySet()) {
            if (columnVisiblityEntry.getValue()) {
                visibleColumns.add(columnVisiblityEntry.getKey());
            }
        }
        return visibleColumns;
    }
}
