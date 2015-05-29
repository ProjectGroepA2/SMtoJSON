package SMParser;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

public class SMFileSearcher {
	private ArrayList<File> files;
	
	public SMFileSearcher(String dir){
		files = new ArrayList<File>();
		File  f = new File("scannedsongs");
		SMParser parser = new SMParser();
		f.mkdir();
		findFiles(dir);
		System.out.println("Total files:"+files.size());
		int current = 1;
		for(File file:files){
			System.out.println("\b\b\b\b\b("+current+"/"+files.size()+") \t"+file.getAbsolutePath());
			parser.parseSmFile(file);
			current++;
		};
    }
	
	public void findFiles(String path){
        File root = new File(path);
        File[] list = root.listFiles();
        if (list == null) return;
        for ( File f : list ) {
            if ( f.isDirectory() ) {
                findFiles( f.getAbsolutePath() );
            }
            else {
            	if(f.getName().endsWith(".sm")){
            		files.add(f);
            	}
            }
        }
	}
}
