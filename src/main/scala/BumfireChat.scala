import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.{Done, NotUsed}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContextExecutor, Promise, duration}

object BumfireChat {
  type ChatMsg = (String, String, String) //message's (type (arrvd/dprtd/msg/ping), chatter, body)

  private case class Config(intf: String = "localhost", port: Int = 8080, wsKeepAliveInSec: Int = 30)

  def main(args: Array[String]): Unit = new ConfigParser().parse(args, Config()).foreach { implicit conf =>
    implicit val system: ActorSystem = ActorSystem("bumfire")
    implicit val materializer: ActorMaterializer = ActorMaterializer.create(system)
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val (roomSink, roomSource) = MergeHub.source[ChatMsg].toMat(BroadcastHub.sink)(Keep.both).run()
    implicit val roomFlow: Flow[ChatMsg, ChatMsg, NotUsed] = Flow.fromSinkAndSource(roomSink, roomSource)

    val route =
      pathPrefix("chat") {
        pathPrefix(Segment) { implicit chatter =>
          parameter("auth") { authToken => handleWebSocketMessages(wsMessageHandler) }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, conf.intf, conf.port)
    println(s"Come join the chat at http://${conf.intf}:${conf.port}/chat/ ...")

    val promise = Promise[Done]()
    sys.addShutdownHook(promise.trySuccess(Done))
    Await.ready(bindingFuture.flatMap(_ => promise.future), Duration.Inf)
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

  private def wsMessageHandler(implicit chatter: String, sys: ActorSystem, conf: Config,
                               roomFlow: Flow[ChatMsg, ChatMsg, NotUsed]): Flow[Message, Strict, NotUsed] = {
    def encode(msg: ChatMsg) = Strict(s"""{"typ":"${msg._1}","chatter":"${msg._2}","msg":"${msg._3}"}""")

    val decoder = Flow[Message].collect{case Strict(msg) => ("msg", chatter, msg)}
    val presenceStage = new PresenceStage(chatter)
    decoder.via(presenceStage).via(roomFlow).map(encode).keepAlive(
      FiniteDuration(conf.wsKeepAliveInSec, duration.SECONDS), () => encode(("ping", null, null))
    )
  }

  private class ConfigParser extends scopt.OptionParser[Config]("Bumfire") {
    head("Bumfire Chat Server")

    help("help").text("prints this usage text")

    opt[String]('i', "if") action {
      (i, cfg) => cfg.copy(intf = i)
    } text "server interface (localhost by default)"

    opt[Int]('p', "port") action {
      (p, cfg) => cfg.copy(port = p)
    } text "server port (8080 by default)"

    opt[Int]('k', "keepalive") action {
      (k, cfg) => cfg.copy(wsKeepAliveInSec = k)
    } text "WS connection keep alive in seconds (30 by default)"
  }

  private class PresenceStage(chatter: String) extends GraphStage[FlowShape[ChatMsg, ChatMsg]] {
    override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) with InHandler with OutHandler{
      private def announceDeparture(): Unit = emit(shape.out, ("dprtd", chatter, "Departed"))
      private def announceArrival(): Unit = emit(shape.out, ("arrvd", chatter, "Arrived"))

      setHandler(shape.in, this)
      setHandler(shape.out, this)

      override def preStart(): Unit = {
        announceArrival()
        super.preStart()
      }

      override def onPush(): Unit = push(shape.out, grab(shape.in))
      override def onPull(): Unit = pull(shape.in)

      override def onUpstreamFinish(): Unit = {
        announceDeparture()
        super.onUpstreamFinish()
      }
      override def onUpstreamFailure(ex: Throwable): Unit = {
        announceDeparture()
        super.onUpstreamFailure(ex)
      }
    }

    override val shape: FlowShape[ChatMsg, ChatMsg] =
      FlowShape.of(Inlet[ChatMsg]("PresenceStage.in"), Outlet[ChatMsg]("PresenceStage.out"))
  }
}
