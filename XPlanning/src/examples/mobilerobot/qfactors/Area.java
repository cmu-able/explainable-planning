package examples.mobilerobot.qfactors;

import examples.mobilerobot.dsm.INodeAttribute;
import language.qfactors.IStateVarAttribute;

/**
 * {@link Area} is an attribute associated with a {@link Location} value.
 * 
 * @author rsukkerd
 *
 */
public enum Area implements IStateVarAttribute, INodeAttribute {
	PUBLIC, SEMI_PRIVATE, PRIVATE
}
