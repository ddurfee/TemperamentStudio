/***************************************

Temperament Studio V1.8.0
(c) 2014 Dallin S. Durfee
This code may be modified and redistributed
under the MIT license

for gui mode, run
java TemperamentStudio

for command line help run
java TemperamentStudio -h

Source code written for Java 1.7

This program plays midi files or notes on
a virtual keyboard.  It retunes the notes
played by adjusting midi pitch bends
to emulate different tuning schemes.
Because midi pitch bends are channel-wide,
All of the C notes are moved to channel 0,
all of the C# notes to channel 1, etc.

Before you scroll down  . . .  be warned!
This started out as a simple program to load
a midi file, and then save a re-tuned file.
The scope of this project evolved gradually,
so the code is spaghetti-ish, there are a
whole lot of global variables, and some
magic numbers.  I've cleaned
up a few things, but I don't have the time to
do more.

Also, there are a few issues that I would
like to address or features to add.  I may
do this in a future version of the code,
but it's more likely that I won't ever find
the time.  Those items are listed below.
If you modify the code, I'd love
to hear about it! - Dallin S. Durfee (April 2014)

Things I've thought about changing, but probably won't have time to:
- Remove references back to parent class, use interfaces instead
- Move sequencer functions into a separate class file.
- Allow user to select the midi device.
    This would allow you to use external midi
    synths, etc.  I tried a simple implementation,
    but it was prone to crashing, so I took it out.
- Only list .mid and .midi files in the file open dialog.
- Put a button to record and save midi sequence based
    on the notes and chords the user plays on the
    virtual musical keyboard.
- Add a splash screen
- Find a way to make sine tones work better on older computers
- Generally clean up the code and add more comments
- See if there is a standard way that MIDI tuning is implemented in the JRE
synths, and maybe implement an option to tune directly so that more than one instrument can play at one.
- Let the user enter a factor, and then play two notes that differ by that factor
- Add a button/hotkey to re-play the notes you just shut off with enter - so you could turn notes on and off in a presentation.


/*
  



When changing tuning reference, tuning, tuning root, tunning freq, redo sequence while playing
check that this is working last thing - most important


*/



import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JFormattedTextField;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.InputVerifier;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import javax.sound.midi.Synthesizer;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import java.awt.Component;
import java.awt.Container;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.Desktop;

import java.io.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import java.net.URLDecoder;
//import java.net.URI;

public class TemperamentStudio extends JFrame {

    // name of the program, version
    public static final String programName = "TemperamentStudio";
    public static final String programPrettyName = "Temperament Studio";
    public static final String versionString = "1.8.0";


    // constants you can change
    private static final String tuningfilename = "textfiles/"+programName+".tunings";
    public static final int nHarmonics = 7; // accessed by SineGenerator
    private static final int defaultprogram = 20; // midi 21 (it starts at 1 not zero) - Reed Organ
    private static final String playstring = "Play (alt-x)";
    private static final String stopstring = "Stop and Rewind (alt-x)";
    private static final String pausestring = "Pause (alt-p)";
    private static final String continuestring = "Continue (alt-p)";
    private static final String nameAndVersionString = new String(programPrettyName+" "+versionString);
    private static final String licensestring = new String("Copyright (c) 2014  Dallin S. Durfee \nPermission to modify and redistribute this software is granted under the MIT License.");
    private static final String description = programPrettyName+" allows you to experiment with different tuning methods using the midi synthesis capabilities built into JAVA modern personal computer operating systems.  It was written to accompany the paper \"Hearing the Physics of Musical Scales with MIDI\" by Dallin S. Durfee and John S. Colton.";

    private static final String usage = "Starting this progam by double clicking on the jar file, or by running it from the command line with no arguments (java -jar "+programName+".jar), will open up a graphics mode in which you can play notes and intervals on a musical keyboard, play retuned midi files, or save new midi files with different tuning schemes.  There is also a command line mode for advanced users.  For more information on the command line mode, enter java -jar "+programName+".jar -h on the command line.";

    private static final String gMeantoneName = "generic_meantone";

    // constants which should probably not be changed
    private static final int middleC = 60;
    private static final int metaEndTrack = 0x2F; // 47 decimal
    public static final int metaCuePoint = 0x07; // accessed by RetuneMIDI
    public static final int metaTempoChange = 0x51; 
    public static final int metaTimeSignature = 0x58;
    private static final int progUseAll = -1;
    public static final int progUseFirst = -2; // accessed by RetuneMIDI
    private static final int progUseSelected = -3;
    private static final int[] progChangeCodes = {progUseAll,progUseFirst,progUseSelected};
    private static final String[] progChangeText = {"Apply instrument changes to all MIDI channels","Use first instrument found in file","Use instrument selected above"};
    private static final String[] notes = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
    private static final String[] Afreqs = {"Standard A:440","European A:442","European A:443","Baroque Pitch A:415","Chorton Pitch A:466","Classical Pitch A:432"};
    private static final String[] intervals = {"unison (DO)","min 2nd","maj 2nd (RE)","min 3rd","maj 3rd (MI)","4th (FA)","aug 4th","5th (SOL)","min 6th","maj 6th (LA)","min 7th","maj 7th (TI)","octave (DO)"};
    private static final String[] chords = {"maj","min","aug","dim","7","maj7","min7","6","sus2","sus4","7 sus4","9","maj9","min9","(add9)"};
    private static final int[][] chordnotes = {{0,4,7},{0,3,7},{0,4,8},{0,3,6},{0,4,7,10},{0,4,7,11},{0,3,7,10},{0,4,7,9},{0,2,7},{0,5,7},{0,5,7,10},{0,4,7,10,14},{0,4,7,11,14},{0,3,7,10,14},{0,4,7,14}};



    // Global variables
    private static String jarPath;
    private static double tuningfrequency;
    private static Sequence midiIn = null;
    //private static Sequence theSequence = null;
    public static int chordroot = middleC;    // accessed by MMTKeyEventDispatcher
    public static Sequencer theSequencer;
    private static Boolean useGUI = false;
    // listen to keystrokes - set to false when keyboard used for something
    // else (i.e. we don't want notes played while you type a file name).
    public static Boolean catchKeyboard = false;  // accessed by MMTKeyEventDispatcher
    // the text for this strings is loaded from "textfiles/License.txt"
    private static String licenseText;
    // used to keep track of last file loaded or saved, so that file chooser opens in same directory
    private static String lastfile = new String("");  
    private static Boolean editingTunings = false;


    // Global objects
    public static TemperamentStudio frame = new TemperamentStudio(nameAndVersionString); // accessed by MMTKeyEventDispatcher
    public static JCheckBox advancedcheckbox;
    public static JPanel jBachChordPanel; // accessed by MMTKeyEventDispatcher
    public static CloakablePanel jHarmonicsPanel;
    public static CloakablePanel cOverloadPanel;
    private static JComboBox<String> sineCombo;
    private static CloakablePanel sineComboPanel;
    public static JButton jHelpButton; // accessed by MMTKeyEventDispatcher
    public static Font dropdownFont;
    public static JButton jAllOff,jMoveDown,jMoveUp,jOctaveDown,jOctaveUp; // accessed by MMTKeyEventDispatcher
    private static ImageIcon theIcon;
    private static JTextField jInfile = new JTextField(30); // contains name of selected midi file
    // tuning schemes, root of tuning schemes, midi instrument
    public static JComboBox<String> jTunings, jNotes, jProgram; // accessed by MMTKeyEventDispatcher
    //  root for inserted chords and intervals, which midi instrument to use for files
    private static JComboBox<String> jChordRoot, jChooseFileProgram;
    private static JComboBox<Integer> jBendRange, jTransposeAmount;
    private static JCheckBox jBendRangeCommand = new JCheckBox("Write Bend Range Command to MIDI Files");
    private static JButton[] jChordButtons;
    public static JButton jPlay = new JButton(playstring); // accessed by MMTKeyEventDispatcher
    public static JButton jSustain = new JButton("sustain on (.)"); // accessed by MMTKeyEventDispatcher
    private static JTuningAdvancedPanel jTuningAdvancedPanel;
    private static Border advancedSettingsBorder, sectionBorder;
    private static JProgressBar jMidiProgress;
    private static JTuningSchemePanel jTuningSchemePanel;
    public static PauseButton jPause; // accessed by MMTKeyEventDispatcher
    private static FocusListener blockKeysFocusListener; 
    public static Timer progressTimer; // accessed by PauseButton
    public static JButton jGetCuePoint, jGoCuePoint; // accessed by MMTKeyEventDispatcher
    public static KeyboardPanel kbPanel = null; // accessed by PauseButton, MMTKeyEventDispatcher
    // object that loads tuning schemes
    public static TuningSchemes tuningSchemes; // accessed by RetuneMIDI
    private static JSlider jTempo;
    private static JMeantoneSlider jMeantoneSlider;
    private static JSlider[] jHarmonicsSliders;
    private static JPanel jMeantoneSliderPanel;
    private static CloakablePanel jEditIntonationsPanel;
    private static JLabel jMeantoneSliderX;
    private static JLabel jOverload, jUnderRun;
    public static JComboBox<String> jKeyStrings;
    public static CloakablePanel jKeystringsPanel;
    public static JLabel keyLockNotice;

