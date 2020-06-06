package jspectrumanalyzer.core;

import java.util.Arrays;

import org.jfree.data.xy.XYSeries;

import jspectrumanalyzer.core.jfc.XYSeriesImmutable;

import java.lang.Math;

public class DatasetSpectrumPeak extends DatasetSpectrum
{
	protected long		lastAddedPeak			= System.currentTimeMillis();
	protected long		lastAddedRef			= System.currentTimeMillis();
	protected long		lastAddedAverage		= System.currentTimeMillis();
	protected long		peakFalloutMillis	= 1000;
	protected long 		averagingSweeps;
	protected float		peakFallThreshold;
	
	protected SMA		smaFilter;
	/**
	 * stores EMA decaying peaks
	 */
	protected float[]	spectrumPeak;
	protected float[]	spectrumRef;
	protected float[]	spectrumAverage;

	/**
	 * stores real peaks and if {@link #spectrumPeak} falls more than preset value below it, start using values from {@link #spectrumPeak}
	 */
	protected float[]	spectrumPeakHold;
	
	
	public DatasetSpectrumPeak(float fftBinSizeHz, int freqStartMHz, int freqStopMHz, float spectrumInitPower, float peakFallThreshold, long peakFalloutMillis,long averagingSweeps)
	{
		super(fftBinSizeHz, freqStartMHz, freqStopMHz, spectrumInitPower);
		this.peakFalloutMillis = peakFalloutMillis;
		this.averagingSweeps = averagingSweeps;
		this.spectrumInitPower = spectrumInitPower;
		this.peakFallThreshold = peakFallThreshold;
		int datapoints = (int) (Math.ceil(freqStopMHz - freqStartMHz) * 1000000d / fftBinSizeHz);
		spectrum = new float[datapoints];
		Arrays.fill(spectrum, spectrumInitPower);
		spectrumPeak = new float[datapoints];
		Arrays.fill(spectrumPeak, spectrumInitPower);
		spectrumPeakHold = new float[datapoints];
		Arrays.fill(spectrumPeakHold, spectrumInitPower);
		
		spectrumRef = new float[datapoints];
		Arrays.fill(spectrumRef, spectrumInitPower);
		
		spectrumAverage = new float[datapoints];
		Arrays.fill(spectrumAverage, spectrumInitPower);
		
		smaFilter = new SMA(averagingSweeps,datapoints);
		

	}

	public void setPeakFalloutMillis(long peakFalloutMillis) {
		this.peakFalloutMillis = peakFalloutMillis;
	}
	
	public void setAveragingSweeps(long averagingSweeps) {
		this.averagingSweeps = averagingSweeps;
	}
	
	public void copyTo(DatasetSpectrumPeak filtered)
	{
		super.copyTo(filtered);
		System.arraycopy(spectrumPeak, 0, filtered.spectrumPeak, 0, spectrumPeak.length);
		System.arraycopy(spectrumPeakHold, 0, filtered.spectrumPeakHold, 0, spectrumPeakHold.length);
	}

	/**
	 * Fills data to {@link XYSeries}, uses x units in MHz
	 * @param series
	 */
	public void fillPeaksToXYSeries(XYSeries series)
	{
		fillToXYSeriesPriv(series, spectrumPeakHold);
//		fillToXYSeriesPriv(series, spectrumPeak);
	}
	

	public XYSeriesImmutable createPeaksDataset(String name) {
		float[] xValues	= new float[spectrum.length];
		float[] yValues	= spectrumPeakHold;
		for (int i = 0; i < spectrum.length; i++)
		{
			float freq = (freqStartHz + fftBinSizeHz * i) / 1000000f;
			xValues[i]	= freq;
		}
		XYSeriesImmutable xySeriesF	= new XYSeriesImmutable(name, xValues, yValues);
		return xySeriesF;
	}

	public double calculateSpectrumPeakPower(){
		double powerSum	= 0;
		for (int i = 0; i < spectrumPeakHold.length; i++) {
			powerSum	+= Math.pow(10, spectrumPeakHold[i]/10); /*convert dB to mW to sum power in linear form*/
		}
		powerSum	= 10*Math.log10(powerSum); /*convert back to dB*/ 
		return powerSum;
	}
	
