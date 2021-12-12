package jpeg;

/*
	This is Exception will be thrown if the input file is not JPEG
 */

public class NotJpegException extends Exception {

	private static final long serialVersionUID = 1L;

	public NotJpegException(String message) {
		super(message);
	}
}
