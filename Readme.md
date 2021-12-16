<h1 align="center" style="border-bottom: none">Java JPEG Exif GeoTag Editor</h1>

## Description
- This is a Java library that can let user scan jpeg/jpg exif, report all tags, and change or remove geotag.
- This project offers App.java which can process all images in assets and a command line tool. For the instruction, please see the content below.
- Project that use this library: https://github.com/tahaafzal5/AugieGeoTag

## Functions
- This library offers <a href="https://github.com/SichengYang/Java-JPEG-GeoTag-Editor/blob/main/src/jpeg/Jpeg.java" target="_blank">Jpeg</a> which can parse jpeg/jpg and find exif raw data.
- This library offers <a href="https://github.com/SichengYang/Java-JPEG-GeoTag-Editor/blob/main/src/jpeg/JpegExif.java">JpegExif</a> can scan the exif data and report to users.
- This library offers <a href="https://github.com/SichengYang/Java-JPEG-GeoTag-Editor/blob/main/src/jpeg/JpegOutputSet.java">JpegOutputSet</a> which can let user customize geotag (remove or update).

## Download
```
git clone https://github.com/SichengYang/Java-JPEG-Exif-GeoTag-Editor.git
```
## Compile Instruction
```
javac -d bin -cp .:./lib/hamcrest-core-1.3.jar:./lib/junit-4.13.2.jar --module-path <your javafx library path> --add-modules javafx.controls */*.java */*/*.java
```
## Running Instruction
- Run App
```
java -cp bin App
```
- Run Tool
```
java -cp bin Tool <command>
```
- Run JUnit Test
```
java -cp .:bin:./lib/hamcrest-core-1.3.jar:./lib/junit-4.13.2.jar tests.<a test runner>
```
- Run JavaFX Progrma Editor
```
java -cp bin --module-path <your javafx library path> --add-modules javafx.controls Editor
```
## Tool Command Instruction
command type:

-m remove for remove geotag, update for update geotag, verify for verify whether file is a jpeg, and print to print geotag (required)
 
-i name of input file or folder in assets folder. It could also be "." means process all images under assets (required)
  
-la latitude as a String (required when you select to update geotag)
  
-lo longitude as a String (required when you select to update geotag)
  
-help print help menu
	
**Note: flag order does not matter**

- remove geotag command sample:
```
-m remove -i <file path under assets>
```
- update geotag command sample:
```
-m update -i <file path under assets> -la <latitude> -lo <longitude>
```
- print geotag command sample:
```
-m print -i <file path under assets>
```
- verify jpeg command sample:
```
-m verify -i <file path under assets>
```
- print all tag command sample:
```
-m tag -i <file path under assets>
```
- print help menu:
```
-help
```
## Geotag Support Format
- 100 30 20.99 N
- 100 40.99 S
- 100.88 W
- 100 30 20.99  (you can type in positive or negative to represent the direction)
- -100 -30 -20.99
- 100 40.99
- -100 -40.99
- 100.88
- -100.88
## License
- It is licensed to <a href="https://creativecommons.org/publicdomain/zero/1.0/">Creative Commons Zero v1.0 Universal</a>.

## Sample Usage
- <a href="https://github.com/SichengYang/Java-JPEG-GeoTag-Editor/blob/main/src/App.java">App.java under src</a>
- <a href="https://github.com/SichengYang/Java-JPEG-GeoTag-Editor/blob/main/src/Tool.java">Tool.java under src</a>

## Import Guide
- import jpeg.*;

## Test Files
- Pease go to folder tests under src
