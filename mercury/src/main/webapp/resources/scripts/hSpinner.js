/**
 * A widget for controlling numeric text inputs. It surrounds the input with buttons for incrementing and decrementing
 * the value by 1, much like a spinner widget but in a horizontal orientation which results in larger click targets for
 * the buttons. Negative values are disallowed.
 */
$j.widget('mercury.hSpinner', {

    _setOption: function(key, value) {
        this._super(key, value);

        if (key == 'disabled') {
            $j(this.controlButtons).attr('disabled', value);
            $j(this.controlButtons).toggleClass('ui-state-disabled', value);
            this.element.attr('disabled', value);
        }
    },
    hidden: function(isHidden) {
        var hideValue = isHidden ? 'none' : '';
        var length = this.controlButtons.length;
        for (var i = 0; i < length; i++) {
            this.controlButtons[i].style.display = hideValue;
        }
    },
    _createButton: function (name) {
            var button = document.createElement("button");
            button.className = "btn btn-small " + name;
            var icon = document.createElement("i");
            if (name === 'decButton') {
                icon.className = "icon-minus";
            } else if (name === 'incButton') {
                icon.className = "icon-plus";
            }
            button.appendChild(icon);
            return button;
        },

    _create: function () {
        elementJs = this.element.get(0);
        var container = elementJs.parentNode;
        var origContainer = container;
        var parent = container.parentNode;
        // this.element.remove();
        // $originalElement = this.element.remove();
        this.inputName = elementJs.name;
        elementJs.className += ' hSpinner';

        this.decButton = this._createButton("decButton");
        this.incButton = this._createButton("incButton");

        // Browser suggestions can obscure the UI and are not very useful in this case.
        elementJs.setAttribute('autocomplete', 'off');
        // Surround the input with decrement/increment buttons.
        var outer = document.createElement("div");
        outer.className ="input-prepend input-append";
        outer.appendChild(this.decButton);
        outer.appendChild(elementJs);
        outer.appendChild(this.incButton);
        container.appendChild(outer);

        this.element = $j(elementJs);
        // this.$decButton = $j(decButton);
        // this.$incButton = $j(incButton);
        // this.$decButton = $j('<button class="decButton btn btn-small"><i class="icon-minus"></i></button>');
        // this.element.before(this.$decButton);
        // this.$incButton = $j('<button class="incButton btn btn-small"><i class="icon-plus"></i></button>');

        this.controlButtons = [this.incButton, this.decButton];

        if (this.options.initialVisibility==='hidden'){
            this.hidden(true)
        }
        parent.replaceChild(container, origContainer);
        $container=$j(container);
        // $container.append(this.$incButton);

        this._on($container, {
            'click .decButton': this._handleDecrement,
            'click .incButton': this._handleIncrement,
            'input .hSpinner': this._handleInput,
            'keydown .hSpinner': this._handleKeydown,
            'change .hSpinner': this._updateDisplayState
        });

        // Parse the current value and coerce it into a number.
        var currentValue = parseFloat(this.element.val());
        if (isNaN(currentValue)) {
            currentValue = 0;
        }
        this.additionalChangedSelector = this.options.additionalChangedSelector;

        // Capture the original value to enable applying a CSS class if the value is changed.
        if ($j.isFunction(this.options.originalValue)) {
            this.originalValue = this.options.originalValue(container);
        } else if (this.options.originalValue != null) {
            this.originalValue = this.options.originalValue;
        } else {
            this.originalValue = currentValue;
        }

        // Set the value to normalize formatting and to display 0 if there was no value.
        this.setValue(currentValue);
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

    /**
     * Handle an input event on the text field. 'input' events are fired when any input is processed, making it more
     * granular than 'change' events. This allows for more interesting live editing features.
     */
    _handleInput: function(event) {
        this._updateDisplayState();
        // Trigger an event after updating the widget so that the page can respond based on the resulting state.
        this._trigger('input', event, this.inputName);
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
        if (this.additionalChangedSelector) {
            this.element.closest(this.additionalChangedSelector).toggleClass('changed', changed);
        }
        if (currentValue === '0') {
            this.decButton.setAttribute('disabled',true);
        }else {
            this.decButton.removeAttribute('disabled');
        }
    },

    /* ================================================================
     * Below are functions to mutate the input's value and update the widget's display state. These functions use the
     * jQuery val() function which does not cause any DOM change events to be fired, so any such events must be fired by
     * the caller if they are needed.
     * ================================================================ */

    /**
     * Sets the input's value to the given value and updates the widget's display state.
     * @param value
     */
    setValue: function (value) {
        this.element.val(value.toString());
        this._updateDisplayState();
        /*
         * I'd like to be able to fire some sort of valueUpdated event (or a native change event) from here, but I've
         * witnessed poor performance when there are many (several hundred to 1000 or more) widgets being affected, such
         * as can happen when bulk editing billing ledgers. Therefore, it must be up to the enclosing page to recognize
         * on its own when the value changes.
         */
        // this._trigger('valueUpdated', null, this.inputName);
    },

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
        this.setValue((newValue).toString());
    },

    /**
     * Increments the input's value by 1 and updates the widget's display state.
     */
    increment: function () {
        var oldValue = parseFloat(this.element.val());
        this.setValue((oldValue + 1.0).toString());
    },
    /**
     * Apply additional additional 'changed' class to this selector.
     */
    additionalChangedSelector: "",
    initialVisibility: "visible"
});
