package examples.mobilerobot.factors;

import examples.mobilerobot.dsm.IConnectionAttribute;
import factors.IActionAttribute;

/**
 * {@link Occlusion} is a derived attribute associated with a {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public enum Occlusion implements IActionAttribute, IConnectionAttribute {
	BLOCKED, PARTIALLY_OCCLUDED, CLEAR
}
