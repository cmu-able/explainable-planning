package mobilerobot.utilities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

public class GraphVizUtils {

	private static final String X_LABEL = "xlabel";
	private static final String LABEL_DELIM = "\n";

	private GraphVizUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static MutableNode lookUpNode(MutableGraph graph, String name) {
		for (MutableNode node : graph.nodes()) {
			String nodeName = node.name().toString();
			if (nodeName.equals(name)) {
				return node;
			}
		}
		return null;
	}

	public static void addUniqueNodeXLabel(MutableNode node, String label) {
		if (node.get(X_LABEL) == null) {
			node.add(X_LABEL, label);
		} else {
			String currXLabel = (String) node.get(X_LABEL);
			String[] currLabels = currXLabel.split(LABEL_DELIM);
			Set<String> uniqueLables = new HashSet<>();
			Collections.addAll(uniqueLables, currLabels);

			if (!uniqueLables.contains(label)) {
				node.add(X_LABEL, currXLabel + LABEL_DELIM + label);
			}
		}
	}

	public static void addUniqueMoveToLink(MutableNode node, Link moveToLink) {
		// Iteratively checking because equals() of Link is not sufficient
		boolean found = false;
		for (Link currLink : node.links()) {
			if (currLink.to().equals(moveToLink.to()) && currLink.attrs().equals(moveToLink.attrs())) {
				found = true;
				break;
			}
		}
		if (!found) {
			node.addLink(moveToLink);
		}
	}

}
