/************************

KeyboardPanel.java

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
 

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.ArrayList;
import java.text.DecimalFormat;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;

public class KeyboardPanel extends JPanel implements MouseListener {
    // the width of the whole keyboard
    private static final int keyboardW = 1000;
    // the height of a white key (in pixels)
    private static final int keyH = 100;
    // the height of a black key
    private static final int blackH = 75;
    // the margin around the keyboard
    private static final int margin = 10;
    private static Synthesizer synth;
    private static MidiChannel[] midichannels = null;
    // Do I turn a note on and off with key press/release
    // or do I toggle it?
    private static Boolean sustain = false;
    // lowest midi note on the virtual musical keyboard
    public static final int lownote = 33;
    // highest midi note on the virtual musical keyboard
    public static final int highnote = 88;
    // place to store the start ane end locations of the keyboard graphics,
    // as well as which midi notes correspond to the notes
    private int[] whitekeystartx, whitekeyendx, whitekeynotes, blackkeystartx, blackkeyendx, blackkeynotes;
    private int[] whiteblacknum;
    // the number of white and black keys on the keyboard
    private int nwhitekeys, nblackkeys, keywidth, blackkeywidth;
    // this array store whether each note is being played or not
    private static Boolean[] keyon,seqkeyon;
    // these are the keys on the keyboard that can be used to play notes on the virtual musical keyboard
    public static Character[] upperwhitekeys = {'q','w','e','r','t','y','u','i','o','p'};
    public static Character[] upperblackkeys = {'1','2','3','4','5','6','7','8','9','0','-'};
    public static Character[] lowerwhitekeys = {'z','x','c','v','b','n','m'};
    public static Character[] lowerblackkeys = {'a','s','d','f','g','h','j','k'};
    // these are the lowest note you can play using the upper and lower rows of keys on your computer
    public static final int upperkeyboardstartnote = 60;
    public static final int lowerkeyboardstartnote = 48;
    // arrays to store which note goes with each key
    public static Integer[] upperwhitekeynotes, upperblackkeynotes, lowerwhitekeynotes, lowerblackkeynotes;
    // the following two arrays are loaded with the characters you can press to play notes,
    // along with the corresponding midi notes they go with.
    public static Character[] allkeychars;
    public static Integer[] allkeynotes;
    private static int currentnote;
    // A list of intruments that the synthesizer can play
    private static ArrayList<String> GMPrograms = new ArrayList<String>();
    // The parent frame
    private static TemperamentStudio frame;
    
    // frequency or ratio labels
    private static Boolean showKeyStrings = false;
    private static String[] keyStrings;

    private SineGenerator sgen;
    private Thread sgenThread;
    private static double frequencies[];
    private boolean useSines = false;


    KeyboardPanel(Synthesizer synthesizer, TemperamentStudio mainFrame){
	super();
	frame = mainFrame;
	synth = synthesizer;
	midichannels = synth.getChannels();

	nwhitekeys = 0;
	nblackkeys = 0;
	for(int i = lownote; i < highnote+1; i++){
	    if(iswhitekey(i))
		nwhitekeys += 1;
	    else
		nblackkeys += 1;
	}
	whitekeystartx = new int[nwhitekeys];
	whitekeyendx = new int[nwhitekeys];
	whitekeynotes = new int[nwhitekeys];
	blackkeystartx = new int[nblackkeys];
	blackkeyendx = new int[nblackkeys];
	blackkeynotes = new int[nblackkeys];
	keyon = new Boolean[highnote-lownote+1];
	seqkeyon = new Boolean[highnote-lownote+1];
	
	whiteblacknum = new int[highnote-lownote+1];
	
	upperwhitekeynotes = new Integer[upperwhitekeys.length];
	upperblackkeynotes = new Integer[upperblackkeys.length];
	lowerwhitekeynotes = new Integer[lowerwhitekeys.length];
	lowerblackkeynotes = new Integer[lowerblackkeys.length];
	
	// fill arrays with 999, then overwrite with note
	// if it's not overwritten, 999 tells us it's not
	// a valid note
	Arrays.fill(upperwhitekeynotes,999);
	Arrays.fill(upperblackkeynotes,999);
	Arrays.fill(lowerwhitekeynotes,999);
	Arrays.fill(lowerblackkeynotes,999);
	// go through notes on upper keyboard, fill in
	// values for notes
	int ikey = 0;
	int currentnote = upperkeyboardstartnote;
	while(ikey < upperblackkeys.length){
	    if(iswhitekey(currentnote)){
		if(ikey >= upperwhitekeys.length){
		    break;
		}
		upperwhitekeynotes[ikey] = currentnote;
		ikey += 1;
		currentnote += 1;
	    }
	    else{
		upperblackkeynotes[ikey] = currentnote;
		currentnote += 1;
	    }
	}
	// now go through lower keyboard
	ikey = 0;
	currentnote = lowerkeyboardstartnote;
	while(ikey < lowerblackkeys.length){
	    if(iswhitekey(currentnote)){
		if(ikey >= lowerwhitekeys.length){
		    break;
		}
		lowerwhitekeynotes[ikey] = currentnote;
		ikey += 1;
		currentnote += 1;
	    }
	    else{
		lowerblackkeynotes[ikey] = currentnote;
		currentnote += 1;
	    }
	}
	// list all notes as being off
	for(int i=0; i < highnote-lownote+1; i++){
	    keyon[i] = false;
	    seqkeyon[i] = false;
	}

	// make two big arrays - one with all of the keyboard characters,
	// and one with all of the notes to play for each of those characters
	ArrayList<Character> charlist = new ArrayList<Character>();
	charlist.addAll(Arrays.asList(upperwhitekeys));
	charlist.addAll(Arrays.asList(upperblackkeys));
	charlist.addAll(Arrays.asList(lowerwhitekeys));
	charlist.addAll(Arrays.asList(lowerblackkeys));
	allkeychars = new Character[charlist.size()];
	allkeychars = charlist.toArray(allkeychars);
	
	ArrayList<Integer> intlist = new ArrayList<Integer>();
	intlist.addAll(Arrays.asList(upperwhitekeynotes));
	intlist.addAll(Arrays.asList(upperblackkeynotes));
	intlist.addAll(Arrays.asList(lowerwhitekeynotes));
	intlist.addAll(Arrays.asList(lowerblackkeynotes));
	allkeynotes = new Integer[intlist.size()];
	allkeynotes = intlist.toArray(allkeynotes);

	// Set up the list of instruments the synth can play
	findPrograms();
	
	// add a mouse listener so we can play notes by clicking
	addMouseListener(this);

	keyStrings = new String[highnote-lownote+1];
	for(int i = lownote; i <= highnote; i++){
	    double f = 440*Math.pow(2,((double)(i-69))/12.0);
	    keyStrings[i-lownote] = new DecimalFormat("#.# Hz").format(f);
	}

	// set up sine wave generators
	frequencies = new double[highnote-lownote+1];
    }

    public int getHighNote(){
	return(highnote);
    }

    public int getLowNote(){
	return(lownote);
    }

    public void startSines(){	
	if(!useSines){
	    allSoundOff();	
	    sgen = new SineGenerator();
	    sgenThread = new Thread(sgen);
	    //sgenThread.setPriority(Thread.MAX_PRIORITY);
	    //Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
	    sgenThread.start();
	    useSines = true;
	    for(int note = lownote; note <= highnote; note++){
		if(keyon[note-lownote]){
		    playNote(note);
		}
	    }
	}
    }

    public void endSines(){
	if(useSines){
	    allSoundOff();
	    sgen.kill();
	    while(sgenThread.isAlive());
	    useSines = false;
	    for(int note = lownote; note <= highnote; note++){
		if(keyon[note-lownote])
		    playNote(note);
	    }
	}
    }

    public void harmonicsOn(boolean onyes){
	if(useSines)
	    sgen.harmonicsOn(onyes);
    }

    public void setHarmonic(int i, int val){
	if(useSines)
	    sgen.setHarmonic(i,val);
    }

    
    public void keyStringsOff(){
	showKeyStrings = false;
	repaint();
    }

    public void setFrequencies(int[] pitchshifts, int rootnote, int tuningroot, double tuningfrequency){
	// assumes bend range of 2
	if(pitchshifts.length < 12){
	    return;
	}
	// middle A (A4) is midi note 69
	// midi note 0 is a C, note 60 is middle C
	int noteA = 69;
	// this is the amount the tuning reference note will be off by if we
	// assume we are tuning to a reference which is equal to the root
	// note of our scale
	int dn = tuningroot-rootnote;
	if(dn < 0)
	    dn += 12;
	double tuningoffset = 2.0*(pitchshifts[dn%12]-8192)/8191.0;
	for(int note = lownote; note <= highnote; note++){
	    dn = note-rootnote;
	    if(dn < 0)
		dn += 12;
	    double m = note + 2.0*(pitchshifts[dn%12]-8192)/8191.0-tuningoffset;
	    
	    frequencies[note-lownote] = tuningfrequency*Math.pow(2,(m-(60.0+tuningroot))/12.0);
	    if((useSines) && (keyon[note-lownote]))
		sgen.tuneNote(note,frequencies[note-lownote]);
	}
    }

    public void setKeyStringFrequencies(){
	for(int i=0; i <= highnote-lownote; i++){
	    keyStrings[i] = new DecimalFormat("0.00 Hz").format(frequencies[i]);
	}
	showKeyStrings = true;
	repaint();
    }

    public void setKeyStringsRatios(Double[] ratios, int rootnote, boolean intratios){
	if(ratios.length < 12)
	    return;
	for(int note = lownote; note <= highnote; note++){
	    int i = note-60-rootnote;
	    while(i < 0)
		i += 12;
	    while(i > 11)
		i -= 12;
	    int pow = ((note-60-rootnote)-i)/12;
	    boolean intratio = false;
	    if(intratios){
		for(int ibottom = 1; ibottom < 2000; ibottom++){
		    for(int itop = (int)Math.floor(ratios[i]*ibottom); itop <= (int)Math.ceil(ratios[i]*ibottom); itop++){
			if(i == 0){
			    if(pow >= 0)
				keyStrings[note-lownote] = Integer.toString((int)(Math.pow(2,pow)));
			    else
				keyStrings[note-lownote] = "1 / "+Integer.toString((int)(Math.pow(2,-pow)));
			    intratio = true;
			    break;
			}
			else if(Math.abs(((double)itop)/((double)ibottom) - ratios[i]) < 1.0/1.0e9){
			    if(pow > 0){
				keyStrings[note-lownote] = Integer.toString((int)(Math.pow(2,pow)*itop))+" / "+Integer.toString(ibottom);
			    }
			    else{
				keyStrings[note-lownote] = Integer.toString(itop)+" / "+Integer.toString((int)(Math.pow(2,-pow)*ibottom));
			    }
			    intratio = true;
			    break;
			}
			if(intratio)
			    break;
		    }
		    if(intratio)
			break;
		}
	    }
	    if(!intratio){
		double ratio = ratios[i]*Math.pow(2,pow);
		keyStrings[note-lownote] = new DecimalFormat("0.0000").format(ratio);
	    }
	}
	showKeyStrings = true;
	repaint();
    }

    public void setKeyStringsMIDINote(){
	for(int note = lownote; note <= highnote; note++){
	    keyStrings[note-lownote] = Integer.toString(note);
	}
	showKeyStrings = true;
	repaint();
    }

    public void setKeyStringsNoteName(){
	String[] notenames = {"A","A#","B","C","C#","D","D#","E","F","F#","G","G#"};
	for(int note = lownote; note <= highnote; note++){
	    int fromAo = note-21;
	    keyStrings[note-lownote] = notenames[fromAo%12]+Integer.toString((fromAo+9)/12);
	}
	showKeyStrings = true;
	repaint();
    }
    

    public void sustainOn(Boolean ison){
	sustain = ison;
    }

    public Boolean getSustain(){
	return(sustain);
    }

    // Given a set of pitch shifts from the root tuning note, and the root tuning note,
    // set the pitch shifts for the different channels to realize a tuning scheme.
    public void setPitchShifts(int pitchshifts[], int rootnote){
	for(int nchannel=0; nchannel<12; nchannel++){
	    int fromroot = nchannel - rootnote;
	    if(fromroot < 0){
		fromroot = fromroot + 12;
	    }
	    // We have to skip midi channel 10 (which is really channel
	    // 9 because midi starts at 1 not zero) because it is a
	    // drum channel.
	    int realchannel=nchannel;
	    if(nchannel > 8){
		realchannel = realchannel+1;
	    }
	    	    
	    midichannels[realchannel].setPitchBend(pitchshifts[fromroot]);
	}
    }

    // set which instrument sound to use
    public void setProgram(int theprogram){	
	for(int i=0; i < 16; i++){
	    midichannels[i].programChange(theprogram);
	}
    }

    // get a list of instruments the synthesizer can play
    private void findPrograms(){
	Instrument[] theinstruments = synth.getAvailableInstruments();
	int i=0;
	for(i=0; i < theinstruments.length; i++){
	    // Don't allow non general midi instrument numbers.
	    if(i > 127)
		break;
	    String program = new String(theinstruments[i].toString());
	    if(program.startsWith("Drum")){
		continue;
	    }
	    if(program.startsWith("Instrument: ")){
		program = program.substring(11);
	    }
	    int pos = program.indexOf("bank");
	    if(pos > 0){
		program = program.substring(0,pos-1);
	    }
	    GMPrograms.add(Integer.toString(i+1)+": "+program);
	}
	// If higher instruments not defined, just put in numbers
	// in case you want to save higher instrument values to midi file
	for(; i < 128; i++){
	    GMPrograms.add(Integer.toString(i+1));
	}

    }

    public ArrayList<String> getPrograms(){
	return(new ArrayList<String>(GMPrograms));
    }

    private Boolean isNoteOn(int note){
	Boolean i = keyon[note-lownote];
	return(i);
    }
    
    public void upHalfStep(){
	upHalfSteps(1);
    }
    
    public void upHalfSteps(int n){	   
	for(int i = highnote; i >= lownote+n ; i--){
	    if(isNoteOn(i))
		stopNote(i);
	    if(isNoteOn(i-n)){
		playNote(i);
	    }
	}
	for(int i = lownote+n-1; i >= lownote; i--){
	    stopNote(i);
	}
    }
    
    public void downHalfStep(){
	downHalfSteps(1);
    }
    
    public void downHalfSteps(int n){
	for(int i = lownote; i <= highnote-n; i++){
	    if(isNoteOn(i))
		stopNote(i);
	    if(isNoteOn(i+n)){
		playNote(i);
	    }
	}
	for(int i = highnote-n+1; i <= highnote; i++){
	    stopNote(i);
	}
    }

    public void replayAllNotes(){
	for(int i=0; i < highnote-lownote; i++){
	    if(keyon[i]){
		stopNote(i+lownote);
		playNote(i+lownote);
	    }
	}
    }
    
    public void allSoundOff(){
	for(int i = 0; i < 16; i++){
	    midichannels[i].allNotesOff();
	}
	if(useSines)
	    sgen.allOff();
    }

    public void allOff(){
	allSoundOff();
	for(int i=0; i < highnote-lownote+1; i++){
	    keyon[i] = false;
	}
	repaint();
    }

    public void seqAllOff(){
	for(int i=0; i < highnote-lownote+1; i++){
	    seqkeyon[i] = false;
	}
	//drawKeyboard(this.getGraphics());
	repaint();
    }

    public void invert(int invDir){
	int lown = -1;
	int highn = -1;
	for(int i=0; i < highnote-lownote+1; i++){
	    if(keyon[i] == true){
		lown = i;
		break;
	    }
	}
	if(lown >= 0){
	    for(int i=highnote-lownote; i>lown; i--){
		if(keyon[i] == true){
		    highn = i;
		    break;
		}
	    }
	}
	if(highn >= 0){
	    if(invDir > 0){
		stopNote(lownote+lown);
		while(lown <= highn)
		    lown += 12;
		playNote(lownote+lown);
	    }
	    else{
		stopNote(lownote+highn);
		while(highn >= lown)
		    highn -= 12;
		playNote(lownote+highn);
	    }
	}
    }


    // the location to draw a dot to indicate
    // that a note is being played on the
    // virtual musical keyboard
    private int[] noteDotPos(int note){
	int rectx, recty;
	int i = whiteblacknum[note-lownote];
	if(iswhitekey(note)){
	    rectx = (whitekeystartx[i]+whitekeyendx[i])/2;
	    recty = keyH-17;		
	}
	else{
	    rectx = (blackkeystartx[i]+blackkeyendx[i])/2;
	    recty = blackH-17;
	}
	return(new int[]{rectx,recty});
    }
    
    // the location to draw a dot to indicate
    // that a note is being played by the midi
    // sequencer
    private int[] noteSeqDotPos(int note){
	int rectx, recty;
	int i = whiteblacknum[note-lownote];
	if(iswhitekey(note)){
	    rectx = (whitekeystartx[i]+whitekeyendx[i])/2;
	    recty = keyH-7;		
	}
	else{
	    rectx = (blackkeystartx[i]+blackkeyendx[i])/2;
	    recty = blackH-7;
	}
	return(new int[]{rectx,recty});
    }
    
    // draw a square to indicate that a note is
    // being played on the virtual musical kbd
    private void drawSquare(int note,Graphics g){
	Graphics2D g2d = (Graphics2D)g;
	/*
	if(g2d == null){
	    g2d = (Graphics2D) this.getGraphics();
	}
	*/
	int[] rect = noteDotPos(note);
	g2d.setColor(Color.BLUE);
	g2d.fillRect(margin/2+rect[0]-4,margin/2+rect[1]-4,8,8);
    }
    
    // Clear the square when the note is off
    /*
    private void clearSquare(int note){
	Graphics2D g2d = (Graphics2D) this.getGraphics();
	int[] rect = noteDotPos(note);
	if(iswhitekey(note)){
	    g2d.setColor(Color.WHITE);
	}
	else{
	    g2d.setColor(Color.BLACK);
	}
	g2d.fillRect(margin/2+rect[0]-5,margin/2+rect[1]-5,10,10);
	if(showKeyStrings)
	    drawKeyString(note,g2d);
    }
    */

    // draw a square to indicate that a note is
    // being played by the sequencer
    public void drawSeqSquare(int note,Graphics g){
	Graphics2D g2d = (Graphics2D)g;
	/*
	if(g2d == null){
	    g2d = (Graphics2D) this.getGraphics();
	}
	*/
	int[] rect = noteSeqDotPos(note);
	g2d.setColor(Color.RED);
	g2d.fillRect(margin/2+rect[0]-4,margin/2+rect[1]-4,8,8);
    }
    
    public void clearSeqSquare(int note){
	Graphics2D g2d = (Graphics2D) this.getGraphics();
	int[] rect = noteSeqDotPos(note);
	if(iswhitekey(note)){
	    g2d.setColor(Color.WHITE);
	}
	else{
	    g2d.setColor(Color.BLACK);
	}
	g2d.fillRect(margin/2+rect[0]-5,margin/2+rect[1]-5,10,10);
	if(showKeyStrings)
	    drawKeyString(note,g2d);
    }
    
    
    public void playNote(int note){
	if((note < lownote)||(note > highnote))
	    return;
	int thechannel = note%12;
	if(thechannel > 8){
	    thechannel += 1;
	}
	if(keyon[note-lownote]==true){
	    stopNote(note);
	}
	if(useSines){
	    sgen.addNote(note,frequencies[note-lownote]);
	}
	else
	    midichannels[thechannel].noteOn(note,127);
	keyon[note-lownote]=true;	    
	repaint();
    }
    
    public void stopNote(int note){
	if((note < lownote)||(note > highnote))
	    return;
	int thechannel = note%12;
	if(thechannel > 8){
	    thechannel += 1;
	}
	if(useSines){
	    sgen.removeNote(note);
	}
	else
	    midichannels[thechannel].noteOff(note);
	keyon[note-lownote]=false;
	repaint();
    }
    
    public void setSeqNote(int note, boolean noteon){
	if(noteon){
	    seqkeyon[note-lownote]=true;
	}
	else{
	    seqkeyon[note-lownote]=false;
	}
	repaint();
    }


    public void toggleNote(int note){
	if((note < lownote)||(note > highnote))
	    return;
	if(keyon[note-lownote]){
	    stopNote(note);
	}
	else{
	    playNote(note);
	}	    
    }
    
    public void mousePressed(MouseEvent e){
	requestFocus();
	int x = e.getX()-margin/2;
	int y = e.getY()-margin/2;
	currentnote = 0;
	if(y < blackH){
	    for(int i = 0; i < nblackkeys; i++){
		if((blackkeystartx[i] < x)&&(blackkeyendx[i] >= x)){
		    currentnote = blackkeynotes[i];
		    break;
		}
	    }
	}
	if(currentnote == 0){
	    for(int i = 0; i < nwhitekeys; i++){
		if((whitekeystartx[i] < x)&&(whitekeyendx[i] >= x)){
		    currentnote = whitekeynotes[i];
		}
	    }
	}
	if(sustain){
	    toggleNote(currentnote);
	}
	else{
	    playNote(currentnote);
	}
    }
    
    public void mouseReleased(MouseEvent e){
	if(!sustain)
	    stopNote(currentnote);
    }
    public void mouseClicked(MouseEvent e){
    }
    public void mouseEntered(MouseEvent e){
    }
    public void mouseExited(MouseEvent e){
    }
    
    
    public void paintComponent(Graphics g){
	super.paintComponent(g);
	drawKeyboard(g);
    }
    
    
    public void drawKeyboard(Graphics g){
	Graphics2D g2d = (Graphics2D) g;
	g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
	//int keyboardW = frame.getSize().width-margin;
	int whitewidth, whitestart;
	if(iswhitekey(lownote)&&iswhitekey(highnote)){
	    whitewidth = keyboardW;
	    whitestart = 0;
	}
	else if(iswhitekey(lownote)){
	    whitewidth = (keyboardW*2*nwhitekeys)/(nwhitekeys*2+1);
	    whitestart = 0;
	}
	else if(iswhitekey(highnote)){
	    whitewidth = (keyboardW*2*nwhitekeys)/(nwhitekeys*2+1);
	    whitestart = keyboardW-whitewidth;
	}
	else{
	    whitewidth = (keyboardW*2*nwhitekeys)/(nwhitekeys*2+2);
	    whitestart = (keyboardW-whitewidth)/2;
	}
	keywidth = whitewidth/nwhitekeys;
	blackkeywidth = (int)(keywidth*0.7);
	g2d.setColor(Color.WHITE);
	g2d.fillRect(margin/2+whitestart,margin/2,keywidth*nwhitekeys,keyH);
	g2d.setColor(Color.BLACK);
	// set up position of keys
	int thiswhite = 0;
	int nwhite = 0;
	int nblack = 0;
	for(int i = 0; i < highnote-lownote+1; i++){
	    if(iswhitekey(lownote+i)){
		whitekeystartx[nwhite] = whitestart+thiswhite*keywidth;
		whitekeyendx[nwhite] = whitestart+(thiswhite+1)*keywidth;
		whitekeynotes[nwhite] = lownote+i;
		whiteblacknum[i] = nwhite;
		nwhite += 1;
		thiswhite += 1;
	    }
	    else{
		blackkeystartx[nblack] = whitestart+thiswhite*keywidth-blackkeywidth/2;
		blackkeyendx[nblack] = whitestart+thiswhite*keywidth+blackkeywidth/2;
		blackkeynotes[nblack] = lownote + i;
		whiteblacknum[i] = nblack;
		nblack += 1;
	    }
	}
	// draw black keys first
	for(int i = 0; i < nblackkeys; i++){
	    g2d.fillRect(margin/2+blackkeystartx[i],margin/2,blackkeywidth,blackH);
	    g2d.setColor(Color.WHITE);
	    g2d.drawRect(margin/2+blackkeystartx[i],margin/2,blackkeywidth,blackH);
	    g2d.setColor(Color.BLACK);
	}
	// now white keys
	for(int i = 0; i < nwhitekeys; i++){
	    g2d.drawRect(margin/2+whitekeystartx[i],margin/2,keywidth,keyH);
	}
	
	// place squares indicating which notes are on
	for(int note = lownote; note <= highnote; note++){
	    if(keyon[note-lownote]){
		drawSquare(note,g);
	    }
	}
	// place squares indicating which seq notes are on
	for(int note = lownote; note <= highnote; note++){
	    if(seqkeyon[note-lownote]){
		drawSeqSquare(note,g);
	    }
	}
	
	// draw letters telling you which notes they play
	drawKeyLabels(g2d, false, upperwhitekeys, upperwhitekeynotes, whitekeynotes, whitekeystartx, keywidth, margin);
	drawKeyLabels(g2d, true, upperblackkeys, upperblackkeynotes, blackkeynotes, blackkeystartx, blackkeywidth, margin);
	drawKeyLabels(g2d, false, lowerwhitekeys, lowerwhitekeynotes, whitekeynotes, whitekeystartx, keywidth, margin);
	drawKeyLabels(g2d, true, lowerblackkeys, lowerblackkeynotes, blackkeynotes, blackkeystartx, blackkeywidth, margin);

	// draw frequencies
	if(showKeyStrings){
	    for(int note = lownote; note <= highnote; note++){
		drawKeyString(note, g2d);
	    }
	}
	
    }

    // draw string on key to show frequency, ratio, etc.
    
    private void drawKeyString(int note, Graphics2D g2d){
	Font thefont = new Font("Dialog", Font.ITALIC, 10);
	g2d.setFont(thefont);
	FontMetrics fmetrics = g2d.getFontMetrics();
	AffineTransform orig = g2d.getTransform();
	g2d.rotate(-Math.PI/2.0);
	String thestr = keyStrings[note-lownote];
	Rectangle strBounds = fmetrics.getStringBounds(thestr,g2d).getBounds();
	int i = whiteblacknum[note-lownote];
	if(iswhitekey(note)){
	    g2d.setColor(Color.BLACK);
	    g2d.drawString(thestr,-keyH,whitekeystartx[i]+keywidth/2+fmetrics.getAscent()-1);		    
	}
	else{
	    g2d.setColor(Color.WHITE);
	    g2d.drawString(thestr,-blackH,blackkeystartx[i]+blackkeywidth/2+fmetrics.getAscent()-1);
	}
	g2d.setTransform(orig);
    }

    // draw the labels which show which keys to press to play different notes
    private void drawKeyLabels(Graphics2D g2d, Boolean isBlack, Character[] compkeychars, Integer[] compkeynotes, int[] muskeynotes, int[] muskeyloc, int keywidth, int margin){ 
	Font thefont = new Font("Dialog", Font.PLAIN, 10);
	g2d.setFont(thefont);
	FontMetrics fmetrics = g2d.getFontMetrics();
	int vloc = 20;
	if(isBlack){
	    g2d.setColor(Color.WHITE);
	    vloc = 15;
	}
	else{
	    g2d.setColor(Color.BLACK);
	}
	for(int i = 0; i < compkeychars.length; i++){
	    if(compkeynotes[i] < 999){
		int j;
		for(j = 0; j < muskeynotes.length; j++){
		    if(muskeynotes[j] == compkeynotes[i]){
			break;
		    }
		}
		Rectangle strBounds = fmetrics.getStringBounds(String.valueOf(compkeychars[i]),g2d).getBounds();
		g2d.drawString(String.valueOf(compkeychars[i]),(margin/2)+muskeyloc[j]+(keywidth/2)-(strBounds.width/2),vloc);
	    }
	}
	
    }
    
    public Dimension getPreferredSize(){
	/*
	int wmin = whitekeystartx[0];
	int bmin = blackkeystartx[0];
	int startx = (wmin < bmin)?wmin:bmin;
	*/
	/*
	int wmax = whitekeyendx[nwhitekeys-1];
	int bmax = blackkeyendx[nblackkeys-1];
	int endx = (wmax > bmax)?wmax:bmax;
	return new Dimension(endx+2*margin,keyH+2*margin);
	//frame.getSize().width-margin,keyH+margin);
	*/
	return new Dimension(keyboardW,keyH+2*margin);
    }
    public Dimension getMinimumSize(){
	/*
	int wmax = whitekeyendx[nwhitekeys-1];
	int bmax = blackkeyendx[nblackkeys-1];
	int endx = (wmax > bmax)?wmax:bmax;
	return new Dimension(endx+2*margin,keyH+2*margin);
	//	return new Dimension(frame.getSize().width-margin,keyH+margin);
	*/
	return new Dimension(keyboardW,keyH+2*margin);
    }
    public Dimension getMaximumSize(){
	return new Dimension(keyboardW,keyH+2*margin);
    }

    // determine if a midi note is a white key (vs. a black key)
    private static Boolean iswhitekey(int note){
	int mn = note%12;
	if( (mn == 0) || (mn == 2) || (mn == 4) || (mn == 5) || (mn == 7) || (mn == 9) || (mn == 11))
	    return(true);
	return(false);
    }
    
}


