package org.broadinstitute.gpinformatics.infrastructure.experiments.seq;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/1/12
 * Time: 2:58 PM
 */
public class BaitSetName {
    private long id;
    private String name;

    public BaitSetName(final String name, final long id) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
