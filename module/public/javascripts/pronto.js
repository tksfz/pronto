function startProntoWebSocket(divId, url) {
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS(url);
        
    // Receive and handle socket messages
    chatSocket.onmessage = function(event) {
        var json = $.parseJSON(event.data);
        if (!json.target) json.target = divId;
        if (json.method && json.method == "replace") {
            $('#' + json.target).html(json.html);
        } else {
            $('#' + json.target).append(json.html);
        }
    };
    chatSocket.onopen = function(event) {
        //chatSocket.send("now here we go");
    };
        
    // Send events back to server via socket
    var prontoInputListener = function(event) {
        var message = {
            target: event.target.id,
            type: event.target.tagName,
            event: event.type,
            data: $(this).serialize()
        };
        chatSocket.send(JSON.stringify(message));
        return false;
    };
        
    $('#' + divId).on("submit", ".prontoInput", prontoInputListener);
    $('#' + divId).on("click", ":not(form).prontoInput", prontoInputListener);
}

