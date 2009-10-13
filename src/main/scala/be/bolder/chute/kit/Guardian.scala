package be.bolder.chute

import _root_.scala.actors.Actor
import _root_.scala.actors.Exit
import _root_.scala.actors.Actor.loop
import _root_.scala.actors.AbstractActor

/**
 * Actor that forwards all messages to target that are accepted by pred.
 * Dies if it receives an exit signal from its target, copying the reason.
 *
 * Useful for granting limited access to another actor, e.g. during testing.
 *
 * Guardians trap exits and link to target in act(). If subclasses link to other actors, exit
 * messages from those actors are silently dropped.
 *
 * @see ExtraExitHandling for differing behaviour concerning extra exit messages
 *
 * @see ActorKit for factory methods for actor guardians with various properties
 *
 * @author Stefan Plantikow <plantikow@zib.de>
 */
abstract
class Guardian[+A <: AbstractActor](protected[this] val target: A)(val pred: Any => Boolean)
  extends Actor {

  def act = {
    trapExit = true;
    link(target)
    startTarget
    loop {
      react {
        case Exit(who, what) if who eq target =>
          Actor.self.exit(what)
        case msg: Exit =>
          handleExtraExitMessage(msg)
        case msg =>
          handleMessage(msg)
    } }
  }

  /**
   * Forwards all messages that match pred by calling @see forwardMessage
   */
  protected[this] def handleMessage(msg: Any)= msg match {
    case any if pred(any) => forwardMessage(msg)
    case any => handleUnknownMessage(msg)
  }

  /**
   * Handles exit messages not originating from target
   * By default, calls handleUnknownMessage
   *
   */
  protected[this] def handleExtraExitMessage(msg: Exit) = handleUnknownMessage(msg)

  /**
   * Forwards msg to target
   */
  protected[this] def forwardMessage(msg: Any) = target.forward(msg)


  /**
   * Does nothing with message
   */
  protected[this] def handleUnknownMessage(msg: Any) = ()

  def startTarget: Unit
}

/**
 * Mixin trait for guardians.  Extra exit messages are treated like regular messages,
 * i.e. if they match pred they get forwarded.
 *
 * @see Guardian
 *
 * @author Stefan Plantikow <plantikow@zib.de>
 *
 */
trait ExtraExitHandling {
  self: Guardian[_] =>

  def handleExtraExitMessage(msg: Exit) = handleMessage(msg)
}

/**
 * Mixin trait for guardians whose targets are actors.  Enables starting of the target
 * through a reference to the guardian.
 *
 * @author Stefan Plantikow <plantikow@zib.de>
 *
 */
trait TargetStarting {
  self: Guardian[Actor] =>

  /**
   * Start the target if it is not yet running
   */
  def startTarget: Unit = target.start
}


/**
 * Mixin trait that provides an empty implementation of startTarget
 *
 * @author Stefan Plantikow <plantikow@zib.de>
 */
trait NoTargetStarting {
  self: Guardian[AbstractActor] =>
  
  /**
   * Does nothing
   */
  def startTarget: Unit = ()
}

/**
 * Guardian that drops all messages, only dealing with exits from the target
 *
 * @see Guardian
 *
 * @author Stefan Plantikow <plantikow@zib.de>
 */
abstract class Watcher[+A <: AbstractActor](_target: A) extends Guardian(_target)({ _ => false }) ;


/**
 * Guardian that forwards all messages
 *
 * @see Guardian
 *
 * @author Stefan Plantikow <plantikow@zib.de>
 */
abstract class Forwarder[+A <: AbstractActor](_target: A) extends Guardian(_target)({ _ => true}) ;