	private long debugLastPeakRerfreshTime	= 0;
	public void refreshPeakSpectrum()
	{
		if (false) {
			long debugMinPeakRefreshTime	= 100;
			if (System.currentTimeMillis()-debugLastPeakRerfreshTime < debugMinPeakRefreshTime)
				return;
			debugLastPeakRerfreshTime	= System.currentTimeMillis();
		}
		
		long timeDiffFromPrevValueMillis = System.currentTimeMillis() - lastAddedPeak;
		if (timeDiffFromPrevValueMillis < 1)
			timeDiffFromPrevValueMillis = 1;
		
		lastAddedPeak = System.currentTimeMillis();
		
//		peakFallThreshold = 10;
//		peakFalloutMillis	= 30000;
		for (int spectrIndex = 0; spectrIndex < spectrum.length; spectrIndex++)
		{
			float spectrumVal = spectrum[spectrIndex];
			if (spectrumVal > spectrumPeakHold[spectrIndex])
			{
				spectrumPeakHold[spectrIndex] = spectrumPeak[spectrIndex] = spectrumVal;
			}

			spectrumPeak[spectrIndex] = (float) EMA.calculateTimeDependent(spectrumVal, spectrumPeak[spectrIndex], timeDiffFromPrevValueMillis,
					peakFalloutMillis);
			if (spectrumPeakHold[spectrIndex] - spectrumPeak[spectrIndex] > peakFallThreshold)
			{
				spectrumPeakHold[spectrIndex] = spectrumPeak[spectrIndex];
			}
		}
	}
	
	public void refreshRefSpectrum()
	{
		long timeDiffFromPrevValueMillis = System.currentTimeMillis() - lastAddedRef;
		if (timeDiffFromPrevValueMillis < 1)
			timeDiffFromPrevValueMillis = 1;
		
		lastAddedRef = System.currentTimeMillis();
		
		for (int spectrIndex = 0; spectrIndex < spectrum.length; spectrIndex++)
		{
			spectrumRef[spectrIndex] = spectrumAverage[spectrIndex];
		}
	}
	
	public void refreshAverageSpectrum()
	{
		long timeDiffFromPrevValueMillis = System.currentTimeMillis() - lastAddedAverage;
		if (timeDiffFromPrevValueMillis < 1)
			timeDiffFromPrevValueMillis = 1;
		
		lastAddedRef = System.currentTimeMillis();
		
		smaFilter.setPeriod(averagingSweeps);
		smaFilter.add(spectrum.clone());
		spectrumAverage =smaFilter.getAverage();
		for (int spectrIndex = 0; spectrIndex < spectrum.length; spectrIndex++)
		{
			float spectrumVal = spectrum[spectrIndex];
			//spectrumAverage[spectrIndex] = (float) EMA.calculateTimeDependent(spectrumVal, spectrumAverage[spectrIndex], timeDiffFromPrevValueMillis,averagingSweeps*timeDiffFromPrevValueMillis);
			//spectrumAverage[spectrIndex] = (float) EMA.MovingAverage(spectrumVal, spectrumAverage[spectrIndex], timeDiffFromPrevValueMillis,averagingSweeps*timeDiffFromPrevValueMillis));
			spectrum[spectrIndex] = spectrumAverage[spectrIndex];
		}
	}
	
	public void refreshRelativeSpectrum()
	{
		
		for (int spectrIndex = 0; spectrIndex < spectrum.length; spectrIndex++)
		{
			float spectrumVal = spectrum[spectrIndex];
			spectrum[spectrIndex] = spectrum[spectrIndex]-spectrumRef[spectrIndex];
		}
	}

	public void resetPeaks()
	{
		Arrays.fill(spectrumPeak, spectrumInitPower);
		Arrays.fill(spectrumPeakHold, spectrumInitPower);
	}
	
	public void resetRef()
	{
		Arrays.fill(spectrumRef, spectrumInitPower);
	}

	@Override protected Object clone() throws CloneNotSupportedException
	{
		DatasetSpectrumPeak copy = (DatasetSpectrumPeak) super.clone();
		copy.spectrumPeakHold = spectrumPeakHold.clone();
		copy.spectrumPeak = spectrumPeak.clone();
		return super.clone();
	}

}
