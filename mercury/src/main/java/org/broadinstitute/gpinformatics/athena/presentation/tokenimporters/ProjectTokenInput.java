package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Token Input support for Research Projects.
 *
 * @author hrafal
 */
public class ProjectTokenInput extends TokenInput<ResearchProject> {

    @Inject
    private ResearchProjectDao researchProjectDao;
    private String tokenObject;

    public ProjectTokenInput() {
    }

    @Override
    protected ResearchProject getById(String key) {
        return researchProjectDao.findByBusinessKey(key);
    }

    public String getJsonString(String query) throws JSONException {
        Collection<ResearchProject> projects = researchProjectDao.searchProjects(query);
        return createItemListString(new ArrayList<ResearchProject>(projects));
    }

    @Override
    protected JSONObject createAutocomplete(JSONArray itemList, ResearchProject project) throws JSONException {
        JSONObject item = getJSONObject(project.getBusinessKey(), project.getTitle(), false);
        String list = "<div class=\"ac-dropdown-text\">" + project.getTitle() + "</div>";
        item.put("dropdownItem", list);
        itemList.put(item);

        return item;
    }

    public String getTokenObject() {
        List<ResearchProject> projects = getTokenObjects();

        if ((projects == null) || projects.isEmpty()) {
            return "";
        }

        return projects.get(0).getBusinessKey();
    }
}
