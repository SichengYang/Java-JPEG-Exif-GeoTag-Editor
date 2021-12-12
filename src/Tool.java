import java.io.*;
import java.util.Scanner;

import jpeg.*;

/*
	This is the test command line tool for write geotag and remove geotag.
	
	command type:
	-m remove for remove geotag, update for update geotag, 
	   verify for verify whether file is a jpeg, and print to print geotag (required)
	-i name of input file, folder in assets folder, or wild card . (required)
	-la latitude as a String (required when you select to update geotag)
	-lo longitude as a String (required when you select to update geotag)
	-help print help menu
	
	**input flag order does not matter**

	remove geotag command sample:
	-m remove -i <file path under assets>
	update geotag command sample:
	-m update -i <file path under assets> -la <latitude> -lo <longitude>
	print geotag command sample:
	-m print -i <file path under assets>
	verify jpeg command sample:
	-m verify -i <file path under assets>
	print all tag command sample:
	-m tag -i <file path under assets>
*/
public class Tool {
	
	private static String mode = null;
		
	private static File jpegFile = null;
	
	private static File assetsFolder = null;
	private static File resultsFolder = new File("./assets/results");
	
	/*
	null pointer is used here to determine whether 
	latitude and longitude are valid
	*/
	private static Double latitude = null;
	private static Double longitude = null;

	//Pre: this is a command line tool for jpeg processing
	//Input: user input a command string
	//Output: a folder that contains all result image or a result image depends on users choice.
	public static void main(String[] args) throws IOException
	{
		//disable print stream from GeoTagFunctions

		if(args.length == 0) {
			System.out.println("No command received.");
			System.exit(0);
		}

		if(!resultsFolder.exists())
			resultsFolder.mkdir();
		
		analyseInput(args);
		
		if(mode != null) {	
			//This means it is a single file processing
			if(jpegFile != null) {
				processFile();
			}
			//This means it is a folder processing
			else if (assetsFolder != null) {
				processFolder();
			} else
				System.out.println("Error on deciding process mode");
		}
		else
			System.out.println("Missing argument mode");
	}

	//Output: all images under user input folder will be processed and output in results folder
	public static void processFolder() throws IOException
	{
		switch (mode)
		{
			case "remove":
				for (File f : assetsFolder.listFiles()){
					Jpeg jpeg = null;
					try{
						jpeg = new Jpeg(f);
					}catch(Exception e){
						System.err.println(e.getMessage());
						continue;
					}
					JpegOutputSet outputSet = new JpegOutputSet(jpeg);
					File result = new File("./assets/results/" + f.getName());
					if(outputSet.removeGeoTag(result))
						System.out.printf("Geotag in %s has been removed \n", f.getName());
					else
						System.out.printf("Failed to remove geotag in %s \n", f.getName());
				}
				break;
			case "update":
				if(latitude == null) {
					System.out.println("Latitude information missing");
					break;
				} 
				if(longitude == null) {
					System.out.println("Longitude information missing");
					break;
				}
				for (File f : assetsFolder.listFiles()){
					Jpeg jpeg = null;
					try{
						jpeg = new Jpeg(f);
					}catch(Exception e){
						System.err.println(e.getMessage());
						continue;
					}
					JpegOutputSet outputSet = new JpegOutputSet(jpeg);
					File result = new File("./assets/results/" + f.getName());
					if(outputSet.updateGeoTag(result, latitude, longitude))
						System.out.printf("Geotag in %s has been update \n", f.getName());
					else
						System.out.printf("Failed to update geotag in %s \n", f.getName());
				}
				break;
			default:
				System.out.println("Mode information error: should be \"update\" or \"remove\"");
				break;
			}
	}

