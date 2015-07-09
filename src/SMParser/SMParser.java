package SMParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;


public class SMParser {
	private static String[] metaDataHeaders = {"offset:","title:","artist:", "displaybpm:", "music","samplestart:", "banner:", "background:", "subtitle:"};
	private String[] metadata;
	private ArrayList<JsonObjectBuilder> arrays;
	public SMParser(){
		
	}
	
	public void parseSmFile(File inputfile){
		try {
			String file = readFile(inputfile.getAbsolutePath(), Charset.defaultCharset());
			String[] mainsplit = file.split("\\#");
			arrays = new ArrayList<JsonObjectBuilder>();
			metadata = new String[metaDataHeaders.length];
			for(String s:mainsplit){
				//searching for the metadata
				int i = 0;
				for(String header:metaDataHeaders){
					if(s.toLowerCase().contains(header) && metadata[i] == null){
						metadata[i] = getValue(s).replace(';', ' ').trim();
					}
					i++;
				}
				//parsing the notes
				if(s.contains("NOTES")){
					String[] notesplit = s.split("\n");
					if(notesplit[1].contains("-single:")){ //valid note (single player mode)
						String difficulty = notesplit[3].replace(":", "").trim().toLowerCase();
						for(int k = 0; k < 6; k++){ //remove first 6 lines
							s = s.substring(s.indexOf('\n')+1);
						}
						s= s.replaceAll("//.*", ""); // remove some more bs
						s = s.replaceAll("\\;", "");
						
						notesplit = s.split("\\,"); //split everything in arrays which contains "beats" for 1 second
						double secondscounter = Double.parseDouble(metadata[0])*-1;
						int extraButtonTime = (notesplit.length / difficultyToMaxButtons(difficulty))*1000;
						int maxButtons = 1;						
						JsonArrayBuilder objectsArrayBuilder = Json.createArrayBuilder();
						JsonArrayBuilder buttonsArrayBuilder = Json.createArrayBuilder();
						buttonsArrayBuilder.add(Json.createObjectBuilder().add("time", 0).add("button", 1).add("color", 0));
						
						double lastNoteTime = 0;
						int objectscounter = 0;
						System.out.println(difficultyToButtonsTiming(difficulty) + "-" + difficulty);
						for(String notespersecond:notesplit){
							notespersecond = notespersecond.trim();
							String[] notes = notespersecond.split("\n");
							double precision = (1.0/notes.length)*1000;
							double time = secondscounter*1000;
							for(String note:notes){
								int direction = noteToDirection(note.trim());
								if(direction != -1){
									if(time - lastNoteTime >= difficultyToButtonsTiming(difficulty)){
										JsonObjectBuilder object = Json.createObjectBuilder();
										object.add("time", time);
										object.add("direction", direction);
										object.add("button", (int)(Math.random()*maxButtons+1));
										objectsArrayBuilder.add(object.build());
										lastNoteTime = time;
										objectscounter++;
									}
								}
								if(extraButtonTime*maxButtons < time && maxButtons < difficultyToMaxButtons(difficulty)){ //add a new/extra button
									buttonsArrayBuilder.add(Json.createObjectBuilder().add("time", time).add("button", maxButtons+1).add("color", maxButtons));
									maxButtons++;
								}
								time += precision;
							}
							secondscounter++;
						}
						if(objectscounter > 15)
						{
							arrays.add(Json.createObjectBuilder());
							arrays.get(arrays.size()-1).add("difficulty", difficulty);
							arrays.get(arrays.size()-1).add("objects", objectsArrayBuilder);
							arrays.get(arrays.size()-1).add("buttons", buttonsArrayBuilder);		
						}
					}
				};
			}
			for(int i = 0; i < metadata.length; i++){
				if(metadata[i] == null){
					metadata[i] = "0";
				}
			}
			writeJsonFile(inputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeJsonFile(File inputfile){
		JsonObjectBuilder jsonfilebuilder = Json.createObjectBuilder();
		JsonArrayBuilder dataArrayBuilder = Json.createArrayBuilder();
		
		double bpm = 0;
		double samples = 0;
		try{
			Double.parseDouble(metadata[3]);
		}catch(java.lang.NumberFormatException e){
			
		}
		try{
			Double.parseDouble(metadata[5]);
		}catch(java.lang.NumberFormatException e){
			
		}
		
		jsonfilebuilder.add("meta", Json.createObjectBuilder().add("title", metadata[1]).add("subtitle", metadata[8]).add("author", metadata[2]).add("BPM", bpm).add("sample_start", samples));
		jsonfilebuilder.add("file", Json.createObjectBuilder().add("audio", metadata[4]).add("background", metadata[7]).add("banner", metadata[6]));

		for(JsonObjectBuilder ar:arrays){
			dataArrayBuilder.add(ar.build());
		}
		jsonfilebuilder.add("data", dataArrayBuilder);
		
		try {
			String name = metadata[1].trim().replaceAll("[^a-zA-Z0-9.-]", "_")+metadata[8].trim().replaceAll("[^a-zA-Z0-9.-]", "_");;
			String dirname = "scannedsongs/"+name+(int)(Math.random()*10);
			File  f = new File(dirname);
			f.mkdir();
			try {
				Files.copy(new File(inputfile.getParent() + "/" + metadata[4]).toPath(), new File(dirname +"/"+metadata[4]).toPath());
			} catch (IOException e) {
				System.out.println("Music file not found");
				f.delete();
				return;
			}
			try {
				Files.copy(new File(inputfile.getParent() + "/" + metadata[6]).toPath(), new File(dirname +"/"+metadata[6]).toPath());
			} catch (IOException e) {
			}
			try {
				Files.copy(new File(inputfile.getParent() + "/" + metadata[7]).toPath(), new File(dirname +"/"+metadata[7]).toPath());
			} catch (IOException e) {
			}
			JsonObject file = jsonfilebuilder.build();
			OutputStream os;
			os = new FileOutputStream(dirname+"/"+name+".csf");
			JsonWriter jsonWriter = Json.createWriter(os);
			jsonWriter.writeObject(file);
			jsonWriter.close();		
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private int difficultyToButtonsTiming(String d){
		if(d.equals("beginner")){
			return 650;
		}else if(d.equals("easy")){
			return 600;
		}else if(d.equals("medium")){
			return 550;
		}else if(d.equals("hard")){
			return 500;
		}else if(d.equals("challenge")){
			return 450;
		}
		return 400;
	}
	
	private int difficultyToMaxButtons(String d){
		if(d.equals("beginner")){
			return 2;
		}else if(d.equals("easy")){
			return 3;
		}else if(d.equals("medium")){
			return 4;
		}else if(d.equals("hard")){
			return 5;
		}else if(d.equals("challenge")){
			return 6;
		}
		return 6;
	}
	
	private String getValue(String s){
		String[] split = s.split("\\:");
		if(split.length == 2 && split[1] != null){
			return split[1];
		}
		return "";
	}
	
	private static String readFile(String path, Charset encoding) throws IOException 
	{
	  byte[] encoded = Files.readAllBytes(Paths.get(path));
	  return new String(encoded, encoding);
	}
	
	private int noteToDirection(String note){
		if(note.length() == 4){
			if(note.charAt(0) == '1'){ 
				if(note.charAt(1) == '1'){ //down left
					return 7;
				}else if(note.charAt(2) == '1'){ //up left
					return 1;
				}				
				return 0; // left
			}else if(note.charAt(3) == '1'){ 
				if(note.charAt(1) == '1'){ //down right
					return 5;
				}else if(note.charAt(2) == '1'){ //up right
					return 3;
				}	
				return 4;// right
			}else if(note.charAt(1) == '1'){ //down
				return 6;
			}else if(note.charAt(2) == '1'){ // up
				return 2;
			}
			return -1;
		}
		return -1;
	}
}
