package org.broadinstitute.sequel.entity.authentication;

/**
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 4:02 PM
 */
public class AuthorizedRole {

    private Long roleId;
    private String roleName;

    public AuthorizedRole(String roleNameInIn) {
        roleName = roleNameInIn;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleIdIn) {
        roleId = roleIdIn;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleNameIn) {
        roleName = roleNameIn;
    }
}
