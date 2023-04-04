package nz.ac.wgtn.shadedetector.cveverification;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SurefireUtilsTest {

    private Path getFile(String path) {
        return Path.of(SurefireUtilsTest.class.getResource(path).getPath());
    }

    @Test
    public void test1() throws IOException, JDOMException {
        Path path = getFile("/surefire/Test-1.xml");
        SurefireUtils.TestResults results = SurefireUtils.parseSurefireReport(path);
        assertEquals(2,results.getTestCount());
        assertEquals(0,results.getFailureCount());
        assertEquals(0,results.getErrorCount());
        assertEquals(0,results.getSkippedCount());
        assertTrue(results.allTestsExecuted());
        assertTrue(results.allTestsSucceeded());
    }

    @Test
    public void test2() throws IOException, JDOMException {
        Path path = getFile("/surefire/Test-2.xml");
        SurefireUtils.TestResults results = SurefireUtils.parseSurefireReport(path);
        assertEquals(2,results.getTestCount());
        assertEquals(0,results.getFailureCount());
        assertEquals(1,results.getErrorCount());
        assertEquals(0,results.getSkippedCount());
        assertTrue(results.allTestsExecuted());
        assertFalse(results.allTestsSucceeded());
    }

    @Test
    public void test3() throws IOException, JDOMException {
        Path path = getFile("/surefire/Test-3.xml");
        SurefireUtils.TestResults results = SurefireUtils.parseSurefireReport(path);
        assertEquals(2,results.getTestCount());
        assertEquals(1,results.getFailureCount());
        assertEquals(0,results.getErrorCount());
        assertEquals(0,results.getSkippedCount());
        assertTrue(results.allTestsExecuted());
        assertFalse(results.allTestsSucceeded());
    }

    @Test
    public void test4() throws IOException, JDOMException {
        Path path = getFile("/surefire/Test-4.xml");
        SurefireUtils.TestResults results = SurefireUtils.parseSurefireReport(path);
        assertEquals(2,results.getTestCount());
        assertEquals(0,results.getFailureCount());
        assertEquals(0,results.getErrorCount());
        assertEquals(1,results.getSkippedCount());
        assertFalse(results.allTestsExecuted());
        assertTrue(results.allTestsSucceeded());
    }

    @Test
    public void test5() throws IOException, JDOMException {
        Path path = getFile("/surefire/Test-5.xml");
        SurefireUtils.TestResults results = SurefireUtils.parseSurefireReport(path);
        assertEquals(2,results.getTestCount());
        assertEquals(1,results.getFailureCount());
        assertEquals(0,results.getErrorCount());
        assertEquals(1,results.getSkippedCount());
        assertFalse(results.allTestsExecuted());
        assertFalse(results.allTestsSucceeded());
    }


    @Test
    public void testMerge1() throws IOException, JDOMException {
        Path path1 = getFile("/surefire/Test-1.xml");
        Path path2 = getFile("/surefire/Test-2.xml");
        SurefireUtils.TestResults results1 = SurefireUtils.parseSurefireReport(path1);
        SurefireUtils.TestResults results2 = SurefireUtils.parseSurefireReport(path2);
        SurefireUtils.TestResults results = SurefireUtils.merge(List.of(results1,results2));
        assertEquals(4,results.getTestCount());
        assertEquals(0,results.getFailureCount());
        assertEquals(1,results.getErrorCount());
        assertEquals(0,results.getSkippedCount());
        assertTrue(results.allTestsExecuted());
        assertFalse(results.allTestsSucceeded());
    }

    @Test
    public void testMerge2() throws IOException, JDOMException {
        Path path = getFile("/surefire");
        SurefireUtils.TestResults results = SurefireUtils.parseSurefireReports(path);
        assertEquals(10,results.getTestCount());
        assertEquals(2,results.getFailureCount());
        assertEquals(1,results.getErrorCount());
        assertEquals(2,results.getSkippedCount());
        assertFalse(results.allTestsExecuted());
        assertFalse(results.allTestsSucceeded());
    }



}