    // constructor for the main frame - used to set up keyeventdispatcher to catch keystrokes over
    // entire frame
    private TemperamentStudio(String thestring){
	super(thestring);
    }


    
    public static void main(String[] args) throws MidiUnavailableException, InvalidMidiDataException, IOException {
	// get jar path - used when reading files from the jar
	String path = TemperamentStudio.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	jarPath = URLDecoder.decode(path, "UTF-8");
	int posSlash = jarPath.lastIndexOf("/");
	jarPath = jarPath.substring(0,posSlash+1);

	// print software name, version, licensing info
	System.out.println(nameAndVersionString);
	System.out.println(licensestring);
	System.out.println();


	if(args.length == 0){
	    useGUI = true;
	}
	else{
	    useGUI = false;
	}

	// Check version file to see if we need to extract files from jar
	String version = readVersionFile();
	Boolean writefiles = false;
	if(version.equals("Error")){
	    displayerror("I can't seem to read files.  This will limit the functionality of this program.  You might try copying the file "+programName+".jar to an empty folder and running it there.");
	}
	else if(version.equals("0")){
	    writefiles = true;
	}
	else if(!version.equals(versionString)){
	    if(getYesNo("It appears that you have previously run a different version of this program.  So I should probably re-extract files that were extracted the first time you ran this program.  Would you like me to do that? (You should probably say yes.)")){
		writefiles = true;
	    }
	}

	if(writefiles){
	    if(!copyFileFromJar(tuningfilename,false)){
		if(getYesNo("I tried to write a fresh copy of the file that contains all of the tuning schemes, but there appears to already be a tuning file present.  Should I overwrite it? (Answer yes unless you have added tuning methods of your own and don't want those changes overwriten.)")){		    
		    if(!overwriteCopyFileFromJar(tuningfilename,false)){
			writefiles=false;
			displayerror("I can't seem to write files.  This will limit the functionality of this program.");
		    }
		}
	    }
	}

	if(writefiles){
	    copyFileFromJar("textfiles/License.txt",false);
	    copyFileListFromJar("midilist.txt","midis");
	    copyFileListFromJar("helplist.txt","help");
	    writeVersionFile();	
	}

	// load the license from the jar
	licenseText = readStringFromJar("textfiles/License.txt");

	boolean tuningsloaded = loadTuningSchemes();

	// set default directory to load or save files to the working dir
	lastfile = jarPath+"midis/";
	// Make sure that a valid number of arguments were given.  If asking for help, give it.
	// If there are no arguments, start up gui mode.  Otherwise do command line mode
	if(useGUI){
	    //useGUI = true;
	    // the gui must be built before setupmidi runs
	    // because setupmidi populates the programs (instruments) list
	    // This could be done elsewhere, but in case I ever add a feature
	    // to allow the user to select the midi device, a call to setupmidi
	    // would be able to change the programs list without a call to
	    // another function.
	    // But, because of this, createGUI doesn't display the GUI.
	    // Otherwise it is possible to try to play notes before the
	    // MIDI channels are set up.  So the frame isn't made visible
	    // until setupmidi is run.
	    KeyboardFocusManager kbfmanager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
	    kbfmanager.addKeyEventDispatcher(new MMTKeyEventDispatcher());

	    createGUI();
	    setupmidi();
	    try{
		kbPanel.setFrequencies(tuningSchemes.getPitchShifts((String)jTunings.getSelectedItem(),2,jNotes.getSelectedIndex(),jTuningAdvancedPanel.getRootNote(), jTuningAdvancedPanel.getFrequency()),jNotes.getSelectedIndex(),jTuningAdvancedPanel.getRootNote(),jTuningAdvancedPanel.getFrequency());
	    } catch (Exception e) {}

	    frame.pack();
	    // hide advanced boxes (they start visible to carve out space)
	    // hide harmonics sliders
	    advancedcheckbox.doClick();
	    advancedcheckbox.doClick();
	    jHarmonicsPanel.cloak();//setVisible(false);
	    cOverloadPanel.cloak();

	    // Put the focus on the help button.
	    // This is where I want it, because one of the first things
	    // the user will want to do is read the help.
	    // Also, if I don't put it somewhere, sometimes it ends up
	    // on a combo box, which disables the hot keys for the 
	    // virtual keyboard (the hot keys are disabled so that they
	    // don't interfere with keyboard input to control the combo
	    // box).
	    // To work properly on all platforms (I hope), this must
	    // be between .pack() and .setVisible(true).
	    jHelpButton.requestFocusInWindow();
	    frame.setVisible(true);
	    // Now that everything is set up, tell the keyboardeventdispatcher
	    // to listen to the keyboard
	    catchKeyboard = true;

	}


	else {
	    // command line mode

	    // if we couldn't load tuning schemes, no point in going on
	    if(!tuningsloaded)
		System.exit(1);
	    
	    // if first argument is -h, they want help
	    if(args[0].equals("-h")){
		String commandlinehelp = readStringFromJar("textfiles/CommandlineUsage.txt");
		System.out.println(description+"\n\n"+commandlinehelp);
		System.exit(0);
	    }
	    // if it's -v, they want version info
	    if(args[0].equals("-v")){
		System.out.println(description+"\n\n"+nameAndVersionString);
		System.exit(0);
	    }
	    // if it's -l, they want the license
	    if(args[0].equals("-l")){
		System.out.println(description+"\n\n"+licenseText);
		System.exit(0);
	    }
	    // otherwise, the first argument is the infile
	    String infile = args[0];

	    // set up defaults
	    String outfile = null;
	    String tuningscheme = "meantone";
	    int tuningroot = 2;
	    int tuningref = 9;
	    double tuningfreq = 440.0;
	    double bendrange = 2.0;
	    Boolean writeBendrange = true;
	    int instrument = progUseAll;
	    int transpose = 0;
	    Boolean overwrite = false;
	    
	    int i = 1;
	    // if the second argument doesn't start with -, it's the output filename
	    if(args.length > 1){
		if(!(args[1].startsWith("-"))){
		    i += 1;
		    outfile = args[1];
		}
	    }

	    // lets go through the rest of the arguments
	    while(i < args.length){
		// if the argument doesn't start with -, there's a problem
		if(!(args[i].startsWith("-"))){
		    printUsageAndExit();
		}
		// Arguments come in pairs - type and then value.
		// So there had better be an argument after this one, or there's a problem
		if(i >= args.length - 1){
		    printUsageAndExit();
		}

		// find out what type of argument we have
		switch(args[i]){
		case "-scheme" :
		    tuningscheme = args[i+1];
		    ArrayList<String> tuninglist = tuningSchemes.getTuningList();
		    if(!tuninglist.contains(tuningscheme)){
			System.out.println("Tuning scheme "+tuningscheme+" doesn't exist.");
			printUsageAndExit();
		    }
		    if(tuningscheme.equals(gMeantoneName)){
			if(args.length < i+3){
			    printUsageAndExit();
			}
			double x = Double.parseDouble(args[i+2]);
			//System.out.println("GMeantone, x = "+x);
			tuningSchemes.changeMeantoneScheme(gMeantoneName,x);
			i+=1; // this argument involves 3 not 2 strings
		    }
		    break;
		case "-root" :
		    tuningroot = Arrays.asList(notes).indexOf(args[i+1].toUpperCase());
		    if(tuningroot < 0)
			printUsageAndExit();
		    break;
		case "-refnote" :
		    tuningref = Arrays.asList(notes).indexOf(args[i+1].toUpperCase());
		    if(tuningref < 0)
			printUsageAndExit();
		    break;
		case "-reffreq" :
		    try{
			tuningfreq = Double.parseDouble(args[i+1]);
		    } catch (NumberFormatException e){
			printUsageAndExit();
		    }
		    break;
		case "-bendrange" :
		    try{
			bendrange = Double.parseDouble(args[i+1]);
		    } catch (NumberFormatException e){
			printUsageAndExit();
		    }
		    break;
		case "-writebendrange" :
		    if(args[i+1].toLowerCase().equals("true"))
			writeBendrange = true;
		    else if(args[i+1].toLowerCase().equals("false"))
			writeBendrange = false;
		    else
			printUsageAndExit();
		    break;
		case "-instrument" :
		    try{
			instrument = Integer.parseInt(args[i+1]);
		    } catch (NumberFormatException e){
			printUsageAndExit();
		    }
		    if((instrument < progUseFirst)||(instrument > 127))
			printUsageAndExit();
		    break;
		case "-transpose" :
		    try{
			transpose = Integer.parseInt(args[i+1]);
		    } catch (NumberFormatException e){
			printUsageAndExit();
		    }
		    break;
		case "-overwrite" :
		    if(args[i+1].toLowerCase().equals("true"))
			overwrite = true;
		    else if(args[i+1].toLowerCase().equals("false"))
			overwrite = false;
		    else
			printUsageAndExit();
		    break;
			
		// if it's not one of these, there is a problem
		default: printUsageAndExit();
		}
		// arguments come in pairs, so we increment by 2
		i += 2;
	    }

	    // if they didn't give a name for the output file, we'll create one
	    if(outfile == null){
		outfile = getSuggestedFilename(infile, tuningscheme,tuningroot,tuningref,tuningfreq);
	    }

	    // write the re-tuned midi file
	    makethefile(infile,outfile,tuningroot,tuningscheme,bendrange,writeBendrange,instrument,tuningref,tuningfreq,transpose,overwrite);
	}  // end of command line mode code	
    }


    public static void setupmidi() {
	// This sets up the sequencer,
	// populates the list of instruments available
	// on the synthesizer, and does the initial
	// tuning of the synthesizer.
	// The synthesizer is set up when we create kbPanel
	// in createGUI().
	try{
	    // For now we'll just use the default
	    // sequencer.  I may change that in future updates
	    theSequencer = MidiSystem.getSequencer(true);
	    try{
		theSequencer.addMetaEventListener(new MetaEventListener(){
			public void meta(MetaMessage msg){
			    if (msg.getType() == metaEndTrack ){ 
				endSequence(true);
			    }
			    if(msg.getType() == metaCuePoint){
				byte[] bmsg = msg.getData();
				String smsg = new String(bmsg);
				if(smsg.startsWith("on:")){
				    int note = Integer.parseInt(smsg.substring(3));
				    kbPanel.setSeqNote(note,true);
				}
				if(smsg.startsWith("off:")){
				    int note = Integer.parseInt(smsg.substring(4));
				    kbPanel.setSeqNote(note,false);
				}
			    }
			    /*
			    // used to debug tempo issues
			    if(msg.getType() == metaTempoChange){
				byte[] thebytes = msg.getData();
				System.out.println("\n");
				RetuneMIDI.printTempoChange(thebytes);
			    }
			    if(msg.getType() == metaTimeSignature){
				byte[] thebytes = msg.getData();
				System.out.println("\n");
				RetuneMIDI.printTimeSignature(thebytes);
			    }
			    */
			    
			}
		    });
	    } catch (Exception ea){
		// meta event listener not working - not fatal, move on
		// but we won't get any dots on the keyboard
	    }
	    
	    theSequencer.open();
	    
	} catch (Exception e) {
	    displayerror("Dang! Couldn't get a sequencer!  "+e.toString());
	}

	// list available instruments on the synthesizer
	ArrayList<String> thePrograms = kbPanel.getPrograms();
	for( String program : thePrograms ){
	    jProgram.addItem(program);
	}
	// set the default instrument to use
	jProgram.setSelectedIndex(defaultprogram);       
	kbPanel.setProgram(defaultprogram);
	// set the pitch bends to tune the synthesizer
	changeLiveTuning();
    }

    // Simple yes / no dialog that works in command line or gui mode
    public static Boolean getYesNo(String thestring){
	Boolean isyes = false;
	if(useGUI){
	    String displayString = "<html><body style=\"width:200px\">"+thestring+"</body></html>";
	    //Object[] options = {"Yes","No"};
	    int answer = JOptionPane.showConfirmDialog(frame,displayString,nameAndVersionString,JOptionPane.YES_NO_OPTION);
	    if(answer == 0)
		isyes = true;
	    else
		isyes = false;
	}
	else{
	    while(true){
		System.out.println(thestring+" (y/n)");
		String input = System.console().readLine();
		if(input.toLowerCase().equals("y")){
		    isyes = true;
		    break;
		}
		if(input.toLowerCase().equals("n")){
		    isyes = false;
		    break;
		}
	    }
	}
	return(isyes);
    }
    
    // The following are a set of methods to display information
    // that work in both command line and gui mode.
    // called by MMTKeyEventListener
    // called by RetuneMIDI

    public static void displaymessage(String thestring){
	displaymessage(thestring,false,theIcon);
    }
    
    public static void displayerror(String thestring){
	displaymessage(thestring,true,theIcon);
    }

    public static void displaymessage(String thestring, Boolean iserror, ImageIcon anicon){
	// a simple function to throw up a message
	if(useGUI) {
	    //JLabel textArea = new JLabel(thestring);
	    String displayString = "<html><body style=\"width:200px\">"+thestring+"</body></html>";
	    int messagetype = JOptionPane.INFORMATION_MESSAGE;
	    if(iserror)
		messagetype = JOptionPane.ERROR_MESSAGE;
	    JOptionPane.showMessageDialog(null, displayString,nameAndVersionString,messagetype,anicon);
	}
	else{
	    System.out.println(thestring);
	}
    }
    

    /////////////////////////////////////////////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    ///////////          Create the GUI     /////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    private static void createGUI() throws  MidiUnavailableException, InvalidMidiDataException, IOException {
	//Set up the GUI.
	dropdownFont = new Font("Dialog", Font.PLAIN, 12);

	// the main frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);



	// add icon for OS tray
	// This icon is also used in various dialogs
	java.net.URL iconURL = TemperamentStudio.class.getResource("icons/MMTIcon.png");
	theIcon = new ImageIcon(iconURL);
	frame.setIconImage(theIcon.getImage());
	
	// load gui help and license files
	/*
	guiusage = readStringFromJar("textfiles/GUIUsage.html");
	thingstotry =readStringFromJar("textfiles/ThingsToTry.html");
	*/
	
	// borders
	advancedSettingsBorder = BorderFactory.createLineBorder(Color.RED);
	sectionBorder = BorderFactory.createEtchedBorder();//BorderFactory.createLineBorder(Color.BLACK);


