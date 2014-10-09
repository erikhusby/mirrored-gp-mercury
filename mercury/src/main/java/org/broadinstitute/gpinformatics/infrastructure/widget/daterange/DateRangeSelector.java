/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2007 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.infrastructure.widget.daterange;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This simple class handles the date range selector data created by the date range selector javascript
 *
 * @author hrafal
 */
public class DateRangeSelector implements Serializable {
    private static final long serialVersionUID = 20111114L;

    /**
     * These statics need to be kept in sync with datRangeSelector.js so that
     * so that the javascript dateOptions array mirrors these values.
     *
     * Don't even think about changing these!
     * They are also used in GAP.PROC_MIGRATE_ESP_REPORT
     */
    public static final int ALL = 0;
    public static final int AFTER = 1;
    public static final int BEFORE = 2;
    public static final int TODAY = 3;
    public static final int YESTERDAY = 4;
    public static final int THIS_WEEK = 5;
    public static final int SEVEN_DAYS = 6;
    public static final int LAST_WEEK = 7;
    public static final int THIS_MONTH = 8;
    public static final int ONE_MONTH = 9;
    public static final int LAST_MONTH = 10;
    public static final int THIS_YEAR = 11;
    public static final int ONE_YEAR = 12;
    public static final int LAST_YEAR = 13;
    public static final int CREATE_CUSTOM = 14;

    private int rangeSelector = DateRangeSelector.ALL;

    private static final Format DATE_FORMATTER = FastDateFormat.getInstance("d-MMM-yyyy");

    public int getRangeSelector() {
        return rangeSelector;
    }

    public void setRangeSelector(int rangeSelector) {
        this.rangeSelector = rangeSelector;
    }

    private Date start;

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    private Date end;

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    private String naturalLanguageString;

    public String getNaturalLanguageString() {
        if ((naturalLanguageString == null) || (naturalLanguageString.isEmpty())) {
            naturalLanguageString = getComputedDateRange().naturalLanguageString;
        }
        return naturalLanguageString;
    }

    public void setNaturalLanguageString(String naturalLanguageString) {
        this.naturalLanguageString = naturalLanguageString;
    }

    // Validate the range

    public boolean isValidRange() {
        return (start == null) || (end == null) || end.equals(start) || end.after(start);
    }

    public String getStartStr() {
        return DateUtils.getDate(getStartTime());
    }

    public String getEndStr() {
        return DateUtils.getDate(getEndTime());
    }

    /**
     * @return Return the start of day for the current start date.
     */
    public Date getStartTime() {
        Date startTime = null;
        if (getStart() != null) {
            startTime = DateUtils.getStartOfDay(getStart());
        }
        return startTime;
    }


    /**
     * @return Return the end of day for the current end date.
     */
    public Date getEndTime() {
        Date endTime = null;
        if (getEnd() != null) {
            endTime = DateUtils.getEndOfDay(getEnd());
        }
        return endTime;
    }

    public String getDateRangeAsString() {
        Date startDate = getStartTime();
        if (startDate == null) {
            startDate = new Date(0);
        }
        Date stopDate = getEndTime();
        if (stopDate == null) {
            stopDate = new Date();
        }
        return DATE_FORMATTER.format(startDate) + " - " + DATE_FORMATTER.format(stopDate);
    }

