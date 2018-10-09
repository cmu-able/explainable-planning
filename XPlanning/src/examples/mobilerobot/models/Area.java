package examples.mobilerobot.models;

import examples.mobilerobot.dsm.INodeAttribute;
import language.domain.models.IStateVarAttribute;

/**
 * {@link Area} is an attribute associated with a {@link Location} value.
 * 
 * @author rsukkerd
 *
 */
public enum Area implements IStateVarAttribute, INodeAttribute {
	PUBLIC, SEMI_PRIVATE, PRIVATE
}
