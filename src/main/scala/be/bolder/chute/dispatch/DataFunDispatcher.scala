package be.bolder.chute.dispatch

/**
 * Dispatcher whose actions are functions that take a single input of type D which
 * is extracted from events via eventData
 */
trait DataFunDispatcher[-E, K, D] extends AbstractDispatcher[E, K, D => Unit] {

  /**
   * @return event data that is used as input to actions
   */
  protected def eventData(evt: E): D

  override protected def executorSink(evt: E): Sink[D => Unit] = new Sink[D => Unit] {
    val data = eventData(evt)

    override def drop(evt: E, action: D => Unit)(implicit actionSink: Sink[D => Unit]) =
      action(data)
  }
}

