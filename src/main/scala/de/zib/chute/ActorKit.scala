package de.zib.chute


import _root_.scala.actors.Actor.{loop, react}
import _root_.scala.actors.AbstractActor
import _root_.scala.actors.{TIMEOUT, AbstractActor, Exit, Actor}
import _root_.scala.compat.Platform


/**
 * Utility functions for actors
 *
 * TODO: awaitExit with condition on sets + any and all variants
 * TODO: cps-variants with react (?)
 * TODO: lots of nice little tests for the above todos... :-(
 * 
 * @author Stefan Plantikow <plantikow@zib.de>
 */

object ActorKit {

  /**
   * Sleep for dur ms, even if interrupted
   * 
   * If dur is negative, nothing happens and dur is returned.
   *
   * @return difference from target sleep time and time beofre the method returns
   */
  def sleepUninterrupted(dur: Long): Long = {
    var rem = dur
    val end = Platform.currentTime + dur
    while (rem >= 0) {
      try { Thread.sleep(rem) }
      catch { case (_:InterruptedException) => () }
      rem = end - Platform.currentTime
    }
    return rem
  }

  /**
   * Wait (blocking) for receiving Exit from actor (compares via eq!)
   * after linking to actor fby executing thunk (trapExit is temporarily set to true)
   *
   * Uses receive
   *
   * @return reason
   */
  def awaitExit[A <: AbstractActor](actor: A)(thunk: => Unit): Any = {
    val traps = Actor.self.trapExit
    try {
      Actor.self.trapExit = true
      Actor.self.link(actor)
      thunk
      val result = Actor.self.receive {
        case Exit(sender, reason) if sender eq actor => reason
      }
      result
    }
    finally { Actor.self.trapExit = traps }
  }

  /**
   * Wait at most time ms for receiving Exit from actor (compares via eq!)
   * after linking to actor fby executing thunk (trapExit is temporarily set to true)
   *
   * Uses receive
   *
   * @return reason
   */
  def awaitExit[A <: AbstractActor](actor: A, time: Long)(thunk: => Unit): Option[Any] = {
    val traps = Actor.self.trapExit
    try {
      Actor.self.trapExit = true
      Actor.self.link(actor)
      thunk
      val result = Actor.self.receiveWithin(time) {
        case Exit(from, reason) if (from eq actor) =>
          Some(reason)
        case TIMEOUT => None
      }
      result
    }
    finally { Actor.self.trapExit = traps }
  }

  /**
   * Wait (blocking) for receiving Exit from actor (compares via eq!)
   * after executing thunk (trapExit is temporarily set to true)
   *
   * Uses receive
   *
   * @return reason
   */
  def awaitExit_[A <: AbstractActor](actor: A)(thunk: => Unit): Any = {
    val traps = Actor.self.trapExit
    try {
      Actor.self.trapExit = true
      thunk
      val result = Actor.self.receive {
        case Exit(sender, reason) if sender eq actor => reason
      }
      result
    }
    finally { Actor.self.trapExit = traps }
  }

  /**
   * Wait at most time ms for receiving Exit from actor (compares via eq!)
   * after executing thunk (trapExit is temporarily set to true)
   *
   * Uses receive
   *
   * @return reason
   */
  def awaitExit_[A <: AbstractActor](actor: A, time: Long)(thunk: => Unit): Option[Any] = {
    val traps = Actor.self.trapExit
    try {
      Actor.self.trapExit = true
      thunk
      val result = Actor.self.receiveWithin(time) {
        case Exit(from, reason) if (from eq actor) =>
          Some(reason)
        case TIMEOUT => None
      }
      result
    }
    finally { Actor.self.trapExit = traps }
  }

  /**
   * Creates an actor that forwards all messages to target that are accepted by pred.
   * Dies if it receives an exit signal from its target, copying the reason.
   *
   * The guardian is autostarted!  Calling start on the guardian starts the target,
   * while exit exits this guardian, i.e. you cannot exit the target actor via its guardian.
   *
   * Useful for granting limited access to another actor, e.g. during testing
   *
   * Starts the target
   *
   * @return guardian actor as described above
   */
  def mkGuardian[A <: Actor](target: A)(pred: Any => Boolean) =
    new Guardian[A](target)(pred) with TargetStarting

  /**
   * Starts the target
   *
   * @return Guardian that forwards everything
   */
  def mkForwarder[A <: Actor](target: A) = new Forwarder[A](target) with TargetStarting

  /**
   * Starts the target
   *
   * @return Guardian that forwards nothing
   */
  def mkWatcher[A <: Actor](target: A) = new Watcher[A](target) with TargetStarting

  /**
   * Creates an actor that forwards all messages to target that are accepted by pred.
   * Dies if it receives an exit signal from its target, copying the reason.
   *
   * Does not start the target
   *
   * Useful for granting limited access to another actor, e.g. during testing
   *
   * @return guardian actor as described above
   */
  def mkGuardian_[A <: Actor](target: A)(pred: Any => Boolean) =
    new Guardian[A](target)(pred) with NoTargetStarting

  /**
   *
   * Does not start the target
   *
   * @return Guardian that forwards everything
   */
  def mkForwarder_[A <: Actor](target: A) = new Forwarder[A](target) with NoTargetStarting

  /**
   *
   * Does not start the target
   *
   * @return Guardian that forwards nothing
   */
  def mkWatcher_[A <: Actor](target: A) = new Watcher[A](target) with NoTargetStarting
}