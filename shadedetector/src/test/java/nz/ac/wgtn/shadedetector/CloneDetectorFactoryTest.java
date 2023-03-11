package nz.ac.wgtn.shadedetector;

import nz.ac.wgtn.shadedetector.clonedetection.DummyCloneDetector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CloneDetectorFactoryTest {

    @Test
    public void testSelectAll() {
        CloneDetector cloneDetector = new CloneDetectorFactory().create("dummy");
        assertNotNull(cloneDetector);
        assertEquals("dummy",cloneDetector.name());
        assertTrue(cloneDetector instanceof DummyCloneDetector);
    }

    @Test
    public void testNonExisting() {
        assertThrows(IllegalArgumentException.class, () -> new CloneDetectorFactory().create("foo"));
    }
}
