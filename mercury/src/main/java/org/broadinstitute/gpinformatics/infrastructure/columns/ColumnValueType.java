package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

/**
 * Uniform method of identifying column data types and handling conversion and formatting.
 * Underlying value types must implement Comparable or be a Collection of Comparable
 */
public enum ColumnValueType {
    /**
     * A java.util.Date value formatted without time
     */
    DATE(
            new Formatter<Date>(){
                @Override
                public String format( Date value ){
                    return value == null?"":dateFormat.format(value);
                }
            },
            new SearchTerm.Evaluator<ColumnValueType>() {
                @Override
                public ColumnValueType evaluate(Object entity, SearchContext context) {
                    return ColumnValueType.DATE;
                }
            }
    ),
    /**
     * A java.util.Date value formatted with time
     */
    DATE_TIME(
            new Formatter<Date>(){
                @Override
                public String format( Date value ){
                    return value == null?"":dateTimeFormat.format(value);
                }
            },
            new SearchTerm.Evaluator<ColumnValueType>() {
                @Override
                public ColumnValueType evaluate(Object entity, SearchContext context) {
                    return ColumnValueType.DATE_TIME;
                }
            }
    ),
    /**
     * A java.lang.Number value formatted without decimal point
     */
    UNSIGNED(
            new Formatter<Number>(){
                @Override
                public String format( Number value ){
                    if( value == null ){
                        return "";
                    } else {
                        return String.valueOf( value.longValue() );
                    }
                }
            },
            new SearchTerm.Evaluator<ColumnValueType>() {
                @Override
                public ColumnValueType evaluate(Object entity, SearchContext context) {
                    return ColumnValueType.UNSIGNED;
                }
            }
    ),
    /**
     * A java.math.BigDecimal value formatted with two decimal places
     */
    TWO_PLACE_DECIMAL(
            new Formatter<BigDecimal>(){
                @Override
                public String format( BigDecimal value ){
                    if( value == null ){
                        return "";
                    } else {
                        return MathUtils.scaleTwoDecimalPlaces(value).toPlainString();
                    }
                }
            },
            new SearchTerm.Evaluator<ColumnValueType>() {
                @Override
                public ColumnValueType evaluate(Object entity, SearchContext context) {
                    return ColumnValueType.TWO_PLACE_DECIMAL;
                }
            }
    ),
    /**
     * A java.math.BigDecimal value formatted with three decimal places
     */
    THREE_PLACE_DECIMAL(
            new Formatter<BigDecimal>(){
                @Override
                public String format( BigDecimal value ){
                    if( value == null ){
                        return "0";
                    } else {
                        return MathUtils.scaleThreeDecimalPlaces(value).toPlainString();
                    }
                }
            },
            new SearchTerm.Evaluator<ColumnValueType>() {
                @Override
                public ColumnValueType evaluate(Object entity, SearchContext context) {
                    return ColumnValueType.THREE_PLACE_DECIMAL;
                }
            }
    ),
    /**
     * A java.lang.String value
     */
    STRING(
            new Formatter<Object>(){
                @Override
                public String format( Object value ){
                    return value == null?"":value.toString();
                }
            },
            new SearchTerm.Evaluator<ColumnValueType>() {
                @Override
                public ColumnValueType evaluate(Object entity, SearchContext context) {
                    return ColumnValueType.STRING;
                }
            }
    ),
    /**
     * Type to add a not null restriction to criteria
     */
    NOT_NULL(
            new Formatter<Object>(){
                @Override
                public String format( Object value ){
                    return "Not Null";
                }
            },
            new SearchTerm.Evaluator<ColumnValueType>() {
                @Override
                public ColumnValueType evaluate(Object entity, SearchContext context) {
                    return ColumnValueType.NOT_NULL;
                }
            }
    ),
    /**
     * A java.lang.Boolean value (legacy type - not used as of 2015/04)
     */
    BOOLEAN(
            new Formatter<Object>(){
                @Override
                public String format( Object value ){
                    return value == null?"":value.toString();
                }
            },
            new SearchTerm.Evaluator<ColumnValueType>() {
                @Override
                public ColumnValueType evaluate(Object entity, SearchContext context) {
                    return ColumnValueType.BOOLEAN;
                }
            }
    ),

    GENDER(
            new Formatter<Object>(){
                @Override
                public String format( Object value ){
                    if (value == null) {
                        return "";
                    } else if (value.toString().startsWith("F")) {
                        return "F";
                    } else if (value.toString().startsWith("M")) {
                        return "M";
                    } else {
                        return value.toString();
                    }
                }
            },
            new SearchTerm.Evaluator<ColumnValueType>() {
                @Override
                public ColumnValueType evaluate(Object entity, SearchContext context) {
                    return ColumnValueType.GENDER;
                }
            }
    );

    ColumnValueType(Formatter formatter, SearchTerm.Evaluator<ColumnValueType> defaultEvaluator){
        this.formatter = formatter;
        this.defaultEvaluator = defaultEvaluator;
    }

    private interface Formatter<T>{
        public String format( T value );
    }

    /**
     * A default evaluator to use to provide a data type for the column value.
     * Used when an explicit dynamic expression is not set in the search definition.
     * @return
     */
    public SearchTerm.Evaluator<ColumnValueType> getDefaultEvaluator() {
        return defaultEvaluator;
    }

    private Formatter formatter;
    private static FastDateFormat dateTimeFormat = FastDateFormat.getInstance( "MM/dd/yyyy HH:mm:ss");
    private static FastDateFormat dateFormat = FastDateFormat.getInstance( "MM/dd/yyyy");
    private SearchTerm.Evaluator<ColumnValueType> defaultEvaluator;

    /**
     * Provide a uniform output format for column data types
     * @param value
     * @param multiValueDelimiter  Optional, only used when the value is a collection and not default delimiter
     * @return
     */
    public String format( Object value, String multiValueDelimiter ) {
        if( value == null ) {
            return "";
        }

        if( value instanceof Collection) {
            Collection values = (Collection) value;
            if( values.isEmpty() ) {
                return "";
            }
            return formatCollection( values, multiValueDelimiter==null?ConfigurableList.DEFAULT_MULTI_VALUE_DELIMITER:multiValueDelimiter );
        } else {
            return formatter.format(value);
        }
    }

    /**
     * Converts scalar value type to a sortable value, defaulting to type String if type does not implement Comparable.
     * Collection elements are individually formatted and converted to a delimited string.
     * @param value
     * @param multiValueDelimiter
     * @return
     */
    public Comparable getComparableValue( Object value, String multiValueDelimiter ){
        if( value == null || value instanceof Comparable ) {
            //
            return (Comparable) value;
        } else {
            return format( value, multiValueDelimiter );
        }
    }

    /**
     * Converts a collection of underlying values to a delimited string
     * @param values
     * @param multiValueDelimiter
     * @return
     */
    private String formatCollection( Collection values, String multiValueDelimiter ) {
        StringBuilder valueDisplay = new StringBuilder();
        for( Object theValue : values ) {
            valueDisplay.append( formatter.format(theValue) );
            valueDisplay.append( multiValueDelimiter );
        }
        return valueDisplay.substring( 0, valueDisplay.length() - multiValueDelimiter.length() );
    }
}
