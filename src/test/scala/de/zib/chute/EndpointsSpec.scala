package be.bolder.chute


import _root_.scala.actors.remote.Node
import _root_.org.specs.runner._
import actors.Actor
/**
 * Tests for Endpoint-Stuff
 * 
 * @author Stefan Plantikow <plantikow@zib.de>
 *
 */

object EndpointsSpec extends Spec {

  "NodeComparator behaves sanely" in {
    NodeComparator.compare(null, null) must be equalTo(0)
    NodeComparator.compare(Node("127.0.0.1", 10), null) must be_>(0)
    NodeComparator.compare(null, Node("127.0.0.1", 10)) must be_<(0)
    NodeComparator.compare(Node("127.0.0.1", 10), Node("127.0.0.1", 10)) must be equalTo(0)
    NodeComparator.compare(Node("127.0.0.1", 0), Node("127.0.0.1", 10)) must be_<(0)
    NodeComparator.compare(Node("127.0.0.1", 10), Node("127.0.0.1", 0)) must be_>(0)
    NodeComparator.compare(Node("alpha", 20), Node("beta", 10)) must be_<(0)
    NodeComparator.compare(Node("beta", 10), Node("alpha", 20)) must be_>(0)
    NodeComparator.compare(Node("127.0.0.1", 10), Node(null, 10)) must be_>(0)
    NodeComparator.compare(Node(null, 10), Node("127.0.0.1", 10)) must be_<(0)
    NodeComparator.compare(Node(null, 10), Node(null, 10)) must be equalTo(0)
    NodeComparator.compare(Node(null, 1), Node(null, 10)) must be_<(0)
    NodeComparator.compare(Node(null, 10), Node(null, 1)) must be_>(0)
  }

  "Endpoints can be started and terminated" in {
    Actor.resetProxy
    val ep = new Endpoints.Endpoint(Node(null, 10), null)
    val actor = ep._guardian
    actor.start
    ActorKit.awaitExit(actor) { ep.shutdown } must beEqualTo('normal)
  }

  "Endpoints die when their parent goes down" in {
    Actor.resetProxy
    object parent extends Actor { def act = { this.exit } }
    val ep = new Endpoints.Endpoint(Node(null, 10), parent)
    val actor = ep._guardian
    actor.start
    ActorKit.awaitExit(actor) { parent.start } must beEqualTo('parent)
  }

  "Endpoints object is initially correctly created" in {
    Endpoints must notBeNull
    Endpoints._shutdown
  }

  "Endpoints object works out of the box" in {
    Endpoints._setup
    Endpoints("foo", 17) must notBe(null)
    Endpoints._shutdown
  }

  "Endpoint instances are instantiated, cached properly, and associated with their node" in {
    Endpoints._setup
    Endpoints(Node("foo", 0)) must be(Endpoints(Node("foo", 0)))
    NodeComparator.compare(Endpoints(Node("foo", 0)).node, Node("foo",0)) must be(0)
    Endpoints(Node("foo", 0)).shutdown
    Endpoints._shutdown
  }

  "Restarting Endpoints object resets entries" in {
    Endpoints._setup
    val n1 = Endpoints("foobar.org", 4096)
    Endpoints._shutdown
    Endpoints._setup
    val n2 = Endpoints("foobar.org", 4096)
    n1 must notBe(n2)
    Endpoints._shutdown
  }

  "Restarting Endpoints object twice does not resets entries" in {
    Endpoints._shutdown
    Endpoints._setup
    val n1 = Endpoints("foobar.org", 4096)
    Endpoints._setup
    val n2 = Endpoints("foobar.org", 4096)
    n1 must be(n2)
    Endpoints._shutdown
  }

  "Endpoint instances are removed on endpoint actor exit" in {
    Endpoints._setup
    val node = Node("shortlived", 2)
    val endp1 = Endpoints(node)
    endp1.shutdown
    Thread.sleep(5000L)
    val endp2 = Endpoints(node)
    endp1 must notBe(endp2)
    Endpoints._shutdown
  }

  "containsFor reflects the current state and does not create or alter endpoints" in {
    Endpoints._setup
    Endpoints.containsEndpointFor(Node("foo", 5)) must beFalse
    val node = Node("foo", 5)
    val endp = Endpoints(node)
    Endpoints.containsEndpointFor(node) must beTrue
    endp must be(Endpoints(node))
    Endpoints._shutdown
  }
  
  "Resetting Endpoints object for further use (no test)" in {
    Endpoints._shutdown
    Endpoints._setup
    0 must be(0)
  }
}
