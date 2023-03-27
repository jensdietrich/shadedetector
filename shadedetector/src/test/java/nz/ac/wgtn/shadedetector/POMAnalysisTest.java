package nz.ac.wgtn.shadedetector;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class POMAnalysisTest {


    @Test
    public void testShadePluginIncludeWithArtifactWildcard() throws Exception {
        File pom = new File(POMAnalysisTest.class.getResource("/poms/common-test-146.pom").getFile());
        Assumptions.assumeTrue(pom.exists());
        assertTrue(POMAnalysis.shadePluginIncludes(pom.toPath(),"org.apache.commons","commons-collections4"));
    }

    @Test
    public void testShadePluginIncludeWithFullArtifactName() throws Exception {
        File pom = new File(POMAnalysisTest.class.getResource("/poms/common-test-146.pom").getFile());
        Assumptions.assumeTrue(pom.exists());
        assertTrue(POMAnalysis.shadePluginIncludes(pom.toPath(),"com.edropple.jregex","jregex"));
    }

    @Test
    public void testShadePluginIncludeWithFullArtifactNameNeg() throws Exception {
        File pom = new File(POMAnalysisTest.class.getResource("/poms/common-test-146.pom").getFile());
        Assumptions.assumeTrue(pom.exists());
        assertFalse(POMAnalysis.shadePluginIncludes(pom.toPath(),"com.edropple.jregex","foo"));
    }

    @Test
    public void testShadePluginIncludeWithGroupPrefix1() throws Exception {
        File pom = new File(POMAnalysisTest.class.getResource("/poms/jmx_prometheus_javaagent-0.3.0.pom").getFile());
        Assumptions.assumeTrue(pom.exists());
        assertTrue(POMAnalysis.shadePluginIncludes(pom.toPath(),"org.yaml","snakeyaml"));
    }

    @Test
    public void testShadePluginIncludeWithGroupPrefix2() throws Exception {
        File pom = new File(POMAnalysisTest.class.getResource("/poms/jmx_prometheus_javaagent-0.3.0.pom").getFile());
        Assumptions.assumeTrue(pom.exists());
        assertTrue(POMAnalysis.shadePluginIncludes(pom.toPath(),"org.yaml.foo","bar"));
    }

    @Test
    public void testShadePluginIncludeWithGroupPrefixNeg1() throws Exception {
        File pom = new File(POMAnalysisTest.class.getResource("/poms/jmx_prometheus_javaagent-0.3.0.pom").getFile());
        Assumptions.assumeTrue(pom.exists());
        assertFalse(POMAnalysis.shadePluginIncludes(pom.toPath(),"org.yaaml.foo","bar"));
    }

}
