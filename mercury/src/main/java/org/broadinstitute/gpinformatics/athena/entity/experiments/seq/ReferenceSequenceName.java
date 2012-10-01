package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/1/12
 * Time: 2:58 PM
 */
public class ReferenceSequenceName {
    private long id;
    private String name;

    public ReferenceSequenceName(String name, long id) {
        this.name = name;
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