    /**
     * Translate the natural language string to its numerical value;
     *
     * @param naturalLanguageString the date range as string
     * @return numerical value of naturalLanguage string according to constants above;
     */
    private static Integer toRangeSelector(String naturalLanguageString) {
        if (naturalLanguageString.startsWith("ALL")) {
            return ALL;
        } else if (naturalLanguageString.startsWith("All")) {
            return ALL;
        } else if (naturalLanguageString.startsWith("Any")) {
            return ALL;
        } else if (naturalLanguageString.startsWith("After")) {
            return AFTER;
        } else if (naturalLanguageString.startsWith("From") && !naturalLanguageString.contains("To")) {
            return AFTER;
        } else if (naturalLanguageString.startsWith("From") && naturalLanguageString.contains("To")) {
            return CREATE_CUSTOM;
        } else if (naturalLanguageString.startsWith("Before")) {
            return BEFORE;
        } else if (naturalLanguageString.equals("Today")) {
            return TODAY;
        } else if (naturalLanguageString.equals("Yesterday")) {
            return YESTERDAY;
        } else if (naturalLanguageString.equals("This Week")) {
            return THIS_WEEK;
        } else if (naturalLanguageString.equals("7 Days")) {
            return SEVEN_DAYS;
        } else if (naturalLanguageString.equals("Last Week")) {
            return LAST_WEEK;
        } else if (naturalLanguageString.equals("This Month")) {
            return THIS_MONTH;
        } else if (naturalLanguageString.equals("One Month")) {
            return ONE_MONTH;
        } else if (naturalLanguageString.equals("Last Month")) {
            return LAST_MONTH;
        } else if (naturalLanguageString.equals("This Year")) {
            return THIS_YEAR;
        } else if (naturalLanguageString.equals("One Year")) {
            return ONE_YEAR;
        } else if (naturalLanguageString.equals("Last Year")) {
            return LAST_YEAR;
        } else if (naturalLanguageString.equals("Custom")) {
            return CREATE_CUSTOM;
        }
        return null;
    }

    public void updateDateRange(int theRangeSelector) {
        DateRangeSelector newRangeSelector = new DateRangeSelector(theRangeSelector);

        // Copy the new values into the current one
        rangeSelector = newRangeSelector.getRangeSelector();
        start = newRangeSelector.getStart();
        end = newRangeSelector.getEnd();
        naturalLanguageString = newRangeSelector.getNaturalLanguageString();
    }

    public DateRangeSelector() {
        // Empty constructor to allow use inside java bean.
    }

    /**
     * Set up a DateRangeSelector object based on custom start and end dates.
     *
     * @param start The start date
     * @param end  The end date
     */
    public DateRangeSelector(Date start, Date end) {
        this(null, DateRangeSelector.CREATE_CUSTOM, start, end);
    }

    /**
     * Set up a DateRangeSelector object based on the value of rangeSelector.
     *
     * @param theRangeSelector rangeSelector value.
     */
    public DateRangeSelector(int theRangeSelector) {
        this(null, theRangeSelector, null, null);
    }

    /**
     * @return This creates a list of strings representing the three values for the date.
     */
    public List<String> createDateStrings() {
        List<String> dateStrings = new ArrayList<>();
        dateStrings.add(String.valueOf(rangeSelector));
        dateStrings.add(getStartStr());
        dateStrings.add(getEndStr());
        return dateStrings;
    }

    private enum RangeDefinition {
        TodayRange(TODAY, DateRange.Today, "Today"),
        YesterdayRange(YESTERDAY, DateRange.Yesterday, "Yesterday"),
        ThisWeekRange(THIS_WEEK, DateRange.ThisWeek, "This Week"),
        SevenDaysRange(SEVEN_DAYS, DateRange.Last7Days, "7 Days"),
        LastWeekRange(LAST_WEEK, DateRange.LastWeek, "Last Week"),
        ThisMonthRange(THIS_MONTH, DateRange.ThisMonth, "This Month"),
        OneMonthRange(ONE_MONTH, DateRange.Last30Days, "One Month"),
        LastMonthRange(LAST_MONTH, DateRange.LastMonth, "Last Month"),
        ThisYearRange(THIS_YEAR, DateRange.ThisYear, "This Year"),
        OneYearRange(ONE_YEAR, DateRange.OneYear, "One Year"),
        LastYearRange(LAST_YEAR, DateRange.LastYear, "Last Year"),
        AllRange(ALL, DateRange.All, "All");

        final int rangeSelector;
        final DateRange dateRange;
        final String naturalLanguageString;

        RangeDefinition(int rangeSelector, DateRange dateRange, String naturalLanguageString) {
            this.rangeSelector = rangeSelector;
            this.dateRange = dateRange;
            this.naturalLanguageString = naturalLanguageString;
        }

        static RangeDefinition fromSelector(int rangeSelector) {
            for (RangeDefinition definition : RangeDefinition.values()) {
                if (definition.rangeSelector == rangeSelector) {
                    return definition;
                }
            }
            return RangeDefinition.AllRange;
        }
    }

