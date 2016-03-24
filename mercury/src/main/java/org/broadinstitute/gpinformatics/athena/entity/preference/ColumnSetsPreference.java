package org.broadinstitute.gpinformatics.athena.entity.preference;

import org.broadinstitute.gpinformatics.infrastructure.ObjectMarshaller;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * A list of column sets, where each set is a name and a list of column definition names.
 */
@XmlRootElement
public class ColumnSetsPreference implements PreferenceDefinitionValue {

    public static class ColumnSet {
        private String name;
        private ColumnEntity columnEntity;
        // todo jmt visibility expression?
        private List<String> columnDefinitions;

        public ColumnSet() {
        }

        public ColumnSet(String name, ColumnEntity columnEntity, List<String> columnDefinitions) {
            this.name = name;
            this.columnEntity = columnEntity;
            this.columnDefinitions = columnDefinitions;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ColumnEntity getColumnEntity() {
            return columnEntity;
        }

        public void setColumnEntity(ColumnEntity columnEntity) {
            this.columnEntity = columnEntity;
        }

        public List<String> getColumnDefinitions() {
            return columnDefinitions;
        }

        public void setColumnDefinitions(List<String> columnDefinitions) {
            this.columnDefinitions = columnDefinitions;
        }
    }

    private ObjectMarshaller<ColumnSetsPreference> marshaller;

    private List<ColumnSet> columnSets = new ArrayList<>();

    public ColumnSetsPreference() throws JAXBException {
        marshaller = new ObjectMarshaller<>(ColumnSetsPreference.class);
    }

    public List<ColumnSet> getColumnSets() {
        return columnSets;
    }

    public void setColumnSets(List<ColumnSet> columnSets) {
        this.columnSets = columnSets;
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
            ColumnSetsPreference definitionValue = new ColumnSetsPreference();
            return definitionValue.unmarshal(xml);
        }
    }
}
