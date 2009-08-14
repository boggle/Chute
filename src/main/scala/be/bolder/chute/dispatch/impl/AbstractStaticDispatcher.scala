package be.bolder.chute.dispatch

import _root_.scala.collection.mutable.HashMap
import _root_.scala.collection.mutable.HashSet
import _root_.scala.Iterator
import _root_.scala.collection.immutable.{ListSet, Set}
import be.bolder.dispatch._

/**
 * Dispatcher for static, unmodifiable mappings
 */
abstract class StaticDispatcher[-E, K, A](val map: PartialFunction[K, Iterator[A]])
        extends AbstractDispatcher[E, K, A] {

  override protected def actionsByKey(key: K): Iterator[A] = map(key)

  /**
   * @throws UnsupportedOperationException
   */
  def +=(key: K, action: A): Unit = throw new UnsupportedOperationException

  /**
   * @throws UnsupportedOperationException
   */
  def -=(key: K, action: A): Unit = throw new UnsupportedOperationException

  /**
   * @throws UnsupportedOperationException
   */
  override def -=(key: K): Unit = throw new UnsupportedOperationException

  override protected def keys: Iterator[K] = throw new UnsupportedOperationException
}
