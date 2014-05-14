/************************

TuningSchemes.java

This class is part of
TemperamentStudio
(c) 2014 Dallin S. Durfee
This code may be modified and redistributed
under the MIT license

This class loads tuning schemes
from a file, and stores all of the
information about the tuning schemes

******************/

//import java.io.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

////// Loads tuning schemes from file, calculates MIDI pitch bends
public class TuningSchemes {
    private ArrayList<String> tuninglist = new ArrayList<String>();
    private ArrayList<Double[]> ratios = new ArrayList<Double[]>();

    // generic constructor sets up equal temperament
    /// used if tuningfile has problems
    public TuningSchemes() {
	tuninglist = new ArrayList<String>();
	tuninglist.add("Error Loading Tunings");
	ratios = new ArrayList<Double[]>();
	Double data[] = new Double[12];
	for(int i=0; i < 12; i++){
	    data[i] = Math.pow(2,((double)i)/12.0);
	}
	ratios.add(data);
    }
    
    public TuningSchemes(InputStream tuningfilestream) throws IOException {
	addFile(tuningfilestream);
    }

    private Double ParseDouble(String thestring){
	Double x;
	int pos = thestring.indexOf("/");
	if( pos > -1){
	    Double numerator = Double.parseDouble(thestring.substring(0,pos));
	    Double denominator = Double.parseDouble(thestring.substring(pos+1));
	    x = numerator/denominator;
	}
	else{
	    x = Double.parseDouble(thestring);
	}
	return(x);
    }

    public void copyScheme(String name, String newName){
	int tuningi = tuninglist.indexOf(name);
	if(tuningi < 0)
	    return;
	String theString = new String(newName);
	Double[] r = new Double[12];
	Double[] oldr = ratios.get(tuningi);
	for(int i=0; i < 12; i++)
	    r[i] = oldr[i];
	
	tuninglist.add(theString);
	ratios.add(r);
	
    }

    public void changeScheme(String name, Double[] theRatios){
	if(theRatios.length < 12)
	    return;
	int tuningi = tuninglist.indexOf(name);
	if(tuningi < 0)
	    return;
	Double[] r = ratios.get(tuningi);
	for(int i=0; i < 12; i++)
	    r[i] = theRatios[i];
    }

    public void changeMeantoneScheme(String name, double x){
	int tuningi = tuninglist.indexOf(name);
	if(tuningi < 0)
	    return;
	Double[] data = makeMeantoneRatios(x);
	Double[] r = ratios.get(tuningi);
	for(int i = 0; i < 12; i++)
	    r[i] = data[i];
    }
    
    public Double[] makeMeantoneRatios(double x){
	Double[] data = new Double[12];
	Double ratio = 1.0;
	int n = 0;
	for(int i = 0; i <= 6; i++){
	    data[n] = ratio;
	    n += 7;
	    if(n >= 12)
		n -= 12;
	    ratio = ratio*x;
	    if(ratio > 2.0)
		ratio /= 2.0;
	}
	ratio = 1.0;
	n = 0;
	for(int i = 0; i < 6; i++){
	    data[n] = ratio;
	    n -= 7;
	    if(n < 0)
		n += 12;
	    ratio = ratio/x;
	    if(ratio < 1.0)
		ratio *= 2.0;
	}
	return(data);
    }

    public void addScheme(String name, Double[] theRatios){
	if(theRatios.length < 12)
	    return;
	String theString = new String(name);
	Double[] r = new Double[12];
	for(int i=0; i < 12; i++)
	    r[i] = theRatios[i];
	
	tuninglist.add(theString);
	ratios.add(r);
    }

    public void addFile(InputStream tuningfilestream) throws IOException {
	try{
	    BufferedReader in = new BufferedReader(new InputStreamReader(tuningfilestream));
	    String line, name;
	    while((line=in.readLine()) != null) {
		line = line.trim();
		if(line.length()>0){
		    if(!line.startsWith("#")){
			// found a new tuning scheme . . .
			Double[] data = new Double[12];
			// Is the tuning in cents?
			if(line.toLowerCase().startsWith("(c)")){
			    name = new String(line.substring(3));
			    for(int i = 0; i < 12; i++){
				line=in.readLine();
				data[i] = Math.pow(2,((double)i/12.0)+ParseDouble(line)/1200.0);
			    }
			}
			// Is the tuning a meantone fifth ratio?
			else if(line.toLowerCase().startsWith("(m)")){
			    name = new String(line.substring(3));
			    line=in.readLine();
			    Double x = this.ParseDouble(line);
			    Double[] mtdata = makeMeantoneRatios(x);
			    for(int i = 0; i < 12; i++){
				data[i] = mtdata[i];
			    }
			}
			// otherwise, the tuning is a list of 12 ratios
			else if(line.toLowerCase().startsWith("(r)")){
			    name = new String(line.substring(3));
			    for(int i = 0; i < 12; i++){
				line=in.readLine();
				data[i] = ParseDouble(line);
			    }
			}
			else{
			    continue;
			}
			tuninglist.add(name);
			ratios.add(data);

		    }
		}
	    }
	    in.close();
	} catch (IOException e) {
	    IOException ex = new IOException("Problem reading tunings file");
	    throw ex;
	}
	tuningfilestream.close();
    }

