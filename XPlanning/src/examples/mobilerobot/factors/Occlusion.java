package examples.mobilerobot.factors;

import examples.mobilerobot.dsm.IEdgeAttribute;
import language.qfactors.IActionAttribute;

/**
 * {@link Occlusion} is a derived attribute associated with a {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public enum Occlusion implements IActionAttribute, IEdgeAttribute {
	BLOCKED, PARTIALLY_OCCLUDED, CLEAR
}
