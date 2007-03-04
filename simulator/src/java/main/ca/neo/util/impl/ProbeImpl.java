package ca.neo.util.impl;

import java.util.ArrayList;
import java.util.List;

import ca.neo.model.Probeable;
import ca.neo.model.SimulationException;
import ca.neo.model.Units;
import ca.neo.util.Probe;
import ca.neo.util.TimeSeries;

/**
 * <p>Collects information from <code>Probeable</code> objects.</p> 
 * 
 * @author Bryan Tripp
 */
public class ProbeImpl implements Probe {
	
	private Probeable myTarget;
	private String myStateName;
	private boolean myRecord;
	private float[] myTimes;
	private List<float[]> myValues;
	private Units[] myUnits;
	private float mySamplingPeriod = -1;
	private float myLastSampleTime = -100000;
	
	/**
	 * @see ca.neo.util.Probe#connect(Probeable, String, boolean)
	 */
	public void connect(Probeable target, String stateName, boolean record) throws SimulationException {
		myTarget = target;
		myStateName = stateName;
		myRecord = record;
		
		//get units; also if the state is bad, we want to throw an exception now
		TimeSeries initial = target.getHistory(stateName);  
		
		myUnits = new Units[initial.getDimension()];
		for (int i = 0; i < myUnits.length; i++) {
			myUnits[i] = initial.getUnits()[i];
		}
		
		reset();		
	}
	
	/**
	 * @see ca.neo.util.Probe#reset() 
	 */
	public void reset() {
		myTimes = new float[1000];
		myValues = new ArrayList<float[]>(1000);
	}
	
	/**
	 * @see ca.neo.util.Probe#collect(float)
	 */
	public void collect(float time) {
		if (mySamplingPeriod > 0) { 
			if (time >= myLastSampleTime + mySamplingPeriod) {
				doCollect();
				myLastSampleTime = time;
			}
		} else {
			doCollect();
		}
	}
	
	private void doCollect() {
		if (myTarget == null) {
			throw new IllegalStateException("This Recorder has not been connected to a Probeable");
		}
		
		TimeSeries stepData;
		try {
			stepData = myTarget.getHistory(myStateName);
		} catch (SimulationException e) {
			throw new RuntimeException("Target appears not to have the state " 
					+ myStateName + ", although this problem should have been detected on connect()", e);
		}
		
		float[] times = stepData.getTimes();
		float[][] values = stepData.getValues();
		int len = times.length;		
		
		if (myRecord) {
			if (myValues.size() + len >= myTimes.length) {
				grow();
			}		
			System.arraycopy(times, 0, myTimes, myValues.size(), len); //don't move this to after the values update			
		} else {
			myTimes = times;
			myValues = new ArrayList<float[]>(10);
		}
		
		for (int i = 0; i < len; i++) {
			myValues.add(values[i]);
		}
	}
	
	private void grow() {
		float[] newTimes = new float[myTimes.length + 1000];
		System.arraycopy(myTimes, 0, newTimes, 0, myTimes.length);
		myTimes = newTimes;
	}
	
	/**
	 * @see ca.neo.util.Probe#getData()
	 */
	public TimeSeries getData() {
		float[] times = new float[myValues.size()];
		System.arraycopy(myTimes, 0, times, 0, myValues.size());
		
		float[][] values = myValues.toArray(new float[0][]);
		return new TimeSeriesImpl(times, values, myUnits);
	}

	/**
	 * @see ca.neo.util.Probe#setSamplingRate(float)
	 */
	public void setSamplingRate(float rate) {
		mySamplingPeriod = 1f / rate;
	}
	
}