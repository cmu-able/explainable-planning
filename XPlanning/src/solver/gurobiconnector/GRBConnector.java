package solver.gurobiconnector;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.metrics.IQFunction;
import language.objectives.AttributeConstraint;
import language.objectives.IAdditiveCostFunction;
import language.policy.Policy;
import solver.common.ExplicitMDP;
import solver.prismconnector.QFunctionEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.explicitmodel.ExplicitMDPReader;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class GRBConnector {

	private QFunctionEncodingScheme mQFunctionEncoding;
	private ExplicitMDPReader mExplicitMDPReader;
	private GRBPolicyReader mPolicyReader;

	public GRBConnector(PrismExplicitModelReader prismExplicitModelReader) {
		mQFunctionEncoding = prismExplicitModelReader.getValueEncodingScheme().getQFunctionEncodingScheme();
		mExplicitMDPReader = new ExplicitMDPReader(prismExplicitModelReader);
		mPolicyReader = new GRBPolicyReader(prismExplicitModelReader);
	}

	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction<?, ?>> attrConstraint)
			throws XMDPException, IOException, ExplicitModelParsingException, GRBException {
		Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints = new HashSet<>();
		attrConstraints.add(attrConstraint);
		return generateOptimalPolicy(objectiveFunction, attrConstraints);
	}

	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints)
			throws IOException, ExplicitModelParsingException, XMDPException, GRBException {
		// Create a new ExplicitMDP for every new objective function, because ConstrainedMDPSolver will fill in the
		// ExplicitMDP with the objective costs
		ExplicitMDP explicitMDP = mExplicitMDPReader.readExplicitMDP();
		ConstrainedMDPSolver solver = new ConstrainedMDPSolver(explicitMDP, objectiveFunction, attrConstraints,
				mQFunctionEncoding);
		double[][] explicitPolicy = solver.solveOptimalPolicy();
		return mPolicyReader.readPolicyFromExplicitPolicy(explicitPolicy, explicitMDP);
	}
}
