package be.bolder.chute.dispatch

import _root_.scala.collection.mutable.HashMap
import _root_.scala.collection.mutable.HashSet
import _root_.scala.Iterator
import _root_.scala.collection.immutable.{ListSet, Set}
import be.bolder.dispatch._

/**
 * Non-thread-safe dispatcher based on mutable HashMap and HashSet
 */
abstract class AbstractMapSetDispatcher[-E, K, A] extends AbstractDispatcher[E, K, A] {
  protected val map = new HashMap[K, HashSet[A]]

  protected def actionSet(key: K) = {
    val actions = map(key)
    if (actions == null) {
      val newActions = new HashSet[A]
      map.put(key, newActions)
      newActions
    }
    else actions
  }

  override protected def actionsByKey(key: K): Iterator[A] = actionSet(key).elements

  override def +=(key: K, action: A) = actionSet(key) += action

  override def -=(key: K, action: A) = {
    val set = actionSet(key)
    set -= action
    if (set.isEmpty) map -= key
  }

  override protected def keys: Iterator[K] = map.keySet.elements
}

