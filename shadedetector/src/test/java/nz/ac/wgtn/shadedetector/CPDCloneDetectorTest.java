package nz.ac.wgtn.shadedetector;

import nz.ac.wgtn.shadedetector.clonedetection.CPDCloneDetector;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CPDCloneDetectorTest {

    @Test
    public void testCPDCloneDetection() throws Exception {
        CPDCloneDetector detector = new CPDCloneDetector();
        URL resourceUrl = getClass().getResource("/f.jar");
        Path fJarPath = Paths.get(resourceUrl.toURI());
        resourceUrl = getClass().getResource("/b.jar");
        Path bJarPath = Paths.get(resourceUrl.toURI());
        detector.detect(fJarPath, bJarPath);
    }
}
