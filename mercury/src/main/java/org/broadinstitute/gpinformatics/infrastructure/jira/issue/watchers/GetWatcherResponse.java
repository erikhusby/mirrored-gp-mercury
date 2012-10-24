package org.broadinstitute.gpinformatics.infrastructure.jira.issue.watchers;

import org.broadinstitute.gpinformatics.infrastructure.jira.issue.AvatarInfo;

import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 10/3/12
 *         Time: 3:39 PM
 */
public class GetWatcherResponse {

    private String self;
    private boolean isWatching;
    private byte watchCount;
    private List<Watcher> watchers;

    public GetWatcherResponse ( String selfIn, boolean watchingIn, byte watchCountIn, List<Watcher> watchersIn ) {
        self = selfIn;
        isWatching = watchingIn;
        watchCount = watchCountIn;
        watchers = watchersIn;
    }

    public class Watcher {
        private String self;
        private String name;
        private String displayName;

        private AvatarInfo avatarUrls;

        private Boolean active;
    }

    public String getSelf ( ) {
        return self;
    }

    public boolean isWatching ( ) {
        return isWatching;
    }

    public byte getWatchCount ( ) {
        return watchCount;
    }

    public List<Watcher> getWatchers ( ) {
        return watchers;
    }
}
