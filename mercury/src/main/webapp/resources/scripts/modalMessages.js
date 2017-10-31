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

var modalMessages = function (options) {
    // initialization:
    (function () {

        this.messageContainer = createElement("<div class='modal alert alert-block'><button class='close'>&times;</button></div>");
        hideContainer();

        var closeButton = this.messageContainer.querySelector("button.close");
        if (options.onClose) {
            if (isFunction(options.onClose)) {
                closeButton.addEventListener("click", function () {
                    options.onClose();
                    this.parentNode.style.visibility = 'hidden';
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

        this.messageContainer.addEventListener("modal-message", function (event) {

            if (event.detail.type === 'clear') {
            } else {
                // setTimeout to prevent clear events from occurring after a set event;
                setTimeout(function (e) {
                    addItem(e);
                }(event), 200);
            }
        }, false);

    }());

    // public functions
    return {
        addError: function (messageText, messageSelector) {
            addItem('error', messageText, messageSelector);
        },
        addInfo: function (messageText, messageSelector) {
            addItem('info', messageText, messageSelector);
        },
        addSuccess: function (messageText, messageSelector) {
            addItem('success', messageText, messageSelector);
        },
        addWarning: function (messageText, messageSelector) {
            addItem('warning', messageText, messageSelector);
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

    function addItem(level, messageText, messageSelector) {
        var levels = ['success', 'info', 'warning', 'error'];
        if (levels.indexOf(level) === -1) {
            throw `Invalid message level '${level}'. valid options are ${levels.join(", ")}.`;
        }

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
        var className = "alert-" + level;
        var listContainer = this.messageContainer.querySelector("ul." + className);
        if (listContainer == undefined) {
            var outerDiv = document.createElement("div");
            outerDiv.classList.add("alert-outer");
            outerDiv.classList.add(className);
            listContainer = document.createElement("ul");
            listContainer.classList.add(className);
            outerDiv.appendChild(listContainer);
            this.messageContainer.appendChild(outerDiv);
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
        var outerDiv = this.messageContainer.getElementsByClassName("alert-outer");
        for (var i = 0; i < outerDiv.length; i++) {
            outerDiv[i].innerHTML = "";
        }
        hideContainer();
    }

    function showContainer() {
        this.messageContainer.style.visibility = 'visible';
    }

    function hideContainer() {
        this.messageContainer.style.visibility = 'hidden';
    }
};
