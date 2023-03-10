package nz.ac.wgtn.shadedetector;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility to fetch resources associated with artifact.
 * Resources will be returned as files (java.io.File), referencing a location in the local cache.
 * @author jens dietrich
 */
public class FetchResources {

    private static Logger LOGGER = LoggerFactory.getLogger(FetchResources.class);
    private static File BIN_CACHE = new File(".cache/bin");
    private static File SRC_CACHE = new File(".cache/src");
    public static final String SEARCH_URL = "https://search.maven.org/remotecontent";


    // for testing only
    // @TODO remove
    public static void main(String[] args) throws ArtifactSearchException {
        ArtifactSearchResponse artifactSearchResult = ArtifactSearch.findShadingArtifacts("InvokerTransformer",5,200);
        AtomicInteger allCounter = new AtomicInteger();
        AtomicInteger fetchSuccessCounter = new AtomicInteger();
        AtomicInteger fetchSuccessButNotAZipCounter = new AtomicInteger();
        for (Artifact artifact:artifactSearchResult.getBody().getArtifacts()) {
            LOGGER.info("Looking for sources for artifact " + artifact.getId());
            allCounter.incrementAndGet();
            try {
                File source = fetchSources(artifact);
                LOGGER.info("\tsource code fetched: " + source.getAbsolutePath());
                fetchSuccessCounter.incrementAndGet();
                if (!Utils.isZip(source)) {
                    LOGGER.warn("\tnot a zip file: " + source.getAbsolutePath());
                    fetchSuccessButNotAZipCounter.incrementAndGet();
                }
            }
            catch (Exception x) {
                LOGGER.error("\terror fetching sources for " + artifact.getId());
            }
        }

        LOGGER.info("artifacts processed: " + allCounter.get());
        LOGGER.info("fetch succeeded: " + fetchSuccessCounter.get());
        LOGGER.info("fetch succeeded but not a zip: " + fetchSuccessButNotAZipCounter.get());
    }

    private static File fetchBinaries (Artifact artifact) throws IOException {
        GAV gav = new GAV(artifact.getGroupId(),artifact.getArtifactId(),artifact.getVersion());
        if (!artifact.getResources().contains(".jar")) {
            throw new IllegalStateException("no source code found for artifact " + artifact.getId());
        }
        File cached = getCachedBin(gav,"jar");
        return fetch(gav,cached,".jar");
    }

    private static File fetchSources (Artifact artifact) throws IOException {
        GAV gav = new GAV(artifact.getGroupId(),artifact.getArtifactId(),artifact.getVersion());
        String sourceSuffix = artifact.getResources().stream()
            .filter(r -> r.contains("sources") || r.contains("src"))
            //.orElse(null);
            .findFirst().orElse(null);
        if (sourceSuffix==null) {
            throw new IllegalStateException("no source code found for artifact " + artifact.getId());
        }

        File cached = getCachedSrc(gav,sourceSuffix);
        return fetch(gav,cached,sourceSuffix);
    }

    private static File fetch(GAV gav,File cached,String suffix) throws IOException {

        if (cached.exists()) {
            LOGGER.info("using cached data from " + cached.getAbsolutePath());
        }
        else {

            // https://search.maven.org/remotecontent?filepath=com/jolira/guice/3.0.0/guice-3.0.0.pom
            OkHttpClient client = new OkHttpClient();

            HttpUrl.Builder urlBuilder = HttpUrl.parse(SEARCH_URL).newBuilder();

            String remotePath = gav.getGroupId().replace(".","/");
            remotePath = remotePath + '/' + gav.getArtifactId() + '/' + gav.getVersion() + '/' + gav.getArtifactId() + '-' + gav.getVersion();

            // urlBuilder.addQueryParameter("filepath", remotePath);

            // https://repo1.maven.org/maven2/com/iussoft/iussoft-api/3.0.1/iussoft-api-3.0.1-sources.jar
            String url = urlBuilder.build().toString();
            url = url + "?filepath=" + remotePath + suffix;
            LOGGER.info("\tsearch url: " + url);
            Request request = new Request.Builder().url(url).build();

            Call call = client.newCall(request);
            Response response = null;
            response = call.execute();

            int responseCode = response.code();
            LOGGER.info("\tresponse code is: " + responseCode);

            if (responseCode==200) {
                try (InputStream in = response.body().byteStream(); OutputStream out = new FileOutputStream(cached)) {
                    LOGGER.info("\tcaching data in " + cached.getAbsolutePath());
                    ByteStreams.copy(in,out);
                }
            }
            else {
                throw new IOException("failed to download resource from " + url + " , response code: " + responseCode + " - " + response.message() );
            }
        }

        assert cached.exists();
        return cached;
    }

    private static File getCachedBin(GAV gav,String suffix) {
        return getCached(BIN_CACHE,gav,gav.getArtifactId()+"-"+gav.getVersion()+suffix);
    }

    private static File getCachedSrc(GAV gav,String suffix) {
        return getCached(SRC_CACHE,gav,gav.getArtifactId()+"-"+gav.getVersion()+suffix);
    }

    // @TODO can we use name given by project -- need to check whether query results return it
    private static File getCached(File cacheRoot, GAV gav, String fileName) {
        File groupFolder = new File(cacheRoot,gav.getGroupId());
        File artifactFolder = new File(groupFolder,gav.getArtifactId());
        File versionFolder = new File(artifactFolder,gav.getVersion());
        if (!versionFolder.exists()) {
            versionFolder.mkdirs();
        }

        return new File(versionFolder,fileName);
    }
}