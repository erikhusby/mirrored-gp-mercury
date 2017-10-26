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


window.modalMessages = (function (window) {
    function init() {

        console.log("init");

        initStyle();
        var closeButton = document.createElement("button");
        closeButton.className = "close";
        closeButton.innerHTML = '&times;';

        this.messageContainer = document.createElement("div");
        this.messageContainer.className = "modal alert alert-block";
        this.messageContainer.id = "mymessage";
        this.messageContainer.style.visibility = "hidden";
        this.messageContainer.appendChild(closeButton);
        this.messageBlock = document.createElement("ul");
        messageContainer.appendChild(this.messageBlock);
        document.body.appendChild(messageContainer);

        document.addEventListener("modal-message", function (event) {
            if (event.detail.type === 'clear') {
                this.messageBlock = document.querySelector(".alert-block ul");
                this.messageBlock.innerHTML = "";
                document.querySelector(".alert-block").style.visibility = 'hidden';
            } else {
                // setTimeout to prevent clear events from occurring after a set event;
                setTimeout(function() {
                    addItem(event.detail);
                },200);
            }
        }, false);

        closeButton.addEventListener("click", function () {
            var container = document.querySelector(".alert-block").style.visibility = "hidden";
        }, false);
    };

    function initStyle() {
        console.log("initStyle");
        var style = document.createElement("style");
        var errorStyle = document.createTextNode('.message-item:{font-weight: bold; margin-left: 50px}');
        style.appendChild(errorStyle);
        document.head.appendChild(style);
    }


    function addItem(eventDetail) {
        var level = eventDetail.type;
        var messageText = eventDetail.message;
        console.log("addItem");
        var className = undefined;
        switch (level) {
            case "error":
                className = 'text-error';
                break;
            case "warning":
                className = 'text-warning';
                break;
            case "info":
                className = 'text-message';
                break;
            default:
                throw "Invalid message level " + level + ". valid options are 'error', 'warning', or 'info'.";
        }

        var messageElement = document.createElement("li");
        messageElement.className = className + " message-item";
        messageElement.innerText = messageText;

        this.messageBlock.appendChild(messageElement);

        if (this.messageContainer.style.visibility === 'hidden') {
            this.messageContainer.style.visibility = 'visible';
        }
    }


    return (function () {
        document.addEventListener("DOMContentLoaded", function (event) {
            init();

            modalMessages = {
                addError: function (messageText) {
                    var messageEvent = new CustomEvent("modal-message", {
                        detail: {
                            type: 'error',
                            message: messageText
                        }
                    });
                    return document.dispatchEvent(messageEvent)
                },
                addInfo: function (messageText) {
                    var messageEvent = new CustomEvent("modal-message", {detail: {type: 'info', message: messageText}});
                    return document.dispatchEvent(messageEvent)

                },
                addWarning: function (messageText) {
                    var messageEvent = new CustomEvent("modal-message", {
                        detail: {
                            type: 'warning',
                            message: messageText
                        }
                    });
                    return document.dispatchEvent(messageEvent)

                },
                clear: function () {
                    var messageEvent = new CustomEvent("modal-message", {detail: {type: 'clear'}});
                    return document.dispatchEvent(messageEvent)

                }
            };
            return modalMessages;
        })
    }() );

}(window));
