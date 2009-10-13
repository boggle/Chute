package be.bolder.chute.endpoints

import _root_.scala.actors.Actor.loop
import _root_.scala.actors.Actor
import _root_.scala.actors.Exit
import _root_.scala.actors.remote.Node
import _root_.be.bolder.chute.kit.ActorKit

/**
* @deprecated Likely will be replaced with something more suitable and based on dispatch.*
*/
object Endpoints {
  /**
   * Sent to endpoint*s* actor when a new endpoint actor is instantiated
   * to link it
   */
  private[Endpoints] case class Init(linkTarget: Actor) ;

  private[Endpoints] case object Terminate ;

  /**
   * Class for a single endpoint actor
   *
   */
  final class Endpoint(val node: Node, private val parent: Actor) {

    private[Endpoints] object actor extends Actor with EndpointHolder {

      def act: Unit = {
        trapExit = true
        if (!(parent eq null)) link(parent)
        loop { react {
          case Exit(who, _) if who eq parent =>
            Actor.self.exit('parent)
          case Terminate =>
            Actor.self.exit
      } } }

      def endpoint = Endpoint.this

      start
    }

    /**
     * Shutdown this endpoints' actor eventually discarding its
     * endpoint entry from the global dictionary
     */
    def shutdown = actor ! Terminate

    /**
     * Create a forwarder to this endpoint's actor (for testing)
     */
    protected[chute] def _guardian = ActorKit.mkForwarder_(actor)
  }

  private[Endpoints] trait EndpointHolder { def endpoint: Endpoint }

  /**
   * Actor for coordinating the different endpoints
   *
   * (Mainly deals with EndpointActor failures)
   */
  private class EndpointsActor extends Actor {
    def act: Unit = {
      trapExit = true
      loop { Actor.self.react {
        case Init(linkTarget) => {
          link(linkTarget)
          reply(null)
        }

        case Exit(hdl: EndpointHolder, reason) =>
          if (reason != 'terminate)
            dict.deferredKillEndpointHandler(hdl.endpoint)

        case Terminate => exit
      } }
    }

    /**
     * Concurrent EndpointDictionary
     */
    private[Endpoints] object dict extends EndpointDictionary[Endpoint] {
      override protected def mkEndpointHandler(node: Node): Endpoint = {
        val newEndp = new Endpoint(node, EndpointsActor.this)
        val newActor = newEndp.actor
        EndpointsActor.this !? Init(newActor)
        newEndp
      }

      override protected def killEndpointHandler(hdl: Endpoint): Unit = hdl.shutdown

      def endpointHandlerNode(hdl: Endpoint): Node = hdl.node

      // This is a bit of an hack
      private[EndpointsActor] def deferredKillEndpointHandler(hdl: Endpoint): Unit =
        super.killEndpointHandler(hdl)
    }

    start
  }

  /**
   * EndpointsActor of this Endpoint object
   */
  private[Endpoints] var endpointsActor = new EndpointsActor

  /**
   * @return endpoint for given node
   */
	def apply(node: Node): Endpoint = endpointsActor.dict(node)

  /**
   * @return endpoint entry for node with given hostname and port
   */
  def apply(hostName: String, port: Int): Endpoint = endpointsActor.dict(hostName, port)

  /**
   * @return true if currently there is an endpoint entry for node
   */
  def containsEndpointFor(node: Node) = endpointsActor.dict.containsEndpointFor(node)


  /**
   * Shutdown and remove Endpoints' EndpointsActor if currently there is one
   */
  protected[chute] def _shutdown =
    synchronized {
      if (! (endpointsActor eq null))
        { endpointsActor ! Terminate; endpointsActor = null }
    }

  /**
   * Start and register a new EndpointsActor for Endpoints if currently there is none
   */
  protected[chute] def _setup =
    synchronized {
      if (endpointsActor eq null)
        { endpointsActor = new EndpointsActor }
    }

}
