package SMParser;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;


public class SMParser {
	private static String[] metaDataHeaders = {"OFFSET:","TITLE:","ARTIST:", "DISPLAYBPM:", "MUSIC","SAMPLESTART:", "BANNER:", "BACKGROUND:", "SUBTITLE:"};
	private String[] metadata;
	private ArrayList<JsonObjectBuilder> arrays;
	
	public SMParser(){
		try {
			String file = readFile("test", Charset.defaultCharset());
			String[] mainsplit = file.split("\\#");
			arrays = new ArrayList<JsonObjectBuilder>();
			metadata = new String[metaDataHeaders.length];
			for(String s:mainsplit){
				//searching for the metadata
				int i = 0;
				for(String header:metaDataHeaders){
					if(s.contains(header)){
						metadata[i] = getValue(s).replace(';', ' ').trim();
					}
					i++;
				}
				//parsing the notes
				if(s.contains("NOTES")){
					String[] notesplit = s.split("\n");
					if(notesplit[1].contains("-single:")){ //valid note (single player mode)
						String difficulty = notesplit[3].replace(":", "").trim();
						for(int k = 0; k < 6; k++){ //remove first 6 lines
							s = s.substring(s.indexOf('\n')+1);
						}
						s= s.replaceAll("//.*", ""); // remove some more bs
						s = s.replaceAll("\\;", "");
						
						notesplit = s.split("\\,"); //split everything in arrays which contains "beats" for 1 second
						double secondscounter = Double.parseDouble(metadata[0])*-1;
						int extraButtonTime = notesplit.length / difficultyToMaxButtons(difficulty);
						int maxButtons = 1;						
						JsonArrayBuilder objectsArrayBuilder = Json.createArrayBuilder();
						
						for(String notespersecond:notesplit){
							notespersecond = notespersecond.trim();
							String[] notes = notespersecond.split("\n");
							double precision = 1.0/notes.length;
							double time = secondscounter;
							for(String note:notes){
								int direction = noteToDirection(note);
								if(direction != -1){
									JsonObjectBuilder object = Json.createObjectBuilder();
									object.add("time", time);
									object.add("direction", direction);
									object.add("button", (int)(Math.random()*maxButtons));
									objectsArrayBuilder.add(object.build());
								}
								if(extraButtonTime*maxButtons < time){
									maxButtons++;
								}
								time += precision;
							}
							secondscounter++;
						}
						
						arrays.add(Json.createObjectBuilder());
						arrays.get(arrays.size()-1).add("difficulty", difficulty);
						arrays.get(arrays.size()-1).add("objects", objectsArrayBuilder);					
					}
				};
			}
			writeJsonFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeJsonFile(){
		JsonObjectBuilder jsonfilebuilder = Json.createObjectBuilder();
		JsonArrayBuilder dataArrayBuilder = Json.createArrayBuilder();
		
		jsonfilebuilder.add("meta", Json.createObjectBuilder().add("title", metadata[1]).add("subtitle", metadata[8]).add("author", metadata[2]).add("BPM", Double.parseDouble(metadata[3])).add("sample_start", Double.parseDouble(metadata[5])));
		jsonfilebuilder.add("file", Json.createObjectBuilder().add("audio", metadata[4]).add("background", metadata[7]).add("banner", metadata[6]));
		for(JsonObjectBuilder ar:arrays){
			dataArrayBuilder.add(ar.build());
		}
		jsonfilebuilder.add("data", dataArrayBuilder);
		
		
		try {
			JsonObject file = jsonfilebuilder.build();
			OutputStream os;
			os = new FileOutputStream("emp.txt");
			JsonWriter jsonWriter = Json.createWriter(os);
			jsonWriter.writeObject(file);
			jsonWriter.close();		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
	
	private int difficultyToMaxButtons(String d){
		if(d.equals("Beginner")){
			return 2;
		}else if(d.equals("Easy")){
			return 3;
		}else if(d.equals("Medium")){
			return 4;
		}else if(d.equals("Hard")){
			return 6;
		}
		return 0;
	}
	
	private String getValue(String s){
		String[] split = s.split("\\:");
		if(split[1] != null){
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
}
