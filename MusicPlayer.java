/*
 * This program is a simple implementation of a single-channel MIDI player
 * A String is supplied into the program:
 * The String contains characters that belong to one of these three categories
 * 	- Pitch adjuster: sharp ('#') or flat ('!') NOTE: MUST BE PLACED AFTER A NOTE CHARACTER
 * 	- Note character: Default letter notes i.e. C, D, E, F, G, A, B
 *  - Octave modifier: Increase by 1 octave = '<' or decrease by 1 octave '>'
 *  					NOTE ON USAGE: Entering one these characters will shift the
 *  - Pauses: instantiates a Note object with the silent attribute set to true
 *  - Numbers: indicate the duration of the succeeding (single) note
 * In the main method you will find a String called notestring which is loaded onto a MidiTrack Object
 * The program then creates MidiEvent objects based on the MidiNote objects present in the notestring
 * It then adds these events to the Track object, which is the loaded into the Sequence object
 * which in turn is played by the Sequencer object.
 * 
 *  
 *  This program is inspired by one of the assignments for one of my programming classes. My motivation
 *  behind writing this program is to explore new ways in which the Java programming language can be used
 *  and also to inquire into the interplay between programming and music technology.
 * 
 */
import java.util.ArrayList;
import java.util.Hashtable;

import javax.sound.midi.*;
public class MusicPlayer {
	
	Sequencer mySequencer;
	Instrument[] instrumentList;
	Synthesizer synth;
	Track myTrack;
	Sequence mySequence;
	int BPM;
	
	public MusicPlayer(){
		try{
			synth = MidiSystem.getSynthesizer();
			synth.open();
			instrumentList = synth.getAvailableInstruments();
			
			//false so we can link the sequencer to the synthesizer we just obtained above
			//i.e. sequencer's transmitter not linked to default synthesier's receiver
			//automatically
			//this allows us to tell our particular synthesizer to e.g. load certain instruments
			//and have the sequencer play that specific instrument
			mySequencer = MidiSystem.getSequencer(false);
			if(mySequencer != null)mySequencer.open();
			//the Receiver object belong to the Synthesizer object
			Receiver toSet = synth.getReceiver();
			//set toSet as the Receiver for the Transmitter of our Sequencer
			
			
			
			mySequencer.getTransmitter().setReceiver(toSet);
			
			//We want 1 pulse-per-quarter as the basic resolution
			mySequence = new Sequence(Sequence.PPQ,1);
			
			myTrack = mySequence.createTrack();//create empty track in our Sequence
			//myTrack now contains end-of-track event
			
			
		}
		catch(Exception e){
			System.out.println(e);
		}
		
	}
	
	public void loadSingleTrack(MidiTrack midiTrack){
		changeInstrument(midiTrack.instrumentID);
        this.BPM = midiTrack.BPM;
        int currentTick = 1;
        for (int i = 0;i<midiTrack.notes.size();i++) {
        	System.out.println(myTrack.size());
            MidiNote note = midiTrack.notes.get(i);
        	if (!note.isSilent()){
        		makeMidiEvent(ShortMessage.NOTE_ON, 
            			note.getPitch(), note.getVolume(), currentTick);
        		makeMidiEvent(ShortMessage.NOTE_OFF, 
                		note.getPitch(), note.getVolume(), currentTick + note.getDuration());
            }
            currentTick+=note.getDuration();
        }
    }
	
	public void changeInstrument(int ID){
		Instrument instrument  = instrumentList[ID];
		synth.loadInstrument(instrument);
		
		//Find out where in memory instrument is loaded
		int bankLocation = instrument.getPatch().getProgram();

		//instrument change
		MidiChannel[] midiChannels = this.synth.getChannels();
		midiChannels[0].controlChange(0, bankLocation);
		midiChannels[0].programChange(bankLocation);
	}
	
	public void makeMidiEvent(int messageType, int data1, int data2, long tick) {
        ShortMessage toAdd = new ShortMessage();
        try {
            toAdd.setMessage(messageType, 0, data1, data2);//single channel  hence the 0
            //for messages such as program_change data2 is ignored (only 1 data byte)
            this.myTrack.add(new MidiEvent(toAdd, tick));
        } catch (Exception e) {
            System.out.println(e);
        }
    }
	
	public void play(){
		try{
			if(!mySequencer.isOpen()) mySequencer.open();
			mySequencer.setSequence(mySequence);
			mySequencer.setTempoInBPM(this.BPM);
		}
		catch(Exception e){
			System.out.println(e);
		}
		mySequencer.start();
		
		long noOfTicks = mySequence.getTickLength(); 
		
		long waitTime = (long)(60000*noOfTicks/mySequencer.getTempoInBPM()) + 500;
		
		//make program sleep while Sequencer plays sequence
		try{
			Thread.sleep(waitTime);
		}
		catch(Exception e){
			System.out.println(e);
		}
	}
	
	public void stop(){
		mySequencer.close();
		synth.close();
	}
	
