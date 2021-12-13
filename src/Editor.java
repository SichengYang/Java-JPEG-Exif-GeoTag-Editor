import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import jpeg.Jpeg;
import jpeg.JpegOutputSet;
import jpeg.NotJpegException;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Editor extends Application{
    
    private static FileChooser fileChoose = new FileChooser();

    private static final String CANCEL_TEXT = "cancel";
    
    public static void main(String[] args)
    {
        launch();
    }

    @Override
    public void start(Stage stage){  
        Button quit = new Button("quit");
        quit.setLayoutX(20);
        quit.setLayoutY(20);
        quit.setOnAction(event -> {
            stage.close();
            System.exit(0);
        });

        Button refresh = new Button("refresh");
        refresh.setLayoutX(720);
        refresh.setLayoutY(20);
        refresh.setOnAction(event -> {
            try {
                start(stage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Text title = new Text("Welcome to GeoTag Editor");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, FontPosture.REGULAR, 20));
        title.setLayoutX(240);
        title.setLayoutY(40);

        Line titleLine = new Line();
        titleLine.setStartX(0);
        titleLine.setStartY(60);
        titleLine.setEndX(800);
        titleLine.setEndY(60);
        titleLine.setStroke(Color.BLUE);

        Label label = new Label("Drag a Image or Click to import image");
        Label dropped = new Label("");
        VBox dragTarget = new VBox();
        dragTarget.getChildren().addAll(label,dropped);
        dragTarget.setOnDragExited(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                List<File> fileList = db.getFiles();
                if(fileList.size() == 1)
                    processFile(fileList.get(0));
                else
                    displayPopup("Input Error", "Please drag in only one file");
            }
        });
        dragTarget.setOnMouseClicked(event -> {
            fileChoose.setTitle("Open JPEG");
            File jpegFile = fileChoose.showOpenDialog(stage);
            if(jpegFile != null)
                processFile(jpegFile);
        });
        dragTarget.setAlignment(Pos.CENTER);
        dragTarget.setMinSize(800, 550);
        dragTarget.setLayoutX(0);
        dragTarget.setLayoutY(50);

        Group group = new Group(quit, refresh, titleLine, title, dragTarget);
        Scene scene = new Scene(group, 800, 600);
        stage.setTitle("GeoTag Editor");
        stage.setScene(scene);
        stage.show();
    }

    //Output: process this import file and output based on user choice
    private void processFile(File jpegFile)
    {
        try {
            Jpeg jpeg = new Jpeg(jpegFile);
            if(jpeg.exif == null || jpeg.exif.getLatitudeDegree() == null || jpeg.exif.getLongitudeDegree() == null)
                confirmUpdateGeoTag(jpeg);
            else
                chooseMode(jpeg);
        } catch (NotJpegException e){
            displayPopup("Not JPEG", "This fileChoose is not JPEG");
        } catch (IOException e) {
            displayPopup("Reading Error", "Error exists on reading fileChoose");
        }
    }

    //Post: if image has geotag, let user choose to remove or add geotag
    private static void chooseMode(Jpeg jpeg)
    {
        Stage popupWindow = new Stage();
      
        popupWindow.initModality(Modality.APPLICATION_MODAL);
        
        popupWindow.setTitle("Confirmation");

        Label label= new Label("Do you want to remove or update geotag?");
        
        Button update = new Button("update");
        Button remove = new Button("remove");
        Button cancel = new Button(CANCEL_TEXT);
        cancel.setOnAction(e -> popupWindow.close());
        update.setOnAction(e -> {
            displayUpdateGeoTag(jpeg);
            popupWindow.close();
        });
        remove.setOnAction(e -> {
            Stage saveWindow = new Stage();
            saveWindow.setTitle("Save JPEG");
            File result = fileChoose.showSaveDialog(saveWindow);
            JpegOutputSet outputSet = new JpegOutputSet(jpeg);
            try {
                if( outputSet.removeGeoTag(result) )
                    displayPopup("Remove Message", "Successfully remove geotag!");
            else
                displayPopup("Unknown Remove Error", "Error on remove geotag!");
            popupWindow.close();
            saveWindow.close();
            } catch (IOException exception) {
                displayPopup("Remove Error", exception.getMessage());
            }
        });
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER);
        buttons.getChildren().addAll(update, remove, cancel);
        buttons.setSpacing(20);

        VBox layout= new VBox();
        
        layout.getChildren().addAll(label, buttons);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(50);
            
        Scene scene= new Scene(layout, 250, 120);
        
        popupWindow.setScene(scene);
            
        popupWindow.showAndWait();
    }

    //Post: if image does not have geotag. This window will pop up to let user confirm update geotag
    private static void confirmUpdateGeoTag(Jpeg jpeg)
    {
        Stage popupWindow = new Stage();
      
        popupWindow.initModality(Modality.APPLICATION_MODAL);
        
        popupWindow.setTitle("Confirmation");

        Label label= new Label("This image does not have geotag.\nDo you want to add one?");
        
        Button confirm = new Button("confirm");
        Button cancel = new Button(CANCEL_TEXT);    
        cancel.setOnAction(e -> popupWindow.close());
        confirm.setOnAction(e -> {
            displayUpdateGeoTag(jpeg);
            popupWindow.close();
        });
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER);
        buttons.getChildren().addAll(confirm, cancel);
        buttons.setSpacing(60);

        VBox layout= new VBox();
        
        layout.getChildren().addAll(label, buttons);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(50);
            
        Scene scene= new Scene(layout, 200, 120);
        
        popupWindow.setScene(scene);
            
        popupWindow.showAndWait();
    }

    //Post: display pop up window with associate prompt
    private static void displayPopup(String title, String prompt)
    {
        Stage popupWindow = new Stage();
      
        popupWindow.initModality(Modality.APPLICATION_MODAL);
        
        popupWindow.setTitle(title);

        Label label= new Label(prompt);
        
        Button button= new Button("Close");
            
        button.setOnAction(e -> popupWindow.close());
            
        VBox layout= new VBox();
        
        layout.getChildren().addAll(label, button);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(50);
            
        Scene scene= new Scene(layout, 200, 120);
        
        popupWindow.setScene(scene);
            
        popupWindow.showAndWait();
    }

    //Post: display menu that add geotag
    private static void displayUpdateGeoTag(Jpeg jpeg)
    {
        Stage popupWindow = new Stage();
      
        popupWindow.initModality(Modality.APPLICATION_MODAL);
        
        popupWindow.setTitle("Process GeoTag");

        Text title= new Text("Please key in the new geotag information and press confirm.");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, FontPosture.REGULAR, 20));
        title.setLayoutY(50);
        title.setLayoutX(70);
        
        //generate latitude and longitude input box
        HBox latitudeField = new HBox();
        Text latitudeLabel = new Text("Latitude:       ");
        TextField latitudeTextField = new TextField("Enter latitude seperated by white space:");
        latitudeTextField.setOnMouseClicked(e -> latitudeTextField.setText(""));
        latitudeField.getChildren().addAll(latitudeLabel, latitudeTextField);
        HBox longitudeField = new HBox();
        Text longitudeLabel = new Text("Longitude:   ");
        TextField longitudeTextField = new TextField("Enter longitude seperated by white space:");
        longitudeTextField.setOnMouseClicked(e -> longitudeTextField.setText(""));
        longitudeField.getChildren().addAll(longitudeLabel, longitudeTextField);

        Button cancel = new Button(CANCEL_TEXT);
        Button confirm = new Button("confirm");
        
        cancel.setOnAction(e -> popupWindow.close()); 
        confirm.setOnAction(e -> {
            Double latitude = getLatitude(latitudeTextField.getText());
            Double longitude = getLongitude(longitudeTextField.getText());
            if (latitude == null)
                displayPopup("Latitude Error", "Error latitude format");
            else if (longitude == null)
                displayPopup("Longitude Error", "Error longitude format");
            else
                outputImage(jpeg, latitude, longitude);
            popupWindow.close();
        });

        VBox layout = new VBox();
        
        layout.setLayoutX(270);
        layout.setLayoutY(250);
        layout.setSpacing(20);
        layout.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(latitudeField, longitudeField, confirm, cancel);
        
        Group group = new Group(title, layout);

        Scene scene= new Scene(group, 800, 600);
        
        popupWindow.setScene(scene);
            
        popupWindow.showAndWait();
    }

    //Post: let user to select a location and output the image
    private static void outputImage(Jpeg jpeg, double latitude, double longitude)
    {
        Stage saveWindow = new Stage();
        fileChoose.setTitle("Save JPEG");
        File result = fileChoose.showSaveDialog(saveWindow);
        JpegOutputSet outputSet = new JpegOutputSet(jpeg);
        try {
            if( outputSet.updateGeoTag(result, latitude, longitude) )
                displayPopup("Update Message", "Successfully update geotag!");
            else
                displayPopup("Unknown Update Error", "Error on updae geotag!");
        } catch (IOException e) {
            displayPopup("Update Error", e.getMessage());
        }
    }

    // Return: null if latitude is not with -90 to 90.
    //         a Double value if it is a valid latitude
    private static Double getLatitude(String input) {
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
    private static Double getLongitude(String input) {
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
