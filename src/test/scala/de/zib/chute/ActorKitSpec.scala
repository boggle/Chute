package de.zib.chute


import _root_.scala.actors.Actor
import _root_.scala.compat.Platform


/**
 * Testing the ActorKit
 * 
 * @author Stefan Plantikow <plantikow@zib.de>
 */

object ActorKitSpec extends Spec {

  import ActorKit.sleepUninterrupted
  
  "sleepUninterrupted sleeps no less than what asked for, even if interrupted" in {
    Actor.resetProxy    
    val start = Platform.currentTime
    val thread: Thread = Thread.currentThread
    Actor.actor { sleepUninterrupted(actorSendDelay / 2); thread.interrupt }
    sleepUninterrupted(actorSendDelay)
    Platform.currentTime-start must be_>=(actorSendDelay)
  }

  "awaitExit waits for actors to exit (forever)" in {
    Actor.resetProxy
    val actor = new Actor { def act = react { case 'terminate => this.exit('terminate) } }
    ActorKit.awaitExit(actor)
      { actor.start; Actor.actor { actor ! 'terminate } } must be('terminate)
    0 must be(0)
  }

  "awaitExit_ waits for actors to exit (forever)" in {
    Actor.resetProxy
    val actor = new Actor { def act = react { case 'terminate => this.exit('terminate) } }
    Actor.self.link(actor)
    actor.start
    ActorKit.awaitExit_(actor) { Actor.actor { actor ! 'terminate } } must be('terminate)
    0 must be(0)
  }

  "timed awaitExit waits for actors to exit (some time)" in {
    Actor.resetProxy
    val actor = new Actor { def act = react { case 'terminate => exit('terminate) } }
    ActorKit.awaitExit(actor, actorSendDelay * 4)
      { actor.start; Actor.actor { actor ! 'terminate } } must beEqualTo(Some('terminate))
  }

  "timed awaitExit_ waits for actors to exit (some time)" in {
    Actor.resetProxy
    val actor = new Actor { def act = react { case 'terminate => exit('terminate) } }
    Actor.self.link(actor)
    actor.start
    ActorKit.awaitExit_(actor, actorSendDelay * 4)
      { Actor.actor { actor ! 'terminate } } must beEqualTo(Some('terminate))
  }

  "awaitExit waits for actors to exit (timeout)" in {
    Actor.resetProxy
    val actor = new Actor { def act = react { case 'terminate => exit('terminate) } }
    ActorKit.awaitExit(actor, actorSendDelay / 2)
      { actor.start; Actor.actor { sleepUninterrupted(actorSendDelay * 4); actor ! 'terminate } } must beNone
  }

  "awaitExit_ waits for actors to exit (timeout)" in {
    Actor.resetProxy
    val actor = new Actor { def act = react { case 'terminate => exit('terminate) } }
    Actor.self.link(actor)
    actor.start

    ActorKit.awaitExit_(actor, actorSendDelay / 2)
      { Actor.actor { sleepUninterrupted(actorSendDelay * 4); actor ! 'terminate } } must beNone
  }

  "mkWatcher dies properly" in {
    Actor.resetProxy
    object actor extends Actor { def act: Unit = { Actor.self.exit('terminate) } }

    val g = ActorKit.mkWatcher(actor)
    ActorKit.awaitExit(g) { g.start } must beEqualTo('terminate)
  }

  "mkWatcher_ dies properly" in {
    Actor.resetProxy
    object actor extends Actor { def act: Unit =
      { receive { case msg => Actor.self.exit('terminate) } } }

    val g = ActorKit.mkWatcher_(actor)
    ActorKit.awaitExit(g) { g.start; actor.start; actor ! 'ping } must beEqualTo('terminate)
  }

  "mkForwarder forwards properly" in {
    Actor.resetProxy
    var box: Any = null
    object actor extends Actor { def act =
      { Actor.self.receive { case  msg => box = msg }; Actor.self.exit } }
    val f = ActorKit.mkForwarder(actor)
    ActorKit.awaitExit(f) { f.start; f ! 'secret }
    box must beEqualTo('secret)
  }

  "mkForwarder_ forwards properly" in {
    Actor.resetProxy
    var box: Any = null
    val actor = Actor.actor { Actor.self.receive { case  msg => box = msg }; Actor.self.exit }
    val f = ActorKit.mkForwarder_(actor)
    ActorKit.awaitExit(f) { f.start; f ! 'secret }
    box must beEqualTo('secret)
  }

  "mkGuardian filters properly" in {
    Actor.resetProxy
    var box: Any = null
    object actor extends Actor { def act =
      { Actor.self.receive { case  msg => box = msg }; Actor.self.exit } }
    val f = ActorKit.mkGuardian(actor) { msg => msg == 'secret }
    ActorKit.awaitExit(f) { f.start; f ! 'pix; f ! 'secret; f ! 'password }
    box must beEqualTo('secret)
  }

  "mkGuardian_ filters properly" in {
    Actor.resetProxy
    var box: Any = null
    val actor = Actor.actor { Actor.self.receive { case msg => box = msg }; Actor.self.exit }
    val f = ActorKit.mkGuardian_(actor) { msg => msg == 'secret }
    ActorKit.awaitExit(f) { f.start; f ! 'pix; f ! 'secret; f ! 'password }
    box must beEqualTo('secret)
  }
}
