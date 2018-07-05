package examples.mobilerobot.factors;

import examples.mobilerobot.dsm.INodeAttribute;
import factors.IStateVarAttribute;

/**
 * {@link Area} is an attribute associated with a {@link Location} value.
 * 
 * @author rsukkerd
 *
 */
public enum Area implements IStateVarAttribute, INodeAttribute {
	PUBLIC, SEMI_PRIVATE, PRIVATE
}
