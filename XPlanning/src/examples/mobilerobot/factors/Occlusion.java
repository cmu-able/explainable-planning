package examples.mobilerobot.factors;

import factors.IActionAttribute;

/**
 * {@link Occlusion} is a derived attribute associated with a {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public enum Occlusion implements IActionAttribute {
	BLOCKED, PARTIALLY_OCCLUDED, CLEAR
}
