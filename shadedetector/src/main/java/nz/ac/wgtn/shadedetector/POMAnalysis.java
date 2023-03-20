package nz.ac.wgtn.shadedetector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utilities related to the POM. In particular, this is used to establish whether
 * dependencies exist.
 * @author jens dietrich
 */
public class POMAnalysis {


    public static boolean hasDependency (Artifact artifact, GAV dependency) throws Exception {
        Predicate<MVNDependency> condition = d ->
            d.getGroupId().equals(dependency.getGroupId())
            && d.getArtifactId().equals(dependency.getArtifactId())
            && d.getVersion().equals(dependency.getVersion());
        return hasDependency(artifact,condition);
    }

    // this can be used to look for a dependency of any version
    public static boolean hasDependency (Artifact artifact, String groupId, String artifactId) throws Exception {
        Predicate<MVNDependency> condition = d -> d.getGroupId().equals(groupId) && d.getArtifactId().equals(artifactId);
        return hasDependency(artifact,condition);    }

    private static boolean hasDependency (Artifact artifact, Predicate<MVNDependency> condition) throws Exception {
        Path pom = FetchResources.fetchPOM(artifact);
        List<MVNDependency> dependencies = getDependencies(pom.toFile());
        return dependencies.stream().anyMatch(condition);
    }

    public static List<MVNDependency> getDependencies(File pom) throws Exception {
        // @TODO support inheriting versions when using <dependencyManagement>
        NodeList nodeList1 = Utils.evalXPath(pom, "/project/dependencies/dependency");
        NodeList nodeList2 = Utils.evalXPath(pom, "//dependencyManagement/dependencies/dependency");
        NodeList mergedList = new NodeList() {
            @Override
            public Node item(int index) {
                return index < nodeList1.getLength() ? nodeList1.item(index) : nodeList2.item(index-nodeList1.getLength());
            }

            @Override
            public int getLength() {
                return nodeList1.getLength() + nodeList2.getLength();
            }
        };
        return MVNDependency.from(mergedList);
    }
}
