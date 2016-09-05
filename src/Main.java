import java.io.IOException;
import crop.*;

public class Main {
	public enum Tools {
	    CROP
	  }
	public static void main(String[] args) throws IOException {
		// 1. simple command line parser
		if(args.length<=1){
			 System.err.println("Need to specify the tool to use." );
			 System.out.println("xx inputfileName outputFolder [processSpeed]");
             System.exit(1);
		}
		String tool = args[0];
	    Tools currentTool = Tools.valueOf(tool.toUpperCase());
	    switch (currentTool){
	    	case CROP:
				if(args.length!=3 && args.length!=4){
					System.out.println("Need three or four arguments: ");
					System.out.println("xx crop inputfileName outputFolder [processSpeed]");
				}
				String inputFile = args[1];
				String outputDir = args[2];
				int processRatio = 1;
				if(args.length==4) processRatio=Integer.parseInt(args[3]);
				SanskritCrop sc = new SanskritCrop(outputDir,processRatio);
				sc.setVerbose(false);
				sc.Crop(inputFile);
				break;
	    	default:
	    	System.err.println("Tool ("+tool+") not found");
			System.exit(1);
	    }
	}
}