package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/1/12
 * Time: 2:58 PM
 */
public class OrganismName {
    private String commonName;
    private long id;
    private String name;

    public OrganismName(String name, String commonName, long id) {
        this.name = name;
        this.commonName = commonName;
        this.id = id;
    }

    public String getCommonName() {
        return commonName;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
