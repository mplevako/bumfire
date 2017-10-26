### Running and using.
 To build the project you should install [SBT](http://www.scala-sbt.org/). Having installed it, type in the command line either `sbt run ` to start the server promptly or `sbt "run --help"` to see how to customize it before running.
 To join the chat, open your browser (the latest versions of Chrome and Firefox should work for sure) and navigate to the client's [default location](http://localhost:8080/chat/) or
 to http://`interface`:`port`/chat/ if you provided custom values for `interface` and/or `port`.
 You will be registered and assigned a unique name automatically, that generated persona is valid only during this session and ceases as soon as you close the browser tab/window. Opening the
 chat's URL in another browser tab/window will create a new persona, reloading the current chat window will wipe your current discussion log and also create a new persona.
 To send a line press `Enter`. Be aware that long postings are not supported. You'll receive others words as soon as they arrived, you'll be also notified about arrival and departure of
 other participants automatically. There is only one public room, private communication is not provided.

### About Bumfire Chat.
 Bumfire Chat is a single room chat with the server written in Scala using Akka Http and Akka Streams and a simple HTML/JS (with JQuery) web client communicating via WebSockets.

 This is a solution to a couple-of-hour Scala coding test task to write a chat server (and a web client), similar to Slack, Flowdock, Campfire or IRC. The server had, at a minimum, to provide a way for two
 connected clients to exchange simple text messages and the client should be able to show messages as they arrived. There were several other, optional to implement, bonus features, among which
 two, client arrival/departure announcement and something like ticket-based client authentication (though in a highly prototipical way), were chosen by me to be implemented.