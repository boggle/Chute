package be.bolder.chute.endpoints

import _root_.sbinary.{Format, DefaultProtocol}
import _root_.scala.reflect.Manifest



/**
 * Possible message target
 */
sealed abstract class Target
final case class SingleTarget(target: Long) extends Target
final case class MultiTargets(targets: Long*) extends Target
final case class NamedTarget[N](name: N) extends Target


class TargetProtocol[N](implicit val nameFormat: Format[N], val nameManifest: Manifest[N])
        extends DefaultProtocol {

  implicit val singleTargetFormat: Format[SingleTarget] =
    wrap[SingleTarget, Long](SingleTarget.unapply(_).get, SingleTarget)

  implicit val multiTargetsFormat: Format[MultiTargets] =
    wrap[MultiTargets, Array[Long]]({ m => m.targets.toArray }, { arr => MultiTargets(arr: _*) })

  implicit val nameTargetFormat: Format[NamedTarget[N]] =
    wrap[NamedTarget[N], N](NamedTarget.unapply[N](_).get, NamedTarget[N])

  implicit val targetFormat: Format[Target] = asUnion[Target](
    singleTargetFormat, multiTargetsFormat, nameTargetFormat)
}

final case class Postcard[P](sender: Long, target: Target, payload: P)

class PostcardProtocol[N, P](implicit
                             override val nameFormat: Format[N],
                             val payloadFormat: Format[P],
                             override val nameManifest: Manifest[N]) extends TargetProtocol[N]
{
  
  implicit val postcardFormat: Format[Postcard[P]] =
    asProduct3[Postcard[P], Long, Target, P](Postcard.apply[P]_)(Postcard.unapply[P](_).get)
}



class ActorMessageProtocol[P](
        implicit val payloadFormat: Format[P], val payloadManifest: Manifest[P])
        extends DefaultProtocol {

  sealed abstract class ActorMessage
  // Plain Message
  sealed case class Async(val payload: P) extends ActorMessage
  // Actor exit
  final case class Exit(override val payload: P) extends Async(payload)
  // Actor link
  final case class Link(override val payload: P) extends Async(payload)
  // Actor unlink
  final case class Unlink(override val payload: P) extends Async(payload)

  // Message via channel object (implicitly creates a channel)
  sealed case class Chan(val channel: Int, override val payload: P) extends Async(payload)
  // Request + implicit channel open
  final case class Request(override val channel: Int, override val payload: P)
    extends Chan(channel, payload)
  // Reply + Implicit channel close
  final case class Reply(override val channel: Int, override val payload: P)
    extends Chan(channel, payload)
  // Explicitly close channel
  final case class Close(val channel: Int) extends ActorMessage
  // Ping-pong-style name lookup
  // Client sends Postcard with resolve as payload and NamedTarget
  // Server replies with same payload and resolved sender (Long id)
  final case class Resolve(val id: Long) extends ActorMessage
  
  implicit val asyncFormat: Format[Async] = wrap[Async, P](Async.unapply(_).get, Async)
  implicit val closeFormat: Format[Close] = wrap[Close, Int](Close.unapply(_).get, Close)
  implicit val resolveFormat: Format[Resolve] = wrap[Resolve, Long](Resolve.unapply(_).get, Resolve)
  implicit val exitFormat: Format[Exit] = wrap[Exit, P](Exit.unapply(_).get, Exit)
  implicit val linkFormat: Format[Link] = wrap[Link, P](Link.unapply(_).get, Link)
  implicit val unlinkFormat: Format[Unlink] = wrap[Unlink, P](Unlink.unapply(_).get, Unlink)

  implicit val chanFormat: Format[Chan] = asProduct2[Chan, Int, P](Chan)(Chan.unapply(_).get)

  implicit val requestFormat: Format[Request] =
    asProduct2[Request, Int, P](Request)(Request.unapply(_).get)

  implicit val replyFormat: Format[Reply] =  asProduct2[Reply, Int, P](Reply)(Reply.unapply(_).get)

  implicit val actorMessageFormat: Format[ActorMessage] = asUnion[ActorMessage](
    asyncFormat, requestFormat, replyFormat, chanFormat,
    resolveFormat, linkFormat, exitFormat, unlinkFormat, closeFormat)
}

class ActorPostcardProtocol[N, P](implicit
                                  val nameFormat: Format[N],
                                  val payloadFormat: Format[P],
                                  val nameManifest: Manifest[N],
                                  val payloadManifest: Manifest[P])
extends DefaultProtocol  {

  object actorMessageProtocol extends ActorMessageProtocol[P]

  object postcardProtocol
          extends PostcardProtocol[N, actorMessageProtocol.ActorMessage]()(
            nameFormat, actorMessageProtocol.actorMessageFormat, nameManifest)

  implicit val actorPostcardFormat = postcardProtocol.postcardFormat
}


// sed