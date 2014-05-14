/************************

RetuneMIDI.java

This class is part of
TemperamentStudio
(c) 2014 Dallin S. Durfee
This code may be modified and redistributed
under the MIT license

This is the heart of the program -
the function that generates a re-tuned
midi sequence.

******************/


import javax.sound.midi.Sequence;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Track;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MetaMessage;
import java.io.IOException;

public class RetuneMIDI{
    /* This is the one - the function that retunes midi!
       It works by putting all of the C notes on channel 0, all of the C#'s on channel 1, etc.,
       and then applying the appropriate pitch bend to each channel to realize the requested
       tuning scheme.
    */
    
    // used to debug Java tempo issue
    public static void printTimeSignature(byte[] thebytes, long thetime){
	System.out.print(thetime+": ");
	printTimeSignature(thebytes);
    }
    public static void printTimeSignature(byte[] thebytes){
	System.out.print("Time Signature: ");
	System.out.print((int)thebytes[0]+"/"+Math.pow(2,(int)thebytes[1]));
	System.out.print("  Clock ticks/beat: "+(int)thebytes[2]);
	System.out.println("  32nd notes per beat: "+(int)thebytes[3]);
    }
    public static void printTempoChange(byte[] thebytes, long thetime){
	System.out.print(thetime+": ");
	printTempoChange(thebytes);
    }
    public static void printTempoChange(byte[] thebytes){
	System.out.print("Tempo Change: ");
	int v = (((int)thebytes[0]&0xFF)<<16) | ((((int)thebytes[1]&0xFF))<<8) | (((int)thebytes[2]&0xFF)<<0);
	System.out.println("BPM = "+(1.0e6*60/(double)v));
    }

