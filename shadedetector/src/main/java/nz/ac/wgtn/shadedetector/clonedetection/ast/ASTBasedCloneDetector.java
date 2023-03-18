package nz.ac.wgtn.shadedetector.clonedetection.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.utils.Pair;
import nz.ac.wgtn.shadedetector.CloneDetector;
import nz.ac.wgtn.shadedetector.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Clone detection based on comparing ASTs.
 * @author jens dietrich
 */
public class ASTBasedCloneDetector implements CloneDetector  {

    private static Logger LOGGER = LoggerFactory.getLogger(ASTBasedCloneDetector.class);
    public static final Predicate<Node> IS_RELEVANT_CHILD_NODE = node -> !(node instanceof Comment) && !(node instanceof ImportDeclaration) && !(node instanceof PackageDeclaration);


    @Override
    public String name() {
        return "ast";
    }

    @Override
    public Set<CloneRecord> detect(Path original, Path cloneCandidate) {
        try {
            List<Path> originalJavaSources = Utils.listJavaSources(original,true);
            List<Path> cloneCandidateJavaSources = Utils.listJavaSources(original,true);

            List<Pair<Path,Path>> potentialMatches = new ArrayList<>();
            for (Path originalSource:originalJavaSources) {
                String cuName1 = originalSource.getName(originalSource.getNameCount()-1).toString();
                // skip package-info.java with package meta data
                if (!cuName1.equals("package-info.java")) {
                    for (Path cloneSource : cloneCandidateJavaSources) {
                        String cuName2 = cloneSource.getName(cloneSource.getNameCount() - 1).toString();
                        if (Objects.equals(cuName1, cuName2)) {
                            potentialMatches.add(new Pair<>(originalSource, cloneSource));
                        }
                    }
                }
            }

            LOGGER.info("Analysing {} pair of java source code",potentialMatches.size());

            return potentialMatches.stream()
                .map(match -> analyseClone(match.a,match.b))
                .collect(Collectors.toSet());
        }
        catch (IOException x) {
            LOGGER.error("Error extracting Java sources from {},{}",original,cloneCandidate,x);
        }
        return Collections.EMPTY_SET;
    }

    static CloneRecord analyseClone(Path path1, Path path2) {
        try {
            CompilationUnit cu1 = StaticJavaParser.parse(path1);
            CompilationUnit cu2 = StaticJavaParser.parse(path2);
            String pck1 = cu1.getPackageDeclaration().isPresent() ? cu1.getPackageDeclaration().get().getNameAsString() : "";
            String pck2 = cu2.getPackageDeclaration().isPresent() ? cu1.getPackageDeclaration().get().getNameAsString() : "";
            boolean samePackage = Objects.equals(pck1,pck2);
            return new CloneRecord(analyseClone(cu1,cu2)?1.0:0.0,path1,path2);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean analyseClone(Node node1, Node node2) {
        List<Node> relevantChildNodes1 = node1.getChildNodes().stream().filter(IS_RELEVANT_CHILD_NODE).collect(Collectors.toList());
        List<Node> relevantChildNodes2 = node2.getChildNodes().stream().filter(IS_RELEVANT_CHILD_NODE).collect(Collectors.toList());
        if (relevantChildNodes1.size()!=relevantChildNodes2.size()) {
            return false;
        }
        for (int i=0;i<relevantChildNodes1.size();i++) {
            Node childNode1 = relevantChildNodes1.get(i);
            Node childNode2 = relevantChildNodes2.get(i);
            if (childNode1.getClass() != childNode2.getClass()) {  // must be of the same kind
                return false;
            }
            // @TODO very coarse, likely to produce some FPs, should also look into actual type names, constants, operators etc

        }
        return true;
    }

}
