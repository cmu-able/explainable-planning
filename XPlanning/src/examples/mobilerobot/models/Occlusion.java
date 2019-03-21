package examples.mobilerobot.models;

import examples.mobilerobot.dsm.IEdgeAttribute;
import language.domain.models.IActionAttribute;

/**
 * {@link Occlusion} is a derived attribute associated with a {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public enum Occlusion implements IActionAttribute, IEdgeAttribute {
	OCCLUDED, PARTIALLY_OCCLUDED, CLEAR
}
