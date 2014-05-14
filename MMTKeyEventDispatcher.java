/************************

MMTKeyEventDispatcher.java

This class is part of
TemperamentStudio
(c) 2014 Dallin S. Durfee
This code may be modified and redistributed
under the MIT license

This class implements a KeyEventDispatcher
which monitors the keyboard and does
appropriate actions for different keystrokes.

******************/

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Robot;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.sound.midi.Sequence;
import javax.sound.midi.MidiSystem;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.InputStream;
import java.util.HashSet;

public class MMTKeyEventDispatcher implements KeyEventDispatcher{
    private static int bacheventnum = 0;
    private static Boolean doingbachevent = false;
    private static JPanel bachButtons;
    private static final String Konami = "uuddlrlrba";
    private static String konamistring = "xxxxxxxxxx";
    private static Random randgen = new Random();
    private static ImageIcon bachIcon, bachBachIcon, telemannIcon;
    private static Integer[] bachchordnotes;
    private static Robot robot;
    private static int robotCode = KeyEvent.VK_EQUALS;//VK_F12;
    private static HashSet<Integer> keysDown;

    MMTKeyEventDispatcher(){
	super();
	keysDown = new HashSet<Integer>();
	try{
	    robot = new Robot();
	} catch (Exception e) {}
	// load icons that will be used
	java.net.URL iconURL = TemperamentStudio.class.getResource("icons/Bachman.png");
	bachIcon = new ImageIcon(iconURL);
	iconURL = TemperamentStudio.class.getResource("icons/BachBach.png");
	bachBachIcon = new ImageIcon(iconURL);
	iconURL = TemperamentStudio.class.getResource("icons/Telemann.png");
        telemannIcon = new ImageIcon(iconURL);

	// set up the big scary bach chord
	int bachlowoctave = -3;
	int bachhighoctave = 3;
	bachchordnotes = new Integer[4*(bachhighoctave-bachlowoctave)];
	int bachn = 0;
	int bachoctave = bachlowoctave;
	while(bachn < bachchordnotes.length){
	    bachchordnotes[bachn] = bachoctave*12;
	    bachchordnotes[bachn+1] = bachoctave*12+3;
	    bachchordnotes[bachn+2] = bachoctave*12+6;
	    bachchordnotes[bachn+3] = bachoctave*12+9;
	    bachoctave += 1;
	    bachn += 4;
	}
	    

    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent e){
	int keycode = e.getKeyCode();
	if(keycode == robotCode){
	    //System.out.println("Robot");
	    return false;
	}
	if(e.getID() == KeyEvent.KEY_PRESSED){
	    if(keysDown.contains(keycode))
		return false;
	    keysDown.add(keycode);
	    // if user types escape, return focus to the musical keyboard
	    if(keycode==KeyEvent.VK_ESCAPE){
		TemperamentStudio.jHelpButton.requestFocusInWindow();
		TemperamentStudio.catchKeyboard = true;
		return false;
	    }
	    // check for alt key combinations
	    if((e.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK){
		switch(Character.toLowerCase(e.getKeyChar())){
		case 's' : TemperamentStudio.jTunings.requestFocusInWindow(); break;
		case 'r' : TemperamentStudio.jNotes.requestFocusInWindow(); break;
		case 'i' : TemperamentStudio.jProgram.requestFocusInWindow(); break;
		case 'g' : TemperamentStudio.jGetCuePoint.doClick(); break;
		case 'c' : TemperamentStudio.jGoCuePoint.doClick(); break;
		case 'x' : TemperamentStudio.jPlay.doClick(); break;
		case 'p' : TemperamentStudio.jPause.doClick(); break;
		default: return false;
		}
		return true;
	    }
	    
	    if(!TemperamentStudio.catchKeyboard)
		return false;
	    // use the robot to send another key - this fixes problem where key held down
	    // causes automatic repeats of key down and key up events.
	    try{
		robot.keyPress(robotCode);
		robot.keyRelease(robotCode);
	    } catch (Exception ea){}

	    // check for keys and do the right thing
	    switch(keycode){
	    case KeyEvent.VK_PERIOD : TemperamentStudio.jSustain.doClick(); break;
	    case KeyEvent.VK_ENTER : TemperamentStudio.jAllOff.doClick(); break;
	    case KeyEvent.VK_UP : TemperamentStudio.jOctaveUp.doClick(); konamistring = konamistring.substring(1,Konami.length())+"u"; break;
	    case KeyEvent.VK_DOWN : TemperamentStudio.jOctaveDown.doClick(); konamistring = konamistring.substring(1,Konami.length())+"d"; break;
	    case KeyEvent.VK_LEFT : TemperamentStudio.jMoveDown.doClick(); konamistring = konamistring.substring(1,Konami.length())+"l"; break;
	    case KeyEvent.VK_RIGHT : TemperamentStudio.jMoveUp.doClick(); konamistring = konamistring.substring(1,Konami.length())+"r"; break;
	    case KeyEvent.VK_F1 : TemperamentStudio.jHelpButton.doClick();
		
	    default :
		// a and b are part of the konami code, but not considered above
		if(Character.toLowerCase(e.getKeyChar()) == 'a')
		    konamistring = konamistring.substring(1,Konami.length())+"a";
		if(Character.toLowerCase(e.getKeyChar()) == 'b')
		    konamistring = konamistring.substring(1,Konami.length())+"b"; 
		// go through all of the keys that trigger notes on the musical keyboard
		for(int i = 0; i < TemperamentStudio.kbPanel.allkeychars.length; i++){
		    if(Character.toLowerCase(e.getKeyChar()) == TemperamentStudio.kbPanel.allkeychars[i]){
			if(TemperamentStudio.kbPanel.getSustain()){
			    TemperamentStudio.kbPanel.toggleNote(TemperamentStudio.kbPanel.allkeynotes[i]);
			}
			else{
			    TemperamentStudio.kbPanel.playNote(TemperamentStudio.kbPanel.allkeynotes[i]);
			}
			break;
		    }
		}
	    }
	    // see if they've entered the Konami code
	    if(konamistring.equals(Konami)&&(!doingbachevent)){
		// set doingbachevent to true so it won't try to process a konami code while
		// we're still doing the last one
		doingbachevent = true;
		// Increase the counter that tells us how many times the user has entered the
		// konami code
		bacheventnum += 1;
		// do different things depending on which time
		switch(bacheventnum){
		case 1:
		    playTheme("Bachman.mid");
		    TemperamentStudio.displaymessage("Character \"Bachman\" unlocked!",false,bachIcon);
		    TemperamentStudio.endSequence(true);

		    TemperamentStudio.jBachChordPanel.add(new JLabel(bachIcon));
		    bachButtons = new JPanel(new FlowLayout());
		    JButton jBachChord = new JButton("Deploy Bach Chord");
		    jBachChord.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				TemperamentStudio.kbPanel.allOff();
				for(int i = 0; i < bachchordnotes.length; i++){
				    TemperamentStudio.kbPanel.playNote(TemperamentStudio.chordroot+bachchordnotes[i]);
				}
				
			    }
			});			
		    bachButtons.add(jBachChord);
		    TemperamentStudio.jBachChordPanel.add(bachButtons);
		    TemperamentStudio.frame.pack();
		    TemperamentStudio.frame.repaint();
		    break;
		case 2: 
		    playTheme("BachBach.mid");
		    TemperamentStudio.displaymessage("Character \"Bach Bach\" unlocked!",false,bachBachIcon);
		    TemperamentStudio.endSequence(true);

		    bachButtons.setLayout(new GridLayout(3,0));
		    TemperamentStudio.jBachChordPanel.add(new JLabel(bachBachIcon),1);
		    JButton jBachmanSong = new JButton("Bachman Theme");
		    jBachmanSong.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				playTheme("Bachman.mid");
			    }
			});			
		    bachButtons.add(jBachmanSong);
		    JButton jBachBachSong = new JButton("Bach Bach Theme");
		    jBachBachSong.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				playTheme("BachBach.mid");
			    }
			});			
		    bachButtons.add(jBachBachSong);
		    TemperamentStudio.frame.pack();
		    TemperamentStudio.frame.repaint();
		    break;
		case 3:
		case 4:
		    TemperamentStudio.displaymessage("Characters \"Bachman\" and \"Bach Bach\" are still unlocked!  What more could you possibly want?",false,bachBachIcon);
		    break;
		case 5:
		    TemperamentStudio.displaymessage("Seriously, there aren't any more characters to unlock!  So stop doing that!",false,bachBachIcon);
		    break;
		case 6:			
		    TemperamentStudio.displaymessage("Character \"The Amazing Tele-Man\" unlocked!",false,telemannIcon);
		    TemperamentStudio.jBachChordPanel.add(new JLabel(telemannIcon),2);
		    TemperamentStudio.frame.pack();
		    TemperamentStudio.frame.repaint();
		    break;
		default: 
		    ArrayList<String> comments = new ArrayList<String>(Arrays.asList("You are really persistent!","Don't you have better things to do?","Du-dup-du-dup-du-dup that's all folks!","Here we go again!","Gotta catch 'em all, eh?","Really, this isn't the best part of this program!","Get back to your homework!","If you're looking for things to do, click on the \"things to try\" button!","Don't you think we've soiled the memory of Baroque composers enough?"));
		    TemperamentStudio.displaymessage((String)comments.get(randgen.nextInt(comments.size())),false,telemannIcon);
		}
		konamistring = "xxxxxxxxxx";
		doingbachevent = false;

	    }
	    // if we get a key released, go through notes and turn them off if appropriate
	} else if(e.getID() == KeyEvent.KEY_RELEASED){
	    keysDown.remove(keycode);
	    for(int i = 0; i < TemperamentStudio.kbPanel.allkeychars.length; i++){
		if(Character.toLowerCase(e.getKeyChar()) == TemperamentStudio.kbPanel.allkeychars[i]){
		    if(!TemperamentStudio.kbPanel.getSustain()){
			TemperamentStudio.kbPanel.stopNote(TemperamentStudio.kbPanel.allkeynotes[i]);
		    }
		    break;
		}
		    }
	    
	}
	return false;
    }

    // play a midi file loaded from the jar file
    public void playTheme(String filename){
	TemperamentStudio.kbPanel.allOff();
	try{
	    Thread.sleep(300);
	    InputStream in = TemperamentStudio.class.getResourceAsStream(filename);
	    Sequence seq = MidiSystem.getSequence(in);
	    seq = RetuneMIDI.retunedSequence(seq, 2, "qc_meantone",2,true,-1,true,9,440.0,0,0);

	
	    TemperamentStudio.startSequence(0,seq);
	    in.close();
	} catch (Exception ea) {
	    return;
	}
	
    }   
}

