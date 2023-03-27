package nz.ac.wgtn.shadedetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utilities related to the POM. In particular, this is used to establish whether
 * dependencies exist.
 * @author jens dietrich
 */
public class POMAnalysis {


    private static Logger LOGGER = LoggerFactory.getLogger(POMAnalysis.class);

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
        return hasDependency(artifact,condition);
    }

    public static boolean hasDependency (Path pom, String groupId, String artifactId) throws Exception {
        Predicate<MVNDependency> condition = d -> d.getGroupId().equals(groupId) && d.getArtifactId().equals(artifactId);
        return hasDependency(pom,condition);
    }


    static boolean hasDependency (Artifact artifact, Predicate<MVNDependency> condition) throws Exception {
        Path pom = FetchResources.fetchPOM(artifact);
        return hasDependency(pom,condition);
    }

    static boolean hasDependency (Path pom, Predicate<MVNDependency> condition) throws Exception {
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

    public static boolean shadePluginIncludes(Artifact artifact,String groupId,String artifactId) throws Exception {
        Path pom = FetchResources.fetchPOM(artifact);
        return shadePluginIncludes(pom,groupId,artifactId);
    }


        /**
         * Check whether the shade plugin includes packages with any of the prefixes passed as argument.
         */
    public static boolean shadePluginIncludes(Path pom,String groupId,String artifactId) throws Exception {
        NodeList nodes = Utils.evalXPath(pom.toFile(), "/project/build/plugins/plugin[artifactId='maven-shade-plugin']//includes/include");
        for (int i=0;i<nodes.getLength();i++) {
            String value = nodes.item(i).getTextContent();
            // glob pattern
            if (value.contains(":")) {
                String[] tokens = value.split(":");
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:"+tokens[0]);
                boolean groupMatches = matcher.matches(Path.of(groupId));
                if (groupMatches) {
                    matcher = FileSystems.getDefault().getPathMatcher("glob:" + tokens[1]);
                    boolean artifactMatches = matcher.matches(Path.of(artifactId));
                    if (artifactMatches) {
                        return true;
                    }
                }
            }
            // package prefix only, used as in org.yaml.**
            else if (value.endsWith(".**")) {
                String prefix = value.replace(".**","");
                if (groupId.startsWith(prefix)) {
                    return true;
                }
            }

            else {
                LOGGER.warn("Unexpected maven-shade-plugin include: {}",value);
            }
        }
        return false;
    }

    public static boolean references(Path pom,String groupId,String artifactId) throws Exception {
        return hasDependency(pom,groupId,artifactId) || shadePluginIncludes(pom,groupId,artifactId);
    }

    public static boolean references(Artifact artifact,String groupId,String artifactId) throws Exception {
        return hasDependency(artifact,groupId,artifactId) || shadePluginIncludes(artifact,groupId,artifactId);
    }

}
