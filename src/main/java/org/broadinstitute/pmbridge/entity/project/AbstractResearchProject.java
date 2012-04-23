package org.broadinstitute.pmbridge.entity.project;

import org.broadinstitute.pmbridge.entity.common.ChangeEvent;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.person.Person;

import java.util.Date;

/**
 * Class to abstract a Research Project with PMBridge.
 * Some members are immutable fields and some are mutable.
 * Also there are no abstract methods here yet but I guess there will be.
 *
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 2:52 PM
 */
public abstract class AbstractResearchProject {

    public final Name title;
    public final ResearchProjectId id;
    public final ChangeEvent creation;
    private String synopsis;
    private ChangeEvent modification;


    //TODO hmc should Id be internally generated?
    public AbstractResearchProject(Person creator, Name title,
                                   ResearchProjectId id,
                                   String synopsis) {

        this.title = title;
        this.id = id;
        this.synopsis = synopsis;
        this.creation = new ChangeEvent(creator);
        this.modification = new ChangeEvent(new Date(this.creation.date.getTime()), creator);

    }

    // Getters
    public String getSynopsis() {
        return synopsis;
    }
    public ChangeEvent getModification() {
        return modification;
    }

    //Setters
    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }
    public void setModification(ChangeEvent modification) {
        this.modification = modification;
    }


}
