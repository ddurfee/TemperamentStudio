/************************

PauseButton.java

This class is part of
TemperamentStudio
(c) 2014 Dallin S. Durfee
This code may be modified and redistributed
under the MIT license

This class implements a pause
button that can pause and restart
a midi sequence.  Pausing
really stops the sequence, and
unpausing really makes a new
sequence with the tuning events
right where we're about to start,
and then begins playback where
we stopped before.

******************/

import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.sound.midi.Sequencer;


public class PauseButton extends JButton implements ActionListener {
    private static long pausePos = 0;
    private static String pausestring,continuestring;

    PauseButton(String pstring, String cstring){
	pausestring = pstring;
	continuestring = cstring;
	setText(pausestring);
	addActionListener(this);
    }
    public void actionPerformed(ActionEvent e){
	if(TemperamentStudio.theSequencer.isRunning()){
	    pausePos = TemperamentStudio.theSequencer.getTickPosition();
	    TemperamentStudio.theSequencer.stop();
	    TemperamentStudio.progressTimer.stop();
	    setText(continuestring);
	    TemperamentStudio.kbPanel.repaint();
	}
	else{
	    TemperamentStudio.startSequence(pausePos);
	    //theSequencer.start();
	    TemperamentStudio.progressTimer.start();
	    setText(pausestring);
	}      
    }
}
