package factors;

/**
 * {@link IStateVarInt} is an interface to an integer value of {@link StateVar}.
 * 
 * @author rsukkerd
 *
 */
public interface IStateVarInt extends IStateVarValue {

	public int getValue();

	public int getLowerBound();

	public int getUpperBound();
}
