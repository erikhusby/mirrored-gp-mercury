package org.broadinstitute.gpinformatics.athena.presentation.projects;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This ActionBean handles the update of the basic PriceList.
 */
@SuppressWarnings("unused")
@UrlBinding(QuickQuoteActionBean.ACTIONBEAN_URL_BINDING)
public class QuickQuoteActionBean extends CoreActionBean {

    public static final String ACTIONBEAN_URL_BINDING = "/projects/quickQuote.action";

    public static final String QUICK_QUOTE = "quickQuote";
    public static final String VIEW_QUICK_QUOTE = "/projects/view_quick_quote.jsp";

    @Inject
    private PriceListCache priceListCache;

    private Quote quote;
    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    private List<String> userPlatforms;
    public List<String> getUserPlatforms() { return userPlatforms; }
    public void setUserPlatforms(List<String> userPlatforms) { this.userPlatforms = userPlatforms; }

    private PriceList priceList;
    public PriceList getPriceList() { return priceList; }
    public void setPriceList(PriceList priceList) { this.priceList = priceList; }

    public Collection<QuotePriceItem> getActivePriceItems() { return priceListCache.getQuotePriceItems(); }

    private List<String> platforms;
    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }

    private Map<String, String> platformOverages;
    public Map<String, String> getPlatformOverages() { return platformOverages; }
    public void setPlatformOverages(Map<String, String> platformOverages) { this.platformOverages = platformOverages; }

    private List<String> quantities;
    public List<String> getQuantities() { return quantities; }
    public void setQuantities(List<String> quantities) { this.quantities = quantities; }

    private List<String> overages;
    public List<String> getOverages() { return overages; }
    public void setOverages(List<String> overages) { this.overages = overages; }

    private List<String> priceItemIds;
    public List<String> getPriceItemIds() { return priceItemIds; }
    public void setPriceItemIds(List<String> priceItemIds) { this.priceItemIds = priceItemIds; }

    private String itemTotal;
    public String getItemTotal() {
        return itemTotal == null ? "" : itemTotal;
    }
    public void setItemTotal(String itemTotal) { this.itemTotal = itemTotal; }

    private String itemOverageTotal;
    public String getItemOverageTotal() {
        return itemOverageTotal == null ? "" : itemOverageTotal;
    }
    public void setItemOverageTotal(String itemOverageTotal) { this.itemOverageTotal = itemOverageTotal; }

    private String platformOverageTotal;
    public String getPlatformOverageTotal() {
        return platformOverageTotal == null ? "" : platformOverageTotal; 
    }
    public void setPlatformOverageTotal(String platformOverageTotal) { this.platformOverageTotal = platformOverageTotal; }

    @Validate(required = true)
    private String researchProject;
    public String getResearchProject() {
        return researchProject;
    }
    public void setResearchProject(String researchProject) {
        this.researchProject = researchProject;
    }

    @DefaultHandler
    @HandlesEvent(QUICK_QUOTE)
    public Resolution showQuickQuote() {
        return new ForwardResolution(VIEW_QUICK_QUOTE);
    }

    @HandlesEvent("downloadQuoteInfo")
    public Resolution downloadQuoteInfo() {

        return new Resolution() {
            @Override
            public void execute(HttpServletRequest request, HttpServletResponse response) throws Exception {

                setFileDownloadHeaders("text/text", "QuickQuote.txt");

                PrintWriter writer = response.getWriter();

                writer.println("Platform Overages");
                writer.println("--------------------------------------");
                for (String platform : getPlatformOverages().keySet()) {
                    writer.println("Platform: " + platform + " overage: " + getPlatformOverages().get(platform));
                }

                Map<String, QuotePriceItem> priceItemMap = getPriceItems();

                writer.println(" ");
                writer.println("Items");
                writer.println("--------------------------------------");

                if (getPriceItemIds() != null) {
                    for (int i=0; i<getPriceItemIds().size(); i++) {
                        if ((getPriceItemIds().get(i) != null) && (!getPriceItemIds().get(i).isEmpty())) {
                            QuotePriceItem priceItem = priceItemMap.get(getPriceItemIds().get(i));
                            Double quantity = getQuantity(i);
                            // If there is no quantity, just ignore
                            if ((priceItem != null) && (getQuantities() != null) && (getQuantities().size() > i) &&
                                (getQuantities().get(i) != null) && (!(getQuantities().get(i).isEmpty())) && (quantity > 0)) {
                                writer.println(getQuantities().get(i) + " at $" + priceItem.getPrice());
                                writer.println("    Platform:   " + priceItem.getPlatformName());
                                String category = (priceItem.getCategoryName() == null) ? "" : priceItem.getCategoryName();
                                writer.println("    Category:   " + category);
                                writer.println("    Price Item: " + priceItem.getName());
                                if ((getOverages() != null) && (getOverages().size() > i) &&
                                    (getOverages().get(i) != null) && (!getOverages().get(i).isEmpty())) {
                                    writer.println("    Overage:    " + getOverages().get(i));
                                }
                                writer.println("    --------------------------    ");
                            }
                        }
                    }
                }

                writer.println("");
                writer.println("");
                writer.println("    Item Total: " + getItemTotal());
                writer.println("    w/Item Overage: " + getItemOverageTotal());
                writer.println("    w/Platform Overage: " + getPlatformOverageTotal());
            }
        };
    }

    @HandlesEvent("createNewQuote")
    public Resolution createNewQuote() {

        String platform = null;
        if ((getUserPlatforms() != null) && (getUserPlatforms().size() > 0)) {
            platform = getUserPlatforms().get(0);
        }

        Quote quote = new Quote();
        quote.setName("quick-quote-" + new Date().getTime());
        quote.setApprovalStatus(ApprovalStatus.PENDING);
//        quote.set
//        quote.setPlatform(platform);
//        quote.setQuoteType(QuoteType.STANDARD);
//        quote.setFundingMethod(FundingMethod.PERCENT);
//        quote.setCreateUserId(getDomainUser().getId());
//        quote.setCreationDate(new Date());
//        quote.setUpdateDate(quote.getCreationDate());
//        quote.setUpdateUserId(quote.getCreateUserId());
//        setQuote(getQuoteManager().saveQuote(quote));
//        // Flush the quote dao so we can have a populated quote id in the db for the upcoming user association.
//        getQuoteManager().flushQuoteDAO();
//        getQuoteManager().addUserAssociation(
//                quote,
//                Collections.singletonList(getDomainUser()),
//                QuoteManager.PM_USER_ASSOCIATION);
//        getQuoteManager().addUserAssociation(
//                quote,
//                Collections.singletonList(getQuoteManager().getDomainUserByUsername(PropertyFileReader.getAppProperty("default.finance.contact"))),
//                QuoteManager.FINANCE_USER_ASSOCIATION);

        // For each full, valid quote item, add it to the quote
        Map<String, QuotePriceItem> priceItemMap = getPriceItems();
        if (getPriceItemIds() != null) {
            for (int i=0; i<getPriceItemIds().size(); i++) {
                if ((getPriceItemIds().get(i) != null) && (!getPriceItemIds().get(i).isEmpty())) {
                    QuotePriceItem priceItem = priceItemMap.get(getPriceItemIds().get(i));

                    Double quantity = getQuantity(i);
                    if ((priceItem != null) && (getQuantities() != null) && (getQuantities().size() > i) &&
                        (getQuantities().get(i) != null) && (!(getQuantities().get(i).isEmpty())) && (quantity > 0)) {

//                        QuoteItem quoteItem = new QuoteItem();
//                        quoteItem.setPriceItem(priceItem);
//                        quoteItem.setQuote(getQuote());
//
//                        if ((getOverages() != null) && (getOverages().size() > i) &&
//                            (getOverages().get(i) != null) && (!getOverages().get(i).isEmpty())) {
//                            quoteItem.setOveragePercent(getParsedIntValue(getOverages().get(i)));
//                        } else {
//                            quoteItem.setOveragePercent(0);
//                        }
//
//                        quoteItem.setQuantity(quantity);
//
//                        quoteItem.setCreateUserId(quote.getCreateUserId());
//                        quoteItem.setCreationDate(quote.getCreationDate());
//                        quoteItem.setUpdateDate(quote.getCreationDate());
//                        quoteItem.setUpdateUserId(quote.getCreateUserId());
//
//                        getQuoteManager().saveQuoteItem(quoteItem, getDomainUser());
                    }
                }
            }
        }

        return new RedirectResolution(ResearchProjectActionBean.class, ResearchProjectActionBean.VIEW_ACTION).addParameter("researchProject", researchProject);
    }

    private int getParsedIntValue(String valueString) {
        int value = 0;
        try {
            value = Integer.parseInt(valueString);
        } catch (Exception ex) {
            // do nothing
        }

        return value;
    }

    private Double getParsedDoubleValue(String valueString) {
        Double value = 0.0D;
        try {
            value = Double.parseDouble(valueString);
        } catch (Exception ex) {
            // do nothing
        }

        return value;
    }

    private Double getQuantity(int i) {

        if ((getQuantities() == null) || (getQuantities().isEmpty())) {
            return null;
        }
        
        // If there is an error parsing, set to null
        Double value = null;
        try {
            value = Double.parseDouble(getQuantities().get(i));
        } catch (Exception ex) {
            // Ignore errors
        }

        if ((value == null) && (getQuantities().size() > i)) {
            getQuantities().set(i, null);
        }

        return value;
    }

    private Map<String, QuotePriceItem> getPriceItems() {
        // Set up the mapping.
        Map<String, QuotePriceItem> priceItemMap = new HashMap<>();
        for (QuotePriceItem priceItem : priceListCache.getQuotePriceItems()) {
            priceItemMap.put(priceItem.getId(), priceItem);
        }

        return priceItemMap;
    }
}

