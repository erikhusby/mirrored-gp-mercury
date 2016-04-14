/**
 * A widget for controlling numeric text inputs. It surrounds the input with button for incrementing and decrementing
 * the value by 1, much like a spinner widget but in a horizontal orientation which results in larger click targets for
 * the buttons. Negative values are disallowed.
 */
$j.widget('mercury.hSpinner', {

    _setOption: function(key, value) {
        this._super(key, value);

        if (key == 'disabled') {
            this.$decButton.attr('disabled', value);
            this.$decButton.toggleClass('ui-state-disabled', value);
            this.element.attr('disabled', value);
            this.$incButton.attr('disabled', value);
            this.$incButton.toggleClass('ui-state-disabled', value);
        }
    },

    _create: function () {
        this.inputName = this.element.attr('name');
        this.element.addClass('hSpinner');

        // Browser suggestions can obscure the UI and are not very useful in this case.
        this.element.attr('autocomplete', 'off');

        // Surround the input with decrement/increment buttons.
        this.element.wrap('<div class="input-prepend input-append">');
        var $container = this.element.parent();
        this.$decButton = $j('<button class="decButton btn btn-small"><i class="icon-minus"></i></button>');
        this.element.before(this.$decButton);
        this.$incButton = $j('<button class="incButton btn btn-small"><i class="icon-plus"></i></button>');
        $container.append(this.$incButton);

        this._on($container, {
            'click .decButton': this._handleDecrement,
            'click .incButton': this._handleIncrement,
            'change .hSpinner': this._updateDisplayState,
            'keydown .hSpinner': this._handleKeydown,
            'input .hSpinner': function(event) {
                this._updateDisplayState();
                // Trigger an event after updating the widget so that the page can respond based on the resulting state.
                this._trigger('input', event, this.inputName);
            }
        });

        // Capture the original value to enable applying a CSS class if the value is changed.
        this.originalValue = parseFloat(this.element.val());
        if (isNaN(this.originalValue)) {
            this.originalValue = 0;
        }

        // Set the value to normalize formatting and to display 0 if there was no value.
        this.setValue(this.originalValue);
    },

    /**
     * When the "-" button is clicked, decrement the value, update the display state, and fire a 'decremented'
     * event.
     */
    _handleDecrement: function (event) {
        this.decrement();
        this._trigger('decremented', event, this.inputName);
        event.preventDefault();
    },

    /**
     * When the "+" button is clicked, increment the value, update the display state, and fire an 'incremented'
     * event.
     */
    _handleIncrement: function (event) {
        this.increment();
        this._trigger('incremented', event, this.inputName);
        event.preventDefault();
    },

    /*
     * Only allow non-negative numbers to be entered.
     * Adapted from http://stackoverflow.com/a/469362
     */
    _handleKeydown: function (event) {
        /*
         * Allow:
         *      backspace (forward delete):     46
         *      delete:                         8
         *      tab:                            9
         *      escape:                         27
         *      enter/return:                   13
         *      . (decimal point on keypad):    110
         *      . (period)                      190
         */
        if ($.inArray(event.keyCode, [46, 8, 9, 27, 13, 110, 190]) !== -1 ||
            // Allow: Ctrl/Cmd+A
            (event.keyCode == 65 && (event.ctrlKey || event.metaKey)) ||
            // Allow: Ctrl/Cmd+C
            (event.keyCode == 67 && (event.ctrlKey || event.metaKey)) ||
            // Allow: Ctrl/Cmd+X
            (event.keyCode == 88 && (event.ctrlKey || event.metaKey)) ||
            // Allow: home, end, left, right
            (event.keyCode >= 35 && event.keyCode <= 39)) {
            // let it happen, don't do anything
            return;
        }
        // Allow Ctrl/Cmd+V, but also update display state
        if (event.keyCode == 86 && (event.ctrlKey || event.metaKey)) {
            this._updateDisplayState();
            return;
        }
        // Ensure that it is a number and stop the key press if it isn't
        var isNumber = event.keyCode >= 48 && event.keyCode <= 57 && !event.shiftKey;
        if (!isNumber) {
            event.preventDefault();
        }
    },

    /**
     * Updates the widget display state based on the current value. Results in the 'changed' class on the input iff the
     * current value is different than the original value when the widget was instantiated. Disables the decrement
     * button if the value is 0.
     */
    _updateDisplayState: function () {
        var currentValue = this.element.val();
        var changed = parseFloat(currentValue) != this.originalValue;
        this.element.toggleClass('changed', changed);
        this.$decButton.prop('disabled', currentValue == 0);
    },

    /**
     * Sets the input's value to the given value and updates the widget's display state.
     * @param value
     */
    setValue: function (value) {
        this.element.val(value.toLocaleString());
        this._updateDisplayState();
        /*
         * I'd like to be able to fire some sort of valueUpdated event (or a native change event) from here, but I've
         * witnessed poor performance when there are many (several hundred to 1000 or more) widgets being affected, such
         * as can happen when bulk editing billing ledgers. Therefore, it must be up to the enclosing page to recognize
         * on its own when the value changes.
         */
        // this._trigger('valueUpdated', null, this.inputName);
    },

    /*
     * Functions to mutate the input's value and update the widget's display state. These functions use the jQuery val()
     * function which does not cause any DOM change events to be fired, so any such events must be fired by the caller
     * if they are needed.
     */

    /**
     * Decrements the input's value by 1 and updates the widget's display state. If the result would be less than 0, the
     * value is set to 0.
     */
    decrement: function () {
        var oldValue = parseFloat(this.element.val());
        var newValue = oldValue - 1.0;
        if (newValue < 0) {
            newValue = 0;
        }
        this.setValue((newValue).toLocaleString());
    },

    /**
     * Increments the input's value by 1 and updates the widget's display state.
     */
    increment: function () {
        var oldValue = parseFloat(this.element.val());
        this.setValue((oldValue + 1.0).toLocaleString());
    }
});
