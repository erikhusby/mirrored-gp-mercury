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

var modalMessages = function (level = "info", options={}) {
    // initialization:
    (function () {
        var levels = ['success', 'info', 'warning', 'error'];
        if (levels.indexOf(level) === -1) {
            throw `Invalid message level '${level}'. valid options are ${levels.join(", ")}.`;
        }

        this.className = 'alert-' + level;

        this.messageBlock = document.querySelector("div.message-block");
        if (this.messageBlock == undefined) {
            this.messageBlock = createElement("<div class='message-block modal'></div>");
        }

        this.messageContainer = document.querySelector("div." + this.className);
        if (this.messageContainer == undefined) {
            this.messageContainer = createElement("<div class='alert'><button class='close'>&times;</button></div>");
            this.messageContainer.classList.add(this.className)
        } else {
            return;
        }
        this.messageBlock.appendChild(this.messageContainer);

        hideContainer();

        var closeButton = this.messageContainer.querySelector("button.close");
        if (options && options.onClose) {
            if (isFunction(options.onClose)) {
                closeButton.addEventListener("click", function () {
                    options.onClose();
                    messageBlock = document.querySelector("div.message-block");
                    messageBlock.removeChild(this.parentElement);
                    if (messageBlock.childElementCount == 0) {
                        messageBlock.remove();
                    }
                }, true);
            }
        } else {
            (function (container) {
                    closeButton.addEventListener("click", function () {
                        container.style.visibility = "hidden";
                    }, false);
                }(this.messageContainer)
            )
        }
    }());

    // public functions
    return {
        add: function (messageText, messageSelector) {
            addItem(messageText, messageSelector);
        },
        clear: function () {
            clearMessages();
        },
        hide: function () {
            hideContainer();
        }
    };

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
        messageElement.innerHTML = messageText;
        var listContainer = getListContainer(level);
        listContainer.appendChild(messageElement);
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

    function clearMessages() {
        var outerDiv = this.messageContainer.getElementsByClassName(this.className);
        for (var i = 0; i < outerDiv.length; i++) {
            outerDiv[i].innerHTML = "";
        }
        hideContainer();
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

