package language.qfactors;

/**
 * {@link IStateVarInt} is an interface to an integer value of {@link StateVar}.
 * 
 * @author rsukkerd
 *
 */
public interface IStateVarInt extends IStateVarValue, Comparable<IStateVarInt> {

	public int getValue();
}