package be.bolder.dispatch.impl

import be.bolder.dispatch._
import java.util.Comparator
import java.util.concurrent.ConcurrentNavigableMap
import be.bolder.chute.dispatch.impl.AbstractConcurrentMultiKeyDispatcher

abstract class AbstractEventDispatcher[-E, K, D](val comparator: Comparator[K])
        extends AbstractConcurrentMultiKeyDispatcher[E, K, D => Unit]
                with DataFunDispatcher[E, K, D] {

  def this() = this(null)
  
  override val map: ConcurrentNavigableMap[K, Set[D => Unit]] =
    if (comparator == null)
      new java.util.concurrent.ConcurrentSkipListMap[K, Set[D => Unit]]()
  else
      new java.util.concurrent.ConcurrentSkipListMap[K, Set[D => Unit]](comparator)

  protected def actionSet(key: K, iter: Iterator[(D) => Unit]) = emptyActionSet(key) ++ iter
}
