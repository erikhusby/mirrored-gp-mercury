package org.broadinstitute.sequel.entity.authentication;

/**
 *
 * AuthorizedRole is an entity that defines the roles that a user can have within the system.  These roles are utilized
 * in the Authorization process of the application level security
 *
 *
 * {@see PageAuthorization}
 *
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 4:02 PM
 */
public class AuthorizedRole {

    private Long roleId;
    private String roleName;

    /**
     * Basic constructor taking in the name of the role
     * @param roleNameInIn
     */
    public AuthorizedRole(String roleNameInIn) {
        setRoleName(roleNameInIn);
    }

    /**
     * Basic accessor for retrieving the database generated ID for a particular Authorized Role Instance
     * @return
     */
    public Long getRoleId() {
        return roleId;
    }

    /**
     * Basic setter for setting the unique ID for this role
     * @param roleIdIn
     */
    public void setRoleId(Long roleIdIn) {
        roleId = roleIdIn;
    }

    /**
     * Basic accessor for retrieving the unique name for the role
     * @return
     */
    public String getRoleName() {
        return roleName;
    }


    /**
     * Basic setter for defining the name for the role
     * @param roleNameIn
     */
    public void setRoleName(String roleNameIn) {

        if(null == roleNameIn) {
            throw new NullPointerException("The role name cannot be null");
        }

        if(roleNameIn.isEmpty()) {
            throw new IllegalArgumentException("The role name must be set");
        }

        roleName = roleNameIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AuthorizedRole))
            return false;

        AuthorizedRole that = (AuthorizedRole) o;

        if (!roleName.equals(that.roleName))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return roleName.hashCode();
    }
}
