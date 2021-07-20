package explanation.rendering;

import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TextBasedExplanationRenderer implements IExplanationRenderer {

	private final IPolicyRenderer mPolicyRenderer;
	private static final Pattern POLICY_PATTERN = Pattern.compile("\\[([^\\[]*)\\]");

	public TextBasedExplanationRenderer(IPolicyRenderer policyRenderer) {
		this.mPolicyRenderer = policyRenderer;
	}

	@Override
	public void renderExplanation(String explanationFile) throws IOException {
		JSONParser jsonParser = new JSONParser();
		System.out.println();
		System.out.println("Explanation: (" + explanationFile + ")");
		System.out.println("============");
		
		try (FileReader fr = new FileReader(explanationFile)) {
			Object xplanation = jsonParser.parse(fr);
			
			JSONObject xplanationJson = (JSONObject )xplanation;
			String explanation = (String) xplanationJson.get("Explanation"); 
			
			// Replace the references to the policies with boilerplate text
			Matcher m = POLICY_PATTERN.matcher(explanation);
			
			String solutionPolicyFile = "";
			int index = 0;
			// Find the solution
			if (m.find(index)) {
				solutionPolicyFile = m.group(1);
				explanation = m.replaceFirst("[Solution] (below)");
				index = m.start(1);
			}
			
			JSONArray alternatePoliciesJSON = (JSONArray) xplanationJson.get("Alternative Policies");
			
			for (int i = 0; i < alternatePoliciesJSON.size(); i++) {
				explanation = explanation.replace((String )alternatePoliciesJSON.get(i), "Alternative " + i + " (below)");
			}
			
			// Split the explanation into separate lines, and word wrap each line
			// on the output
			String[] lines = explanation.split("\n");
			for (String line : lines) {
				System.out.println(WordUtils.wrap(line, 80));
			}
			
			// Print out the solution
			System.out.println();
			System.out.println("Solution");
			System.out.println("========");
			
			this.mPolicyRenderer.renderPolicy(solutionPolicyFile, "  ");
			renderPolicyQualities(xplanationJson, "solnPolicy.json");
			
			for (int i = 0; i < alternatePoliciesJSON.size(); i++) {
				System.out.println();
				System.out.println("Alternative " + i);
				System.out.println("=============");
				String altPolicy = (String )alternatePoliciesJSON.get(i);
				String[] policyParts = altPolicy.split("/");
				String policyKey = policyParts[policyParts.length-1];
				this.mPolicyRenderer.renderPolicy(altPolicy, "  ");
				renderPolicyQualities(xplanationJson, policyKey);
			}
			
			
		} catch (ParseException e) {
			throw new IOException("Could not parse " + explanationFile);
		}
	}

	private void renderPolicyQualities(JSONObject xplanationJson, String policyKey) {
		System.out.println("Qualities:");
		JSONObject qualities = (JSONObject) xplanationJson.get(policyKey);
		Set qualityKeys = qualities.keySet();
		String ruler = StringUtils.repeat("+" + StringUtils.repeat("-", 15), 2) + "+";
		System.out.println("  " + ruler);
		System.out.println("  | Quality       | Value         |");
		System.out.println("  " + StringUtils.repeat("+" + StringUtils.repeat("=", 15), 2) + "+");
		for (Object key : qualityKeys) {
			System.out.print("  | ");
			System.out.print(key + StringUtils.repeat(" ", 14 - ((String )key).length()));
			System.out.print("| ");
			Object value = qualities.get(key);
			if (value instanceof String) {
				String valStr = (String )value;
				valStr = String.format("%.2f", Double.parseDouble(valStr));
				System.out.print(valStr);
				System.out.print(StringUtils.repeat(" ", 14 - valStr.length()));
				System.out.println("|");
			}
			else if (value instanceof JSONObject && ((JSONObject )value).containsKey("Value")) {
				String val = (String) ((JSONObject )value).get("Value");
				val = String.format("%.2f", Double.parseDouble(val));
				System.out.print(val);
				System.out.print(StringUtils.repeat(" ", 14 - ((String )val).length()));
				System.out.println("|");
			}
			System.out.println("  " + ruler);

			
		}
	}

}
