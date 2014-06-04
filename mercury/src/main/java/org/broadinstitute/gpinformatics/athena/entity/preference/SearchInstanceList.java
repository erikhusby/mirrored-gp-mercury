package org.broadinstitute.gpinformatics.athena.entity.preference;

import org.broadinstitute.gpinformatics.infrastructure.ObjectMarshaller;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;

/**
 * Preference that holds all search instances for a scope.
 */
public class SearchInstanceList implements PreferenceDefinitionValue {
    private List<SearchInstance> searchInstances = new ArrayList<>();
    private ObjectMarshaller<SearchInstanceList> marshaller;

    public SearchInstanceList() throws JAXBException {
        marshaller = new ObjectMarshaller<>(SearchInstanceList.class);
    }

    public List<SearchInstance> getSearchInstances() {
        return searchInstances;
    }

    public void setSearchInstances(List<SearchInstance> searchInstances) {
        this.searchInstances = searchInstances;
    }

    @Override
    public String marshal() {
        return marshaller.marshal(this);
    }

    @Override
    public PreferenceDefinitionValue unmarshal(String xml) {
        return marshaller.unmarshal(xml);
    }

    public static class SearchInstanceListPreferenceDefinitionCreator implements PreferenceDefinitionCreator {
        @Override
        public PreferenceDefinitionValue create(String xml) throws Exception {
            NameValueDefinitionValue definitionValue = new NameValueDefinitionValue();

            // This unmarshalls that definition and populates the data map on a newly created definition.
            return definitionValue.unmarshal(xml);
        }
    }
}
