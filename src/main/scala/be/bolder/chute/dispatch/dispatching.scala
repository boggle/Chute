package be.bolder.dispatch

import _root_.scala.collection.mutable.HashMap
import _root_.scala.collection.mutable.HashSet
import _root_.scala.Iterator
import java.util.concurrent.{ConcurrentNavigableMap, ConcurrentSkipListMap}
import java.util.{Collections, Comparator, NavigableMap}
import _root_.scala.collection.immutable.{ListSet, Set}

/**
 * Generic dispatcher interface for events of type E.
 *
 * Events are submitted by calling apply.  This maps the event to some keys of type K.
 * Each such key is resolved to some actions of type A which eventually will be run
 * by calling execute.
 *
 */
trait Dispatcher[-E, K, A] extends (E => Unit) {
  /**
   *  All action objects for given event
   */
  protected def actions(evt: E): Iterator[A]

  // Subscription related methods
  
  def +=(key: K, action: A): Unit

  def +=(key: K, actions: Iterator[A]): Unit = for (action <- actions) +=(key, action)
  def +=(key: K, actions: Iterable[A]): Unit = +=(key, actions.elements)

  def -=(key: K, action: A): Unit

  def -=(key: K, actions: Iterator[A]): Unit = for (action <- actions) -=(key, action)
  def -=(key: K, actions: Iterable[A]): Unit = -=(key, actions.elements)

  def -=(key: K): Unit

  def :=(key: K, actions: Iterator[A]): Unit = { -=(key); +=(key, actions) }
  final def :=(key: K, actions: Iterable[A]): Unit = :=(key, actions.elements)

  /**
   * @return Iterator for all keys to which currently someone has been subscribed
   */
  protected def keys: Iterator[K]
}

/**
 * Abstract skeleton implementation of Dispatcher
 *
 * @see Dispatcher
 */
trait AbstractDispatcher[-E, K, A] extends Dispatcher[E, K, A] {

  /**
   * Not guaranteed to be synchronous
   *
   * @see Dispatcher
   */
  def apply(evt: E): Unit = execute(evt, actions(evt))

  /**
   * Execute subset of matching actions for some event evt
   */
  protected def execute(event: E, actions: Iterator[A]): Unit

  /**
   *  All action objects for given key
   */
  override protected def actions(evt: E): Iterator[A] = actionsViaKeys(evt)

  /**
   * Generate matching actions for given event evt via eventKeys and actionsByKey
   */
  protected def actionsViaKeys(evt: E): Iterator[A] = {
    val allKeys = eventKeys(evt)
    if (allKeys == null) null
    else AbstractDispatcher.nullFlatten(allKeys.map { key => actionsByKey(key) })
  }

  /**
   * Override in subclass
   *
   * @return all matching keys for event evt
   */
  protected def eventKeys(evt: E): Iterator[K]

  /**
   * @return all actions matching key k
   */
  protected def actionsByKey(key: K): Iterator[A]

  override def -=(key: K): Unit = actionsByKey(key).foreach { action => this -= (key, action) }
}

object AbstractDispatcher {
  def nullFlatten[T](iters: Iterator[Iterator[T]]) =
    Iterator.flatten(iters.filter { iter => iter != null })

  def javaIter2iter[K](iter: java.util.Iterator[K]) = new Iterator[K] {
    def hasNext = iter.hasNext
    def next = iter.next
  }
}

/**
 * Dispatcher whose actions are functions that take a single input of type D which
 * is extracted from events via eventData
 */
trait DataFunDispatcher[-E, K, D] extends AbstractDispatcher[E, K, (D => Unit)] {

  /**
   * @return event data that is used as input to actions
   */
  protected def eventData(evt: E): D

  override protected def execute(evt: E, actions: Iterator[D => Unit]): Unit = {
    val data = eventData(evt)
    actions.foreach { action => action(data) }
  }
}

