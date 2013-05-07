package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class handles the support for autocomplete widgets in Mercury. There is a fair amount of boiler plate code
 * needed to make the tokens work and this class handles that.
 *
 * @author hrafal
 */
public abstract class TokenInput<TOKEN_OBJECT> {

    protected static final boolean SINGLE_LINE_FORMAT = true;
    protected static final boolean DOUBLE_LINE_FORMAT = false;

    private static final short SINGLE_LINE_MAX_DISPLAYED = 20;
    private static final short DOUBLE_LINE_MAX_DISPLAYED = 12;

    // These are generic menu entry divs that go in between <li> tags in the token input javascript
    // There are two forms (single line and double line) which have different format classes. Subclasses populate
    // just the details.
    private static final String MENU_ONE_LINE = "<div class=\"ac-dropdown-text\">{0}</div>";
    private static final String MENU_TWO_LINE = MENU_ONE_LINE + "<div class=\"ac-dropdown-subtext\">{1}</div>";
    private static final String EXTRA_COUNT_LINE = "<div class=\"ac-dropdown-subtext\">...and {0} more</div>";

    // The UI will have a comma separated list of keys that needs to be
    // parsed out for turning into real TOKEN_OBJECTs
    private String listOfKeys = "";

    /** A cache of the key list as a list of tokens. */
    private List<TOKEN_OBJECT> tokenObjects = Collections.emptyList();

    /** A cache of the result of calling generateCompleteData. */
    private String completeDataCache;

    private final String formatString;
    private final int maxDisplayed;

    public TokenInput(boolean isSingleLine) {
        if (isSingleLine) {
            formatString = MENU_ONE_LINE;
            maxDisplayed = SINGLE_LINE_MAX_DISPLAYED;
        } else {
            formatString = MENU_TWO_LINE;
            maxDisplayed = DOUBLE_LINE_MAX_DISPLAYED;
        }
    }

    // The UI needs to get at these so they must be public.
    public void setListOfKeys(String listOfKeys) {
        this.listOfKeys = listOfKeys;
        if (StringUtils.isBlank(listOfKeys)) {
            tokenObjects = Collections.emptyList();
        } else {
            String[] keys = listOfKeys.split(getSeparator());

            tokenObjects = new ArrayList<TOKEN_OBJECT>(keys.length);
            for (String key : keys) {
                tokenObjects.add(getById(key.trim()));
            }
        }
    }

    private String getSeparator() {
        return ",,,,,";
    }

    public String getListOfKeys() {
        return listOfKeys;
    }

    public List<TOKEN_OBJECT> getTokenObjects() {
        // setListOfKeys updates the objects, so when the action bean first populates the list of keys
        // directly, there will be no objects. This ensures that there will be objects around later.
        if (!StringUtils.isBlank(listOfKeys) && CollectionUtils.isEmpty(tokenObjects)) {
            setListOfKeys(listOfKeys);
        }

        return tokenObjects;
    }

    /**
     * Generate the completion data for this TokenInput.  Only called by TokenInput itself.  Subclasses should return
     * the empty string, not null, for cases where no completions exist.
     * * @return the completion data
     * @throws JSONException if an error occurs
     */
    public String generateCompleteData() throws JSONException {

        JSONArray itemList = new JSONArray();
        for (TOKEN_OBJECT tokenObject : getTokenObjects()) {
            itemList.put(createAutocomplete(tokenObject));
        }

        return itemList.toString();
    }

    /**
     * This gets the JSON object with the generic token input data for displaying the menu list (along with enough
     * information to populate the token list with any selected item.
     *
     * @param tokenObjects The generic object that is underlying the entries.
     *
     * @return The JSON string with the menu fields
     * @throws JSONException Any errors making JSON objects
     */
    protected String createItemListString(List<TOKEN_OBJECT> tokenObjects) throws JSONException {

        int extraCount = 0;

        // assume chunk is full size
        List<TOKEN_OBJECT> tokenObjectChunk = tokenObjects;

        // If there are more items than we want to efficiently display, grab the first chunk of appropriate size
        if (tokenObjects.size() > maxDisplayed) {
            extraCount = tokenObjects.size() - maxDisplayed;
            tokenObjectChunk = tokenObjects.subList(0, maxDisplayed);
        }

        // Create the json array of items for the chunk
        JSONArray itemList = new JSONArray();
        JSONObject item = null;
        for (TOKEN_OBJECT tokenObject : tokenObjectChunk) {
            item = createAutocomplete(tokenObject);
            itemList.put(item);
        }

        // If there are extra items, this inserts a second line into the second line so that the count gets
        // grouped with the last item for selection. The style places a line, so it looks like a footer.
        if ((item != null) && (extraCount > 0)) {
             item.put("extraCount", MessageFormat.format(EXTRA_COUNT_LINE, extraCount));
        }

        return itemList.toString();
    }

    /**
     * Populate the item list with its appropriate token object. Overrides are for special null handling.
     *
     * @param tokenObject The token object itself
     *
     * @return The created item in case we want to do something with it.
     * @throws JSONException Any errors
     */
    protected JSONObject createAutocomplete(TOKEN_OBJECT tokenObject) throws JSONException {
        JSONObject item = getJSONObject(getTokenId(tokenObject), getTokenName(tokenObject));
        item.put("dropdownItem", formatMessage(formatString, tokenObject));
        return item;
    }

    // To build the generic JSON object for token input
    protected abstract String getTokenId(TOKEN_OBJECT tokenObject);
    protected abstract String getTokenName(TOKEN_OBJECT tokenObject);

    // Format the message into a string
    protected abstract String formatMessage(String messageString, TOKEN_OBJECT tokenObject);

    /**
     * During an action bean session there are a number of places where the token objects may be used, this makes
     * sure to fetch the list only once.
     *
     * @return The JSON string for the token objects
     * @throws JSONException Any errors generating JSON
     */
    public final String getCompleteData() throws JSONException {
        if (completeDataCache == null) {
            completeDataCache = generateCompleteData();
        }

        return completeDataCache;
    }

    protected abstract TOKEN_OBJECT getById(String key);

    public void setup(Long... longIds) {
        setListOfKeys(StringUtils.join(longIds, getSeparator()));
    }

    public void setup(String... ids) {
        setListOfKeys(StringUtils.join(ids, getSeparator()));
    }

    /**
     * Given the arguments, create a JSON object that represents it.  This is used to create the response for a
     * JQeury Tokeninput auto-complete UI.
     *
     * @param id the ID
     * @param name the name
     * @return the JSON object that contains these fields.
     * @throws JSONException
     */
    public static JSONObject getJSONObject(String id, String name) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("name", name);
        item.put("readonly", false);
        return item;
    }
}
