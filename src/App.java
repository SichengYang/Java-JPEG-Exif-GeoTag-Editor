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
		
		File remove_results = new File("./assets/remove");
		if(!remove_results.exists())
			remove_results.mkdir();
		
		File update_results = new File("./assets/update");
		if(!update_results.exists())
			update_results.mkdir();
		
		File resource = new File("./assets");

		for(File f : resource.listFiles()) {
			if(f.isFile()) {
				try {
				File output = new File("./assets/remove/" + f.getName());
				Jpeg jpeg = new Jpeg(f);
				JpegOutputSet outputSet = new JpegOutputSet(jpeg);
				if (outputSet.removeGeoTag(output))
					System.out.println("Remove " + f.getName());
				} catch (Exception e) {
					System.out.println("Failed to remove " + f.getName());
				}
			}
		}

		for(File f : resource.listFiles()) {
			if(f.isFile()) {
				try {
					File output = new File("./assets/update/" + f.getName());
					Jpeg jpeg = new Jpeg(f);
					JpegOutputSet outputSet = new JpegOutputSet(jpeg);
					if (outputSet.updateGeoTag(output, latitude, longitude))
						System.out.println("Update " + f.getName());
				} catch (Exception e) {
					System.out.println("Failed to update " + f.getName());
				}
			}
		}
	}
}