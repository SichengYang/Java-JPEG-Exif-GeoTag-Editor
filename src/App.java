import java.io.*;
import java.util.*;
import jpeg.JpegExif;
import jpeg.JpegOutputSet;
import jpeg.Jpeg;

public class App
{
	public static void main(String[] args) throws FileNotFoundException, IOException
	{
		/*
		File photo = new File("./assets/Cannon-EOS-M50-no-geotag.JPG");
		Jpeg jpeg = new Jpeg(photo);
		File output = new File("./assets/results/editted-" + photo.getName());
		JpegOutputSet outputSet = new JpegOutputSet(jpeg);
		double latitude = 50.0 + 30.0 / 60 + 55.77 / 3600;
		double longitude = 100.0 + 50.0 / 60 + 10.8 / 3600;
		outputSet.updateGeoTag(output, latitude, longitude);
		*/
		
		double latitude = 50.0 + 30.0 / 60 + 55.77 / 3600;
		double longitude = 100.0 + 50.0 / 60 + 10.8 / 3600;
		
		File results = new File("./assets/results");
		if(!results.exists())
			results.mkdir();
		
		File photo = new File("./assets/internet.JPG");
		File output = new File("./assets/results/editted-" + photo.getName());
		Jpeg jpeg = new Jpeg(photo);
		JpegOutputSet outputSet = new JpegOutputSet(jpeg);
		outputSet.removeGeoTag(output);
		
		photo = new File("./assets/results/editted-" + photo.getName());
		jpeg = new Jpeg(photo);
		jpeg.exif.print();
		
		photo = new File("./assets/iPhone-6.JPG");
		output = new File("./assets/results/editted-" + photo.getName());
		jpeg = new Jpeg(photo);
		outputSet = new JpegOutputSet(jpeg);
		outputSet.updateGeoTag(output, latitude, longitude);
	}
}