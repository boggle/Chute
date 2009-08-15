package be.bolder.chute.dispatch.impl

import _root_.scala.collection.mutable.HashMap
import _root_.scala.collection.mutable.HashSet
import be.bolder.chute.dispatch.AbstractDispatcher

/**
 * Non-thread-safe dispatcher based on mutable HashMap and HashSet
 *
 * @author Stefan Plantikow
 *
 */
abstract class AbstractMapSetDispatcher[-E, K, A] extends AbstractDispatcher[E, K, A] {
  protected val map = new HashMap[K, HashSet[A]]

  override protected val keySink = new Sink[K] {
    override def drop(evt: E, key: K)(implicit actionSink: Sink[A]) = {
      val set = actionSet(key)
      if (! set.isEmpty) actionSink.dropIterable(evt, set)
    }
  }

  /**
   * @return current set of actions for given key
   */
  protected def actionSet(key: K) = {
    val actions = map(key)
    if (actions == null) {
      val newActions = new HashSet[A]
      map.put(key, newActions)
      newActions
    }
    else actions
  }

  override def +=(key: K, action: A) = actionSet(key) += action

  override def -=(key: K, action: A) = {
    val set = actionSet(key)
    set -= action
    if (set.isEmpty) map -= key
  }
}