    public static Sequence retunedSequence(Sequence midiIn, int rootnote, String temperamentstring, double maxpitchbend, Boolean writemaxpitchbend, int selectProgram, Boolean addmetas, int tuningroot, double tuningfreq, int transposeamt, long tickpos)  throws MidiUnavailableException, InvalidMidiDataException, IOException {
	// midiIn is the midi sequence we're going to retune
	// rootnote is the root note of the tuning scheme
	// temperamentstring is the name of the tuning scheme
	// maxpitchbend is the maximum number of semitones the synthesizer can pitch bend by
	//    normally this is 2.0, meaning +/- 2 semitones
	// writemaxpitchbend - if this is true, midi events will be included to set the pitch bend range of the synthesizer
	// selectProgram - if this is -1, all program changes in midiIn will be echoed to all channels
	//    If it is -2, only the first program change found will be echoed to all channels, the rest will be ignored
	//    If it is a number from 0 to 127, it will use that program on all channels and ignore all program changes in midiIn
	// addmetas - if this is true, a meta event will be inserted with each note on or note off event.  These events
	//    are listened to by TemperamentStudio and used to add markers to the virtual musical keyboard to show which notes
	//    are being played in a sequence.  I had to do this, because Java doesn't have listeners for note on or note off,
	//    which is really lame if you ask me.
	// tuningroot - The note that the instrument will be tuned to.  This is different from the root of the tuning scheme.
	//    The root of the tuning scheme is the note we start with when applying intervals to get all of the notes in a
	//    chromatic scale.  The tuningroot is a note that we want to be at a specific frequency - A440 for an example.
	//    So the instrument is tuned to the tuning scheme, and then an offset is applied to all of the notes to shift the
	//    tuning scheme such that the tuningroot note is at the tuningfreq frequency.
	// tuningfreq - see the previous item
	// transposeamt - a number of half-steps by which to transpose the music.
	// tickpos - this is where to put the "beginning of file" commands.
	//    tickpos is normally zero - except when you jump to a point in the song,
	//    JAVA seems to reset controllers.  So we'll set the controllers
	//    right where we are jumping to.

	int pitchshift[] = {0,0,0,0,0,0,0,0,0,0,0,0};
	// load the pitch shift values for each channel
	try{
	    pitchshift = TemperamentStudio.tuningSchemes.getPitchShifts(temperamentstring,maxpitchbend, rootnote, tuningroot, tuningfreq);
	} catch (Exception e){
	    TemperamentStudio.displayerror(e.getMessage());
	    return(null);
	}
	
	// get the tracks from the midi file
	Track[] tracks = midiIn.getTracks();
	if(tracks.length == 0){
	    TemperamentStudio.displayerror("Input file does not appear to contain any tracks!");
	    return(null);
	}
	
	// set up the new midi sequence
	// I make the tempo track first - Java wants all tempo events
	// on one track or the tempo doesn't get set
	Track tempoTrack = null;
	Sequence newMidi = null;
	try{
	    newMidi = new Sequence(midiIn.getDivisionType(),midiIn.getResolution());
	    tempoTrack = newMidi.createTrack();
	} catch (Exception e){
	    return(null);
	}
	
	// this will be used to make midi events on new midi sequenc
	ShortMessage sm = null;

	// this tracks if a program (instrument) has been set
	Boolean progset = false;
	
	// If there is a problem with notes being transposed out of the midi range,
	// we only want to send an error notice the first time we notice.  We'll
	// use the following variable to track whether we've sent an error notice.
	Boolean senttransposemessage = false;
	// go through all of the tracks in the input file
	for(int ntrack = 0; ntrack < tracks.length; ntrack++){

	    // Make a new track in the new midi sequence where we can store events from the corresponding track in the input file
	    Track thisTrack = newMidi.createTrack();
	    
	    // put pitch bends and program change onto every channel in this track to achieve correct tuning
	    int realchannel=0;
	    int fromroot=0;
	    int nchannel;
	    for(nchannel = 0; nchannel < 12; nchannel++){
		// midi channel 10 is reserved for percussion - but the midi "parlance" numbers the channels from 1, while the file
		// format numbers them from 0.  So in this code the percussion channel is really 9.  We want to avoid that one, so
		// for any channel greater than 8, we will go up a channel (thereby using channels 0,1,2,3,4,5,6,7,8,10,11)
		realchannel=nchannel;
		if(nchannel > 8){
		    realchannel = realchannel+1;
		}
		// set program
		if(selectProgram >= 0){
		    sm = new ShortMessage();
		    sm.setMessage(ShortMessage.PROGRAM_CHANGE, realchannel,selectProgram,0 );
		    thisTrack.add(new MidiEvent(sm,tickpos));			
		    progset = true;
		}


		// We're going to assume that channel 0 has all of the c's, channel 1 has all of the c#'s, etc.
		// But our array of pitch shifts start at the root.  If the root we selected isn't c, then we don't
		// want to use pitchshift[0] for channel 0.  The variable "fromroot" tells us how many half steps 
		// the notes on the current channel are from the root note, so we can use the correct pitch shift.
		fromroot = nchannel - rootnote;
		if(fromroot < 0){
		    fromroot = fromroot + 12;
		}
		// Midi pitch shifts are written in two 7-bit bytes.  The code below calculates those two bytes from the pitch shift number
		int pitchshiftlsb = pitchshift[fromroot] & 0x7F;
		int pitchshiftmsb = (pitchshift[fromroot]>>7) & 0x7F;
		// Write the correct pitch shift value to the midi channel
		if(writemaxpitchbend){
		    // Set the pitch bend range
		    sm = new ShortMessage();
		    sm.setMessage(ShortMessage.CONTROL_CHANGE, realchannel, 101,0);
		    thisTrack.add(new MidiEvent(sm,tickpos));
		    sm = new ShortMessage();
		    sm.setMessage(ShortMessage.CONTROL_CHANGE, realchannel, 100,0);
		    thisTrack.add(new MidiEvent(sm,tickpos));
		    sm = new ShortMessage();
		    sm.setMessage(ShortMessage.CONTROL_CHANGE, realchannel, 6,(int)maxpitchbend);
		    thisTrack.add(new MidiEvent(sm,tickpos));
		    sm = new ShortMessage();
		    sm.setMessage(ShortMessage.CONTROL_CHANGE, realchannel, 38,0);
		    thisTrack.add(new MidiEvent(sm,tickpos));
		    // reset controllers 101 and 100 so that future writes to controller 6 don't change pitch bend range
		    sm = new ShortMessage();		    
		    sm.setMessage(ShortMessage.CONTROL_CHANGE, realchannel, 100,127);
		    thisTrack.add(new MidiEvent(sm,tickpos));
		    sm = new ShortMessage();
		    sm.setMessage(ShortMessage.CONTROL_CHANGE, realchannel, 101,127);
		    thisTrack.add(new MidiEvent(sm,tickpos));
		}
		sm = new ShortMessage();
		sm.setMessage(ShortMessage.PITCH_BEND, realchannel, pitchshiftlsb, pitchshiftmsb);
		thisTrack.add(new MidiEvent(sm,tickpos));	
	    }
	    
	    // go through each event in the current midi track in the input file, adjust the event, and put it into the new track
	    for(int nevent = 0; nevent < tracks[ntrack].size(); nevent++){	   
		MidiEvent thisevent = tracks[ntrack].get(nevent);
		MidiMessage message = thisevent.getMessage();
                int statusInt = (int)message.getStatus();
                int channel = (statusInt & 0x000F)+1;
		int note = 0;

		// Each event has an event type.  What we do with it depends on what type of event it is.
		int eventtype = (statusInt & 0x00F0)>>4; 
		int newchannel = 0;
                switch (eventtype) {
		    // First deal with events which only affect one note
		    // these are the midi events which only affect one note
		    // hex   midi event        param 1              param 2
		    // 0x8 = note off          note number          velocity
		    // 0x9 = note on           note number          velocity
		    // 0xA = note aftertouch   note number          aftertouch value
		    
		case 0x8:
		    // note off       
		    note = (int)message.getMessage()[1] & 0xFF;
		    note = note + transposeamt;
		    if((note < 0)||(note > 127)){
			if(!senttransposemessage){
			    TemperamentStudio.displayerror("Transposing this piece puts notes out if midi range.  Generating garbage until you use a smaller transpose amount.");
			    senttransposemessage = true;
			}
			note = 0;
		    }
		    sm = new ShortMessage();
		    // find the new channel based on which note is being turned off, stored in byte 1 of the message
		    // uses modulo 12, because we don't care what it's octave is
		    newchannel = note % 12;
		    // move channels 10 and above to avoid using channel 10, which is reserved for percussion
		    // note that the channels are colloquially labeled 1-16, but the actual values in the midi file go from 0-15
		    // so the channel we are avoiding, channel 10, is actually channel 9
		    if(newchannel > 8){
			newchannel = newchannel + 1;
		    }
		    // add adjusted event to new track
		    sm.setMessage(ShortMessage.NOTE_OFF, newchannel, note, ((int)message.getMessage()[2] & 0xFF));
		    thisTrack.add(new MidiEvent(sm,thisevent.getTick()));			
		    if(addmetas){
			String ms = "off:"+Integer.toString(note);
			byte[] ba = ms.getBytes();
			MetaMessage mm = new MetaMessage(TemperamentStudio.metaCuePoint,ba,ba.length);
			thisTrack.add(new MidiEvent(mm,thisevent.getTick()));
		    }
		    break;
		case 0x9:
		    // note on
		    // see notes for note off above
		    sm = new ShortMessage();
		    note = ((int)message.getMessage()[1] & 0Xff);
		    note = note + transposeamt;
		    if((note < 0)||(note > 127)){
			if(!senttransposemessage){
			    TemperamentStudio.displayerror("Transposing this piece puts notes out if midi range.  Generating garbage until you use a smaller transpose amount.");
			    senttransposemessage = true;
			}
			note = 0;
		    }

		    newchannel = note % 12;
		    if(newchannel > 8){
			newchannel = newchannel + 1;
		    }
		    sm.setMessage(ShortMessage.NOTE_ON, newchannel, note, ((int)message.getMessage()[2] & 0xFF));
		    thisTrack.add(new MidiEvent(sm,thisevent.getTick()));
		    if(addmetas){
			String ms;
			if( ((int)message.getMessage()[2] & 0xFF) == 0)
			    ms = "off:"+Integer.toString(note);
			else
			    ms = "on:"+Integer.toString(note);
			byte[] ba = ms.getBytes();
			MetaMessage mm = new MetaMessage(TemperamentStudio.metaCuePoint,ba,ba.length);
			thisTrack.add(new MidiEvent(mm,thisevent.getTick()));
		    }
		    break;
		case 0xA:		      
		    // note aftertouch
		    // see notes for note off above
		    sm = new ShortMessage();
		    note = ((int)message.getMessage()[1] & 0Xff);
		    note = note + transposeamt;
		    if((note < 0)||(note > 127)){
			if(!senttransposemessage){
			    TemperamentStudio.displayerror("Transposing this piece puts notes out if midi range.  Generating garbage until you use a smaller transpose amount.");
			    senttransposemessage = true;
			}
			note = 0;
		    }
		    newchannel = note % 12;
		    if(newchannel > 8){
			newchannel = newchannel + 1;
		    }
		    sm = new ShortMessage();
		    sm.setMessage(ShortMessage.POLY_PRESSURE, newchannel, note, ((int)message.getMessage()[2] & 0xFF));
		    thisTrack.add(new MidiEvent(sm,thisevent.getTick()));
		    break;
		    
		    // Now we'll deal with events that affect the entire channel.  First we need to stop any pitch bend from being copied over
		    // since that will mess up our tuning.
		case 0xE:
		    // pitch bend - we won't copy this one over
		    break;
		    
		    // Now we'll deal with other channel-wide events
		    // hex   midi event          param 1              param 2
		    // 0xB = controller change   controller           value
		    // 0xC = program change      new program          ---
		    // 0xD = channel pressure    new pressure         ---
		    
		    // since we're moving notes into different channels, it's probably best to 
		    // duplicate these events onto all of the channels used (0,1,2,3,4,5,6,7,8,10,11,12).
		    
		    
		case 0xC:
		    if(progset){
			break;
		    }
		    if(selectProgram == TemperamentStudio.progUseFirst){
			progset = true;
		    }
		    // if progset wasn't true, we'll go on to copy the event for all channels, just
		    // like we're going to do for controller change and channel pressure
		case 0xB:
		case 0xD:
		    // copy event to all channels
		    for(nchannel = 0; nchannel < 12; nchannel++){
			realchannel = nchannel;
			if(realchannel > 8){
			    realchannel++;
			}
			sm = new ShortMessage();
			if(message.getMessage().length < 3){
			    sm.setMessage(eventtype<<4, realchannel, ((int)message.getMessage()[1] & 0xFF), 0);
			}
			else{
			    sm.setMessage(eventtype<<4, realchannel, ((int)message.getMessage()[1] & 0xFF), ((int)message.getMessage()[2] & 0xFF));
			}
			thisTrack.add(new MidiEvent(sm,thisevent.getTick()));			
		    }
		    break;
		    
		    // Anything else must not be channel specific, so we'll just copy it over
		default:
		    // put timing in separate track
		    // Java doesn't apply tempo changes unless they are
		    // on a separate track (I think it needs to be the first
		    // track).
		    // It would appear that no matter what format the file
		    // you loaded was in, it assumes a type one midi file
		    // with multiple tracks, and all tempo info in the first
		    // one.  So here we force the data to be type 1 by
		    // moving all tempo info to track one.
		    byte[] thebytes = thisevent.getMessage().getMessage();
		    if(thebytes[0] == (byte)0xFF){
			boolean eventwritten = false;
			if(thebytes[1]==(byte)0x51){
			    byte[] messagebytes = {thebytes[3],thebytes[4],thebytes[5]};
			    //printTempoChange(messagebytes,thisevent.getTick());
			    tempoTrack.add(thisevent);		   
			    eventwritten = true;
			}
			if(thebytes[1]==(byte)0x58){
			    byte[] messagebytes = {thebytes[3],thebytes[4],thebytes[5],thebytes[6]};
			    //printTimeSignature(messagebytes,thisevent.getTick());
			    tempoTrack.add(thisevent);		   
			    eventwritten = true;
			}
			if(!eventwritten){
			    thisTrack.add(thisevent);
			}
		    }
		    else{
			thisTrack.add(thisevent);		   
		    }
		    break;
		}
	    }	
	}
	return(newMidi);
    }
}
