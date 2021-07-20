package explanation.rendering;

import java.io.IOException;

public interface IPolicyRenderer {
	public void renderPolicy(String policyFile) throws IOException;
	public void renderPolicy(String policyFile, String prefix) throws IOException;

}
