package nz.ac.wgtn.shadedetector;

import java.io.File;

/**
 * Abstraction for a utility to detect the similarity of source code files.
 * TODO: at the moment the intention is to use this to compare .java files, but this could be extended to .kotlin files etc
 * @author jens dietrich
 */
public interface CloneDetector extends NamedService {

    /**
     * Compare two source code files (.java). Return a similarity score between 0 (not similar) to 1 (very similar / identical).
     * @param src1
     * @param src2
     * @return
     */
    double getSimilarityScore(File src1, File src2) ;
}
