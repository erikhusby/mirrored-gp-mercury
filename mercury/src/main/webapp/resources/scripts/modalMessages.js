/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2017 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */
/**
 * Add a modal message with similar appearance to stripes:errors and stripes:messages.
 * Different message levels are available which changes the appearance of the message and allows message grouping.
 *
 * usage: modalMessages("info").add(messageText, [namespace]);
 * this example will show two error messages in a modal dialog box style for errors:
 *      <ul style="list-style-type:none">
 *          <li>var message = modalMessages("info");</li>
 *          <li>message.add("hello there");</li>
 *          <li>message.add("hello again");</li>
 *      </ul>
 * This example will show an error message and then overwrite it with a different one;
 *      <ul style="list-style-type:none">
 *          <li>var message = modalMessages("info");</li>
 *          <li>message.add("hello there", "hello");</li>
 *          <li>message.add("hello again", "hello");</li>
 *      </ul>
 * @param level: 'success', 'info', 'warning', 'error'
 * @param options: {
 *            onClose: function(),  callback to execute when the close box is clicked.
 *            clearOnError: []      alert levels which should be cleared when an error message is shown.
 *        }
 * @returns {{add: add message, clear: clear messages, hide: hide messages}}
 */
var modalMessages = function (level = "info", options={}) {
    var defaults = {
        onClose: clearMessages,
        clearOnError: ["success", "info"]
    };

    // automatic initialization:
    (function () {
        var levels = ['success', 'info', 'warning', 'error', 'intercept'];
        if (levels.indexOf(level) === -1) {
            throw `Invalid message level '${level}'. valid options are ${levels.join(", ")}.`;
        }
        if (level === 'intercept') {
            interceptMessages();
            return;
        }

        // merge passed in options with defaults.
        Object.assign({}, defaults, options);
        this.className = 'alert-' + level;
        this.messageBlock = document.querySelector("div.message-block");
        if (this.messageBlock == undefined) {
            this.messageBlock = createElement("<div class='message-block modal'></div>");
        }

        this.messageContainer = getMessageContainer(className);

        if (this.messageContainer == undefined) {
            this.messageContainer = createElement("<div class='alert'><button class='close'>&times;</button></div>");
            this.messageContainer.classList.add(this.className)
        } else {
            return;
        }
        this.messageBlock.prepend(this.messageContainer);

        hideContainer();

        var closeButton = this.messageContainer.querySelector("button.close");
        if (isFunction(options.onClose)) {
            closeButton.addEventListener("click", function () {
                options.onClose(this.parentNode);
                defaults.onClose(this.parentNode);
            }, false);
        }
    }());

    // public functions
    return {
        add: function (messageText, messageSelector) {
            if (Array.isArray(messageText)) {
                for (var i = 0; i < messageText.length; i++) {
                    addItem(messageText[i], messageSelector);
                }
            } else {
                addItem(messageText, messageSelector);
            }
        },
        clear: function () {
            clearMessages();
        },
        hide: function () {
            hideContainer();
        },
        show: function () {
            showContainer();
        }
    };

    function interceptMessages() {
        var messages = [];
        var alerts = document.querySelectorAll(".alert");
        for (var i = 0; i < alerts.length; i++){
            var alert = alerts[i];
            var level="success";
            if (alert.className.indexOf("error")>0){
                level = "error";
            }

            var messageItems = alert.querySelectorAll(".alert-" + level + " ul li");
            messageItems.forEach(function (node) {
                if (messages[level] == undefined) {
                    messages[level] = [];
                }
                messages[level].push(node.textContent);
            });
            alert.parentNode.removeChild(alert);
        };


        for (var key in messages) {
            var message = modalMessages(key);
            message.add(messages[key]);
        }
    }

    function getMessageContainer(className) {
        return document.querySelector("div." + className);
    }

    function isFunction(functionToCheck) {
        var getType = {};
        return functionToCheck && getType.toString.call(functionToCheck) === '[object Function]';
    }

    function addItem(messageText, messageSelector) {
        var messageElement = undefined;
        if (messageSelector != undefined) {
            messageElement = this.messageContainer.querySelector("[data-message-item=" + messageSelector + "]");
        }

        if (messageElement == undefined) {
            messageElement = createElement(`<li class='message-item'></li>`);
            if (messageSelector !== undefined) {
                messageElement.setAttribute("data-message-item", messageSelector);
            }
        }
        if (this.messageContainer.querySelector("ul") == undefined) {
            this.messageContainer.visibility = 'hidden';
        }
        messageElement.innerHTML = messageText;
        var listContainer = getListContainer(level);
        listContainer.appendChild(messageElement);
        if (options.clearOnError && level === 'error') {
            for (var i = 0; i <= options.clearOnError.length; i++) {
                var container = getMessageContainer("alert-" + options.clearOnError[i]);
                if (container != undefined) {
                    clearMessages(container);
                }
            }
        }
        showContainer();
    }

    function getListContainer(level) {
        var listContainer = this.messageContainer.querySelector("ul");
        if (listContainer == undefined) {
            listContainer = document.createElement("ul");
            this.messageContainer.appendChild(listContainer);
        }
        return listContainer;
    }

    function createElement(value) {
        var frag = document.createRange().createContextualFragment(value);
        var child = frag.firstChild;
        document.body.appendChild(child);
        return child;
    }

    function clearMessages(container = this.messageContainer) {
        if (container==undefined){
            return;
        }
        var messageBlock = container.parentNode;
        if (messageBlock != undefined) {
            messageBlock.removeChild(container);
            if (messageBlock.childElementCount === 0) {
                messageBlock.parentNode.removeChild(messageBlock)
            }
        }
    }

    function showContainer() {
        this.messageContainer.style.visibility = 'visible';
        this.messageBlock.style.visibility = 'visible';
    }

    function hideContainer() {
        this.messageContainer.style.visibility = 'hidden';
        if (this.messageBlock.childElementCount <= 1) {
            this.messageBlock.style.visibility = 'hidden';
        }
    }
};

