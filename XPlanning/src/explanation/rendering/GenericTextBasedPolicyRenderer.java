package explanation.rendering;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class GenericTextBasedPolicyRenderer implements IPolicyRenderer {

	@Override
	public void renderPolicy(String policyFile) throws IOException {
		renderPolicy(policyFile, "");
	}

	@Override
	public void renderPolicy(String policyFile, String prefix) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(policyFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(prefix + line);
			}
		}		
	}

}
