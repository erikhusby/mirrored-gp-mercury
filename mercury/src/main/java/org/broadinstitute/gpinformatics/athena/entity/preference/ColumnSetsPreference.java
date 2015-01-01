package org.broadinstitute.gpinformatics.athena.entity.preference;

import org.broadinstitute.gpinformatics.infrastructure.ObjectMarshaller;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;

import javax.xml.bind.JAXBException;
import java.util.List;

/**
 * A list of column sets, where each set is a name and a list of column definition names.
 */
public class ColumnSetsPreference implements PreferenceDefinitionValue {

    public static class ColumnSet {
        private String name;
        private ColumnEntity columnEntity;
        // todo jmt visibility expression?
        private List<String> columnDefinitions;

        public String getName() {
            return name;
        }

        public ColumnEntity getColumnEntity() {
            return columnEntity;
        }

        public List<String> getColumnDefinitions() {
            return columnDefinitions;
        }
    }

    private ObjectMarshaller<ColumnSetsPreference> marshaller;

    private List<ColumnSet> columnSets;

    public ColumnSetsPreference() throws JAXBException {
        marshaller = new ObjectMarshaller<>(ColumnSetsPreference.class);
    }

    public List<ColumnSet> getColumnSets() {
        return columnSets;
    }

    @Override
    public String marshal() {
        return marshaller.marshal(this);
    }

    @Override
    public PreferenceDefinitionValue unmarshal(String xml) {
        return marshaller.unmarshal(xml);
    }

    public static class ColumnSetsPreferenceDefinitionCreator implements PreferenceDefinitionCreator {
        @Override
        public PreferenceDefinitionValue create(String xml) throws Exception {
            NameValueDefinitionValue definitionValue = new NameValueDefinitionValue();

            // This unmarshalls that definition and populates the data map on a newly created definition.
            return definitionValue.unmarshal(xml);
        }
    }
}
