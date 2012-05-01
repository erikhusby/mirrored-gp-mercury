package org.broadinstitute.sequel.entity.authentication;

/**
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 4:02 PM
 */
public class AuthorizedGroup {

    private Long groupId;
    private String groupName;

    public AuthorizedGroup(String groupNameIn) {
        groupName = groupNameIn;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupIdIn) {
        groupId = groupIdIn;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupNameIn) {
        groupName = groupNameIn;
    }
}
