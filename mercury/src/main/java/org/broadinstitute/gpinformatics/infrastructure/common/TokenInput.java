package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class handles the support for autocomplete widgets in Mercury. There is a fair amount of boiler plate code
 * needed to make the tokens work and this class handles that.
 *
 * @author hrafal
 */
public abstract class TokenInput<TOKEN_OBJECT> {

    public static final String TOKEN_INPUT_SEPARATOR = ",,,,,";

    protected static final boolean SINGLE_LINE_FORMAT = true;
    protected static final boolean DOUBLE_LINE_FORMAT = false;

    private static final short SINGLE_LINE_MAX_DISPLAYED = 20;
    private static final short DOUBLE_LINE_MAX_DISPLAYED = 12;

    // These are generic menu entry divs that go in between <li> tags in the token input javascript
    // There are two forms (single line and double line) which have different format classes. Subclasses populate
    // just the details.
    /** The one line menu format string. */
    private static final String MENU_ONE_LINE = "<div class=\"ac-dropdown-text\">{0}</div>";
    /** The two line menu format string. */
    private static final String MENU_TWO_LINE = MENU_ONE_LINE + "<div class=\"ac-dropdown-subtext\">{1}</div>";
    private static final String EXTRA_COUNT_LINE = "<div class=\"ac-dropdown-subtext\">...and {0} more</div>";

    // The UI will have a comma separated list of keys that needs to be
    // parsed out for turning into real TOKEN_OBJECTs
    private String listOfKeys = "";

    /** A cache of the key list as a list of tokens. */
    // FIXME: make this final
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
    public void setListOfKeys(List<String> listOfKeys) {
        setListOfKeys(StringUtils.join(listOfKeys, getSeparator()));
    }

    // Called from stripes.  From Java use setListOfKeys(List<String>) above.
    public void setListOfKeys(String listOfKeys) {
        this.listOfKeys = listOfKeys;
        if (StringUtils.isBlank(listOfKeys)) {
            tokenObjects = Collections.emptyList();
        } else {
            String[] keys = listOfKeys.split(getSeparator());

            tokenObjects = new ArrayList<>(keys.length);
            for (String key : keys) {
                TOKEN_OBJECT object = getById(key.trim());
                if (object != null) {
                    tokenObjects.add(getById(key.trim()));
                }
            }
        }
    }

    public String getSeparator() {
        return TOKEN_INPUT_SEPARATOR;
    }

    public String getListOfKeys() {
        return listOfKeys;
    }

    @Nonnull
    public List<TOKEN_OBJECT> getTokenObjects() {
        // setListOfKeys updates the objects, so when the action bean first populates the list of keys
        // directly, there will be no objects. This ensures that there will be objects around later.
        if (!StringUtils.isBlank(listOfKeys) && CollectionUtils.isEmpty(tokenObjects)) {
            setListOfKeys(listOfKeys);
        }

        return tokenObjects;
    }

    @Nonnull
    public List<String> getTokenBusinessKeys() {
        List<TOKEN_OBJECT> objects = getTokenObjects();

        List<String> businessKeys = new ArrayList<>(objects.size());
        for (TOKEN_OBJECT object : objects) {
            businessKeys.add(getTokenId(object));
        }

        return businessKeys;
    }

    /**
     * This method should only be used when a TokenInput is being used to select a single element. (e.g. using
     * tokenLimit: 1).
     * @return the currently selected object, or null if no object is selected.
     */
    @Nullable
    public TOKEN_OBJECT getTokenObject() {
        List<TOKEN_OBJECT> objects = getTokenObjects();

        if (objects.isEmpty()) {
            return null;
        }

        return objects.get(0);
    }

    /**
     * Generate the completion data for this TokenInput.  Only called by TokenInput itself.  Subclasses should return
     * the empty string, not null, for cases where no completions exist.
     *
     * @return the completion data
     * @throws JSONException if an error occurs
     */
    private String generateCompleteData(boolean readOnly) throws JSONException {
        JSONArray itemList = new JSONArray();
        for (TOKEN_OBJECT tokenObject : getTokenObjects()) {
            itemList.put(createAutocomplete(tokenObject, readOnly));
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
        return createAutocomplete(tokenObject, false);
    }

    protected JSONObject createAutocomplete(TOKEN_OBJECT tokenObject, boolean readonly) throws JSONException {
        JSONObject item = getJSONObject(getTokenId(tokenObject), getTokenName(tokenObject), readonly);
        item.put("dropdownItem", formatMessage(formatString, tokenObject));
        return item;
    }

    /**
     * Split input string on whitespace and return as Collection.
     *
     * @param query tokenized string
     *
     * @return Collection of strings split on whitespace.
     */
    protected Collection<String> extractSearchTerms(@Nullable String query) {
        if (query == null) {
            return Collections.emptyList();
        } else {
            List<String> returnValue=new ArrayList<>();
            for (String theString : query.split("\\s")){
                if (!theString.trim().isEmpty()){
                    returnValue.add(theString);
                }
            }

            return returnValue;
        }
    }

    /** Used to build the generic JSON object for token input */
    protected abstract String getTokenId(TOKEN_OBJECT tokenObject);
    protected abstract String getTokenName(TOKEN_OBJECT tokenObject);

    /**
     * Format the message into a string.
     *
     * @param messageString the format string, either a ONE or TWO line format
     * @param tokenObject the object to format.
     *
     */
    protected abstract String formatMessage(String messageString, TOKEN_OBJECT tokenObject);

    /**
     * During an action bean session there are a number of places where the token objects may be used, this makes
     * sure to fetch the list only once.
     *
     * @return The JSON string for the token objects
     * @throws JSONException Any errors generating JSON
     */
    public final String getCompleteData() throws JSONException {
        return getCompleteData(false);
    }

    public final String getCompleteData(boolean readOnly) throws JSONException {
        if (completeDataCache == null) {
            completeDataCache = generateCompleteData(readOnly);
        }

        return completeDataCache;
    }

    protected abstract TOKEN_OBJECT getById(String key);

    public void setup(Object... ids) {
        setListOfKeys(StringUtils.join(ids, getSeparator()));
    }

    /**
     * Given the arguments, create a JSON object that represents it.  This is used to create the response for a
     * jQuery Tokeninput auto-complete UI.
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

    /**
     * Given the arguments, create a JSON object that represents it.  This is used to create the response for a
     * jQuery Tokeninput auto-complete UI.
     *
     * @param id the ID
     * @param name the name
     * @param readonly If it's read only
     * @return the JSON object that contains these fields.
     * @throws JSONException
     */
    public static JSONObject getJSONObject(String id, String name, boolean readonly) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("name", name);
        item.put("readonly", readonly);
        return item;
    }
}
