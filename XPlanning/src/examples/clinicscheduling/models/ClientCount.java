package examples.clinicscheduling.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarInt;
import language.exceptions.AttributeNameNotFoundException;

public class ClientCount implements IStateVarInt {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mNumClients;

	public ClientCount(int numClients) {
		mNumClients = numClients;
	}

	public int getClientCount() {
		return mNumClients;
	}

	@Override
	public int getValue() {
		return getClientCount();
	}

	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ClientCount)) {
			return false;
		}
		ClientCount clientCount = (ClientCount) obj;
		return clientCount.mNumClients == mNumClients;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Integer.hashCode(mNumClients);
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return Integer.toString(mNumClients);
	}

}
