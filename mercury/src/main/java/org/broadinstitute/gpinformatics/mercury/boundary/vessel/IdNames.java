package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class IdNames implements Serializable {
    private List<IdName> idNames;

    public IdNames() {
    }

    public List<IdName> getIdNames() {
        return idNames;
    }

    public void setIdNames(List<IdName> idNames) {
        this.idNames = idNames;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class IdName implements Serializable {
        protected long id;
        protected String name;

        /** No-arg constructor required for use by the JAX-RS framework. */
        @SuppressWarnings("UnusedDeclaration")
        public IdName() {
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
