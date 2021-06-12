package org.skarph.discordBF;

import java.text.NumberFormat;
import java.util.HashMap;

class EndOfCodeException extends Exception {
	EndOfCodeException(String msg){
		super(msg);
	}
}

class MaxLoopsExceededException extends Exception {
	MaxLoopsExceededException(String msg){
		super(msg);
	}
}


public class BrainfuckMachine {
	public final static int LOOP_MAX = 1000000; //maximum amount of jumps backward ( "]") that can be performed in a single execution
	public final static int DUMP_SIZE = 512; //maximum amount of cells to dump
	
	private char[] sourceCode; //source code to execute from
	private HashMap<Integer, Byte> tape; //machine's tape; minimizes space as opposed to an array or arraylist
	private String in; //program input
	private int cptr = 0; //code pointer
	private int tptr = 0; //tape pointer
	private int backJumps = 0; //current number of jumps backwards, compared to LOOP_MAX
	private String out = ""; //output
	
	public BrainfuckMachine(String strCode, String strIn) {
		sourceCode = strCode.toCharArray();
		if(strIn==null)
			in = "";
		in = strIn;
		tape = new HashMap<>();
	}
	
	/*
	 * Executes the instruction at the current pointer and then moves increments the pointer right.
	 * Non-valid operators are ignored and treated as comments
	 * returns false if end of sourceCode is reached
	 */
	public boolean step() throws Exception{
		if(cptr>=sourceCode.length) //THIS SHOULD NEVER HAPPEN. HOPEFULLY.
			throw new EndOfCodeException("Code pointer is at ["+cptr+"], but the code ends at ["+(sourceCode.length-1)+"]. Please report this bug!");
		
		Byte v = tape.get(tptr);
		v = v == null ? 0 : v; //default to 0 if null
		int bs = 1; //bracket stack size, used to match brackets
		
		switch(sourceCode[cptr]) { //instruction parse
		
		case '>': //tape left
			tptr++;
			break;
			
		case '<': //tape right
			tptr = Math.max(tptr-1,0);
			break;
			
		case '+': //cell increment
			tape.put(tptr, ++v);
			break;
			
		case '-': //cell decrement
			tape.put(tptr, --v);
			break;
			
		case '.': //write
			out = out + (char) (byte)v; //has to be Byte --> byte --> char
			break;
		
		case ',': //read
			v = 0;
			if(!in.isEmpty()) {
				v = (byte) in.charAt(0);
				in = in.substring(1);
			}
			tape.put(tptr,v);
			break;
			
		case '[': //goto matching ] if 0
			if(v!=0)
				break;
			while( (cptr < sourceCode.length) && bs!=0 ) {
				char ins = sourceCode[++cptr];
				if(ins == '[') {
					bs++;
				}else if(ins== ']') {
					bs--;
				}
			}
			break;
			
		case ']': //goto matching [ if non-0
			if(v==0) //0 check
				break;
			backJumps++; //only increment on successful jump
			if(backJumps == LOOP_MAX)
				throw new MaxLoopsExceededException("»»»"+sourceCode[cptr]+"."+cptr+"««« (P)"+tptr+String.format(" (V)%02X",tape.get(tptr))+" Program exceeded more than "+NumberFormat.getInstance().format(LOOP_MAX)+" jumps backwards, this may be an infinte loop!");
			while( (cptr > 0) && bs!=0 ) {
				char ins = sourceCode[--cptr];
				if(ins == '[') {
					bs--;
				}else if(ins == ']') {
					bs++;
				}
			}
			cptr--;
			break;
			
		default:
			break;
			
		}
		
		cptr++;
		return cptr<sourceCode.length;
	}
	
	//executes all the instructions in sourceCode sequentually; returns the output of the code
	public String run() throws Exception{
		try {
			while(step()) {}
		} catch (EndOfCodeException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	//returns the current output
	public String getOutput() {
		return out;
	}
	
	//dumps the tape to a string with formatting
	public String dumpStr() {
		String dump = "";
		for(int i = 0;i<DUMP_SIZE;) {
			byte v = tape.get(i) == null ? 0 : tape.get(i);
			if(i==tptr)
				dump += String.format("»%02X«",v);
			else
				dump += String.format(" %02X",v);
			
			if(++i%16==0) //increment here instead of loop top
				dump+=" \n";
		}
		
		return dump;
	}

}
