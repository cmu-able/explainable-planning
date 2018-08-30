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
import solver.prismconnector.QFunctionEncodingScheme;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.InitialStateParsingException;
import solver.prismconnector.explicitmodel.ExplicitMDPReader;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;

public class GRBConnector {

	private PrismExplicitModelPointer mPrismExplicitModelPtr;
	private ValueEncodingScheme mEncodings;
	private GRBPolicyReader mPolicyReader;

	public GRBConnector(PrismExplicitModelPointer prismExplicitModelPtr, ValueEncodingScheme encodings) {
		mPrismExplicitModelPtr = prismExplicitModelPtr;
		mEncodings = encodings;
		mPolicyReader = new GRBPolicyReader(prismExplicitModelPtr, encodings);
	}

	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction<?, ?>> attrConstraint)
			throws XMDPException, IOException, InitialStateParsingException, GRBException {
		Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints = new HashSet<>();
		attrConstraints.add(attrConstraint);
		return generateOptimalPolicy(objectiveFunction, attrConstraints);
	}

	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints)
			throws IOException, InitialStateParsingException, XMDPException, GRBException {
		QFunctionEncodingScheme qFunctionEncoding = mEncodings.getQFunctionEncodingScheme();
		ExplicitMDPReader explicitMDPReader = new ExplicitMDPReader(mPrismExplicitModelPtr, qFunctionEncoding);
		ExplicitMDP explicitMDP = explicitMDPReader.readExplicitMDP();

		ConstrainedMDPSolver solver = new ConstrainedMDPSolver(explicitMDP, objectiveFunction, attrConstraints,
				qFunctionEncoding);
		double[][] explicitPolicy = solver.solveOptimalPolicy();
		return mPolicyReader.readPolicyFromExplicitPolicy(explicitPolicy, explicitMDP);
	}
}
