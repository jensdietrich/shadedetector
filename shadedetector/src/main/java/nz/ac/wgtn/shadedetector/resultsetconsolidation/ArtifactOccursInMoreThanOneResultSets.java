package nz.ac.wgtn.shadedetector.resultsetconsolidation;

import nz.ac.wgtn.shadedetector.Artifact;
import nz.ac.wgtn.shadedetector.ArtifactSearchResponse;
import nz.ac.wgtn.shadedetector.ArtifactSearchResultConsolidationStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Merge result sets for multiple queries only considering artifacts that occur in more than one result sets.
 * @author jens dietrich
 */
public class ArtifactOccursInMoreThanOneResultSets implements ArtifactSearchResultConsolidationStrategy {
    @Override
    public String name() {
        return "moreThanOne";
    }

    @Override
    public List<Artifact> consolidate(Map<String, ArtifactSearchResponse> searchResults) {
        if (searchResults==null || searchResults.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        Map<Artifact,Integer> counter = countArtifactOccurrences(searchResults);
        return counter.keySet().stream()
            .filter(artifact -> counter.get(artifact)>1)
            .collect(Collectors.toList());
    }
}