	public static void main(String[] args){
		MidiTrack newTrack = new MidiTrack("name",180,25);
		String notestring = ">>b<f#>b<3d>g<f#>g<3d>b<f#>b<3d>g<f#>g<3d3g";
		newTrack.loadNoteString(notestring);
		System.out.println(newTrack.notes);
		MusicPlayer playMusic = new MusicPlayer();
		playMusic.loadSingleTrack(newTrack);
		System.out.println(playMusic.myTrack.size());
		for(int i = 0;i<playMusic.myTrack.size();i++){
			System.out.println(playMusic.myTrack.get(i));
		}
		playMusic.play();
		playMusic.stop();
		
		
		
	}
}
class MidiTrack{
	String name = "My Song";
	int BPM = 180;
	ArrayList<MidiNote> notes = new ArrayList<MidiNote>();
	int instrumentID = 1;//default is Piano 1
	
	Hashtable<Character, Integer> pitchDictionary;

	
	public MidiTrack(String s, int BPM, int ID){
		this.name = s;
		this.BPM = BPM;
		this.instrumentID = ID;
		pitchDictionary  = new Hashtable<Character, Integer>();
        pitchDictionary.put('C', 60);
        pitchDictionary.put('D', 62);
        pitchDictionary.put('E', 64);
        pitchDictionary.put('F', 65);
        pitchDictionary.put('G', 67);
        pitchDictionary.put('A', 69);
        pitchDictionary.put('B', 71);
	}
	
	 
    public void loadNoteString(String notestring){
        notestring = notestring.toUpperCase();
        int duration = 0;
        int pitch = 60;//we set the pitch to its default value 60
        int octave = 0;
        //we then traverse through the String input using a for loop
        for(int i=0;i<notestring.length();i++)
        {
          MidiNote addNote = new MidiNote(pitch,1);
          //if we encounter  a < or a >, the octave is modified accordingly for the
          //subsequent notes
          
          while(notestring.charAt(i)=='<' || notestring.charAt(i)=='>')
          {
            if(i==notestring.length()-1)
            {
              return;
            }
            if(notestring.charAt(i)=='<')
            {
              octave+=12;
              
            }
            else
            {
              octave-=12;
              
            }
            i++;
          }
          
         
          if(notestring.charAt(i)-48<10&& notestring.charAt(i)-48>=0 )
          {
            //if we encounter a number, we use the following while loop to concatenate
            //the digits into the empty string below
            String stringDuration = "";
            
            
            while(notestring.charAt(i)-48<10 && notestring.charAt(i)-48>=0 )
            {
              stringDuration+=""+(notestring.charAt(i)-48);
              i++;
              //the while loop stops executing if we encounter a character that is not a
              //number between 0 and 9 (both endpoints inclusive)
            }

            //we then set the duration of the note equal to the integer value of the
            //stringDuration variable
            addNote.setDuration(Integer.parseInt(stringDuration));
          }
          if(notestring.charAt(i)=='P')
          {
            
            //if we encounter a P, we set the note to be silent
            addNote.setSilent(true);
            
            notes.add(addNote);//we then add it to the arraylist
            
            continue;//we then disregard the succeeding lines and re-start the for loop
          }
          int sharpFlat = 0;//this variable is used to adjust the pitch value (i.e. sharp/flat)
          if(i+1<notestring.length())
          {
            //if the next character is a sharp or a flat indicator, we change the sharpFlat variable
            //accordingly
            if(notestring.charAt(i+1)=='#')
            {
              sharpFlat=1;
            }
            if(notestring.charAt(i+1)=='!')
            {
              sharpFlat = -1;
            }
          }
          if(notestring.charAt(i)=='#' || notestring.charAt(i)=='!')
          {
            //when we encounter a # or a ! at position i, we ignore it
            //this is because it had already been checked when we looked at the 
            //character before it
            continue;
          }
          //we then set the pitch of our note as the sum of the contributions from the 
          //octave, the sharpFlat pitch adjuster variable and the character at index i
          addNote.setPitch(pitchDictionary.get(notestring.charAt(i)) + octave+ sharpFlat);
          
          notes.add(addNote);
        }

    }
	
}
class MidiNote{
	private int duration = 1;//default duration
	//private so getter/setter must be used
	private int pitch = 60;//This is the pitch of a C3 note
	boolean silent = false;//default value
	private int volume = 50;
	
	public MidiNote(int pitch, int duration){
		this.setPitch(pitch);
		this.setDuration(duration);
	}
	
	public void setDuration(int duration){
		if(duration<0){
			return;//do nothing
		}
		this.duration = duration;
	}
	
	public void setPitch(int newPitch){
		if(newPitch < 21){
			this.pitch = 21;
			System.out.println("Given pitch is out of range. Pitch set to 21");
		}
		else if(newPitch > 108){
			this.pitch = 108;
			System.out.println("Given pitch is out of range. Pitch set to 108");
		}
		else{
			this.pitch = newPitch;
		}
	}
	
	public void setSilent(boolean b){
		this.silent = b;
	}
	
	public int getPitch(){
		return this.pitch;
	}
	
	public boolean isSilent(){
		return silent;
	}
	
	public int getDuration(){
		return this.duration;
	}
	
	public int getVolume(){
		return this.volume;
	}
	
	public String toString(){
		return "["+this.duration+" "+this.pitch+" "+this.silent+"]";
	}
}
