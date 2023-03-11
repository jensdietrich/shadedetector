package nz.ac.wgtn.shadedetector;

import nz.ac.wgtn.shadedetector.resultsetconsolidation.ArtifactOccursInAllResultSets;
import nz.ac.wgtn.shadedetector.resultsetconsolidation.ArtifactOccursInMoreThanOneResultSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instantiates and sets up a search result consolidation strategy from a configuration string containing a name.
 * @author jens dietrich
 */
public class ArtifactSearchResultConsolidationStrategyFactory extends  AbstractServiceLoaderFactory<ArtifactSearchResultConsolidationStrategy> {

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(ArtifactSearchResultConsolidationStrategyFactory.class);
    }

    @Override
    public ArtifactSearchResultConsolidationStrategy getDefault() {
        return new ArtifactOccursInMoreThanOneResultSets();
    }

    @Override
    public ArtifactSearchResultConsolidationStrategy create(String configuration) {
        return create(ArtifactSearchResultConsolidationStrategy.class,"artifact search result consolidation strategy",configuration);
    }
}