    public ArrayList<String> getTuningList(){
	ArrayList<String> tlist = new ArrayList<String>();
	for(String str : tuninglist){
	    tlist.add(new String(str));
	}
	return(tlist);
    }

    public Double[] getRatios(String tuningscheme) throws IndexOutOfBoundsException {
	int tuningi = tuninglist.indexOf(tuningscheme);
	Double[] pitchratios;
	try{
	    pitchratios = ratios.get(tuningi);    
	} catch (IndexOutOfBoundsException e){
	    pitchratios = new Double[12];
	    for(int i = 0; i < 12; i++){
		pitchratios[i] = Math.pow(2,((double)(i))/12.0);
	    }
	    IndexOutOfBoundsException ex = new IndexOutOfBoundsException("Problem finding tuning data for tuning scheme "+tuningscheme+".  Tuning failed.");
	}
	
	return(pitchratios);
    }
    
    public Double[] getFrequencies(String tuningscheme, int tuningnote, double tuningfreq){
	Double[] pitchratios = getRatios(tuningscheme);
	Double[] freqs = new Double[12];
	freqs[0] = tuningfreq/pitchratios[tuningnote];
	for(int i=1; i < 12; i++){
	    freqs[i] = freqs[0]*pitchratios[i];
	}
	return(freqs);
    }

    // Calculate the midi pitch shifts based on the midi bend range and the
    // tuning scheme.  It also takes three additional parameters - the 
    // tuning root, the frequence of the tuning root, and the
    // intonation root.  This lets you
    // shift all notes by a fixed number of cents to realize a tuning
    // reference, such as A440, etc.
    public int[] getPitchShifts(String tuningscheme, double bendrange, int rootnote, int tuningnote, double tuningfreq) throws Exception, IndexOutOfBoundsException {	    
	
	Double[] pitchratios = getRatios(tuningscheme);
	int[] pitchshift = {0,0,0,0,0,0,0,0,0,0,0,0};
	
	// midi specs use a numerical value from (0 to 16383) with 8192 meaning no bend
	Boolean messagesent = false;	
	for(int nnote=0; nnote < 12; nnote++){
	    // convert frequency ratio from file into a midi pitch shift parameter
	    pitchshift[nnote] = 8192+(int)(8191.0*((12.0*Math.log(pitchratios[nnote])/Math.log(2.0))-nnote)/bendrange);
	    if((pitchshift[nnote] < 0)||(pitchshift[nnote] > 16383)){
		pitchshift[nnote] = 0;
		if(!messagesent){
		    Exception ex = new Exception("A pitch bend is out of range!  Your tuning is too extreme for this method.  Try using a tuning root closer to A440 or a tuning scheme closer to equal temperament.  Outputing garbage until you fix this.");
		    messagesent = true;
		    throw ex;
		}
	    }
	}

	// add offset to tune tuningnote to tuningfreq
	// This is what the frequency of the tuning note is in A440 TET
	double standardfreq = 440.0*Math.pow(2.0,((double)(tuningnote-9))/12.0);
	// This is how many semitones the desired frequency is from the freq above
	double semisoff = 12*Math.log(tuningfreq/standardfreq)/Math.log(2.0);
	// This is what the pitchshift should be for the tuningnote to achieve this
	int tunerootoffset = 8192 + (int)((8191.0*semisoff)/bendrange);
	// This is how many half-steps the tunintnote is above the rootnote
	int tuneaboveroot = tuningnote - rootnote;
	if(tuneaboveroot < 0)
	    tuneaboveroot += 12;
	// This is what we need to add to the tuningnote pitch shift to get it
	// to the right frequency.  So we'll move everything by this much
	int offset = tunerootoffset - pitchshift[tuneaboveroot];
	for(int nnote = 0; nnote < 12; nnote++){
	    pitchshift[nnote] += offset;
	    if((pitchshift[nnote] < 0)||(pitchshift[nnote] > 16383)){
		pitchshift[nnote] = 0;
		//System.out.println("Note: "+Integer.toString(nnote)+"  Pitchshift: "+Integer.toString(pitchshift[nnote])+"   Offset: "+Integer.toString(offset));
		if(!messagesent){
		    Exception ex = new Exception("A pitch bend is out of range!  Your tuning is too extreme for this method.  Try using a tuning root closer to A440 or a tuning scheme closer to equal temperament.  Outputing garbage until you fix this.");
		    messagesent = true;
		    //throw ex;
		}
	    }
	}

	return(pitchshift);
    }
    
    private void displayerror(String thestring){
    }
}
