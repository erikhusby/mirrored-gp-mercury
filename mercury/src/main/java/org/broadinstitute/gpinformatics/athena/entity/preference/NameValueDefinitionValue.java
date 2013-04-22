package org.broadinstitute.gpinformatics.athena.entity.preference;

import org.broadinstitute.gpinformatics.infrastructure.ObjectMarshaller;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a simple name-value preference.
 */
@XmlRootElement(name = "nameValuePreferenceDefinition")
public class NameValueDefinitionValue implements PreferenceDefinitionValue {

    private ObjectMarshaller<NameValueDefinitionValue> marshaller;

    private HashMap<String, List<String>> dataMap;

    private NameValueDefinitionValue(NameValueDefinitionValue definitionValue) {
        this.dataMap = definitionValue.getDataMap();
    }

    public NameValueDefinitionValue() throws JAXBException {
        dataMap = new HashMap<String, List<String>> ();
        marshaller = new ObjectMarshaller<NameValueDefinitionValue>(NameValueDefinitionValue.class);
    }

    @XmlJavaTypeAdapter(NameValueAdapter.class)
    @XmlElement(name = "dataMap")
    public HashMap<String, List<String>> getDataMap() {
        return dataMap;
    }

    public void setDataMap(HashMap<String, List<String>> dataMap) {
        this.dataMap = dataMap;
    }

    public void put(String key, List<String> values) {
        dataMap.put(key, values);
    }

    public void put(String key, String value) {
        dataMap.put(key, Collections.singletonList(value));
    }

    @Override
    public String marshal() {
        return marshaller.marshal(this);
    }

    @Override
    public PreferenceDefinitionValue unmarshal(String xml) {
        return marshaller.unmarshal(xml);
    }

    public static class NameValuePreferenceDefinitionCreator implements PreferenceDefinitionCreator {
        @Override
        public PreferenceDefinitionValue create(String xml) throws Exception {
            NameValueDefinitionValue definitionValue = new NameValueDefinitionValue();

            // This unmarshalls that definition and populates the data map on a newly created definition.
            return definitionValue.unmarshal(xml);
        }
    }

    /**
     * This is an adapter to stream the map using the jaxb annotations above. This lets us marshall and unmarshall
     * the data to a string for storage in the preferences table.
     */
    public static class NameValueAdapter extends XmlAdapter<NameValueAdapter.AdaptedMap, HashMap<String, List<String>>> {

        public static class AdaptedMap {
            public List<Entry> entry = new ArrayList<Entry>();
        }

        public static class Entry {
            public String key;
            public List<String> value;
        }

        @Override
        public HashMap<String, List<String>> unmarshal(AdaptedMap adaptedMap) throws Exception {
            HashMap<String, List<String>> map = new HashMap<String, List<String>>();
            for (Entry entry : adaptedMap.entry) {
                map.put(entry.key, entry.value);
            }

            return map;
        }

        @Override
        public AdaptedMap marshal(HashMap<String, List<String>> map) throws Exception {
            AdaptedMap adaptedMap = new AdaptedMap();
            for (Map.Entry<String, List<String>> mapEntry : map.entrySet()) {
                Entry entry = new Entry();
                entry.key = mapEntry.getKey();
                entry.value = mapEntry.getValue();
                adaptedMap.entry.add(entry);
            }

            return adaptedMap;
        }
    }

}
