package org.broadinstitute.gpinformatics.mercury.presentation.queue;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SimpleMessage;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.GenericQueueDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UrlBinding("/queue/Queue.action")
public class QueueActionBean extends CoreActionBean {

    private QueueType queueType;

    private GenericQueue queue;

    private Long queueGroupingId;
    private Integer positionToMoveTo;
    private String excludeVessels;

    private QueueGrouping queueGrouping;

    private Map<Long, BspUser> userIdToUsername = new HashMap<>();

    @Inject
    private QueueEjb queueEjb;

    @Inject
    private GenericQueueDao queueDao;

    @Inject
    private BSPUserList userList;


    @DefaultHandler
    @HandlesEvent("showQueuePage")
    public Resolution showQueuePage() {

        if (queueType == null) {
            flashMessage(new SimpleMessage("You attempted to load the Queue page without specifying a queue."));
            return new RedirectResolution(ProductOrderActionBean.class).addParameter("list", "");
        }

        queue = queueEjb.findQueueByType(queueType);

        return new ForwardResolution("/queue/show_queue.jsp");
    }

    @HandlesEvent("viewGrouping")
    public Resolution viewGrouping() {

        queueGrouping = queueDao.findById(QueueGrouping.class, queueGroupingId);

        List<Long> userIds = new ArrayList<>();
        for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
            if (queueEntity.getCompletedBy() != null) {
                userIds.add(queueEntity.getCompletedBy());
            }
        }

        for (Long userId : userIds) {
            userIdToUsername.put(userId, userList.getById(userId));
        }

        return new ForwardResolution("/queue/show_queue_grouping.jsp");
    }

    @HandlesEvent("moveToTop")
    public Resolution moveToTop() {

        queueEjb.moveToTop(queueType, queueGroupingId);

        return showQueuePage();
    }

    @HandlesEvent("moveToBottom")
    public Resolution moveToBottom() {

        queueEjb.moveToBottom(queueType, queueGroupingId);

        return showQueuePage();
    }

    @HandlesEvent("updatePositions")
    public Resolution updatePositions() {
        MessageCollection messageCollection = new MessageCollection();

        positionToMoveTo = null;
        queueEjb.reOrderQueue(queueGroupingId, positionToMoveTo, queueType, messageCollection);

        queue = queueEjb.findQueueByType(queueType);
        addMessages(messageCollection);
        return showQueuePage();
    }

    @HandlesEvent("excludeLabVessels")
    public Resolution excludeLabVessels() {
        MessageCollection messageCollection = new MessageCollection();

        String[] barcodes = excludeVessels.trim().toUpperCase().split("\\n");

        queueEjb.excludeItemsById(Arrays.asList(barcodes), queueType, messageCollection);

        queue = queueEjb.findQueueByType(queueType);
        addMessages(messageCollection);
        return showQueuePage();
    }

    public QueueType getQueueType() {
        return queueType;
    }

    public void setQueueType(QueueType queueType) {
        this.queueType = queueType;
    }

    public GenericQueue getQueue() {
        return queue;
    }

    public void setQueue(GenericQueue queue) {
        this.queue = queue;
    }

    public Long getQueueGroupingId() {
        return queueGroupingId;
    }

    public void setQueueGroupingId(Long queueGroupingId) {
        this.queueGroupingId = queueGroupingId;
    }

    public QueueGrouping getQueueGrouping() {
        return queueGrouping;
    }

    public void setQueueGrouping(QueueGrouping queueGrouping) {
        this.queueGrouping = queueGrouping;
    }

    public Map<Long, BspUser> getUserIdToUsername() {
        return userIdToUsername;
    }

    public void setUserIdToUsername(Map<Long, BspUser> userIdToUsername) {
        this.userIdToUsername = userIdToUsername;
    }

    public Integer getPositionToMoveTo() {
        return positionToMoveTo;
    }

    public void setPositionToMoveTo(Integer positionToMoveTo) {
        this.positionToMoveTo = positionToMoveTo;
    }

    public String getExcludeVessels() {
        return excludeVessels;
    }

    public void setExcludeVessels(String excludeVessels) {
        this.excludeVessels = excludeVessels;
    }
}
