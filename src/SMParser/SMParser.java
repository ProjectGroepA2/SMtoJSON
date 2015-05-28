package SMParser;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;


public class SMParser {
	private static String[] metaDataHeaders = {"OFFSET:","TITLE:","ARTIST:", "DISPLAYBPM:", "MUSIC","SAMPLESTART:"};
	String[] metadata;
	
	
	public SMParser(){
		try {
			String file = readFile("test", Charset.defaultCharset());
			String[] mainsplit = file.split("\\#");
			String[] metadata = new String[metaDataHeaders.length];
			
			for(String s:mainsplit){
				//searching for the metadata
				int i = 0;
				for(String header:metaDataHeaders){
					if(s.contains(header)){
						metadata[i] = getValue(s).replace(';', ' ');
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
						System.out.println(difficulty);
						notesplit = s.split("\\,"); //split everything in arrays which contains "beats" for 1 second
						double secondscounter = Double.parseDouble(metadata[0])*-1;
						for(String notespersecond:notesplit){
							notespersecond = notespersecond.trim();
							String[] notes = notespersecond.split("\n");
							double precision = 1.0/notes.length;
							double time = secondscounter;
							for(String note:notes){
								int direction = noteToDirection(note);
								if(direction != -1){
									System.out.println(time + "\t\t" + direction);
								}
								time += precision;
							}
							secondscounter++;
						}
						System.out.println("----------");
					}
				};
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
