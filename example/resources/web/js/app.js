$(function() {

  if (!window.WebSocket) {
    window.WebSocket = window.MozWebSocket;
  }

  if (window.WebSocket) {
    var l = window.location;
    var wsUrl =  ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname + (((l.port != 80) && (l.port != 443)) ? ":" + l.port : "") + l.pathname + "socket";
    var socket = new WebSocket(wsUrl);
    socket.onmessage = function (event) {
      $(".wikipedia-updates").append('<li>' + event.data + '</li>');
    };
    socket.onopen = function (_event) {
      console.log("ready to rumble :)");
          };

  } else {
    console.log("You don't have websockets :(")
  }

});