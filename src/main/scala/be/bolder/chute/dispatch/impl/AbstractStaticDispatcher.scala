package be.bolder.chute.dispatch.impl

import be.bolder.chute.dispatch.AbstractDispatcher

/**
 * Dispatcher for static, unmodifiable mappings
 *
 * @author Stefan Plantikow
 *
 */
abstract class StaticDispatcher[-E, K, A](val map: PartialFunction[K, Iterator[A]])
        extends AbstractDispatcher[E, K, A] {

  override protected val keySink = new Sink[K] {
    override def drop(evt: E, key: K)(implicit actionSink: Sink[A]) =
      actionSink.dropIterator(evt, map(key))
  }

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
}
