package mobilerobot.policyviz;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import guru.nidi.graphviz.model.MutableGraph;
import mobilerobot.utilities.FileIOUtils;

public class MapRenderer {

	private GraphVizRenderer mGraphRenderer;

	public MapRenderer() {
		this(GraphVizRenderer.METER_PER_INCH, GraphVizRenderer.SCALING_FACTOR);
	}

	public MapRenderer(double meterPerInch, double scalingFactor) {
		mGraphRenderer = new GraphVizRenderer(meterPerInch, scalingFactor);
	}

	public void render(File mapJsonFile) throws MapTopologyException, IOException, ParseException {
		render(mapJsonFile, FileIOUtils.getOutputDir());
	}

	public void render(File mapJsonFile, File outDir) throws MapTopologyException, IOException, ParseException {
		MapJSONToGraphViz mapToGraph = new MapJSONToGraphViz(mapJsonFile, mGraphRenderer);
		MutableGraph mapGraph = mapToGraph.convertMapJsonToGraph();
		String outputName = FilenameUtils.removeExtension(mapJsonFile.getName());
		GraphVizRenderer.drawGraph(mapGraph, outDir, null, outputName);
	}

	public static void main(String[] args)
			throws MapTopologyException, IOException, ParseException, URISyntaxException {
		File mapsDir;
		if (args.length > 0) {
			String mapsPath = args[0];
			mapsDir = new File(mapsPath);
		} else {
			mapsDir = FileIOUtils.getMapsResourceDir(MapRenderer.class);
		}

		File[] mapJsonFiles = mapsDir.listFiles();
		MapRenderer mapRenderer = new MapRenderer();

		for (File mapJsonFile : mapJsonFiles) {
			mapRenderer.render(mapJsonFile);
		}
	}

}
