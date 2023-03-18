package nz.ac.wgtn.shadedetector.clonedetection.ast;

import nz.ac.wgtn.shadedetector.CloneDetector;
import nz.ac.wgtn.shadedetector.Utils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ASTCloneDetectionTests {

    public static Path COMMONS_COLLECTIONS4_40_ORIGINAL = null;
    public static Path COMMONS_COLLECTIONS4_40_RENAMED_PACKAGES = null;


    @BeforeAll
    public static void setupPath () throws Exception {
        COMMONS_COLLECTIONS4_40_ORIGINAL = Path.of(ASTCloneDetectionTests.class.getResource("/commons-collections4-4.0").getPath());
        Assumptions.assumeTrue(Files.exists(COMMONS_COLLECTIONS4_40_ORIGINAL));

        COMMONS_COLLECTIONS4_40_RENAMED_PACKAGES = Path.of(ASTCloneDetectionTests.class.getResource("/clones/completely-shaded-packages-renamed").getPath());
        Assumptions.assumeTrue(Files.exists(COMMONS_COLLECTIONS4_40_RENAMED_PACKAGES));
    }

    // baseline test -- test whether a library will be detected as clone of itself
    @Test
    public void testPerfectCopy() throws IOException {
        Set<Path> javaSources = new HashSet(Utils.listJavaSources(COMMONS_COLLECTIONS4_40_ORIGINAL,true));
        Set<CloneDetector.CloneRecord> records = new ASTBasedCloneDetector().detect(COMMONS_COLLECTIONS4_40_ORIGINAL,COMMONS_COLLECTIONS4_40_ORIGINAL);

        assertEquals(javaSources.size(),records.size());
        for (CloneDetector.CloneRecord record:records) {
            assertEquals(1.0,record.getConvidence());
        }

        Set<Path> set1 = new HashSet<>(javaSources);
        Set<Path> set2 = records.stream()
            .map(r -> r.getOriginal())
            .collect(Collectors.toSet());
        assertEquals(set1,set2);
    }

    @Test
    public void testPackagesRenamed() throws IOException {
        Set<Path> javaSources = new HashSet(Utils.listJavaSources(COMMONS_COLLECTIONS4_40_ORIGINAL,true));
        Set<CloneDetector.CloneRecord> records = new ASTBasedCloneDetector().detect(COMMONS_COLLECTIONS4_40_ORIGINAL,COMMONS_COLLECTIONS4_40_RENAMED_PACKAGES);
        assertEquals(javaSources.size(),records.size());
        for (CloneDetector.CloneRecord record:records) {
            assertEquals(1.0,record.getConvidence());
        }

        Set<Path> set1 = new HashSet<>(javaSources);
        Set<Path> set2 = records.stream()
            .map(r -> r.getOriginal())
            .collect(Collectors.toSet());
        assertEquals(set1,set2);
    }



}
