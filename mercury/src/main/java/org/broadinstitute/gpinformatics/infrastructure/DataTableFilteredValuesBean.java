package org.broadinstitute.gpinformatics.infrastructure;


import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named
@ConversationScoped
/**
 * Reusable class for holding filtered values from a PrimeFaces {@link org.primefaces.component.datatable.DataTable}
 * in conversation scope to be compatible with sortable columns.  This may not be enough conversation state to enable
 * all filtration for a given UI; see {@link org.broadinstitute.gpinformatics.athena.boundary.products.ProductsBean} for an example.
 */
public class DataTableFilteredValuesBean implements Serializable {

    @Inject
    private Conversation conversation;

    private List filteredValues;

    /**
     * Called by PrimeFaces DataTable filter to retrieve stashed filtered values
     * @return
     */
    public List getFilteredValues() {
        return filteredValues;
    }

    /**
     * Called by the PrimeFaces DataTable filter to stash filtered values
     *
     * @param filteredValues
     */
    public void setFilteredValues(List filteredValues) {
        this.filteredValues = filteredValues;
    }

    /**
     * Method called by client pages in their preRenderView hooks to make the conversation long-running
     */
    public void beginConversation() {
        if (conversation.isTransient()) {
            conversation.begin();
        }
    }
}
