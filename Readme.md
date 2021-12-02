<h1 align="center" style="border-bottom: none">Java JPEG GeoTag Editor</h1>

## Description
- This is a Java library that can let user scan jpeg/jpg exif, report all tags, and change or remove geotag.

## Functions
- This library offers <a href="https://github.com/SichengYang/Java-JPEG-GeoTag-Editor/blob/main/src/jpeg/Jpeg.java">Jpeg</a> which can parse jpeg/jpg and find exif raw data.
- This library offers <a href="https://github.com/SichengYang/Java-JPEG-GeoTag-Editor/blob/main/src/jpeg/JpegExif.java">JpegExif</a> can scan the exif data and report to users.
- This library offers <a href="https://github.com/SichengYang/Java-JPEG-GeoTag-Editor/blob/main/src/jpeg/JpegOutputSet.java">JpegOutputSet</a> which can let user customize geotag (remove or update).

## Download
```
git clone https://github.com/SichengYang/Java-Exif-Editor.git
```
## Compile Instruction
```
javac -cp .:./lib/hamcrest-core-1.3.jar:./lib/junit-4.13.2.jar *.java */*.java
```

## License
- It is licensed to <a href="https://creativecommons.org/publicdomain/zero/1.0/">Creative Commons Zero v1.0 Universal</a>.

## Sample Usage
- <a href="https://github.com/SichengYang/Java-JPEG-GeoTag-Editor/blob/main/src/App.java">App.java under src</a>

## Import Guide
- import jpeg.*;
