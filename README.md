# MIDI-Based-Music-Player
This program is a simple implementation of a single-channel MIDI player
A String is supplied into the program:
The String contains characters that belong to one of these three categories
- Pitch adjuster: sharp ('#') or flat ('!') NOTE: MUST BE PLACED AFTER A NOTE CHARACTER
- Note character: Default letter notes i.e. C, D, E, F, G, A, B
- Octave modifier: Increase by 1 octave = '<' or decrease by 1 octave '>'
 					NOTE ON USAGE: Entering one these characters will shift the octave of all the characters after it.
- Pauses: instantiates a Note object with the silent attribute set to true
- Numbers: indicate the duration of the succeeding (single) note
In the main method you will find a String called notestring which is loaded onto a MidiTrack Object
The program then creates MidiEvent objects based on the MidiNote objects present in the notestring
It then adds these events to the Track object, which is the loaded into the Sequence object
which in turn is played by the Sequencer object.
  
  
This program is inspired by one of the assignments for one of my programming classes. My motivation
behind writing this program is to explore new ways in which the Java programming language can be used
and also to inquire into the interplay between programming and music technology.
