package be.bolder.chute.dispatch.multi

/**
 * Class describing keys and key ranges
 *
 * @author Stefan Plantikow
 *
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
 * Trait for dispatchers that support *looking*up* matching actions via key ranges
 * (subscription is still key-based only)
 *
 * @author Stefan Plantikow
 *
 */
trait AbstractMultiKeyDispatcher[-E, K, A] extends AbstractDispatcher[E, K, A] {

  /***
   * @see collectKeySpec
   *
   * Sink for collecting all actions matching according to some KeySpecs
   */
  protected val specSink: Sink[KeySpec[K]] = new Sink[KeySpec[K]] {
    override def drop(evt: E, spec: KeySpec[K])(implicit actionSink: Sink[A]) =
      collectKeySpec(evt, spec)
  }

  /**
   * Override in subclass
   *
   * Collect all actions for given key into actionSink
   */
  protected def collectKeySpec(evt: E, spec: KeySpec[K])(implicit actionSink: Sink[A])
}
