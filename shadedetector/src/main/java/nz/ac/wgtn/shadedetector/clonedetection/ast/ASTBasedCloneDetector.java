package nz.ac.wgtn.shadedetector.clonedetection.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.*;
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
        if (node1.getClass() != node2.getClass()) {  // must be of the same kind
            return false;
        }
        if (node1 instanceof SimpleName) {
            return analyseCloneForSimpleNames((SimpleName)node1,(SimpleName)node2);
        }
        else if (node1 instanceof LiteralStringValueExpr)  {
            return analyseCloneForLiterals((LiteralStringValueExpr)node1, (LiteralStringValueExpr)node2);
        }
        else if (node1 instanceof BooleanLiteralExpr)  {
            return analyseCloneForLiterals((BooleanLiteralExpr)node1, (BooleanLiteralExpr)node2);
        }
        else if (node1 instanceof BinaryExpr)  {
            return analyseCloneForBinaryExpressions((BinaryExpr)node1, (BinaryExpr)node2);
        }
        else if (node1 instanceof UnaryExpr)  {
            return analyseCloneForUnaryExpressions((UnaryExpr)node1, (UnaryExpr)node2);
        }

        boolean result = true;
        for (int i=0;i<relevantChildNodes1.size();i++) {
            Node childNode1 = relevantChildNodes1.get(i);
            Node childNode2 = relevantChildNodes2.get(i);
            result = result && analyseClone(childNode1,childNode2);
            // @TODO very coarse, likely to produce some FPs, should also look into actual type names, constants, operators etc

        }
        return result;
    }

    static boolean analyseCloneForSimpleNames(SimpleName node1, SimpleName node2) {
        return node1.getId().equals(node2.getId());
    }

    static boolean analyseCloneForLiterals(LiteralStringValueExpr node1, LiteralStringValueExpr node2) {
        return node1.getValue().equals(node2.getValue());
    }

    static boolean analyseCloneForLiterals(BooleanLiteralExpr node1, BooleanLiteralExpr node2) {
        return node1.getValue()==node2.getValue();
    }

    static boolean analyseCloneForBinaryExpressions(BinaryExpr node1, BinaryExpr node2) {
        return node1.getOperator() == node2.getOperator() &&
            analyseClone(node1.getLeft(),node2.getLeft())
            && analyseClone(node1.getRight(),node2.getRight())
            ;
    }

    static boolean analyseCloneForUnaryExpressions(UnaryExpr node1, UnaryExpr node2) {
        return node1.getOperator() == node2.getOperator() &&
            analyseClone(node1.getExpression(),node2.getExpression())
            ;
    }


}
