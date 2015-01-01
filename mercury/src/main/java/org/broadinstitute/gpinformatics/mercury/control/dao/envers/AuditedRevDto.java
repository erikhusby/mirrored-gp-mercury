package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditedRevDto {
    private final Long revId;
    private final Date revDate;
    private final String username;
    private final List<String> entityTypeNames;

    public AuditedRevDto(Long revId, Date revDate, String username, Collection<String> entityClassNames) {
        this.revId = revId;
        this.revDate = revDate;
        this.username = username;
        this.entityTypeNames = new ArrayList<>(entityClassNames);
    }

    public Long getRevId() {
        return revId;
    }

    public Date getRevDate() {
        return revDate;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getEntityTypeNames() {
        return entityTypeNames;
    }

    public static Comparator<AuditedRevDto> BY_REV_ID = new Comparator<AuditedRevDto>() {
        @Override
        public int compare(AuditedRevDto o1, AuditedRevDto o2) {
            return o1.getRevId().compareTo(o2.getRevId());
        }
    };

    public static Comparator<AuditedRevDto> BY_REV_DATE = new Comparator<AuditedRevDto>() {
        @Override
        public int compare(AuditedRevDto o1, AuditedRevDto o2) {
            return o1.getRevDate().compareTo(o2.getRevDate());
        }
    };

    /** Returns a map of revId -> List of AuditedRevDto */
    public static Map<Long, List<AuditedRevDto>> mappedByRevId(Collection<AuditedRevDto> dtos) {
        Map<Long, List<AuditedRevDto>> map = new HashMap<>();
        for (AuditedRevDto dto : dtos) {
            List<AuditedRevDto> list = map.get(dto.getRevId());
            if (list == null) {
                list = new ArrayList<AuditedRevDto>();
                map.put(dto.getRevId(), list);
            }
            list.add(dto);
        }
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditedRevDto)) {
            return false;
        }

        AuditedRevDto that = (AuditedRevDto) o;

        if (!revDate.equals(that.revDate)) {
            return false;
        }
        if (!revId.equals(that.revId)) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = revId.hashCode();
        result = 31 * result + revDate.hashCode();
        result = 31 * result + (username != null ? username.hashCode() : 0);
        return result;
    }
}
