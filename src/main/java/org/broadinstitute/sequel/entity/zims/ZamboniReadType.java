package org.broadinstitute.sequel.entity.zims;

import org.codehaus.jackson.annotate.JsonAutoDetect;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public enum ZamboniReadType {
    TEMPLATE,INDEX
}
