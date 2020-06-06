package jspectrumanalyzer.core;
/*
 * Simple Moving Average
 */
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class SMA
{
	public class MovingAverage {

	    private final Queue<float[]> window = new LinkedList<float[]>();
	    private final int period;
	    private int datapoints;
	    private float[] sum;
	    private float[] result;

	    public MovingAverage(int period,int datapoints) {
	    	this.datapoints = datapoints;
	    	sum = new float[datapoints];
	        assert period > 0 : "Period must be a positive integer";
	        this.period = period;
		    Arrays.fill(sum, 0);
	    }

	    public void add(float[] num) {
	    	for(int i = 0; i < datapoints; i++) {
		        sum[i] = sum[i]+num[i];
		        window.add(num);
		        if (window.size() > period) {
		            sum[i] = sum[i]-window.remove()[i];
		        }
	    	}
	    }

	    public float[] getAverage() {
	        if (window.isEmpty()) {
	        	result = new float[datapoints];
	        	Arrays.fill(result, 0);
	        	return result;
	        }
	        float divisor = window.size();
	        for(int i = 0; i < datapoints; i++) {
	        	result[i] = sum[i]/divisor;
	        }
	        return result;
	    }
	}
}
