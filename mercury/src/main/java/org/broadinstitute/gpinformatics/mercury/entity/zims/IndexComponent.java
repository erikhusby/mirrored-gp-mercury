package org.broadinstitute.gpinformatics.mercury.entity.zims;


import com.fasterxml.jackson.annotation.JsonProperty;
import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;

public class IndexComponent {

    @JsonProperty("hint")
    private String hint;

    @JsonProperty("sequence")
    private String sequence;

    public IndexComponent() {}

    public IndexComponent(IndexPosition thriftPosition,String sequence) {

        this.hint = thriftPosition.name();
        this.sequence = sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexComponent that = (IndexComponent) o;

        if (hint != null ? !hint.equals(that.hint) : that.hint != null) return false;
        if (sequence != null ? !sequence.equals(that.sequence) : that.sequence != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = hint != null ? hint.hashCode() : 0;
        result = 31 * result + (sequence != null ? sequence.hashCode() : 0);
        return result;
    }

    public String getSequence() {
        return sequence;
    }

    public String getHint() {
        return hint;
    }
}
