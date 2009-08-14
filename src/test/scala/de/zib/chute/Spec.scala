package be.bolder.chute


import compat.Platform
import org.specs.{ScalaCheck, Specification}

/**
 * Default Specification type used by all test specs
 * 
 * @author Stefan Plantikow <plantikow@zib.de>
 *
 */

class Spec extends Specification with ScalaCheck {
    var actorSendDelay = 2000L


}