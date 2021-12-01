package tests;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class OutputSetTestRunner {
	public static void main(String[] args)
	{
		Result result = JUnitCore.runClasses(OutputSetTest.class);

		for (Failure failure : result.getFailures())
			System.out.println(failure.getTrimmedTrace());

		System.out.println(result.wasSuccessful());
	}
}