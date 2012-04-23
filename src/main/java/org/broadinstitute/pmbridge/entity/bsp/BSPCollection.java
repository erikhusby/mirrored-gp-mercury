package org.broadinstitute.pmbridge.entity.bsp;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:25 PM
 */
public class BSPCollection {

    public final BSPCollectionID id;
    public final String name;

    public BSPCollection(BSPCollectionID id, String name) {
        this.id = id;
        this.name = name;
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
