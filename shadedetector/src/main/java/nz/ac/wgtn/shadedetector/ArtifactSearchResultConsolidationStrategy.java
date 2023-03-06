package nz.ac.wgtn.shadedetector;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Given search results by class name used by query, select the ones to proceed by some heuristics.
 * Example: merge sets, use intersection, select all that occur in at least two result sets, etc.
 * @author jens dietrich
 */
public interface ArtifactSearchResultConsolidationStrategy {
    List<Artifact> consolidate (Map<String,ArtifactSearchResponse> searchResults);

    ArtifactSearchResultConsolidationStrategy Merge = new ArtifactSearchResultConsolidationStrategy() {
        @Override
        public List<Artifact> consolidate(Map<String, ArtifactSearchResponse> searchResults) {
            if (searchResults==null || searchResults.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            return searchResults.values().stream()
                .flatMap(result -> result.getBody().getArtifacts().stream())
                .collect(Collectors.toList());
        }
    };

    ArtifactSearchResultConsolidationStrategy Intersect = new ArtifactSearchResultConsolidationStrategy() {
        @Override
        public List<Artifact> consolidate(Map<String, ArtifactSearchResponse> searchResults) {
            if (searchResults==null || searchResults.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            Map<Artifact,Integer> counter = countArtifactOccurrences(searchResults);
            return counter.keySet().stream()
                .filter(artifact -> counter.get(artifact)==searchResults.size())
                .collect(Collectors.toList());

        }
    };

    ArtifactSearchResultConsolidationStrategy MultipleOccurrences = new ArtifactSearchResultConsolidationStrategy() {
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
    };

    default Map<Artifact,Integer> countArtifactOccurrences (Map<String, ArtifactSearchResponse> searchResults) {
        Map<Artifact,Integer> counter = new HashMap<>();
        searchResults.values().stream()
            .flatMap(result -> result.getBody().getArtifacts().stream())
            .forEach(artifact -> {
                if (counter.containsKey(artifact)) {
                    counter.put(artifact,counter.get(artifact)+1);
                }
                else {
                    counter.put(artifact,1);
                }
            });
        return counter;
    }
}
