package org.broadinstitute.gpinformatics.athena.entity.person;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/11/12
 * Time: 2:24 PM
 */
public enum RoleType {

    BROAD_SCIENTIST("Broad Scientist"),
    EXTERNAL("External Collaborator"),
    PM_IN_CHARGE("Principal Project Manager"),
    PM("Project Manager"),
    PDM("Product Manager"),
    BROAD_PI("Broad Investigator"),
    SCIENTIST("Sponsoring Scientist"),
    UNSPECIFIED(null);

    private String description;

    RoleType(String description) {
        this.description = description;
    }

    public static RoleType findByName(String searchName) {
        for (RoleType theEnum : RoleType.values()) {
            if (theEnum.name().equals(searchName)) {
                return theEnum;
            }
        }

        return null;
    }

    public static RoleType findByDescription(String searchDescription) {
        for (RoleType theEnum : RoleType.values()) {
            if ((theEnum.description != null) && (theEnum.description.equals(searchDescription))) {
                return theEnum;
            }
        }

        return null;
    }
}
