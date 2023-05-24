package nz.ac.wgtn.shadedetector.resultsetconsolidation;

import nz.ac.wgtn.shadedetector.Artifact;
import nz.ac.wgtn.shadedetector.ArtifactSearchResponse;
import nz.ac.wgtn.shadedetector.ArtifactSearchResultConsolidationStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Merge result sets for multiple queries using union.
 * I.e. an artifact will be investigated if it is in any result set (each result set corresponding to a class name).
 * @author jens dietrich
 */
public class ArtifactOccursInAnyResultSet implements ArtifactSearchResultConsolidationStrategy {
    @Override
    public String name() {
        return "any";
    }

    @Override
    public List<Artifact> consolidate(Map<String, ArtifactSearchResponse> searchResults) {
        if (searchResults==null || searchResults.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        return searchResults.values().stream()
            .flatMap(result -> result.getBody().getArtifacts().stream())
            .collect(Collectors.toList());
    }
}
