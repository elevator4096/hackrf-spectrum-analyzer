package jspectrumanalyzer.core;
/*
 * Simple Moving Average
 */
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;


public class SMA {

    private Queue<float[]> window = new LinkedList<float[]>();
    private long oldPeriod = 0;
    private long period;
    private int datapoints;
    private float[] sum;
    private float[] result;

    public SMA(long period,int datapoints) {
    	this.datapoints = datapoints;
    	sum = new float[datapoints];
        this.period = period;
	    Arrays.fill(sum, 0);
    }
    
    public void setPeriod(long period) {
    	this.period = period;
    	if (period != oldPeriod) {
    		window.clear();
    		oldPeriod = period;
    		Arrays.fill(sum, 0);
    	}
    }

    public void add(float[] num) {
    	this.datapoints = num.length;
    	for(int i = 0; i < datapoints; i++) {
	        sum[i] = sum[i]+num[i];
    	}
    	window.add(num);
    	
        if (window.size() > period) {
        	float[] tempWindow = window.remove();
        	for(int i = 0; i < datapoints; i++) sum[i] = sum[i]-tempWindow[i];
        }
    }

    public float[] getAverage() {
    	result = new float[datapoints];
        if (window.isEmpty()) {
        	Arrays.fill(result, 0);
        	System.out.println("window empty");
        	return result;
        }
        float divisor = window.size();
        for(int i = 0; i < datapoints; i++) {
        	result[i] = sum[i]/divisor;
        }
        return result;
    }
}
