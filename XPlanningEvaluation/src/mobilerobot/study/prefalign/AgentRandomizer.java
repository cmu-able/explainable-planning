package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.parser.JSONSimpleParserUtils;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.Randomizer;

public class AgentRandomizer {

	private static final Pattern AGENT_POLICY_PATTERN = Pattern.compile("agentPolicy([0-9]+)");

	private File mQuestionDir;
	private double mAlignProb;
	private double mUnalignThreshold;
	private Randomizer<String> mRandomizer;

	public AgentRandomizer(File questionDir, double alignProb, double unalignThreshold)
			throws IOException, ParseException {
		mQuestionDir = questionDir;
		mAlignProb = alignProb;
		mUnalignThreshold = unalignThreshold;
		initializeRandomizer();
	}

	private void initializeRandomizer() throws IOException, ParseException {
		List<String> alignedAgents = new ArrayList<>();
		List<String> unalignedAgents = new ArrayList<>();
		JSONObject scoreCardJsonObj = QuestionUtils.getScoreCardJSONObject(mQuestionDir);
		for (Object key : scoreCardJsonObj.keySet()) {
			Object value = scoreCardJsonObj.get(key);
			String agentPolicy = (String) key;
			double score = JSONSimpleParserUtils.parseDouble(value);
			if (score == 1) {
				alignedAgents.add(agentPolicy);
			} else if (score < mUnalignThreshold) {
				unalignedAgents.add(agentPolicy);
			}
		}

		String[] allAgents = Stream.concat(alignedAgents.stream(), unalignedAgents.stream()).toArray(String[]::new);
		double[] allAgentsProbs = new double[allAgents.length];
		Arrays.fill(allAgentsProbs, 0, alignedAgents.size(), mAlignProb / alignedAgents.size());
		Arrays.fill(allAgentsProbs, alignedAgents.size(), allAgents.length, (1 - mAlignProb) / unalignedAgents.size());

		mRandomizer = new Randomizer<>(allAgents, allAgentsProbs);
	}

	public int randomAgentIndex() {
		String agentPolicy = mRandomizer.randomSample();
		Matcher m = AGENT_POLICY_PATTERN.matcher(agentPolicy);
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		}

		throw new IllegalStateException("Cannot parse agent index from " + agentPolicy);
	}
}