	//Output: information print on the screen based on user choice
	//		  image output based on user choice
	public static void processFile() throws IOException
	{
		//process mode information
		switch(mode)
		{
			case "remove": 
				Jpeg jpeg = null;
				try{
					jpeg = new Jpeg(jpegFile);
				}catch(Exception e){
					System.err.println(e.getMessage());
				}
				JpegOutputSet outputSet = new JpegOutputSet(jpeg);
				File result = new File("./assets/results/" + jpegFile.getName());
				if(outputSet.removeGeoTag(result))
					System.out.printf("Geotag in %s has been removed \n", jpegFile.getName());
				else
					System.out.printf("Failed to remove geotag in %s \n", jpegFile.getName());
				break;
			case "update":
				if(latitude == null) {
					System.out.println("Latitude information missing");
					break;
				} 
				if(longitude == null) {
					System.out.println("Longitude information missing");
					break;
				}
				
				jpeg = null;
				try{
					jpeg = new Jpeg(jpegFile);
				}catch(Exception e){
					System.err.println(e.getMessage());
				}
				outputSet = new JpegOutputSet(jpeg);
				result = new File("./assets/results/" + jpegFile.getName());
				if(outputSet.updateGeoTag(result, latitude, longitude))
					System.out.printf("Geotag in %s has been update \n", jpegFile.getName());
				else
					System.out.printf("Failed to update geotag in %s \n", jpegFile.getName());
				break;
			case "print":
				try{
					jpeg = new Jpeg(jpegFile);
					if(jpeg.exif != null && jpeg.exif.getLatitudeRef() != null && jpeg.exif.getLongitudeRef() != null){
						System.out.printf("Latitude: %dº%d'%.2f\" %s %n", 
										  jpeg.exif.getLatitudeDegree(), 
										  jpeg.exif.getLatitudeMinute(), 
										  jpeg.exif.getLatitudeSecond(), 
										  jpeg.exif.getLatitudeRef());
						System.out.printf("Longitude: %dº%d'%.2f\" %s %n", 
										  jpeg.exif.getLongitudeDegree(), 
										  jpeg.exif.getLongitudeMinute(), 
										  jpeg.exif.getLongitudeSecond(), 
										  jpeg.exif.getLongitudeRef());
					}
					else
						System.out.println("There is not geotag in jpeg");
				} catch (Exception e){
					System.err.println("Error on read jpeg");
				}
				break;
			case "verify":
				FileInputStream input= new FileInputStream(jpegFile);
				if (input.read() == 0xFF && input.read() == 0xD8)
					System.out.println("It is a jpeg");
				else
					System.out.println("It is not a jpeg");
				input.close();
				break;
			case "tag":
				try {
					jpeg = new Jpeg(jpegFile);
					System.setOut(System.out);
					if(jpeg.exif != null)
						jpeg.exif.print();
					else
						System.out.println("There is no exif data in this image");
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
				break;
			default:
				System.out.println("Mode information error: should be \"update\", \"remove\", or \"print\"");
				break;
		}
	}

	//Post: Analyse input and set associate value to global variables
	public static void analyseInput(String[] args)
	{
		//if user want help, print help menu.
		if(args[0].equals("-help")){
			printHelp();
			System.exit(0);
		}

		//if the first flag is not help, parse input string.
		int position = 0;
		while( position < args.length ) {
			if(position + 1 >= args.length) {
				System.out.println("Missing argument after flag " + args[position]);
				System.exit(0);
			}
			switch(args[position])
			{
				case "-m":
					mode = args[position + 1];
					position += 2;
					break;
				case "-i":
					if(args[position + 1].equals(".")){
						assetsFolder = new File("./assets/");
					} else {
						File temp = new File("./assets/" + args[position + 1]);
						if(temp.isFile())
							jpegFile = temp;
						else if(temp.isDirectory())
							assetsFolder = temp;
						else {
							System.out.println("Error on analyse file, please check the file name.");
							System.exit(0);
						}
					}
					position += 2;
					break;
				case "-la":
					if (mode.equals("remove")) {
						System.out.println("Remove mode error: Error argument latitude received.");
						System.exit(0);
					}	
					String latitudeInfo = "";
					while(position + 1 < args.length && !isCommand(args[position + 1])) {
						latitudeInfo += args[position + 1] + " ";
						position += 1;
					}
					latitude = getLatitude(latitudeInfo.trim());
					if(latitude == null) {
						System.out.println("Error latitude format.");
						System.exit(0);
					}
					
					//move to next section 
					position += 1;
					break;
				case "-lo":
					if (mode.equals("remove")) {
						System.out.println("Remove mode error: Error argument longitude received.");
						System.exit(0);
					}
					String longitudeInfo = "";
					while(position + 1 < args.length && !isCommand(args[position + 1])) {
						longitudeInfo += args[position + 1] + " ";
						position += 1;
					}
					longitude = getLongitude(longitudeInfo.trim());
					if(longitude == null) {
						System.out.println("Error longitude format.");
						System.exit(0);
					}
					
					//move to next section 
					position += 1;
					break;
				default:
					System.out.println("Error on command type " + args[position]);
					System.exit(0);
			}
		}
	}

	//Output: help menu printed on the screen
	public static void printHelp(){
		System.out.println("-m remove for remove geotag, update for update geotag");
		System.out.println("-i name of input file or folder in assets folder, or wild card .");
		System.out.println("-o output file name (only for single file processing)");
		System.out.println("-la latitude as a String");
		System.out.println("-lo longitude as a String");
		System.out.println("remove geotag:");
		System.out.println("-m remove -i <file path under assets>");
		System.out.println("update geotag:");
		System.out.println("-m update -i <file path under assets> -la <latitude> -lo <longitude>");
		System.out.println("print geotag:");
		System.out.println("-m print -i <file path under assets>");
		System.out.println("verify jpeg command sample:");
		System.out.println("-m verify -i <file path under assets>");
		System.out.println("print all tag command sample:");
		System.out.println("-m tag -i <file path under assets>");
	}

	public static boolean isCommand(String cmd){
		switch(cmd)
		{
			case "-m": case "-i": case "-o":
			case "-la": case "-lo": case"-help":
				return true;
			default:
				return false;
		}
	}

	// Return: null if latitude is not with -90 to 90.
    //         a Double value if it is a valid latitude
    public static Double getLatitude(String input) {
        Scanner coordScanner = new Scanner(input);
        
    	// skip all numeric value
    	while (coordScanner.hasNextDouble())
            coordScanner.nextDouble();
    	
        // check direction if exists
        if (coordScanner.hasNext()) {
    		char direction = coordScanner.next().toLowerCase().charAt(0);
    		
            if (direction != 'n' && direction != 's') {
    			System.err.println("Error: for direction use N or S");
    			
                coordScanner.close();
    		
                return null;
    		}
    	}
    	coordScanner.close();
    	
    	// range check
    	Double latitude = getCoordinate(input);
        
        if (latitude == null)
            return null;
        
        if (latitude <= -90 || latitude >= 90) {
        	System.err.println("Latitude should be within -90 to 90");
        	
            return null;
        }

        return latitude;
    }

    // Return: null if longitude is not with -180 to 180.
    //         a Double value if it is a valid longitude
    public static Double getLongitude(String input) {
        Scanner coordScanner = new Scanner(input);
    	
        // skip all numeric value
    	while (coordScanner.hasNextDouble())
            coordScanner.nextDouble();
    	
    	// check direction if exists
        if (coordScanner.hasNext()) {
    		char direction = coordScanner.next().toLowerCase().charAt(0);
    		
            if (direction != 'e' && direction != 'w') {
    			System.err.println("Error: for direction use E or W");
    			
                coordScanner.close();
    			
                return null;
    		}
    	}
    	coordScanner.close();
    	
        // range check
    	Double longitude = getCoordinate(input);
        
        if (longitude == null)
        	return null;
        
        if (longitude <= -180 || longitude >= 180) {
        	System.err.println("Longitude should be within -180 to 180");
        	
            return null;
        }

        return longitude;
    }

    // Pre: each should be separated by white space (example: 100 30 20.99 N)
    // Return: a double that represents passed latitude or longitude coordinate
    //  	   N and E would be a positive value. S and W would be negative value.
    // Reminder: Null pointer would be returned if format is wrong.
    // Support format example:
    //	  100 30 20.99 N
    //	  100 40.99 S
    //	  100.88 W
    //    100 30 20.99  (you can type in positive or negative to represent the direction)
    //    -100 -30 -20.99
    //	  100 40.99
    //    -100 -40.99
    //	  100.88
    //	  -100.88
    private static Double getCoordinate(String input) {
    	final int MINUTES_PER_DEGREE = 60;
    	final int SECONDS_PER_DEGREE = 3600;
    	
    	double result = 0;
    	
    	Scanner coordScanner = new Scanner(input);
    	
    	// get degree
    	if (coordScanner.hasNextDouble())
    		result += coordScanner.nextDouble();
    	else {
    		System.err.println("Unable to read degrees.");
    		coordScanner.close();
    		
            return null;
    	}
    	
    	// get minute if it exists
    	if (coordScanner.hasNextDouble()) {
    		result += (coordScanner.nextDouble() / MINUTES_PER_DEGREE);
    	}
    	
    	// get second if it exists
    	if (coordScanner.hasNextDouble())
    		result += (coordScanner.nextDouble() / SECONDS_PER_DEGREE);
    	
    	// if the direction is N or E, result should be positive.
    	// if the direction is S or W, result should be negative.
    	if (coordScanner.hasNext()) {
    		String direction = coordScanner.next();
    		
            if (result < 0) {
    			System.err.println("Please use either negative value or direction reference.");
    			coordScanner.close();
    			
                return null;
    		}

    		if (direction.equals("S") || direction.equals("W"))
    			result = -result;
        }

    	coordScanner.close();

    	return result;
    }
}
