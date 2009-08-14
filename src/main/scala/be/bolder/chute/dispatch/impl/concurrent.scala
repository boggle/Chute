package be.bolder.chute.dispatch.impl

import be.bolder.dispatch._
import java.util.concurrent.ConcurrentNavigableMap
import collection.immutable.{TreeSet, ListSet}

/**
 * Dispatcher implementation based on ConcurrentNavigableMap and immutable sets
 */
abstract class AbstractConcurrentDispatcher[-E, K, A] extends AbstractDispatcher[E, K, A] {
  val map: ConcurrentNavigableMap[K, Set[A]]

  /**
   * Create empty set for holding actions
   *
   * @return ListSet.emtpy
   */
  protected def emptyActionSet(key: K): Set[A] = ListSet.empty

  protected def actionSet(key: K, iter: Iterator[A]): Set[A]

  protected def actionSet(key: K): Set[A] = {
     val actions = map.get(key)
     if (actions == null) {
       val newActions = emptyActionSet(key)
       val result = map.putIfAbsent(key, newActions)
       if (result == null) newActions else result 
     }
     else actions
   }

  override protected def actionsByKey(key: K): Iterator[A] = actionSet(key).elements

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
 * ConcurrentDispatcher nearly-ready for use with multikeys.
 *
 * Concrete subclasses need to decide wether they want to either override eventKeys or
 * override eventSpecs and inherit from SpecDispatcher to use specs
 *
 * In any case, incoming events get compiled down to an action iterator which is then sequentially
 * processed to dispatch the event.
 *
 * However a smarter implementation in a subclass that e.g. directly works on Iterator[KeySpec], 
 * forking an actor per spec, is left possible.
 */
abstract class AbstractConcurrentMultiKeyDispatcher[-E, K, A]
        extends AbstractConcurrentDispatcher[E, K, A]
                with AbstractMultiKeyDispatcher[E, K, A] {

  protected def actionsBySpec(spec: KeySpec[K]): Iterator[A] =
    AbstractDispatcher.nullFlatten(spec2iter(spec).map { key => actionsByKey(key) })

  override protected def eventKeys(evt: E): Iterator[K] =
    AbstractDispatcher.nullFlatten(eventSpecs(evt).map { spec => spec2iter(spec) })


  protected def spec2iter(spec: KeySpec[K]): Iterator[K] = spec match {
    case null => Iterator.empty
    
    case Key(key: K) => Iterator.single(key)

    case KeyIterator(iter: Iterator[K]) => iter

    case KeyIterable(iterable: Iterable[K]) => iterable.elements

    case HeadKeys(toKey: K, inclusive: Boolean) =>
      AbstractDispatcher.javaIter2iter(map.headMap(toKey, inclusive).keySet.iterator)

    case TailKeys(fromKey: K, inclusive: Boolean) =>
      AbstractDispatcher.javaIter2iter(map.tailMap(fromKey, inclusive).keySet.iterator)

    case RangeKeys(fromKey: K, fromInclusive: Boolean, toKey: K, toInclusive: Boolean) =>
      AbstractDispatcher.javaIter2iter(
        map.subMap(fromKey, fromInclusive, toKey, toInclusive).keySet.iterator)
  }
}
