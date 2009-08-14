package be.bolder.dispatch

/**
 * Class describing keys and key ranges
 */
sealed abstract class KeySpec[K]

case class Key[K](key: K) extends KeySpec[K]

case class KeyIterator[K](iter: Iterator[K]) extends KeySpec[K]

case class KeyIterable[K](iter: Iterable[K]) extends KeySpec[K]

case class HeadKeys[K](toKey: K, inclusive: Boolean) extends KeySpec[K] {
  def this(toKey: K) = this(toKey, false)
}

case class TailKeys[K](fromKey: K, inclusive: Boolean) extends KeySpec[K] {
  def this(fromKey: K) = this(fromKey, true)
}

case class RangeKeys[K](fromKey: K, fromInclusive: Boolean, toKey: K, toInclusive: Boolean)
extends KeySpec[K] {
  def this(fromKey: K, toKey: K) = this(fromKey, true, toKey, false)
}

/**
 * Trait for dispatchers that support looking up matching actions via key ranges
 */
trait MultiKeyDispatcher[K, A] {
  /***
   * @return All actions matching according to KeySpec spec
   */
  protected def actionsBySpec(spec: KeySpec[K]): Iterator[A]
}

/**
 * Default implementation of MultiKeyDispatchers for AbstractDispatchers
 */
trait AbstractMultiKeyDispatcher[-E, K, A] extends AbstractDispatcher[E, K, A]
  with MultiKeyDispatcher[K, A] {

  /**
   * @return matching actions for given event, computed using eventSpec and actionsBySpec
   */
  protected def actionsViaSpecs(evt: E): Iterator[A] = {
    val allSpecs = eventSpecs(evt)
    if (allSpecs == null) null
    else AbstractDispatcher.nullFlatten(allSpecs.map { spec => actionsBySpec(spec) })
  }

  /**
   * @return KeySpec iterator for all matching keys of Event evt
   */
  protected def eventSpecs(evt: E): Iterator[KeySpec[K]] = Iterator.single(KeyIterator(eventKeys(evt)))

  /**
   * @return Actions for given spec
   */
  protected def actionsBySpec(spec: KeySpec[K]): Iterator[A]


  /**
   * Convert KeySpec to key iterable
   *
   * @return key iterable
   */
  protected def spec2iterable(spec: KeySpec[K]): Iterable[K] = new Iterable[K] {
    def elements: Iterator[K] = spec2iter(spec)
  }

  /**
   * Convert KeySpec to key iterator
   *
   * @return key iterator
   */
  protected def spec2iter(spec: KeySpec[K]): Iterator[K];
}

/**
 * Trait for AbstractMultiKeyDispatchers that chose to use eventSpecs instead of eventKeys
 */
trait SpecDispatcher[-E, K, A] extends AbstractMultiKeyDispatcher[E, K, A] {
  override def actions(evt: E): Iterator[A] = actionsViaSpecs(evt)
}

