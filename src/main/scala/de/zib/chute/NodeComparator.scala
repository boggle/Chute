package de.zib.chute


import actors.remote.Node

/**
 * Plain comparator for instances of scala.actor.remote.Node
 *
 * @author Stefan Plantikow <plantikow@zib.de>
 *
 */
object NodeComparator extends java.util.Comparator[Node] {
  def compare(a: Node, b: Node): Int = {
    if (a eq null) return (if (b eq null) 0 else -1)
    if (b eq null) return (if (a eq null) 0 else +1)
    if (a.address eq null) return (if (b.address eq null) a.port-b.port else -1)
    if (b.address eq null) return (if (a.address eq null) a.port-b.port else +1)
    val addrCmp = a.address.compareTo(b.address)
    if (addrCmp == 0) a.port-b.port	else addrCmp
  }

  override def equals(obj: Any): Boolean = obj == this
}
