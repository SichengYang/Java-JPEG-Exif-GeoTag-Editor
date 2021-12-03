package tests;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class BigEndianTestRunner {
	public static void main(String[] args)
	{
		Result result = JUnitCore.runClasses(BigEndianTest.class);

		for (Failure failure : result.getFailures())
			System.out.println(failure.getTrimmedTrace());

		System.out.println("Test success: " + result.wasSuccessful());
	}
}