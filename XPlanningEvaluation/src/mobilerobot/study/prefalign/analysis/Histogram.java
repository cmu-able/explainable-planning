package mobilerobot.study.prefalign.analysis;

import java.util.Arrays;

public class Histogram {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int[] mHistogram;
	private double mBinSize;
	private double mMinValue;

	public Histogram(int numBins, double minValue, double maxValue) {
		mHistogram = new int[numBins];
		Arrays.fill(mHistogram, 0);
		mBinSize = (maxValue - minValue) / numBins;
		mMinValue = minValue;
	}

	public void addData(double value) {
		int binIndex = (int) Math.floor(value / mBinSize);
		if (binIndex < mHistogram.length) {
			mHistogram[binIndex]++;
		} else {
			// value == maxValue
			mHistogram[binIndex - 1]++;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Bin\tCount\n");
		for (int i = 0; i < mHistogram.length; i++) {
			double lowerBound = i * mBinSize + mMinValue;
			double upperBound = lowerBound + mBinSize;
			int count = mHistogram[i];

			String from = String.format("%.3f", lowerBound);
			String to = String.format("%.3f", upperBound);

			builder.append(from + " to " + to);
			builder.append("\t");
			builder.append(count);
			builder.append("\n");
		}
		return builder.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Histogram)) {
			return false;
		}
		Histogram histogram = (Histogram) obj;
		return Arrays.equals(histogram.mHistogram, mHistogram) && histogram.mBinSize == mBinSize
				&& histogram.mMinValue == mMinValue;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Arrays.hashCode(mHistogram);
			result = 31 * result + Double.hashCode(mBinSize);
			result = 31 * result + Double.hashCode(mMinValue);
			hashCode = result;
		}
		return hashCode;
	}
}
