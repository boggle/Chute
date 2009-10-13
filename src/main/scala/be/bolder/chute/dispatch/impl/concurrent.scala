package be.bolder.chute.dispatch.impl

import be.bolder.chute.dispatch._
import java.util.concurrent.ConcurrentNavigableMap
import collection.immutable.ListSet
import multi._

/**
 * Dispatcher implementation based on ConcurrentNavigableMap and immutable sets
 * (currently ListSets but this can be overridden easily, see emptyActionSet)
 *
 * @author Stefan Plantikow
 *
 */
abstract class AbstractConcurrentDispatcher[-E, K, A] extends AbstractDispatcher[E, K, A] {
  val map: ConcurrentNavigableMap[K, Set[A]]

  /**
   * KeySink for dispatching via keys
   */
  override protected val keySink = new Sink[K] {
    override def drop(evt: E, key: K)(implicit actionSink: Sink[A]) = {
      val set = actionSet(key)
      if (! set.isEmpty) actionSink.dropIterable(evt, set)
    }
  }

  /**
   * Create empty set for holding actions
   *
   * @return ListSet.emtpy
   */
  protected def emptyActionSet(key: K): Set[A] = ListSet.empty

  /**
   * Create an action set for key populated from iter
   */
  protected def actionSet(key: K, iter: Iterator[A]) = emptyActionSet(key) ++ iter

  /**
   * @return current actionSet for given key
   */
  protected def actionSet(key: K): Set[A] = {
     val actions = map.get(key)
     if (actions == null) {
       val newActions = emptyActionSet(key)
       val result = map.putIfAbsent(key, newActions)
       if (result == null) newActions else result 
     }
     else actions
   }

  override def +=(key: K, action: A) = {
    var oldValue: Set[A] = null;
    var newValue: Set[A] = null;
    do {
      oldValue = actionSet(key)
      newValue = oldValue + action
    }
    while (! map.replace(key, oldValue, newValue))
  }

  override def +=(key: K, actions: Iterator[A]): Unit = {
    // materialize since iterator might not be reusable
    val addValue: Set[A] = actionSet(key, actions)
    var oldValue: Set[A] = null;
    var newValue: Set[A] = null;
    do {
      oldValue = actionSet(key)
      newValue = oldValue ++ actions
    }
    while (! map.replace(key, oldValue, newValue))
  }

  override def +=(key: K, actions: Iterable[A]): Unit = {
    var oldValue: Set[A] = null;
    var newValue: Set[A] = null;
    do {
      oldValue = actionSet(key)
      newValue = oldValue ++ actions
    }
    while (! map.replace(key, oldValue, newValue))
  }

  override def -=(key: K, action: A) = {
    var oldValue: Set[A] = null;
    var newValue: Set[A] = null;
    do {
      oldValue = actionSet(key)
      newValue = oldValue - action
    }
    while (! (if (newValue.isEmpty) map.remove(key, oldValue)
              else map.replace(key, oldValue, newValue)))
   }

  override def -=(key: K, actions: Iterator[A]): Unit = {
    // materialize since iterator might not be reusable
    val addValue: Set[A] = actionSet(key, actions)
    var oldValue: Set[A] = null;
    var newValue: Set[A] = null;
    do {
      oldValue = actionSet(key)
      newValue = oldValue -- actions
    }
    while (! map.replace(key, oldValue, newValue))
  }

  override def -=(key: K, actions: Iterable[A]): Unit = {
    var oldValue: Set[A] = null;
    var newValue: Set[A] = null;
    do {
      oldValue = actionSet(key)
      newValue = oldValue -- actions
    }
    while (! map.replace(key, oldValue, newValue))
  }

  override def -=(key: K) = {
    var oldValue: Set[A] = null;
    var newValue: Set[A] = null;
    do {
      oldValue = actionSet(key)
    }
    while (! map.remove(key, oldValue))
   }

  override def :=(key: K, actions: Iterator[A]): Unit = {
    // materialize since iterator might not be reusable
    val newValue: Set[A] = actionSet(key, actions)
    var oldValue: Set[A] = null;
    do {
      oldValue = actionSet(key)
    }
    while (! map.replace(key, oldValue, newValue))
  }

  protected def keys: Iterator[K] = AbstractDispatcher.javaIter2iter(map.keySet.iterator)
}

/**
 * ConcurrentDispatcher ready for use with multikeys.
 *
 * @author Stefan Plantikow
 * 
 */
abstract class AbstractConcurrentMultiKeyDispatcher[-E, K, A]
        extends AbstractConcurrentDispatcher[E, K, A]
                with AbstractMultiKeyDispatcher[E, K, A] {

  override protected val specSink = new Sink[KeySpec[K]] {
    override def drop(evt: E, spec: KeySpec[K])(implicit actionSink: Sink[A]) = spec match {
      case null => ()

      case Key(key: K) => keySink.drop(evt, key)(actionSink)

      case KeyIterator(iter: Iterator[K]) => keySink.dropIterator(evt, iter)(actionSink)

      case KeyIterable(iterable: Iterable[K]) => keySink.dropIterable(evt, iterable)(actionSink)

      case HeadKeys(toKey: K, inclusive: Boolean) =>
        keySink.dropIterator(evt, AbstractDispatcher.javaIter2iter(
          map.headMap(toKey, inclusive).keySet.iterator))(actionSink)

      case TailKeys(fromKey: K, inclusive: Boolean) =>
        keySink.dropIterator(evt, AbstractDispatcher.javaIter2iter(
          map.tailMap(fromKey, inclusive).keySet.iterator))(actionSink)

      case RangeKeys(fromKey: K, fromInclusive: Boolean, toKey: K, toInclusive: Boolean) =>
        keySink.dropIterator(evt, AbstractDispatcher.javaIter2iter(
          map.subMap(fromKey, fromInclusive, toKey, toInclusive).keySet.iterator))(actionSink)
    }
  }
}