    private static final int RANGE_SELECTOR_INDEX = 0;
    private static final int START_DATE_INDEX = 1;
    private static final int END_DATE_INDEX = 2;

    /**
     * This is a special constructor that is useful for pulling user preference information into the date range
     * selector object.
     *
     * @param rangeSelectorStrings The list of strings representation of this DateRangeSelector.
     * @throws ParseException Any errors
     */
    public DateRangeSelector(List<String> rangeSelectorStrings) throws ParseException {
        int rangeSelectorValue = Integer.parseInt(rangeSelectorStrings.get(RANGE_SELECTOR_INDEX));

        String start = rangeSelectorStrings.get(START_DATE_INDEX);
        Date startDate = StringUtils.isBlank(start) ? null : DateUtils.parseDate(start);

        String end = rangeSelectorStrings.get(END_DATE_INDEX);
        Date endDate = StringUtils.isBlank(end) ? null : DateUtils.parseDate(end);

        setupRangeSelector(null, rangeSelectorValue, startDate, endDate);
    }

    private DateRangeSelector(
        @Nullable String naturalLanguageString, int rangeSelector, @Nullable Date start, @Nullable Date end) {
        setupRangeSelector(naturalLanguageString, rangeSelector, start, end);
    }

    private void setupRangeSelector(
        @Nullable String naturalLanguageString, int rangeSelector, @Nullable Date start, @Nullable Date end) {
        if ((null != naturalLanguageString) && (!naturalLanguageString.isEmpty())) {
            Integer var = toRangeSelector(naturalLanguageString);
            if (var != null) {
                rangeSelector = var;
            }
        }

        this.rangeSelector = rangeSelector;

        switch (this.rangeSelector) {
        case ALL:
            this.naturalLanguageString = RangeDefinition.AllRange.naturalLanguageString;
            this.end = null;
            this.start = null;
            break;
        case AFTER:
            this.naturalLanguageString = "After";
            this.end = null;
            this.start = start;
            break;
        case BEFORE:
            this.naturalLanguageString = "Before";
            this.start = null;
            this.end = end;
            break;
        case CREATE_CUSTOM:
            this.naturalLanguageString = "Custom";
            this.start = (start == null) ? new Date() : start;
            this.start = (end == null) ? new Date() : end;
            break;
        default:
            RangeDefinition range = RangeDefinition.fromSelector(this.rangeSelector);
            this.naturalLanguageString = range.naturalLanguageString;
            Date[] dates = range.dateRange.startAndStopDate();
            this.start = dates[0];
            this.end = dates[1];
        }
    }

    /**
     * Set up a DateRangeSelector object based on the value of rangeSelector
     *
     * @return new DateRangeSelector fully initialized to the current value of rangeSelector
     */
    public DateRangeSelector getComputedDateRange() {
        return new DateRangeSelector(naturalLanguageString, rangeSelector, start, end);
    }

    public void addCriteria(String dateFieldName, Criteria criteria) {
        if (isValidRange()) {
            if (getStart() != null && getEnd() == null) {
                criteria.add(Restrictions.ge(dateFieldName, getStartTime()));
            } else if (getStart() == null && getEnd() != null) {
                criteria.add(Restrictions.le(dateFieldName, getEndTime()));
            } else if (getStart() != null && getEnd() != null) {
                criteria.add(Restrictions.between(dateFieldName, getStartTime(), getEndTime()));
            }
        }
    }

    public static void addDateCriteria(DateRangeSelector range, String dateFieldName, Criteria criteria) {
        if (range != null) {
            range.addCriteria(dateFieldName, criteria);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DateRangeSelector that = (DateRangeSelector) o;

        if (end != null ? !end.equals(that.end) : that.end != null) return false;
        if (naturalLanguageString != null ? !naturalLanguageString.equals(that.naturalLanguageString) : that.naturalLanguageString != null)
            return false;
        if (rangeSelector != that.rangeSelector)
            return false;
        if (start != null ? !start.equals(that.start) : that.start != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = rangeSelector;
        result = 31 * result + (start != null ? start.hashCode() : 0);
        result = 31 * result + (end != null ? end.hashCode() : 0);
        result = 31 * result + (naturalLanguageString != null ? naturalLanguageString.hashCode() : 0);
        return result;
    }
}
