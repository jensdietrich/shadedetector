package nz.ac.wgtn.shadedetector;


import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility to transparently merge responses. Some meta information of individual responses is lost,
 * but individual responses are stored in the cache and can be retried there.
 * @author jens dietrich
 */
public class ArtifactSearchResponseMerger {

   public static ArtifactSearchResponse merge (List<ArtifactSearchResponse> responses) {
       ArtifactSearchResponse merged = new ArtifactSearchResponse();

       // merge head -- attributes are not very important for future processing as origional
       // responses are retained in cache, this is mainly to merge the artifact sets

       ResponseHeader mergedHeader = new ResponseHeader();
       // use first , as this is mainly used to determine whether the dataset is outdated
       // -1 encode error
       mergedHeader.setQtime(responses.stream().mapToInt(r -> r.getHeader().getQtime()).reduce(Integer::min).orElse(-1));
       // positive status represents error code
       mergedHeader.setStatus(responses.stream().mapToInt(r -> r.getHeader().getStatus()).reduce(Integer::max).orElse(0));

       ResponseHeader.Parameters mergedParameters = new ResponseHeader.Parameters();
       mergedParameters.setRows(responses.stream().mapToInt(r -> r.getHeader().getParameters().getRows()).reduce(Integer::sum).orElse(0));
       mergedHeader.setParameters(mergedParameters);
       merged.setHeader(mergedHeader);

       // merge body
       ResponseBody mergedBody = new ResponseBody();
       mergedBody.setArtifacts(responses.stream().flatMap(r -> r.getBody().getArtifacts().stream()).collect(Collectors.toList()));
       mergedBody.setNumFound(responses.stream().mapToInt(r -> r.getBody().getNumFound()).reduce(Integer::sum).orElse(0));
       mergedBody.setStart(responses.stream().mapToInt(r -> r.getBody().getStart()).reduce(Integer::min).orElse(0));
       merged.setBody(mergedBody);

       return merged;
   }
}
