package nz.ac.wgtn.shadedetector.clonedetection;

import com.google.common.io.RecursiveDeleteOption;
import net.lingala.zip4j.ZipFile;
import net.sourceforge.pmd.cpd.*;
import nz.ac.wgtn.shadedetector.CloneDetector;
import com.google.common.io.MoreFiles;
import java.io.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.*;

/**
 * Implementation that uses CPD clone detection provided with PMD.
 * @author shawn
 */
public class CPDCloneDetector implements CloneDetector  {
    static int  MIN_TOKENS = 10;
    public static final File TMP = new File(".tmp");

    @Override
    public Set<CloneRecord> detect(Path original, Path cloneCandidate) {


        CPDConfiguration configuration = new CPDConfiguration();
        configuration.setMinimumTileSize(MIN_TOKENS);
        configuration.setLanguage(new JavaLanguage());
        CPD cpd = new CPD(configuration);

        try {

            if (TMP.exists()) {
                MoreFiles.deleteRecursively(TMP.toPath(), RecursiveDeleteOption.ALLOW_INSECURE);
            }

            TMP.mkdirs();
            ZipFile originalZip = new ZipFile(original.toFile());
            ZipFile candidateZip = new ZipFile(cloneCandidate.toFile());

            originalZip.extractAll(TMP.getAbsolutePath()+"/original");
            candidateZip.extractAll(TMP.getAbsolutePath()+"/candidate");
            cpd.addRecursively(TMP);

        } catch (Exception e) {
            e.printStackTrace();
        }
        cpd.go();

        Iterator<Match> matches = cpd.getMatches();
        while (matches.hasNext()) {
            Match match = matches.next();
            String firstFileName = match.getFirstMark().getFilename();
            String secondFileName = match.getSecondMark().getFilename();
            int tokenCount = match.getTokenCount();
        }
        // TODO interpret matched token count as a confidence score
        return Collections.EMPTY_SET;
    }

    @Override
    public String name() {
        return "CPD";
    }
}
