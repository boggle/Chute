package be.bolder.chute.dispatch.impl

import java.util.Comparator
import java.util.concurrent.ConcurrentNavigableMap
import be.bolder.chute.dispatch.DataFunDispatcher

abstract class AbstractEventDispatcher[-E, K, D](val comparator: Comparator[K])
        extends AbstractConcurrentMultiKeyDispatcher[E, K, D => Unit]
                with DataFunDispatcher[E, K, D] {

  def this() = this(null)
  
  override val map: ConcurrentNavigableMap[K, Set[D => Unit]] =
    if (comparator == null)
      new java.util.concurrent.ConcurrentSkipListMap[K, Set[D => Unit]]()
  else
      new java.util.concurrent.ConcurrentSkipListMap[K, Set[D => Unit]](comparator)

}
