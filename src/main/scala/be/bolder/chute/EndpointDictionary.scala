package be.bolder.chute


import _root_.scala.actors.remote.Node
import java.util.concurrent.ConcurrentSkipListMap

/***
 * Concurrent, shared-memory dictionary from Nodes to Endpoints of type E
 *
 * If there is no entry for a given node, it is created lazily.
 *
 * Removal of unused entries is left to subclasses.
 *
 * @author Stefan Plantikow <plantikow@zib.de>
 *
 */
trait EndpointDictionary[E] {

	private[this] val dict = new ConcurrentSkipListMap[Node, E](NodeComparator)


  /**
   * @return endpoint for given node
   */
	def apply(node: Node): E = {
		val curEndp = dict.get(node)
	  if (curEndp == null) runEndpointHandler(node) else curEndp
	}


  /**
   * @return endpoint entry for node with given hostname and port
   */
  def apply(hostName: String, port: Int): E = apply(Node(hostName, port))


  def containsEndpointFor(node: Node) = dict.containsKey(node)

  
  /**
   * @return a fresh endpoint for node
   */
  protected def mkEndpointHandler(node: Node): E

  /**
   * Runs a new endpoint by first creating it with mkEndpointHandler and
   * then trying to register it in the internal dictionary.
   *
   * If registration fails, the handle present in the dictionary is returned and the
   * newly created handler is shutdown with killEndpointHandler
   *
   * Otherwise, the newly created and registered handler is returned.
   */

  protected def runEndpointHandler(node: Node) = {
		val hdl = mkEndpointHandler(node)
		val winner = dict.putIfAbsent(node, hdl)
		if (winner == null) hdl else {
			killEndpointHandler(hdl)
			winner
		}
  }

  /**
   * Remove EndpointHandler from internal dictionary, called by subclasses
   * to remove idle or failed Endpointhandlers
   */
  protected def killEndpointHandler(hdl: E): Unit = dict.remove(endpointHandlerNode(hdl), hdl)

  /**
   * @return Node originally associated to the given handler
   */
	def endpointHandlerNode(hdl: E): Node
}
