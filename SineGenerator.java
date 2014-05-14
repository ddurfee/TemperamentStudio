/************************

SineGenerator.java

This class is part of
TemperamentStudio
(c) 2014 Dallin S. Durfee
This code may be modified and redistributed
under the MIT license

This class creates a virtual
musical keyboard that can be played
by pressing on keys or clicking
with the mouse.

******************/

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
 

public class SineGenerator implements Runnable{

    private int sampleRate = 44100;
    private final double dt = 1.0/sampleRate;
    private final int bufferSize = 2048;//2200;
    private final int overloadPersistance = sampleRate/3;
    private int overload = 0;
    private int underrun = 0;
    private final int flushval = 200;
    private final int bits = 8;
    private final int sleepTime = ((1000*bufferSize)/sampleRate)/4;
    private SourceDataLine line;
    private volatile ArrayList<Integer> notes;
    private volatile ArrayList<Double> freqs;
    private volatile ArrayList<Double> phases;
    private volatile boolean keepPlaying = false;
    private boolean hasline = false;
    private final int nHarmonics = TemperamentStudio.nHarmonics;
    private boolean addHarmonics = false;
    private volatile double[] harmonicamps;
    // set this to true while changing things to pause the thread
    private volatile int doneChangingParams = 0;
    // set this to true while calculating new buffer - don't change until done
    private volatile int canChangeParams = 0;
    private volatile int wantToChangeParams = 0;
    private int eventNumber = 0;
    private final int nSinePoints = 100;
    private static int[] sinePoints;

    SineGenerator(){
	//double maxSineFreq = 440.0*Math.pow(2,(TemperamentStudio.kbPanel.getHighNote()+2-69)/12.0)*TemperamentStudio.nHarmonics;
	//int sampleRate = (int)(maxSineFreq);
	//System.out.println("Minimum sample rate required = "+sampleRate);


	try{
	    AudioFormat format = new AudioFormat(sampleRate,bits,1,true,false);
	    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
	    line = (SourceDataLine) AudioSystem.getLine(info);
	    line.open(format,bufferSize);
	    hasline = true;
	} catch (Exception e) {
	    hasline = false;
	}


	harmonicamps = new double[nHarmonics];
	notes = new ArrayList<Integer>();
	freqs = new ArrayList<Double>();
	phases = new ArrayList<Double>();

	sinePoints = new int[nSinePoints];
	for(int i = 0; i < nSinePoints; i++){
	    sinePoints[i] = (int)(127*Math.sin(Math.PI*2.0*i/(double)nSinePoints));
	}
    }

    private int fastSine(double phase){
	return(sinePoints[(int)((phase%1.0)*nSinePoints)]);
    }

    public void harmonicsOn(boolean onyes){
	addHarmonics = onyes;
    }

    public void setHarmonic(int i, int val){
	harmonicamps[i] = val;
    }




    public void run(){
	if(!hasline)
	   return;
	line.start();
	keepPlaying = true;
	byte[] buffer = new byte[bufferSize];
	// preload buffer with zeros so that we don't throw a buffer
	// underrun error the first time through
	int bufAvail;
	int paramchanger = 0;
	int y;
	while(keepPlaying){
	    //System.out.print("*");
	    paramchanger = wantToChangeParams;
	    while(paramchanger > 0){
		//System.out.println("EventNumber "+paramchanger);
		canChangeParams = paramchanger;
		//Thread.yield();
		while(!(doneChangingParams==paramchanger)){
		    if(!keepPlaying)
			break;
		    //System.out.print(".");
		}
		doneChangingParams = 0;
		if(wantToChangeParams == 0){
		    try{
			Thread.sleep(1);
		    } catch (Exception e) {}
		}
		paramchanger = wantToChangeParams;
		//System.out.print("-");
	    }

	    for(int i = 0; i < bufferSize; i++){
		y = 0;
		for(int j=0; j < notes.size(); j++){
		    phases.set(j,phases.get(j)+freqs.get(j)*dt);
		    if(phases.get(j)>1.0){
			phases.set(j,phases.get(j)-1.0);
		    }
		    if(addHarmonics){
			for(int h = 0; h < nHarmonics; h++){
			    y += harmonicamps[h]*fastSine(phases.get(j)*(h+1));//126*Math.sin(Math.PI*2*phases.get(j)*(h+1));//
			}
		    }
		    else{
			y += 100*fastSine(phases.get(j));//126*Math.sin(Math.PI*2*phases.get(j));//
		    }
		}
		// this divides out the 100 from the harmonic sliders, plus an exra factor of 8 to make it possible to play several notes with harmonics at once.
		y /= 800;
		if(y>126){
		    y = 126;
		    overload = overloadPersistance;
		    TemperamentStudio.overloadIndicator(true);
		}
		else if(y < -126){
		    y = -126;
		    overload = overloadPersistance;
		    TemperamentStudio.overloadIndicator(true);
		}
		if(overload > 0){
		    overload --;
		    if(overload < 1)
			TemperamentStudio.overloadIndicator(false);
		}
		buffer[i] = (byte)y;
	    }
	    bufAvail = line.available();
	    //System.out.println(bufAvail);
	    if(bufAvail == 0){
		underrun = overloadPersistance;
		TemperamentStudio.underRunIndicator(true);
	    // 	try{
	    // 	    Thread.sleep(sleepTime);
	    // 	} catch (Exception e) {}
	    //System.out.println(bufferSize-line.available());
		//line.flush();
	    }
	    if(underrun > 0){
		underrun -= bufferSize;
		if(underrun < 1){
		    TemperamentStudio.underRunIndicator(false);
		}

	    }

	    line.write(buffer,0,bufferSize);
	}
	return;
    }

    private synchronized void waitToChange(int paramchanger){
	while(doneChangingParams != 0);
	wantToChangeParams = paramchanger;
	while(!(canChangeParams == paramchanger));
    }

    private synchronized void doneChanging(int paramchanger){
	canChangeParams = 0;
	wantToChangeParams = 0;
	doneChangingParams = paramchanger;
    }

    public synchronized int getEvent(){
	eventNumber++;
	return(eventNumber);
    }

    public void kill(){
	keepPlaying = false;
    }


    public synchronized void addNote(int note, double frequency){
	if(notes.indexOf(note) < 0){
	    int eventn = getEvent();
	    waitToChange(eventn);
	    notes.add(note);
	    freqs.add(frequency);
	    phases.add(0.5);
	    doneChanging(eventn);
	}
    }

    public synchronized void removeNote(int note){
	int i = notes.indexOf(note);
	if(i >= 0){
	    int eventn = getEvent();
	    waitToChange(eventn);
	    notes.remove(i);
	    freqs.remove(i);
	    phases.remove(i);
	    doneChanging(eventn);
	}
    }

    public synchronized void allOff(){
	int eventn = getEvent();
	waitToChange(eventn);
	while(notes.size() > 0){
	    notes.remove(0);
	    freqs.remove(0);
	    phases.remove(0);
	}
	doneChanging(eventn);
    }

    public synchronized void tuneNote(int note, double frequency){
	int eventn = getEvent();
	waitToChange(eventn);
	int i = notes.indexOf(note);
	if(i >= 0){
	    freqs.set(i,frequency);
	}
	doneChanging(eventn);
    }

}
