package tests;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class EqualsTestRunner {
	public static void main(String[] args)
	{
		Result result = JUnitCore.runClasses(TestEquals.class);

		for (Failure failure : result.getFailures())
			System.out.println(failure.toString());

		System.out.println("Test success: " + result.wasSuccessful());
	}
}