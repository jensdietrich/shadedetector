package nz.ac.wgtn.shadedetector;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * Abstraction for a utility to detect the similarity of source code files.
 * TODO: at the moment the intention is to use this to compare .java files, but this could be extended to .kotlin files etc
 * @author jens dietrich
 */
public interface CloneDetector extends NamedService {

    // represents the results of a clone analysis
    // the paths may point to locations within a jar (using the zip file system)
    class CloneRecord {
        // a value between 0 and 1 (0 - no similarity, 1 -- highest similarity / equality)
        private double convidence = 0;
        // class references are fully qualified class names / paths.
        private Path original = null;
        private Path clone = null;

        public CloneRecord(double convidence, Path original, Path clone) {
            this.convidence = convidence;
            this.original = original;
            this.clone = clone;
        }

        public double getConvidence() {
            return convidence;
        }

        public void setConvidence(double convidence) {
            this.convidence = convidence;
        }

        public Path getOriginal() {
            return original;
        }

        public void setOriginal(Path original) {
            this.original = original;
        }

        public Path getClone() {
            return clone;
        }

        public void setClone(Path clone) {
            this.clone = clone;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CloneRecord that = (CloneRecord) o;
            return Double.compare(that.convidence, convidence) == 0 && Objects.equals(original, that.original) && Objects.equals(clone, that.clone);
        }

        @Override
        public int hashCode() {
            return Objects.hash(convidence, original, clone);
        }
    }

    /**
     * Compare two zip / jar files or folders containing source files, and measure similarity.
     * The result contains records describing potenial clones.
     *
     * @param original a jar, zip or folder containing sources of the original library
     * @param cloneCandidate a jar, zip or folder containing sources of the library that may contain clones
     * @return
     */
    Set<CloneRecord> detect(Path original, Path cloneCandidate) ;

}
