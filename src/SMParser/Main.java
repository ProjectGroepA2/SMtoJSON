package SMParser;

public class Main {

	public static void main(String[] args) {
		if(args.length > 0 && args[0] != null){
			new SMFileSearcher(args[0]);
		}else{
			System.out.println("No inputdir given");
		}
	}

}