	// focus listener
	// This focus listener will be used to stop the keyboard dispatcher
	// from acting on keys typed.  It is used on elements that accept 
	// keystrokes so that the keys aren't also sent to other components
	blockKeysFocusListener = new FocusListener(){
		@Override
		public void focusGained(FocusEvent e){
		    catchKeyboard = false;
		    keyLockNotice.setOpaque(true);
		    keyLockNotice.setBackground(Color.red);
		    frame.repaint();
		}
		public void focusLost(FocusEvent e){
		    catchKeyboard = true;
		    keyLockNotice.setOpaque(false);
		    frame.repaint();
		}
	    };
	
	
	// the content pane for the main frame
	final Container contentPane = frame.getContentPane();
	contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));



	///////////////////////////////////////////// make main GUI objects
	

	final JPanel topPanel = new JPanel();
	topPanel.setLayout(new BoxLayout(topPanel,BoxLayout.X_AXIS));

	JHelpPanel jHelpPanel = new JHelpPanel();
	
	// create panel to right of the help panel with tuning scheme, root note,
	// and other stuff
	//final JPanel jTopRightPanel = new JPanel();
	//jTopRightPanel.setLayout(new BoxLayout(jTopRightPanel,BoxLayout.Y_AXIS));

	// A panel to select tuning parameters
	jTuningSchemePanel = new JTuningSchemePanel();

	// A panel for advanced tuning settings
	jTuningAdvancedPanel = new JTuningAdvancedPanel(10,10);
	// This box will hold space for the advanced panel when it is not visible

	// A panel for midi file loading, saving, and playback
	final JPanel jFileSuperPanel = new JPanel(new FlowLayout());
	final JFilePanel jFilePanel = new JFilePanel();

	// A panel to play chords and intervals
	JChordIntervalPanel jChordIntervalPanel = new JChordIntervalPanel();

	// make the keyboard
	// This is the musical keyboard that you can click on
	// to play notes.
	//kbPanel = new KeyboardPanel();
	Synthesizer synth = MidiSystem.getSynthesizer();
	synth.open();
	kbPanel = new KeyboardPanel(synth, frame);




	/////////////////////////////////////////////////// add items to panels

	contentPane.add(topPanel);
	topPanel.add(jHelpPanel);
	topPanel.add(Box.createHorizontalGlue()); // slide jHelpPanel to the far left, other stuff to the right

	//topPanel.add(jTopRightPanel);

	topPanel.add(Box.createRigidArea(new Dimension(10,10)));	


       	topPanel.add(jTuningSchemePanel);

	// this panel has the options to show the file panel and the advanced panels
	final JPanel jOptionsPanel = new JPanel(new GridBagLayout());
	GridBagConstraints cop = new GridBagConstraints();
	

	final JCheckBox showFilePanelCheckBox = new JCheckBox("File Controls");
	
	showFilePanelCheckBox.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e){
		    if(showFilePanelCheckBox.isSelected()){
			jFileSuperPanel.add(jFilePanel);
			//frame.revalidate();
			frame.pack();
			frame.repaint();
		    }
		    else{
			jFileSuperPanel.remove(jFilePanel);
			frame.pack();
			frame.repaint();
		    }
		}
	    });
	cop.gridx=0;
	cop.gridy=0;
	jOptionsPanel.add(showFilePanelCheckBox,cop);

	advancedcheckbox = new JCheckBox("Advanced");
	//jTuningAdvancedPanel.setVisible(false);
	advancedcheckbox.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if(advancedcheckbox.isSelected()){
			jTuningAdvancedPanel.uncloak();//setVisible(true);
			jKeystringsPanel.uncloak();//setVisible(true);
			jFilePanel.addAdvancedPanel();
			//sineComboPanel.uncloak();//setVisible(true);
			jEditIntonationsPanel.uncloak();//.setVisible(true);
			frame.pack();
			frame.repaint();
		    }			
		    else{
			jFilePanel.removeAdvancedPanel();
			jTuningAdvancedPanel.cloak();//setVisible(false);
			jKeystringsPanel.cloak();//setVisible(false);
			//sineComboPanel.cloak();//setVisible(false);
			jEditIntonationsPanel.cloak();//.setVisible(false);
			frame.pack();
			frame.repaint();
		    }
		}
	    });
	cop.gridx = 1;
	cop.gridy = 0;
	jOptionsPanel.add(advancedcheckbox,cop);
	cop.gridx = 0;
	cop.gridy = 1;
	cop.gridwidth = 2;
	jOptionsPanel.add(jTuningAdvancedPanel,cop);
	
	

	topPanel.add(jOptionsPanel);

	//contentPane.add(Box.createRigidArea(new Dimension(10,10)));	

	JPanel jFileRowPanel = new JPanel();
	jFileRowPanel.setLayout(new BoxLayout(jFileRowPanel,BoxLayout.X_AXIS));
	jFileRowPanel.add(jFileSuperPanel);
	jBachChordPanel = new JPanel();
	jFileRowPanel.add(jBachChordPanel);



	contentPane.add(jFileRowPanel);

	// This panel has the musical keyboard as well as sub-panels to
	// add intervals, transpose notes, etc.
	JPanel keyboardSuperPanel = new JPanel();
	keyboardSuperPanel.setLayout(new BoxLayout(keyboardSuperPanel,BoxLayout.Y_AXIS));
	keyboardSuperPanel.setBorder(sectionBorder);
	jChordIntervalPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
	keyboardSuperPanel.add(jChordIntervalPanel);
	kbPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
	keyboardSuperPanel.add(kbPanel);
	JPanel belowKBPanel = new JPanel(new FlowLayout());
	//belowKBPanel.setBorder(sectionBorder);
	keyboardSuperPanel.add(belowKBPanel);
	

	JPanel sustainAndTransposePanel = new JPanel();
	sustainAndTransposePanel.setLayout(new BoxLayout(sustainAndTransposePanel,BoxLayout.Y_AXIS));
	//sustainAndTransposePanel.setBorder(sectionBorder);
	belowKBPanel.add(sustainAndTransposePanel);

	// Panel to change sustain, turn notes off
	JPanel kbSustainPanel = new JPanel(new FlowLayout());
	jKeyStrings = new JComboBox<String>();
	jKeyStrings.setFont(dropdownFont);
	jKeyStrings.addFocusListener(blockKeysFocusListener);
	jKeyStrings.addItem("None");
	jKeyStrings.addItem("Frequencies");
	jKeyStrings.addItem("Decimal Ratios");
	jKeyStrings.addItem("Integer Ratios");
	jKeyStrings.addItem("Note Names");
	jKeyStrings.addItem("MIDI Note Number");
	jKeyStrings.addItemListener(new ItemListener(){
		public void itemStateChanged(ItemEvent e) {
		    updateKeyStrings(jKeyStrings.getSelectedIndex());
		}
	    });
	jKeystringsPanel = new CloakablePanel(10,10);
	jKeystringsPanel.setLayout(new FlowLayout());
	jKeystringsPanel.add(new JLabel("Show:"));
	jKeystringsPanel.setBorder(advancedSettingsBorder);

	jKeystringsPanel.add(jKeyStrings);
	kbSustainPanel.add(jKeystringsPanel);
	//jKeystringsPanel.setVisible(false);

	jAllOff = new JButton("   ----  ALL NOTES OFF ----    (enter)");
	jAllOff.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    kbPanel.allOff();
		}
	    });
	kbSustainPanel.add(jAllOff);
	kbPanel.sustainOn(true);
	jSustain.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if(jSustain.getText() == "sustain on (.)"){
			kbPanel.sustainOn(false);
			jSustain.setText("sustain off (.)");
		    }
		    else{
			kbPanel.sustainOn(true);
			jSustain.setText("sustain on (.)");
		    }
		}
	    });
	kbSustainPanel.add(jSustain);
	sustainAndTransposePanel.add(kbSustainPanel);


	// Panel to transpose notes up and down half steps and octaves
	JPanel kbTransposePanel = new JPanel();
	kbTransposePanel.setLayout(new GridBagLayout());
	GridBagConstraints c = new GridBagConstraints();
	c.fill = GridBagConstraints.HORIZONTAL;

	//	JPanel kbTransposePanelA = new JPanel(new FlowLayout());
	//kbTransposePanel.add(kbTransposePanelA);
	//JPanel kbTransposePanelB = new JPanel(new FlowLayout());
	//kbTransposePanel.add(kbTransposePanelB);

	jOctaveDown = new JButton("octave down (\u2193)");
	jOctaveDown.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    kbPanel.downHalfSteps(12);
		}
	    });
	c.gridx = 0;
	c.gridy = 0;
	c.gridwidth = 2;
	c.gridheight = 1;
	kbTransposePanel.add(jOctaveDown,c);	


	jMoveDown = new JButton("1/2 step down (\u2190)");
	jMoveDown.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    kbPanel.downHalfStep();
		}
	    });
	c.gridx = 2;
	c.gridy = 0;
	c.gridwidth = 2;
	c.gridheight = 1;
	kbTransposePanel.add(jMoveDown,c);	

        jMoveUp = new JButton("1/2 step up (\u2192)");
	jMoveUp.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    kbPanel.upHalfStep();
		}
	    });
	c.gridx = 4;
	c.gridy = 0;
	c.gridwidth = 2;
	c.gridheight = 1;
	kbTransposePanel.add(jMoveUp,c);


	jOctaveUp = new JButton("octave up (\u2191)");
	jOctaveUp.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    kbPanel.upHalfSteps(12);
		}
	    });
	c.gridx = 6;
	c.gridy = 0;
	c.gridwidth = 2;
	c.gridheight = 1;
	kbTransposePanel.add(jOctaveUp,c);
	sustainAndTransposePanel.add(kbTransposePanel);



	c.gridx = 0;
	c.gridy = 1;
	c.gridwidth = 1;
	c.gridheight = 1;
	kbTransposePanel.add(Box.createRigidArea(new Dimension(50,1)),c);
	
	JButton jFifthDown = new JButton("5th down");
	jFifthDown.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    kbPanel.downHalfSteps(7);
		}
	    });
	c.gridx = 1;
	c.gridy = 1;
	c.gridwidth = 1;
	c.gridheight = 1;
	kbTransposePanel.add(jFifthDown,c);	

	JButton jFourthDown = new JButton("4th down");
	jFourthDown.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    kbPanel.downHalfSteps(5);
		}
	    });
	c.gridx = 2;
	c.gridy = 1;
	c.gridwidth = 1;
	c.gridheight = 1;
	kbTransposePanel.add(jFourthDown,c);	


	c.gridx = 3;
	c.gridy = 1;
	c.gridwidth = 1;
	c.gridheight = 1;
	kbTransposePanel.add(Box.createRigidArea(new Dimension(50,1)),c);


	c.gridx = 4;
	c.gridy = 1;
	c.gridwidth = 1;
	c.gridheight = 1;
	kbTransposePanel.add(Box.createRigidArea(new Dimension(50,1)),c);

	JButton jFourthUp = new JButton("4th up");
	jFourthUp.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    kbPanel.upHalfSteps(5);
		}
	    });
	c.gridx = 5;
	c.gridy = 1;
	c.gridwidth = 1;
	c.gridheight = 1;
	kbTransposePanel.add(jFourthUp,c);	

	JButton jFifthUp = new JButton("5th up");
	jFifthUp.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    kbPanel.upHalfSteps(7);
		}
	    });
	c.gridx = 6;
	c.gridy = 1;
	c.gridwidth = 1;
	c.gridheight = 1;
	kbTransposePanel.add(jFifthUp,c);	

	c.gridx = 7;
	c.gridy = 1;
	c.gridwidth = 1;
	c.gridheight = 1;
	kbTransposePanel.add(Box.createRigidArea(new Dimension(50,1)),c);

	//keyboardSuperPanel.add(Box.createRigidArea(new Dimension(20,20)));
	
	jHarmonicsPanel = new CloakablePanel(0,0);
	jHarmonicsPanel.setBorder(sectionBorder);
	jHarmonicsPanel.setLayout(new BoxLayout(jHarmonicsPanel,BoxLayout.Y_AXIS));
	JLabel harmonicsLabel = new JLabel("Harmonics");
	harmonicsLabel.setAlignmentX(CENTER_ALIGNMENT);
	jHarmonicsPanel.add(harmonicsLabel);
	JPanel jHarmonicsSliderPanel = new JPanel();
	jHarmonicsSliderPanel.setLayout(new BoxLayout(jHarmonicsSliderPanel,BoxLayout.X_AXIS));
	jHarmonicsSliderPanel.setOpaque(true);
	jHarmonicsSliderPanel.setBackground(Color.DARK_GRAY);

	jHarmonicsSliders = new JSlider[nHarmonics];
	JLabel harmonicSliderLabels[] = new JLabel[nHarmonics];
	for(int i = 0; i < nHarmonics; i++){
	    harmonicSliderLabels[i] = new JLabel("n="+Integer.toString(i+1));
	    harmonicSliderLabels[i].setForeground(Color.WHITE);
	    jHarmonicsSliderPanel.add(harmonicSliderLabels[i]);
	    jHarmonicsSliders[i] = new JSlider(JSlider.VERTICAL,0,100,0);
	    jHarmonicsSliders[i].setOpaque(true);
	    jHarmonicsSliders[i].setBackground(Color.DARK_GRAY);
	    jHarmonicsSliders[i].addFocusListener(blockKeysFocusListener);
	    jHarmonicsSliderPanel.add(jHarmonicsSliders[i]);
	    jHarmonicsSliders[i].setValue(0);
	}
	jHarmonicsSliders[0].setValue(100);
	// set up action listeners last so that they don't get fired when we set values
	for(int i = 0; i < nHarmonics; i++){
	    jHarmonicsSliders[i].addChangeListener(new ChangeListener() {
		    public void stateChanged(ChangeEvent e){
			JSlider source = (JSlider) e.getSource();
			for(int j=0; j < nHarmonics; j++){
			    if(source == jHarmonicsSliders[j]){
				kbPanel.setHarmonic(j,jHarmonicsSliders[j].getValue());
			    }
			}
		    }
		});

	    
	}

	
	//jHarmonicsPanel.add(Box.createRigidArea(new Dimension(25,25)));
	jHarmonicsPanel.add(jHarmonicsSliderPanel);
	//jHarmonicsPanel.add(Box.createRigidArea(new Dimension(25,25)));
	jHarmonicsPanel.setPreferredSize(new Dimension(jHarmonicsPanel.getPreferredSize().width+10,kbSustainPanel.getPreferredSize().height+kbTransposePanel.getPreferredSize().height+10));
	belowKBPanel.add(jHarmonicsPanel);
	

	
	contentPane.add(keyboardSuperPanel);

	// make panel to display software version and licensing info
	JPanel infoPanel = new JPanel(new FlowLayout());
	contentPane.add(infoPanel);
	JLabel infotextarea = new JLabel(licensestring);
	Font thefont = new Font("Times Roman", Font.ITALIC, 10);
	infotextarea.setFont(thefont);
	infoPanel.add(infotextarea);

	contentPane.add(Box.createRigidArea(new Dimension(2,2)));
	
        //Display the window.

    }

    ////////////////////////////////////////
    ////////////////////////// Various Panels
    //////////////////////////////////////////



    /////////////////////////////////////////////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    ///////////          JHelpPanel         /////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////


    private static class JHelpPanel extends JPanel{
	JHelpPanel(){
	    setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
	    add(new JLabel(theIcon));
	    JPanel buttonPanel = new JPanel();
	    buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.Y_AXIS));
	    jHelpButton = new JButton("Help (F1)");
	    // Build and display help frame when help button pressed
	    jHelpButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			try{
			    String helppath = jarPath+"help/index.html";
			    File helpfile = new File(helppath);
			    Desktop.getDesktop().open(helpfile);
			    //.browse(new URI("http://sqcomic.com/")));
			} catch (Exception ee){
			    displayerror("Couldn't open the local help file in your default browser.  You can open it yourself by going to the directory that this program is in and opening help/index.html, or you can go to http://www.physics.byu.edu/faculty/durfee/Intonation/");
			}
			/*
			JFrame helpFrame = new JFrame(versionstring);
			JPanel jPanel = new JPanel();
			jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
			JLabel topText = new JLabel("<html><body style='width:800px;'><h1>"+versionstring+"</h1><h2>"+description+"</h2>"+licensestring);
			jPanel.add(topText);
			JButton licenseButton = new JButton("View Licence");
			licenseButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
				    JFrame jLicenseFrame = new JFrame(licensestring);
				    JLabel jLicenseText = new JLabel("<html><body style='width:800px;'>"+licenseText.replaceAll("\n","<br /><br />")+"</body></html>");
				    JScrollPane licenseSP = new JScrollPane(jLicenseText,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				    Dimension spSize = licenseSP.getPreferredSize();
				    if(spSize.height > 400)
					spSize.height = 400;
				    // when it adds the scrollbar on the right, it doesn't increase the scrollpane width or reduce the width of the text, and
				    // the text gets cut off.  So I'll add a little buffer space.
				    spSize.width = spSize.width+20;
				    licenseSP.setPreferredSize(spSize);
				    jLicenseFrame.add(licenseSP);
				    jLicenseFrame.pack();
				    jLicenseFrame.setVisible(true);
				}
			    });
			jPanel.add(licenseButton);
			JLabel textArea = new JLabel("<html><body style='width:800px;'>"+guiusage+"</body></html>");
			jPanel.add(textArea);
			JScrollPane scrollpane = new JScrollPane(jPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			// the scrollpane tries to make itself big enough to hold everything, even if it goes below the screen.
			// so we'll adjust the vertical dimension to limit how tall it can be.
			Dimension spSize = scrollpane.getPreferredSize();
			if(spSize.height > 400)
			    spSize.height = 400;
			// when it adds the scrollbar on the right, it doesn't increase the scrollpane width or reduce the width of the text, and
			// the text gets cut off.  So I'll add a little buffer space.
			spSize.width = spSize.width+20;
			scrollpane.setPreferredSize(spSize);
			
			helpFrame.add(scrollpane);
			helpFrame.pack();
			helpFrame.setVisible(true);
			*/
		    }
		});

	    JButton jToTry = new JButton("Things to Try");
	    // build and display frame to give ideas of things to try
	    jToTry.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			try{
			    String helppath = jarPath+"help/thingstotry.html";
			    //System.out.println(helppath);
			    File helpfile = new File(helppath);
			    Desktop.getDesktop().open(helpfile);
			} catch (Exception ee){
			    displayerror("Couldn't open the local help file in your default browser.  You can open it yourself by going to the directory that this program is in and opening help/thingstotry.html, or you can go to http://www.physics.byu.edu/faculty/durfee/Intonation/");
			}
			/*
			JFrame tryFrame = new JFrame(versionstring);
			JPanel tryPanel = new JPanel(new FlowLayout());
			JLabel trytext = new JLabel("<html><body style='width:800px;'><h1>"+versionstring+"</h1>"+thingstotry+"</body></html>");
			tryPanel.add(trytext);
			JScrollPane sp = new JScrollPane(tryPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			Dimension spSize = sp.getPreferredSize();
			if(spSize.height > 400)
			    spSize.height = 400;
			// when it adds the scrollbar on the right, it doesn't increase the scrollpane width or reduce the width of the text, and
			// the text gets cut off.  So I'll add a little buffer space.
			spSize.width = spSize.width+20;
			sp.setPreferredSize(spSize);
			tryFrame.add(sp);
			tryFrame.pack();
			tryFrame.setVisible(true);
			*/
		    }
		});
	    Dimension btdims = jToTry.getPreferredSize();
	    btdims.height = 32;
	    jHelpButton.setPreferredSize(btdims);
	    jHelpButton.setMaximumSize(btdims);
	    jToTry.setPreferredSize(btdims);
	    jToTry.setMaximumSize(btdims);
	    buttonPanel.add(jHelpButton);
	    buttonPanel.add(jToTry);
	    add(buttonPanel);
	}
    }



    /////////////////////////////////////////////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    ///////////      JTuningSchemePanel     /////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////



    // This panel has all of the controls for tuning
    public static class JTuningSchemePanel extends JPanel{
	JTuningSchemePanel(){
	    setLayout(new GridLayout(2,2));
	
	    jNotes = new JComboBox<String>();
	    jNotes.setFont(dropdownFont);
	    jNotes.addFocusListener(blockKeysFocusListener);
	    jTunings = new JComboBox<String>();
	    jTunings.setFont(dropdownFont);
	    jTunings.addFocusListener(blockKeysFocusListener);

	    
	    // Tuning Schemes
	    JPanel jts = new JPanel(new FlowLayout());
	    JLabel tuningLabel = new JLabel("Tuning (alt-s):");
	    jts.add(tuningLabel);
	    //ArrayList<String> tuninglist = new ArrayList<String>();
	    
	    // populate with the list of tuning schemes loaded earlier
	    populatejTunings();
	    jTunings.addItemListener(new ItemListener(){
		    public void itemStateChanged(ItemEvent e) {
			if(jTunings.getSelectedItem().equals(gMeantoneName)){
			    jTuningSchemePanel.remove(jEditIntonationsPanel);
			    jMeantoneSliderPanel.setPreferredSize( jEditIntonationsPanel.getSize());
			    jMeantoneSliderPanel.setMinimumSize( jEditIntonationsPanel.getSize());
			    jTuningSchemePanel.add(jMeantoneSliderPanel,2);
			    //jMeantoneSliderPanel.setVisible(true);
			    //jEditIntonationsPanel.setVisible(false);
			}
			else{
			    //jMeantoneSliderPanel.setVisible(false);
			    jTuningSchemePanel.remove(jMeantoneSliderPanel);
			    jTuningSchemePanel.add(jEditIntonationsPanel,2);
			    if(advancedcheckbox.isSelected()){		
				jEditIntonationsPanel.setVisible(true);
			    }
			}
			changeLiveTuning();
			frame.pack();
			frame.repaint();
		    }
		});
	    jts.add(jTunings);
	    add(jts);
	    

	    //add(Box.createRigidArea(new Dimension(10,1)));


	    
	    // Root Notes used for the tuning scheme
	    JPanel jrn = new JPanel(new FlowLayout());
	    jrn.add(new JLabel("Tuning Root (alt-r):"));
	    for(int i=0; i<notes.length; i++) {
		jNotes.addItem(notes[i]);
	    }
	    jNotes.setSelectedIndex(0);
	    jNotes.addItemListener(new ItemListener(){
		    public void itemStateChanged(ItemEvent e) {
			changeLiveTuning();
		    }
		});
	    jrn.add(jNotes);
	    add(jrn);

	    // slider for generic meantone
	    jMeantoneSliderPanel = new JPanel(new GridBagLayout());

	    jEditIntonationsPanel = new CloakablePanel(10,10);//JPanel();
	    jEditIntonationsPanel.setBorder(advancedSettingsBorder);
	    JButton jEditIntonationsButton = new JButton("Edit Schemes");
	    jEditIntonationsButton.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			editIntonationsFile();
		    }
		});
	    jEditIntonationsPanel.add(jEditIntonationsButton);
	    add(jEditIntonationsPanel);
	    //add(jMeantoneSliderPanel);
	    
	    jMeantoneSliderX = new JLabel();
	    jMeantoneSlider = new JMeantoneSlider();

	    jMeantoneSliderPanel.setBorder(sectionBorder);
	    jMeantoneSliderPanel.add(jMeantoneSlider);
	    jMeantoneSliderPanel.add(jMeantoneSliderX);

	    

	    //jMeantoneSliderPanel.setVisible(false);

	    //	    add(Box.createRigidArea(new Dimension(10,1)));

	    // Programs (instruments)
	    JPanel jinst = new JPanel(new FlowLayout());
	    jinst.add(new JLabel("Inst (alt-i):"));
	    jProgram = new JComboBox<String>();
	    jProgram.setFont(dropdownFont);
	    // instruments are added in setupmidi()
	    jProgram.addFocusListener(blockKeysFocusListener);
	    jProgram.addItemListener(new ItemListener(){
		    public void itemStateChanged(ItemEvent e) {
			changeLiveTuning();
			kbPanel.replayAllNotes();
		    }
		});
	    jinst.add(jProgram);
	    add(jinst);
	}
    }

    // advanced tuning options
    public static class JTuningAdvancedPanel extends CloakablePanel {
	private JPanel jTuningNotePanel;
	private JComboBox<String> tuningNotes;
	private JComboBox<String> tuningFreq;
	private Double doubleFreq; // the frequency of the reference note
	// the maximum and minimum frequencies allowed for the
	// selected tuning note - limited by a +/-2 semitone bend range
	private Double minfreq,maxfreq; 
	


	JTuningAdvancedPanel(int extraXMargin, int extraYMargin) {
	    super(extraXMargin,extraYMargin);
	    setLayout(new FlowLayout());
	    jTuningNotePanel = new JPanel(new FlowLayout());
	    jTuningNotePanel.setBorder(advancedSettingsBorder);
	    jTuningNotePanel.add(new JLabel("Tuning Ref"));
	    // What note is used as the tuning reference
	    tuningNotes = new JComboBox<String>();
	    tuningNotes.setFont(dropdownFont);
	    tuningNotes.addFocusListener(blockKeysFocusListener);
	    for(int i=0; i<notes.length; i++) {
		tuningNotes.addItem(notes[i]);
	    }
	    tuningNotes.setSelectedItem("A");
	    
	    setMinMaxFreqs();

	    // What frequency is the tuning reference note set to?
	    tuningFreq = new JComboBox<String>();
	    tuningFreq.setFont(dropdownFont);
	    tuningFreq.setEditable(true);
	    // apparently JComboBoxes don't do focus listeners or input verifiers how I would expect.
	    // following code is a workaround
	    tuningFreq.getEditor().getEditorComponent().addFocusListener(blockKeysFocusListener);	  
	    tuningfrequency = 440;
	    // Make sure a valid value is entered
	    ((JTextField) tuningFreq.getEditor().getEditorComponent()).setInputVerifier(new InputVerifier(){
		    public boolean verify(JComponent comp) {
			Double tryfreq = parseFrequency(((JTextField)comp).getText());
			if( tryfreq < 0){
			    displayerror("Please enter a positive number for the frequency");
			    ((JTextField)comp).setText(Double.toString(tuningfrequency));
			    return(false);
			}
			if(( tryfreq < minfreq )||( tryfreq > maxfreq ) ){
			    displayerror("For the selected root note, the frequency must be between "+Double.toString(minfreq)+" Hz and "+Double.toString(maxfreq)+" Hz.  If you are setting the note in a different octave than I was expecting, you might try multiplying or dividing by 2 one or more times to see if you get a frequency in this range.");
			    ((JTextField)comp).setText(Double.toString(tuningfrequency));
			    return(false);
			}
			tuningfrequency = tryfreq;
			((JTextField)comp).setText(Double.toString(tuningfrequency));
			return(true);
		    }
		});
	    tuningFreq.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			// When changing the list of items in this combo box,
			// we disable it.  That way, when this actionlistener
			// is fired, we don't try to run updateValues
			// which would crash the system because at that moment
			// there is no selection.
			if(tuningFreq.isEnabled())
			    updateValues();
		    }
		});
	    
	    // list the standard A tunings
	    for(int i=0; i < Afreqs.length;i++){
		tuningFreq.addItem(Afreqs[i]);
	    }
	    tuningFreq.setSelectedItem(Integer.toString(440));

	    tuningNotes.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			String defaultfreq = new DecimalFormat("#.##").format(440.0*Math.pow(2.0,((double)(tuningNotes.getSelectedIndex()-9)/12.0)));
			//Double.toString(440.0*Math.pow(2.0,((double)(tuningNotes.getSelectedIndex()-9))/12.0));
			// If we attempt to access the frequency selectio while there are no items, we get a crash. 
			// So the action listener doesn't call the command to update midi settings if tuningFreq
			// is disabled.  That's why I disable it here.
			tuningFreq.setEnabled(false);
			tuningFreq.removeAllItems();
			// if A is the tuning note, list the standard A tuning schemes
			if(tuningNotes.getSelectedItem()=="A"){
			    for(int i=0; i < Afreqs.length; i++){
				tuningFreq.addItem(Afreqs[i]);
			    }
			    tuningFreq.setSelectedItem("440");
			}
			// if C is the tuning note, list "scientific" tuning, along with
			// standard "A440" tuning, but referenced to a C
			else if(tuningNotes.getSelectedItem() == "C"){
			    tuningFreq.addItem("Standard C:"+defaultfreq);
			    tuningFreq.addItem("\"Scientific\" C:256");
			    tuningFreq.setSelectedItem(defaultfreq);
			}
			// otherwise, list the standard frequency this note would have
			// if we tuned to A440 as a starting point.
			else{
			    tuningFreq.addItem("Standard "+tuningNotes.getSelectedItem()+":"+defaultfreq);
			    tuningFreq.setSelectedItem(defaultfreq);
			}
			tuningFreq.setEnabled(true);
			updateValues();
		    }
		});
	    jTuningNotePanel.add(tuningNotes);

	    jTuningNotePanel.add(tuningFreq);

	    //jTuningNotePanel.add(Box.createRigidArea(new Dimension(10,1)));

	    add(jTuningNotePanel);
	}

	public int getRootNote(){
	    return(tuningNotes.getSelectedIndex());
	}

	public Double getFrequency(){
	    return(doubleFreq);
	}

	private void updateValues(){
	    setMinMaxFreqs();
	    doubleFreq = parseFrequency((String)tuningFreq.getSelectedItem());
	    changeLiveTuning();
	}

	private void setMinMaxFreqs(){
	    // assumes a +/- 2 semitone bend range
	    Double stdfreq = 440 * Math.pow(2.0,((double)(tuningNotes.getSelectedIndex()-9))/12.0);
	    minfreq = stdfreq * Math.pow(2.0,-2.0/12.0);
	    maxfreq = stdfreq * Math.pow(2.0,2.0/12.0);
	}

	// find the frequency from what the user enters
	private Double parseFrequency(String thestring){
	    // parse the frequency string from the frequency combo box
	    // return negative frequency if there is a problem
	    String shortstring;
	    int colonpos = thestring.lastIndexOf(":");
	    if(colonpos >= 0){
		shortstring = thestring.substring(colonpos+1);
	    }
	    else{
		shortstring = thestring;
	    }
	    Double theval;
	    try{
		theval = Double.parseDouble(shortstring);
	    } catch (Exception e){
		theval = -1.0;
	    }
	    return(theval);	   
	} 


    }



    /////////////////////////////////////////////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    ///////////          JFilePanel         /////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    

    // this is the panel where you load, save, and play midi files
    public static class JFilePanel extends JPanel {
	private JPanel fileadvancedSuperPanel;
	private JPanel infilePanel;

	JFilePanel(){
	    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
	    //setBorder(sectionBorder);
	    setBorder(BorderFactory.createLineBorder(Color.BLUE,4));

	     //setOpaque(true);
	     //setBackground(Color.BLUE);
	    
	    // make panel to select input file
	    infilePanel = new JPanel(new FlowLayout());
	    add(infilePanel);
	    infilePanel.add(new JLabel("Input File:"));
	    JButton jInfileButton = new JButton("Select File");
	    infilePanel.add(jInfileButton);
	    jInfileButton.addActionListener(new ActionListener() {
		    // code to run when file button is clicked on
		    public void actionPerformed(ActionEvent e) {		
			catchKeyboard = false;
			JFileChooser fileChooser = new JFileChooser(lastfile);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int rVal = fileChooser.showOpenDialog(null);
			if (rVal == JFileChooser.APPROVE_OPTION) {
			    jInfile.setText(fileChooser.getSelectedFile().toString());
			    lastfile = new String(jInfile.getText());
			    File infile = new File(lastfile);	        
			    try {
				midiIn = MidiSystem.getSequence(infile);
				//midiIn = RetuneMIDI.retunedSequence(midiIn,0,"pythagorean",2,false,0,false,0,tuningfrequency, 0,0);
			    } catch (Exception ea) {
				displayerror("Error opening midi file "+lastfile+" for reading");
				return;
			    } 
			    endSequence(true);
			}
			catchKeyboard = true;
		    }
		});
	    jInfile.addFocusListener(blockKeysFocusListener);
	    infilePanel.add(jInfile);

	    // A slider to allow you to adjust tempo
	    infilePanel.add(Box.createRigidArea(new Dimension(30,1)));
	    infilePanel.add(new JLabel("Tempo: (%)"));
	    jTempo = new JSlider(JSlider.HORIZONTAL,50,150,100);
	    jTempo.setMajorTickSpacing(25);
	    jTempo.setMinorTickSpacing(5);
	    jTempo.setPaintTicks(true);
	    jTempo.setPaintLabels(true);
	    Font tempoFont = new Font("Dialog", Font.PLAIN, 8);
	    jTempo.setFont(tempoFont);
	    jTempo.addChangeListener(new ChangeListener() {
		    public void stateChanged(ChangeEvent e){
			setTempo();
		    }
		});


	    infilePanel.add(jTempo);
	    
	    fileadvancedSuperPanel = new JPanel(new FlowLayout());
	    
	    JPanel fileadvancedPanel = new JPanel();
	    
	    fileadvancedPanel.setLayout(new BoxLayout(fileadvancedPanel,BoxLayout.Y_AXIS));
	    fileadvancedPanel.setBorder(advancedSettingsBorder);
	    fileadvancedSuperPanel.add(fileadvancedPanel);
	    
	    JPanel bendrangeSubPanel = new JPanel(new FlowLayout());
	    
	    fileadvancedPanel.add(bendrangeSubPanel);
	    
	    bendrangeSubPanel.add(new JLabel("Assumed Maximum Bend"));
	    //jBendRange = new JComboBox	    
	    jBendRange = new JComboBox<Integer>();
	    jBendRange.setFont(dropdownFont);
	    for(int i=1; i<12; i++){
		jBendRange.addItem(i);
	    }
	    jBendRange.setSelectedIndex(1);
	    jBendRange.addFocusListener(blockKeysFocusListener);
	    bendrangeSubPanel.add(jBendRange);
	    
	    jBendRangeCommand.setSelected(true);
	    bendrangeSubPanel.add(jBendRangeCommand);
	    
	    
	    JPanel chooseFileProgramPanel = new JPanel(new FlowLayout());
	    chooseFileProgramPanel.add(new JLabel("Instrument for Saved File / Playback"));
	    jChooseFileProgram = new JComboBox<String>();
	    jChooseFileProgram.setFont(dropdownFont);
	    jChooseFileProgram.addFocusListener(blockKeysFocusListener);
	    for(int i = 0; i < progChangeText.length; i++){
		jChooseFileProgram.addItem(progChangeText[i]);
	    }
	    jChooseFileProgram.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			changeLiveTuning();
		    }
		});
	    chooseFileProgramPanel.add(jChooseFileProgram);
	    fileadvancedPanel.add(chooseFileProgramPanel);
	    
	    // You can transpose midi for playback or saving
	    JPanel jTransposePanel = new JPanel(new FlowLayout());
	    jTransposePanel.add(new JLabel("Transpose by (semitones):"));
	    jTransposeAmount = new JComboBox<Integer>();
	    jTransposeAmount.setFont(dropdownFont);
	    for(int i = -12; i < 13; i++){
		jTransposeAmount.addItem(i);
	    }
	    jTransposeAmount.setSelectedItem(0);
	    jTransposeAmount.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			changeLiveTuning();
			kbPanel.repaint();
		    }
		});	    
	    jTransposePanel.add(jTransposeAmount);

	    jTransposePanel.add(Box.createRigidArea(new Dimension(30,1)));

	    // Cue points allow you to listen to the same part of a
	    // song using different tunings
	    jGetCuePoint = new JButton("Get CuePoint (alt-g)");
	    jTransposePanel.add(jGetCuePoint);
	    NumberFormat doubleFormat = NumberFormat.getNumberInstance();
	    final JFormattedTextField jCuePoint = new JFormattedTextField(doubleFormat);
	    jCuePoint.addFocusListener(blockKeysFocusListener);
	    jCuePoint.setValue(new Double(0.00000));
	    jCuePoint.setColumns(8);
	    jGetCuePoint.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			jCuePoint.setValue((((double)theSequencer.getMicrosecondPosition())/((double)1.0e6)));
		    }
		});
	    jTransposePanel.add(jCuePoint);
	    jGoCuePoint = new JButton("Goto CuePoint (alt-c)");
	    jGoCuePoint.setEnabled(false);
	    jGoCuePoint.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){			
			double cuepoint = ((Number)jCuePoint.getValue()).doubleValue()*1.0e6;		     
			Sequence theSequence = theSequencer.getSequence();
			if(theSequence == null){
			    return;
			}
			double usectotick = ((double)theSequence.getTickLength())/((double)theSequence.getMicrosecondLength());
			startSequence((long)(cuepoint*usectotick));
		    }
		});
	    jTransposePanel.add(jGoCuePoint);

	    fileadvancedPanel.add(jTransposePanel);
	    	  
	    // panel with button to save or play modified midi file
	    JPanel goPanel = new JPanel();
	    goPanel.setLayout(new BoxLayout(goPanel,BoxLayout.X_AXIS));
		
	    add(goPanel);
	    
	    goPanel.add(Box.createRigidArea(new Dimension(20,20)));
	    JButton jGoButton = new JButton("Generate and save re-tuned midi file");
	    goPanel.add(jGoButton);
	    goPanel.add(Box.createRigidArea(new Dimension(30,10)));	

	    //goPanel.add(Box.createHorizontalGlue());
	    
	    jGoButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			catchKeyboard = false;
			JFileChooser fileChooser = new JFileChooser(lastfile);
			// suggest a new name based on loaded file and tuning selected
			String suggestedfilename = jInfile.getText();
			// get rid of path
			int lastslash = suggestedfilename.lastIndexOf('/');
			if(lastslash >= 0){
			    suggestedfilename = suggestedfilename.substring(lastslash+1);
			}
			lastslash = suggestedfilename.lastIndexOf('\\');
			if(lastslash >=0){
			    suggestedfilename = suggestedfilename.substring(lastslash+1);
			}
			suggestedfilename = getSuggestedFilename(suggestedfilename, (String)jTunings.getSelectedItem(),jNotes.getSelectedIndex(),jTuningAdvancedPanel.getRootNote(),jTuningAdvancedPanel.getFrequency());

			fileChooser.setSelectedFile(new File(suggestedfilename));
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int rVal = fileChooser.showSaveDialog(null);
			if (rVal == JFileChooser.APPROVE_OPTION) {
			    try{
				// save selected file in string "lastfile"
				lastfile = new String(fileChooser.getSelectedFile().toString());
				// make sure it has the proper extension for a midi file
				if(! ((lastfile.toLowerCase().endsWith(".mid"))||(lastfile.toLowerCase().endsWith(".midi")))){
				    lastfile = new String(lastfile+".mid");
				}
				// make and save the re-tuned midi file			  
				int theInstrument = progChangeCodes[jChooseFileProgram.getSelectedIndex()];
				if(theInstrument < -2){
				    theInstrument = jProgram.getSelectedIndex();
				}
				makethefile(jInfile.getText(),lastfile,jNotes.getSelectedIndex(),jTunings.getSelectedItem().toString(),1.0*((Integer)jBendRange.getSelectedItem()),jBendRangeCommand.isSelected(),theInstrument,jTuningAdvancedPanel.getRootNote(),jTuningAdvancedPanel.getFrequency(),(Integer)jTransposeAmount.getSelectedItem(),false);
				
			    } catch (Exception ee) {}
			}
			catchKeyboard = true;
		    }
		});
	    
	    jMidiProgress = new JProgressBar(0,1);
	    jMidiProgress.addMouseMotionListener(new MouseMotionListener() {
		    public void mouseMoved(MouseEvent e) {
			int x = e.getX();
			int W = jMidiProgress.getWidth();
			double t = (((double)theSequencer.getMicrosecondLength())/(double)1.0e6);
			DecimalFormat df = new DecimalFormat("#.00");
			jMidiProgress.setToolTipText(df.format((double)(t*x)/((double)W))+"s");
		    }
		    public void mouseDragged(MouseEvent e) {
		    }
		});
	    jMidiProgress.addMouseListener(new MouseListener() {
		    public void mouseClicked(MouseEvent e){
			int x = e.getX();
			int W = jMidiProgress.getWidth();
			jMidiProgress.setValue((jMidiProgress.getMaximum()*x)/W);
			startSequence(theSequencer.getTickLength()*x/W);
		    }
		    
		    public void mousePressed(MouseEvent e){
		    }
		    public void mouseReleased(MouseEvent e){
		    }
		    public void mouseEntered(MouseEvent e){
		    }
		    public void mouseExited(MouseEvent e){
		    }
		});
	    ActionListener updateMidiProgress = new ActionListener() {
		    public void actionPerformed(ActionEvent e){
			jMidiProgress.setValue((int)((100.0*(double)theSequencer.getMicrosecondPosition())/1.0e6));
			jMidiProgress.repaint();
		    }
		};
	    progressTimer = new Timer(40, updateMidiProgress);
	    jPause = new PauseButton(pausestring,continuestring);//new JButton(pausestring);	
	    jPlay.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			if(midiIn != null){
			    if(jPlay.getText()==playstring){
				startSequence(0);
			    }
			    
			    else{
				endSequence(false);
			    }
			}
		    }
		});
	    

	    goPanel.add(jPlay);

	    //final PauseAction pauseAction = new TemperamentStudio.PauseAction();
	    //jPause.addActionListener(pauseAction);
	    jPause.setEnabled(false);
	    goPanel.add(jPause);
	    goPanel.add(jMidiProgress);
	    goPanel.add(Box.createRigidArea(new Dimension(20,20)));
	    add(Box.createRigidArea(new Dimension(20,10)));
	}
	
	public void addAdvancedPanel(){
	    int i = getComponentIndex(infilePanel,this);
	    add(fileadvancedSuperPanel,i+1);
	}
	public void removeAdvancedPanel(){
	    remove(fileadvancedSuperPanel);
	}
    }


    /////////////////////////////////////////////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    ///////////    JChordIntervalPanel      /////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    
    // This panel has buttons you can use to play chords and intervals
    public static class JChordIntervalPanel extends JPanel {
	JChordIntervalPanel(){
	    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

	    // make buttons to add chords and intervals
	    JPanel chordRootPanel = new JPanel(new FlowLayout());
	    //chordRootPanel.setLayout(new BoxLayout(chordRootPanel,BoxLayout.X_AXIS));


	    chordRootPanel.add(new JLabel("Insert interval/chord with root"));
	    jChordRoot = new JComboBox<String>();
	    jChordRoot.setFont(dropdownFont);
	    jChordRoot.addFocusListener(blockKeysFocusListener);
	    for(int i=0; i<notes.length; i++) {
		jChordRoot.addItem(notes[i]);
	    }
	    jChordRoot.addItemListener(new ItemListener(){
		    public void itemStateChanged(ItemEvent e) {
			chordroot = jChordRoot.getSelectedIndex()+middleC;
			changeChordLabels();
		    }
		});
	    chordRootPanel.add(jChordRoot);
	    keyLockNotice = new JLabel("  (if hot keys are not working, press esc)  ");
	    chordRootPanel.add(keyLockNotice);
	    sineComboPanel = new CloakablePanel(0,0);
	    sineComboPanel.setLayout(new GridBagLayout());
	    sineComboPanel.add(new JLabel("      Source"));
	    //xsineComboPanel.setBorder(advancedSettingsBorder);
	    sineCombo = new JComboBox<String>();
	    sineCombo.setFont(dropdownFont);
	    sineCombo.addFocusListener(blockKeysFocusListener);
	    sineCombo.addItem("MIDI");
	    sineCombo.addItem("Sine Waves");
	    sineCombo.addItem("Sine + harmonics");
	    sineCombo.addItemListener(new ItemListener(){
		    public void itemStateChanged(ItemEvent e) {
			switch(sineCombo.getSelectedIndex()){
			case 0: jHarmonicsPanel.cloak();//setVisible(false);
			    kbPanel.endSines(); 
			    cOverloadPanel.cloak();
			    break;
			case 1: jHarmonicsPanel.cloak();//setVisible(false);
			    kbPanel.startSines(); 
			    kbPanel.harmonicsOn(false);
			    cOverloadPanel.uncloak();
			    break;
			case 2: jHarmonicsPanel.uncloak();//setVisible(true);
			    kbPanel.startSines(); 
			    kbPanel.harmonicsOn(true);
			    for(int i = 0; i < nHarmonics; i++){
				kbPanel.setHarmonic(i,jHarmonicsSliders[i].getValue());
			    }
			    cOverloadPanel.uncloak();
			    break;
			}
			changeLiveTuning();

		    }
		});

	    sineComboPanel.add(Box.createRigidArea(new Dimension(5,5)));	
	    sineComboPanel.add(sineCombo);
	    chordRootPanel.add(sineComboPanel);

	    sineComboPanel.add(Box.createRigidArea(new Dimension(10,10)));	


	    Font overloadFont = new Font("Arial",Font.PLAIN,6);
	    JPanel jOverloadPanel = new JPanel(new GridBagLayout());
	    cOverloadPanel = new CloakablePanel(10,10);
	    GridBagConstraints c = new GridBagConstraints();
	    jOverload = new JLabel("  clip  ");
	    jUnderRun = new JLabel(" under ");
	    jOverload.setOpaque(true);
	    jUnderRun.setOpaque(true);
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 0;
	    c.gridy = 0;
	    c.gridwidth = 1;
	    c.gridheight = 1;
	    jOverloadPanel.add(jOverload,c);
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 1;
	    c.gridheight = 1;
	    jOverloadPanel.add(jUnderRun,c);

	    overloadIndicator(false);
	    underRunIndicator(false);

	    cOverloadPanel.add(jOverloadPanel);
	    sineComboPanel.add(cOverloadPanel);
	    
	    add(chordRootPanel);
	    chordRootPanel.setMaximumSize(chordRootPanel.getPreferredSize());

	    Font chordFont = new Font("Arial",Font.PLAIN,11);
	    
	    JPanel intervalPanel = new JPanel(new GridBagLayout());
	    intervalPanel.add(new JLabel("Intervals:"));
	    // insert intervals
	    final JButton[] intervalbuttons = new JButton[intervals.length];
	    for(int i=0; i < intervals.length; i++){
		intervalbuttons[i] = new JButton(intervals[i]);
		intervalbuttons[i].setFont(chordFont);
		intervalbuttons[i].setMargin(new Insets(1,1,1,1));
		intervalbuttons[i].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    kbPanel.allOff();
			    int interval = 0;
			    JButton thisbutton = (JButton) e.getSource();
			    for(int i = 0; i < intervals.length; i++){
				if(thisbutton == intervalbuttons[i]){
				    interval = i;
				    break;
				}
			    }
			    kbPanel.playNote(chordroot);
			    kbPanel.playNote(chordroot+interval);
			}
		    });
		intervalPanel.add(intervalbuttons[i]);
	    }
	    //JPanel intervalSuperPanel = new JPanel(new FlowLayout());
	    //intervalSuperPanel.add(intervalPanel);
	    //add(intervalSuperPanel);
	    //intervalSuperPanel.setMaximumSize(intervalSuperPanel.getPreferredSize());
	    add(intervalPanel);
	    intervalPanel.setMaximumSize(intervalPanel.getPreferredSize());

	    JPanel chordPanel = new JPanel(new GridLayout());
	    chordPanel.add(new JLabel("Chords:"));
	    // insert intervals
	    jChordButtons = new JButton[chords.length];
	    for(int i=0; i < chords.length; i++){
		jChordButtons[i] = new JButton();
		jChordButtons[i].setFont(chordFont);
		jChordButtons[i].setMargin(new Insets(0,0,0,0));
		jChordButtons[i].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    kbPanel.allOff();
			    int chord = 0;
			    JButton thisbutton = (JButton) e.getSource();
			    for(int i = 0; i < chords.length; i++){
				if(thisbutton == jChordButtons[i]){
				    chord = i;
				    break;
				}
			    }
			    for(int i = 0; i < chordnotes[chord].length; i++){
				kbPanel.playNote(chordroot+chordnotes[chord][i]);
			    }
			}
		    });
		chordPanel.add(jChordButtons[i]);
	    }
	    changeChordLabels();
	    
	    chordPanel.add(Box.createRigidArea(new Dimension(1,1)));	


	    JButton jInversionN = new JButton("invert-");
	    jInversionN.setFont(chordFont);
	    jInversionN.setMargin(new Insets(0,0,0,0));
	    jInversionN.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			kbPanel.invert(-1);
		    }
		});
	    chordPanel.add(jInversionN);
	    JButton jInversionP = new JButton("invert+");
	    jInversionP.setFont(chordFont);
	    jInversionP.setMargin(new Insets(0,0,0,0));
	    jInversionP.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			kbPanel.invert(1);
		    }
		});
	    chordPanel.add(jInversionP);

	    JPanel chordSuperPanel = new JPanel(new FlowLayout());
	    chordSuperPanel.add(chordPanel);
	    chordPanel.setMaximumSize(chordPanel.getPreferredSize());
	    //jBachChordPanel = new JPanel();
	    //chordSuperPanel.add(jBachChordPanel);
	    add(chordSuperPanel);
	    //chordSuperPanel.setMaximumSize(chordSuperPanel.getPreferredSize());
	}
    }

    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////                   /////////////////////////////////////
    ////////////////////////  Misc functions   /////////////////////////////////////
    ////////////////////////                   /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    // this is used to change the chord labels when the chord root is changed
    public static void changeChordLabels(){
	for(int i=0; i < chords.length; i++){
	    jChordButtons[i].setText((String)jChordRoot.getSelectedItem()+chords[i]);
	}
	frame.pack();
	frame.repaint();
    }



    ////////////////////////  Change live tuning     /////////////////////////////////////

    // This is called whenever a tuning parameter is changed to update what
    // is being played by kbPanel (the virtual musical keyboard) and by
    // the midi sequencer.
    public static void changeLiveTuning() {
	if(kbPanel == null){
	    return;
	}
	if((sineCombo.getSelectedIndex() > 0) || (jKeyStrings.getSelectedIndex() > 0))
	    try{
		kbPanel.setFrequencies(tuningSchemes.getPitchShifts((String)jTunings.getSelectedItem(),2,jNotes.getSelectedIndex(),jTuningAdvancedPanel.getRootNote(), jTuningAdvancedPanel.getFrequency()),jNotes.getSelectedIndex(),jTuningAdvancedPanel.getRootNote(),jTuningAdvancedPanel.getFrequency());
	    } catch (Exception e) {}

	// set the program
	int program = jProgram.getSelectedIndex();

	// set the tuning
	int pitchshift[] = {0,0,0,0,0,0,0,0,0,0,0,0};
	// load the pitch shift values for each channel
	int rootnote = jNotes.getSelectedIndex();
	try{
	    pitchshift = tuningSchemes.getPitchShifts((String)jTunings.getSelectedItem(),2,rootnote,jTuningAdvancedPanel.getRootNote(), jTuningAdvancedPanel.getFrequency());
	} catch (Exception e){
	    displayerror(e.getMessage());
	}
	
	
	kbPanel.setProgram(program);
	kbPanel.setPitchShifts(pitchshift,rootnote);
	// fire an event on jKeyStrings to update the strings if
	// strings are shown
	//int keystringi = ;
	if(jKeyStrings.getSelectedIndex() > 0){
	    updateKeyStrings(jKeyStrings.getSelectedIndex());

	    //jKeyStrings.setSelectedIndex(-1);
	    //jKeyStrings.setSelectedIndex(keystringi);
	}
	
	/////////////////////
	if(!(theSequencer == null)){
	    // a call to seSequencerTickPosition will automatically write
	    // the changed data where it will be immediately processed
	    // by the synth
	    if(theSequencer.isRunning()){
		long pos = theSequencer.getTickPosition();
		startSequence(pos);
	    }
	    //setSequencerTickPosition(pos);
	}
	/////////////
	// pitch shifts should take place immediately - don't know
	// why I had the next line - but having it makes things not
	// smooth when you slide the general meantone slider
	//kbPanel.replayAllNotes();
    }


    /////////////////////////////////////////////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    ///////////    makethefile              /////////////////////////////////////////
    ///////////                             /////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////


    // create and save tuned midi file
    // infile is the midi file to read in, 
    // outfile is the path for the modified file to be saved, 
    // rootnote is the note to base the tuning scheme on, 
    // temperamentstring is a string that tells what tuning scheme to use, 
    // maxpitchbend is the number of semitones the target midi synth can bend a note
    // writemaxpitchbend is true if you want to write code to set the synth's max pithbend
    // selectProgram is -1 to apply all program changes to all channels,
    //   -2 to apply just the first program change to all channels and ignore the rest, or
    //   a number from 0 to 127 to set a specific midi program for all channels for the whole file
    // tuningroot is the note that the instrument is tuned too - the tuning scheme will be applied
    //   using the tuning scheme and tuning root above, and then all pitched will be adjusted so
    //   that this note will have the frequency given by
    // tuningfrequency - the frequency of the tuning root
    // transposeamt - the number of semitones to transpose by
    // overwrite - if this is true, existing files will be overwritten without asking
    public static void makethefile(String infile, String outfile, int rootnote, String temperamentstring, double maxpitchbend, Boolean writemaxpitchbend, int selectProgram, int tuningroot, double tuningfrequency, int transposeamt, Boolean overwrite)  throws MidiUnavailableException, InvalidMidiDataException, IOException {

	// open up the input midi file
        Sequence midiIn = null;
        try {
            midiIn = MidiSystem.getSequence(new File(infile));
        } catch (Exception e) {
            displayerror("Error opening midi file "+infile+" for reading");
	    return;
        }


	// make a new midi sequence to store the modified midi stream
	Sequence newMidi = RetuneMIDI.retunedSequence(midiIn,rootnote,temperamentstring,maxpitchbend,writemaxpitchbend,selectProgram,false,tuningroot,tuningfrequency, transposeamt,0);
	if(newMidi == null){
	    displayerror("Error generating new midi sequence");
	    return;
	}


	// Now write the new midi file
	try{
	    File f = new File(outfile);
	    if(!overwrite){
		if(f.exists() && !f.isDirectory()){
		    if(!getYesNo("File "+outfile+" exists.  Overwrite it?")){
			displayerror("No file written");
			return;
		    }
		}
	    }
	    MidiSystem.write(newMidi,1,f);
	} catch (IOException e) {
	    displayerror("Problem writing to file "+outfile);
	    return;
	}
	displaymessage("Done!  New midi file written to "+outfile);
    }


    /////////////////////////////////////////////////////////////////////
    ////////////////////////// If a badly formed command line is used,
    ////////////////////////// give info and exit.
    
    public static void printUsageAndExit() {
	System.out.println("Error parsing command line\n\n"+description+"\n\n"+usage);
	System.exit(1);
    }
    

    // Find where a component is in a panel, so that we can insert a component
    // before or after it.
    public static int getComponentIndex(Component component, Container container){
	Component[] components = container.getComponents();
	for(int i=0; i < components.length; i++){
	    if(components[i] == component){
		return(i);
	    }
	}
	return(-1);
    }

    // Loads sequence, sets up tuning, and starts playing it at the given location.
    // The first version assumes you want to play the selected file,
    // the second lets you choose the sequence
    public static void startSequence(long ticks){
	//	try{
	int fileprogram = progChangeCodes[jChooseFileProgram.getSelectedIndex()];
	if(fileprogram == progUseSelected){
	    fileprogram = jProgram.getSelectedIndex();
	}
	Sequence theSeq = null;
	try{
	    theSeq = RetuneMIDI.retunedSequence(midiIn,jNotes.getSelectedIndex(), jTunings.getSelectedItem().toString(), 2.0, false,fileprogram,true,jTuningAdvancedPanel.getRootNote(),jTuningAdvancedPanel.getFrequency(),(Integer)jTransposeAmount.getSelectedItem(),ticks);
	} catch (Exception ea){
	    // couldn't make sequence, so give up
	    return;
	}
	startSequence(ticks,theSeq);
    }

    public static void startSequence(long ticks, Sequence theSequence){
	// clear red squares from note previously being played
	kbPanel.seqAllOff();
	//frame.repaint();
	jPlay.setText(stopstring);
	jPause.setText(pausestring);
	jPause.setEnabled(true);				
	try{
	    if(theSequencer.isOpen()){
		theSequencer.stop();
		theSequencer.close();
	    }
	}
	catch (Exception ea){
	    // can't imagine how we'd get here . . .
	    endSequence(true);
	    return;
	}
	
	
	try{
	    theSequencer.setSequence(theSequence);
	    theSequencer.open();
	} catch (Exception ea) {
	    System.out.println("Can't set the sequence or open the sequencer");
	    System.out.println(ea.toString());
	    // can't set the sequence
	    endSequence(true);
	    return;
	}
	long tixlength = theSequencer.getTickLength();
	double seclength = theSequencer.getMicrosecondLength()/1.0e6;
	jMidiProgress.setMaximum((int)(100.0*seclength));
	jMidiProgress.setValue((int)(100.0*ticks*seclength/tixlength));
	try{
	    progressTimer.start();
	} catch (Exception ea){
	    System.out.println("Progress timer issue");
	    endSequence(true);
	    return;
	}
	try{
	    theSequencer.start();
	    jGoCuePoint.setEnabled(true);
	}
	catch (Exception ea){
	    System.out.println("Can't start the sequencer");
	    System.out.println(ea.toString());
	    endSequence(true);
	    return;
	}
	setTempo();
	if(ticks > 0)
	    theSequencer.setTickPosition(ticks-1);
    }


    // stops playing a sequence and cleans up
    public static void endSequence(Boolean resetprogress){
	// clear red squares from note previously being played
	kbPanel.seqAllOff();
	//frame.repaint();
	jGoCuePoint.setEnabled(false);
	if(theSequencer.isOpen()){
	    theSequencer.stop();
	    theSequencer.close();
	}
	jPlay.setText(playstring);
	jPause.setText(pausestring);
	if(resetprogress){
	    jMidiProgress.setValue(0);
	}
	jPause.setEnabled(false);
	kbPanel.seqAllOff();
    }

    // Writes version file so that I know that files have been
    // previously extracted.  Returns true if there were no
    // problems writing it, false otherwise
    private static Boolean writeVersionFile(){
	try{
	    File thefile = new File(jarPath+programName+".ver");
	    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(thefile)));
	    out.write(versionString,0,versionString.length());
	    out.close();
	} catch (Exception e){ return(false); }
	return(true);
    }

    // Reads version file to see if files have been installed
    // before, and if so, what version of them were written
    // If no version file is found (meaning files haven't been
    // previously installed) it returns "0".  If there was an
    // error, it returns "Error"
    private static String readVersionFile(){
	File thefile = new File(jarPath+programName+".ver");
	if(!thefile.exists()){
	    return("0");
	}
	String s = "0";
	try{
	    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(thefile)));
	    s = in.readLine().trim();
	    in.close();
	} catch (Exception e){ return("Error"); }
	if(s.length() == 0){
	    s = "Error";
	}
	return(s);
    }

    // Read a text file in the jar file into a string
    private static String readStringFromJar(String filename){
	InputStream jarinput = TemperamentStudio.class.getResourceAsStream(filename);
	StringBuilder sb = new StringBuilder();
	try{
	    BufferedReader br = new BufferedReader(new InputStreamReader(jarinput));
	    String line = br.readLine();
	    while(line != null){
		sb.append(line+"\n");
		line = br.readLine();
	    }
	    br.close();
	    jarinput.close();
	} catch (Exception e) {}
	return(sb.toString());
    }

    // Create a suggested filename based on the original filename and various parameters
    private static String getSuggestedFilename(String infilename, String tuningmethod, int tuningroot, int tuningref, double tuningfreq){
	// builds a suggested filename to save a retuned version of the midi file infilename
	// tuningmethod is the string that describes the tuning method
	// tuningroot is the root note of the tuning method.  C is 0, C# is 1, etc.
	// tuningref is which note is set to a fixed frequency - for example, for A440, it's "A."  C is 0, etc.
	// tuningfreq is the frequency of the tuningref note.  For A440, it would be 440.0, etc.

	String suggestedfilename = infilename;
	// find where the extention .mid starts
	int lastdot = suggestedfilename.lastIndexOf('.');
	if(lastdot >=0){
	    suggestedfilename = suggestedfilename.substring(0,lastdot);
	}		    
	// add info about tuning method and root, tuning frequency
	suggestedfilename += "_"+tuningmethod+notes[tuningroot]+"_"+notes[tuningref]+Integer.toString((int)Math.round(tuningfreq));
	suggestedfilename = suggestedfilename+".mid";
	return(suggestedfilename);
    }


    // Copy a file out of the jar
    // I use this to copy midi files, a configuration file, and the license
    // file the first time the program is run.  This way I only have to
    // distribute the jar file.  Then the user doesn't have to worry about
    // multiple files, unzipping a zip file, etc.
    // thefile is the name of the file in the jar (and the name where it will
    // be extracted, and binary says if it is a binary (vs text) file.
    private static Boolean copyFileFromJar(String thefile, Boolean binary){
	File tfile = new File(jarPath+thefile);
	if(tfile.exists() && !tfile.isDirectory()){
	    // file exists, don't overwrite
	    return(false);
	}
	return(overwriteCopyFileFromJar(thefile, binary));
    }

    // this function does the copying - it will overwrite a file if it exists
    private static Boolean overwriteCopyFileFromJar(String thefile, Boolean binary){
	File tfile = new File(jarPath+thefile);
	// make subdirectories if needed
	File parent = tfile.getParentFile();
	if(!parent.exists() && !parent.mkdirs()){
	    return(false);
	}
	try{
	    InputStream streamin = TemperamentStudio.class.getResourceAsStream(thefile);
	    OutputStream streamout = new FileOutputStream(tfile);
	    if(binary){
		int readbytes;
		byte[] buffer = new byte[4096];
		while((readbytes = streamin.read(buffer)) > 0){
		    streamout.write(buffer,0,readbytes);
		}
	    }
	    else{
		BufferedReader in = new BufferedReader(new InputStreamReader(streamin));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(streamout));

		String line;
		while((line=in.readLine()) != null) {
		    out.write(line);
		    out.newLine();
		}
		in.close();
		out.close();
	    }
	    streamin.close();
	    streamout.close();
	} catch (Exception e){
	    return(false);
	}
	return(true);
    }
    
    // copy list of files from the Jar
    private static void copyFileListFromJar(String listname, String folder){
	InputStream streamin = TemperamentStudio.class.getResourceAsStream(listname);
	try{		    
	    BufferedReader in = new BufferedReader(new InputStreamReader(streamin));
	    String line;
	    while((line=in.readLine()) != null) {
		if(line.length() > 0)
		    copyFileFromJar(folder+"/"+line.trim(),true);
	    }
	    in.close();
	    //streamin.close();
	} catch (Exception e){}
	finally{
	    try{
		streamin.close();	    
	    } catch (Exception ea) {}
	}
    }
    

    // set the midi playback tempo based
    // on the tempo slider
    public static void setTempo(){
	int speed = jTempo.getValue();
	theSequencer.setTempoFactor(((float)speed)/(float)100.0);
    }

    // slider for generic meantone
    private static class JMeantoneSlider extends JSlider{
	final int meantoneMult = 30000;
	

	JMeantoneSlider(){
	    int meantoneLow = (int)Math.round(1.4905*meantoneMult);
	    int meantoneHigh = (int)Math.round(1.505*meantoneMult);
	    int meantoneQC = (int)Math.round(Math.pow(5,0.25)*meantoneMult);
	    int meantoneET = (int)Math.round(Math.pow(2,7.0/12.0)*meantoneMult);
	    int meantonePY = (int)Math.round((3.0/2.0)*meantoneMult);
	    Font meantoneFont = new Font("Dialog", Font.PLAIN, 8);
	    //jMeantoneSlider = new JSlider(
	    setOrientation(JSlider.HORIZONTAL);
	    setMinimum(meantoneLow);
	    setMaximum(meantoneHigh);
	    setValue(meantoneQC);
	    jMeantoneSliderX.setText(new DecimalFormat("#.00000").format(getValue()/(1.0*meantoneMult)));

	    Hashtable<Integer,JLabel> meantoneLabels = new Hashtable<Integer,JLabel>();
	    JLabel meantoneLowLabel = new JLabel(Double.toString(meantoneLow/(double)meantoneMult));
	    meantoneLowLabel.setFont(meantoneFont);
	    meantoneLabels.put(meantoneLow, meantoneLowLabel);

	    JLabel meantoneQCLabel = new JLabel("QC");
	    meantoneQCLabel.setFont(meantoneFont);
	    meantoneLabels.put(meantoneQC, meantoneQCLabel);

	    JLabel meantoneETLabel = new JLabel("ET");
	    meantoneETLabel.setFont(meantoneFont);
	    meantoneLabels.put(meantoneET, meantoneETLabel);

	    JLabel meantonePYLabel = new JLabel("Py");
	    meantonePYLabel.setFont(meantoneFont);
	    meantoneLabels.put(meantonePY, meantonePYLabel);

	    JLabel meantoneHighLabel = new JLabel(Double.toString(meantoneHigh/(double)meantoneMult));
	    meantoneHighLabel.setFont(meantoneFont);
	    meantoneLabels.put(meantoneHigh, meantoneHighLabel);
	    
	    setLabelTable( meantoneLabels);
	    setPaintLabels(true);

	    addChangeListener(new ChangeListener() {
		    public void stateChanged(ChangeEvent e){
			double x = jMeantoneSlider.getValue()/(double)meantoneMult;
			jMeantoneSliderX.setText(new DecimalFormat("#.00000").format(getValue()/(1.0*meantoneMult)));
			tuningSchemes.changeMeantoneScheme(gMeantoneName,x);
			changeLiveTuning();
		    }
		});
	    addFocusListener(blockKeysFocusListener);
	    //setVisible(false);
	    setMinimumSize(new Dimension(150,30));
	}
    }

    public static void overloadIndicator(boolean x){
	if(x){
	    jOverload.setBackground(Color.RED);
	    jOverload.setForeground(Color.BLACK);
	}
	else{
	    jOverload.setBackground(Color.DARK_GRAY);
	    jOverload.setForeground(Color.BLACK);
	}
    }

    public static void underRunIndicator(boolean x){
	if(x){
	    jUnderRun.setBackground(Color.RED);
	    jUnderRun.setForeground(Color.BLACK);
	}
	else{
	    jUnderRun.setBackground(Color.DARK_GRAY);
	    jUnderRun.setForeground(Color.BLACK);
	}
    }

    public static void updateKeyStrings(int x){
	switch(x){
	case 1:
	    try{
		kbPanel.setFrequencies(tuningSchemes.getPitchShifts((String)jTunings.getSelectedItem(),2,jNotes.getSelectedIndex(),jTuningAdvancedPanel.getRootNote(), jTuningAdvancedPanel.getFrequency()),jNotes.getSelectedIndex(),jTuningAdvancedPanel.getRootNote(),jTuningAdvancedPanel.getFrequency());
		kbPanel.setKeyStringFrequencies();
	    } catch (Exception ea) {}
	    break;
	case 2:
	    kbPanel.setKeyStringsRatios(tuningSchemes.getRatios((String)jTunings.getSelectedItem()),jNotes.getSelectedIndex(),false);
	    break;
	case 3:
	    kbPanel.setKeyStringsRatios(tuningSchemes.getRatios((String)jTunings.getSelectedItem()),jNotes.getSelectedIndex(),true);
	    break;
	case 4:
	    kbPanel.setKeyStringsNoteName();
	    break;
	case 5: 
	    kbPanel.setKeyStringsMIDINote();
	    break;
	default:
	    kbPanel.keyStringsOff();
	}
	
    }

    // this class is so that I can have a final pointer to 
    // a string but still be able to change the string
    public static class StringContainer{
	public String string;

	StringContainer(){
	}

	StringContainer(String txt){
	    string = txt;
	}
    }

    public static void editIntonationsFile(){
	if(editingTunings){
	    return;
	}
	editingTunings = true;
	catchKeyboard = false;
	final JFrame editframe = new JFrame(programName+" - "+tuningfilename);
	editframe.setVisible(false);
	editframe.addFocusListener(blockKeysFocusListener);
	editframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

	final JTextArea jTextArea = new JTextArea();//200,70);
	jTextArea.addFocusListener(blockKeysFocusListener);
	File textfile = new File(jarPath+tuningfilename);
	final StringContainer text = new StringContainer();
	try{
	    text.string = new Scanner( new File(jarPath+tuningfilename) ).useDelimiter("\\A").next();
	} catch (Exception e){
	    displayerror("Error opening tuning file");
	    return;
	}
	jTextArea.setText(text.string);
	jTextArea.setCaretPosition(0);
	jTextArea.getDocument().addDocumentListener(new DocumentListener(){
		public void insertUpdate(DocumentEvent e){
		    changed(e);
		}
		public void removeUpdate(DocumentEvent e){
		    changed(e);
		}
		public void changedUpdate(DocumentEvent e){
		    changed(e);
		}
		public void changed(DocumentEvent e){
		    editframe.setTitle(programName+" - *"+tuningfilename);
		}
	    });


	final StringContainer origText = new StringContainer(text.string);

	final Container editcontentPane = editframe.getContentPane();
	JPanel contentPanel = new JPanel(new BorderLayout());
	//contentPanel.setLayout(new BoxLayout(contentPanel,BoxLayout.Y_AXIS));
	editcontentPane.add(contentPanel);
	editcontentPane.addFocusListener(blockKeysFocusListener);


	JPanel buttonPanel = new JPanel(new FlowLayout());
	buttonPanel.addFocusListener(blockKeysFocusListener);
	contentPanel.add(buttonPanel,BorderLayout.PAGE_START);
	JButton saveButton = new JButton("Save Tunings");
	saveButton.addFocusListener(blockKeysFocusListener);

	saveButton.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e){
		    text.string = jTextArea.getText();
		    try (PrintStream out = new PrintStream(new FileOutputStream(jarPath+tuningfilename))) {
			    out.print(text.string);
			    origText.string = text.string;
			} catch (Exception ea) {
			displayerror("Unable to save file");
		    }
		    editframe.setTitle(programName+" - "+tuningfilename);
		    Object[] tuningselection = jTunings.getSelectedObjects();
		    loadTuningSchemes();
		    populatejTunings();
		    jTunings.setSelectedItem(tuningselection[0]);
		    changeLiveTuning();
		}
	    });

	buttonPanel.add(saveButton);
	// /*
	// final JButton cancelButton = new JButton("Cancel");
	// cancelButton.addActionListener(new ActionListener(){
	// 	public void actionPerformed(ActionEvent e){
	// 	    if(!jTextArea.getText().equals(origText)){
	// 		int answer = JOptionPane.showConfirmDialog(editframe,"Are you sure you want to close the editor without saving your changes?","Closing Editor",JOptionPane.YES_NO_OPTION);
	// 		if(answer > 0)
	// 		    return;
	// 	    }
	// 	    editframe.setVisible(false);
	// 	    editframe.dispose();
	// 	    catchKeyboard = true;
	// 	}
	//     });
	// */
	editframe.addWindowListener(new WindowAdapter(){
		@Override
		public void windowClosing(WindowEvent e){
		    if(!jTextArea.getText().equals(origText.string)){
			int answer = JOptionPane.showConfirmDialog(editframe,"Are you sure you want to close the editor without saving your changes?","Closing Editor",JOptionPane.YES_NO_OPTION);
			if(answer > 0)
			    return;
		    }
		    editframe.setVisible(false);
		    editframe.dispose();
		    editingTunings = false;
		    catchKeyboard = true;
		}
	    });
	//buttonPanel.add(cancelButton);
	JButton resetButton = new JButton("Reset to default tunings");
	resetButton.addFocusListener(blockKeysFocusListener);

	resetButton.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e){
		    int answer = JOptionPane.showConfirmDialog(editframe,"This will reset the tuning schemes file to the way it was when you first ran this program, deleting any changes you have made.  Are you sure you want to do this?","Restoring default tuning schemes file",JOptionPane.YES_NO_OPTION);
		    if(answer == 0){
			overwriteCopyFileFromJar(tuningfilename,false);
			try{
			    text.string = new Scanner( new File(jarPath+tuningfilename) ).useDelimiter("\\A").next();
			} catch (Exception ea){
			    displayerror("Error opening tuning file");
			    text.string = origText.string;
			}
			jTextArea.setText(text.string);
			jTextArea.setCaretPosition(0);
		    }
		}
	    });
	buttonPanel.add(resetButton);
	
	

	JScrollPane jScrollPane = new JScrollPane(jTextArea);
	jScrollPane.addFocusListener(blockKeysFocusListener);

	contentPanel.add(jScrollPane,BorderLayout.CENTER);

	//	jScrollPane.setBounds(0,0,800,500);
	//jScrollPane.setSize(800,500);

	editframe.setPreferredSize(new Dimension(450,500));
	editframe.pack();
	editframe.setVisible(true);
	
    }

    public static Boolean loadTuningSchemes(){
	// load tuning schemes - first check for local file, then read from jar
	Boolean tuningsloaded = false;
	// see if there is a custom tuning file
	File tuningfile = new File(jarPath+tuningfilename);
	if(tuningfile.exists() && !tuningfile.isDirectory()){
	    // if there is, load it
	    tuningsloaded = true;
	    try{
		InputStream instream = new FileInputStream(tuningfile);
		try{
		    tuningSchemes = new TuningSchemes(instream);
		} catch (Exception e){
		    tuningsloaded = false;
		    displayerror("Unable to load custom tuning scheme file, using defaults");
		} finally {
		    try{
			instream.close();
		    } catch (Exception e) {
			tuningsloaded = false;
		    }
		}
	    } catch (Exception e) {
		tuningsloaded = false;
	    }
		// Just in case something went wrong and wasn't caught
	    if(tuningSchemes == null){
		tuningsloaded = false;
		displayerror("Error loading custom tunings file.  Using default tuning schemes.");
	    }
	    else if(tuningSchemes.getTuningList().size() < 1){
		// we didn't actually load any tuning schemes!
		displayerror("Error loading custom tunings file.  Using default tuning schemes.");
		tuningsloaded = false;
	    }
	}

	// If we haven't successfully loaded tunings, load from tunings file in jar
	if(!tuningsloaded){
	    InputStream instream =TemperamentStudio.class.getResourceAsStream(tuningfilename);
	    try{
		tuningsloaded = true;
		tuningSchemes = new TuningSchemes(instream);
	    } catch (Exception e){
		// if all else fails, just call TuningSchemes without a stream argument to
		// make an equal temperament tuning scheme and move on.
		tuningSchemes = new TuningSchemes();
		displayerror(e.getMessage()+" "+tuningfilename);
		tuningsloaded = false;
	    }
	    finally{
		try{
		    instream.close();
		} catch (Exception e) {
		    tuningsloaded = false;
		}
	    }
	}
	if(!tuningsloaded)
	    return(false);
	// add generic meantone
	Double[] gMeantoneRatios = tuningSchemes.makeMeantoneRatios(1.4953487812212205);
	tuningSchemes.addScheme(gMeantoneName,gMeantoneRatios);
	/*
	  ArrayList<String> tuninglist = tuningSchemes.getTuningList();
	  if(tuninglist.indexOf("qc_meantone") >= 0){
	  tuningSchemes.copyScheme("qc_meantone",gMeantoneName);
	  }
	*/
	return(tuningsloaded);
    }

    public static void populatejTunings(){
	    ArrayList<String> tuninglist = tuningSchemes.getTuningList();

	    jTunings.addItem("temp");
	    // get rid of old tunings - keep one until end to avoid null pointer
	    while(jTunings.getItemCount() > 1){
		jTunings.removeItemAt(0);
	    }
	    for(int i=0; i < tuninglist.size(); i++){
		jTunings.addItem(tuninglist.get(i));
	    }
	    jTunings.removeItemAt(0);
	    if(tuninglist.size() > 0)
		jTunings.setSelectedIndex(0);
    }
}
