package be.bolder.chute.dispatch

/**
 * Generic dispatcher interface for events of type E.
 *
 * Events are submitted by calling apply.  This maps the event to some keys of type K.
 * Each such key is resolved to some subscribed actions of type A which eventually will be run
 * by calling execute.
 *
 * @author Stefan Plantikow
 *
 */
trait Dispatcher[-E, -K, -A] extends (E => Unit) {

  /**
   * Apply is the major method of a dispatcher
   *
   * Called with event evt, all actions subscribed to this event are executed
   *
   * In threadsafe implementations, execution may be asynchronously
   *
   */
  def apply(evt: E): Unit

  // Subscription related methods

  /**
   * Add action as subscriptions for given key
   *
   * In threadsafe implementations, guaranteed to be executed atomically, synchronized and fair
   *
   */
  def +=(key: K, action: A): Unit

  def +=(key: K, actions: Iterator[A]): Unit = for (action <- actions) +=(key, action)
  def +=(key: K, actions: Iterable[A]): Unit = +=(key, actions.elements)

  /**
   * Remove action as subscriptions for given key
   *
   * In threadsafe implementations, guaranteed to be executed atomically, synchronized and fair
   *
   */
  def -=(key: K, action: A): Unit

  def -=(key: K, actions: Iterator[A]): Unit = for (action <- actions) -=(key, action)
  def -=(key: K, actions: Iterable[A]): Unit = -=(key, actions.elements)

  // protected by default since these interfere with subscriptions seemingly not available
  // to the caller

  /**
   * Remove all subscriptions for given key
   */
  protected def -=(key: K): Unit

  protected def :=(key: K, actions: Iterator[A]): Unit = { -=(key); +=(key, actions) }
  protected def :=(key: K, actions: Iterable[A]): Unit = :=(key, actions.elements)
}

/**
 *   Abstract skeleton implementation of Dispatcher  
 *
 * @author Stefan Plantikow
 *
 * @see Dispatcher
 */
abstract class AbstractDispatcher[-E, -K, A] extends Dispatcher[E, K, A] {

  /**
   * Sink for instances of some type T in the context of handling of events of type E
   *
   * Used to generalize event processing. Action execution for some event is abstracted
   * over in collect.  Collect takes an event and an ActionSink as the target sink for
   * action execution as parameters and may use locally available
   * intermediary sinks (like keySink) to build up all actions that need to be executed
   *
   * @author Stefan Plantikow
   *
   * @see Sink[A]
   * @see KeySink
   */
  trait Sink[-T] {
    def drop(evt: E, trigger: T)(implicit actionSink: Sink[A]): Unit

    def dropIterator(evt: E, triggers: Iterator[T])(implicit actionSink: Sink[A]): Unit =
      triggers.foreach { trigger => drop(evt, trigger) }

    def dropIterable(evt: E, triggers: Iterable[T])(implicit actionSink: Sink[A]): Unit =
      dropIterator(evt, triggers.elements)

    def collect(thunk: (Sink[A]) => Unit): Iterator[A] = {
      val collector = new ActionCollector
      thunk(collector)
      collector.elements
    }
  }

  /**
   * May be implemented by implementation data structures to fully utilize
   * the sink interface for collecting results
   */
  trait Source[+T] { def collect(evt: E, targetSink: Sink[T]) }

  /**
   * Silently collects everything that is dropped into this sink into a list which may
   * be queried via elements
   */
  class CollectorSink[T] extends Sink[T] {
    var list = List[T]()

    override def drop(evt: E, trigger: T)(implicit actionSink: Sink[A]) =
      { list = trigger :: list }

    def elements = list.elements
  }

  class ActionCollector extends CollectorSink[A] ;



  /**
   * Signal event evt to all subscribers
   * 
   * Not guaranteed to be synchronous
   *
   * @see Dispatcher
   */
  def apply(evt: E): Unit = collect(evt)(executorSink(evt))

  /**
   *  All action objects for given key
   */
  protected def actions(evt: E): Iterator[A] = {
    val sink = new ActionCollector
    collect(evt)(sink)
    sink.elements
  }

  /**
   * Override in subclass
   *
   * @see Sink
   *
   * Collect all matching actions for event by dropping them in actionSink
   */
  protected def collect(evt: E)(implicit actionSink: Sink[A])

  /**
   * Override in subclass
   *
   * @return Sink[A] that describes action execution for event handling as used by apply
   */
  protected def executorSink(evt: E): Sink[A]

  /**
   * Override in subclass
   *
   * @return Sink for triggering actions for an event by key
   */
  protected val keySink: Sink[K] = null
}

object AbstractDispatcher {
  /**
   * @return iters flattened after filtering out nulls
   */
  def nullFlatten[T](iters: Iterator[Iterator[T]]) =
    Iterator.flatten(iters.filter { iter => iter != null })

  /**
   * @return scala iterator for given java iterator
   */
  def javaIter2iter[K](iter: java.util.Iterator[K]) = new Iterator[K] {
    def hasNext = iter.hasNext
    def next = iter.next
  }
}