package org.skarph.discordBF;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
	public static final String ver = "1.0.0";
	public final static int OUTPUT_MAX = 2000; //discord max message length
			
	public static void main(final String[] args) {
		System.out.println("Discord Brainfuck Version "+ver);
		System.out.println("https://github.com/skarph/DiscordBrainFuck");
		System.out.println();
		if(args.length < 1) {
			System.err.println("[ERROR] Please provide the Bot's Token");
			System.exit(1);
		}
		
		//assume first arg is key
		final DiscordClient client = DiscordClient.create(args[0]);
		final GatewayDiscordClient gateway = client.login().block();
		
		System.out.println("!!Awaiting Messages!!");
		//message reactor
		gateway.on(MessageCreateEvent.class).subscribe(event -> {
			final Message message = event.getMessage();			
			if(message.getAuthor().get().equals(gateway.getUsers().blockFirst())) //no self-echoing
				return;
			final String text = message.getContent();
			
			//check if bf codeblock is present; separate code & input
			Pattern codeRegex = Pattern.compile("(?<=`bf)[^`]+(?=`)");
			Matcher m = codeRegex.matcher(text);
			if (!m.find()) 
				return;
			String code = m.group(0);
			String input = "";
			int inStart = m.end(0)+1; //input start
			try {
				if(text.charAt(inStart-1) == '`' && text.charAt(inStart) == '`' && text.charAt(inStart+1) == '`') //skip after ``` if present
					inStart += 2;
				if( Character.isWhitespace(text.charAt(inStart)) ) //if the first character is whitespace, it's probably used as a deliminator 
					inStart++;
				input = text.substring(inStart);
			} catch (StringIndexOutOfBoundsException e) {} //if OOB we cant move the start of string pointer any further
			
			//initialize the brainfuck VM, and execute the code with the input
		   	BrainfuckMachine bfm = new BrainfuckMachine(code,input);
		   	String output;
		   	String error = "";
		   	String response = "";
		   	try{
		   		output = bfm.run();
		   	}catch(Exception e) {
		   		e.printStackTrace();
		   		Pattern errRegex = Pattern.compile("\\d+(?=«)");
		   		m = errRegex.matcher(e.getMessage().toString());
		   		m.find(); //guaranteed to match the index of the character that caused the code, see BrainfuckMachine for exceptions
		   		int erri = Integer.parseInt(m.group(0));
		   		code = code.substring(0,erri) + "»»»" + code.substring(erri,erri+1) + "«««" + code.substring(erri+1); //highlight the offending character
		   		error = "ERROR: `"+e.getMessage()+"`\n DUMP: ```"+bfm.dumpStr()+"```";
		   		output = bfm.getOutput();
		   	}
		   	
		   	//console output
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd|HH:mm:ss");  
		   	LocalDateTime now = LocalDateTime.now();
		   	response = "Code: ```bf\n"+code+"```\n"+error+"\nInput: " + (input.isEmpty() ? "" : "`"+input.replaceAll("`","\\\\`")+"`") + "\nOutput: " + output;
		   	System.out.println("\n=="+message.getAuthor().get().getTag()+" @ "+dtf.format(now)+" ==");
		   	if(response.length() < OUTPUT_MAX || !error.isEmpty())
		   		System.out.println(response);
		   	else {
		   		System.out.println("Code: ```bf\n"+code+"\nInput: " + (input.isEmpty() ? "" : "`"+input.replaceAll("`","\\\\`")+"`"));
		   		System.out.println("\n<response shrunk>");
		   	}
		   	
			//discord message output
			final MessageChannel channel = message.getChannel().block();
			if(response.length() > OUTPUT_MAX) {
				splitSendBlocking("Code: ```bf\n"+code+"```",channel,OUTPUT_MAX);
				if(!error.isEmpty())
					splitSendBlocking(error,channel,OUTPUT_MAX);
				splitSendBlocking("Input: " + (input.isEmpty() ? "" : "`"+input.replaceAll("`","\\\\`")+"`"),channel,OUTPUT_MAX);
				splitSendBlocking("Output: " + output,channel,OUTPUT_MAX);
			} else
				splitSendBlocking(response, channel, OUTPUT_MAX);
			System.out.println("Finished response to "+message.getAuthor().get().getTag()+" @ "+dtf.format(now));
		});
		
		gateway.onDisconnect().block();
	}
	
	//(blocking) puts string [msg] in [channel] split up into [size]-character messages
	public static void splitSendBlocking(String msg, MessageChannel chn, int size) {
		if(msg.length() > size) {
			chn.typeUntil(chn.createMessage(msg.substring(0,size))).blockLast(); //use typeUntil so the user knows the bot didnt stop randomly
			splitSendBlocking(msg.substring(size),chn,size);
		}else {
			chn.createMessage(msg).block();
		}
	}
}
