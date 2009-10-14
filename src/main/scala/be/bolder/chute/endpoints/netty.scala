package be.bolder.chute.endpoints

import _root_.java.net.InetSocketAddress
import _root_.org.jboss.netty.channel.socket.nio.{NioClientSocketChannelFactory, NioServerSocketChannelFactory}
import _root_.org.jboss.netty.bootstrap.{ClientBootstrap, ServerBootstrap}
import _root_.org.jboss.netty.channel._
import _root_.java.util.concurrent.{Executor, Executors}
import _root_.sbinary._


class EndpointServer[P]
(val port: Int, val bossPool: Executor, val workerPool: Executor, val payloadFormat: Format[P])
        extends Runnable {

  def this(aPort: Int)(implicit aFormat: Format[P]) =
    this(aPort, Executors.newCachedThreadPool, Executors.newCachedThreadPool, aFormat)

  // implicit val messageFormat: Format[Message[P]] = MessageProtocol.messageFormat[P](payloadFormat)

  val connectTimeout: Long = 10000L

  val bootstrap: ServerBootstrap =
    new ServerBootstrap(new NioServerSocketChannelFactory(bossPool, workerPool))

  val serverHandler: ChannelUpstreamHandler = new ServerHandler

  bootstrap.getPipeline.addLast("handler", serverHandler)

  bootstrap.setOption("child.tcpNoDelay", true)
  bootstrap.setOption("child.keepAlive", true)
  bootstrap.setOption("reuseAddress", true); 
  
  def run = bootstrap.bind(new InetSocketAddress(port))

  @ChannelPipelineCoverage("all")
  class ServerHandler extends SimpleChannelHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) = ()

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) = {
      e.getCause.printStackTrace()
      e.getChannel.close
    }
  }

  class Client(val host: String, val port: Int) {
    val bootstrap: ClientBootstrap =
      new ClientBootstrap(new NioClientSocketChannelFactory(bossPool, workerPool))

    // val timeoutHandler: ReadTimeoutHandler = new ReadTimeoutHandler
    // bootstrap.getPipeline.addLast("timeout", timeoutHandler)

    val clientHandler: ClientHandler = new ClientHandler
    bootstrap.getPipeline.addLast("handler", clientHandler)

    bootstrap.setOption("remoteAddress", new InetSocketAddress(host, port))

    bootstrap.setOption("tcpNoDelay", true);
    bootstrap.setOption("keepAlive", true);
    bootstrap.setOption("reuseAddress", true);
    bootstrap.setOption("connectTimeoutMillis", connectTimeout);
    bootstrap.connect



    @ChannelPipelineCoverage("one")
    class ClientHandler extends SimpleChannelUpstreamHandler {

    }

//    @ChannelPipelineCoverage("one")
//    class ReadTimeoutHandler extends SimpleChannelUpstreamHandler
//            with LifeCycleAwareChannelHandler with ExternalResourceReleasable {
//      val timer: Timer = new HashedWheelTimer
//
//      @volatile var task: ReadTimeoutTask = null
//      @volatile var lastReadTime: Long = 0L
//      @volatile var timeout: Timeout = null
//
//      @throws(classOf[Exception])
//      def beforeAdd(ctx: ChannelHandlerContext): Unit = initialize(ctx)
//
//      @throws(classOf[Exception])
//      def afterAdd(ctx: ChannelHandlerContext): Unit = ()
//
//      @throws(classOf[Exception])
//      def beforeRemove(ctx: ChannelHandlerContext): Unit = destroy
//
//      @throws(classOf[Exception])
//      def afterRemove(ctx: ChannelHandlerContext): Unit = ()
//
//      @throws(classOf[Exception])
//      override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
//        initialize(ctx)
//        ctx.sendUpstream(e)
//      }
//
//      @throws(classOf[Exception])
//      override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
//        destroy
//        ctx.sendUpstream(e)
//      }
//
//      @throws(classOf[Exception])
//      override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
//        lastReadTime = System.currentTimeMillis
//        ctx.sendUpstream(e)
//      }
//
//      private def initialize(ctx: ChannelHandlerContext): Unit = {
//        lastReadTime = System.currentTimeMillis
//        task = new ReadTimeoutTask(ctx)
//        resetTimeout
//      }
//
//      protected def setTimeout(value: Long) =
//        { timeout = timer.newTimeout(task, value, TimeUnit.MILLISECONDS) }
//      protected def resetTimeout = setTimeout(readTimeout)
//
//      private def destroy {
//        if (timeout != null) timeout.cancel
//        timeout = null
//        task = null
//      }
//
//      def releaseExternalResources = timer.stop
//
//      protected def readTimedOut(ctx: ChannelHandlerContext): Unit =
//        Channels.fireExceptionCaught(ctx, new RuntimeException("Read Timeout"))
//
//      class ReadTimeoutTask(val ctx: ChannelHandlerContext) extends TimerTask {
//
//        @throws(classOf[Exception])
//        def run(timeout: Timeout): Unit = {
//          if (timeout.isCancelled) return ()
//          if (!ctx.getChannel.isOpen) return ()
//
//          val currentTime = System.currentTimeMillis
//          val nextDelay = readTimeout - (currentTime - lastReadTime)
//
//          if (nextDelay <= 0L) {
//            resetTimeout
//            try { readTimedOut(ctx) }
//            catch { case (t: Throwable) => Channels.fireExceptionCaught(ctx, t) }
//          }
//          else
//            setTimeout(nextDelay)
//        }
//      }
//    }
  }
}


