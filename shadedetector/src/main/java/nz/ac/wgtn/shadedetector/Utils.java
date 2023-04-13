package nz.ac.wgtn.shadedetector;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.nio.file.FileSystem;

/**
 * Miscellaneous utilities.
 * @author jens dietrich
 */
public class Utils {

    private static Logger LOGGER = LoggerFactory.getLogger(ArtifactSearch.class);

    public static List<File> listSourcecodeFilesInFolder(File folder) throws IOException {
        return Files.walk(folder.toPath())
            .map(p -> p.toFile())
            .filter(f -> f.getName().endsWith(".java"))
            .collect(Collectors.toList());
    }

    public static List<String> loadClassListFromFile(File file) throws IOException {
        if (file!=null) {
            LOGGER.info("loading classlist from file " + file.getAbsolutePath());
        }
        Preconditions.checkArgument(file.exists());
        Preconditions.checkArgument(!file.isDirectory());
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return loadClassListFromResource(reader);
        }
    }

    public static List<String> loadClassListFromFile(String fileName) throws IOException {
        return loadClassListFromFile(new File(fileName));
    }

    public static List<String> loadClassListFromResource(String path) throws IOException {
        if (path!=null) {
            LOGGER.info("loading classlist from resource " + path);
        }
        URL url = Utils.class.getClassLoader().getResource(path);
        LOGGER.info("loading classlist from url " + url);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return loadClassListFromResource(reader);
        }
    }

    public static List<String> loadClassListFromResource(BufferedReader reader) throws IOException {
        return reader.lines()
            .filter(line -> !line.isBlank())
            .filter(line -> !line.trim().startsWith("#")) // remove comments
            .collect(Collectors.toList());
    }

    public static boolean isZip (File f) {
        int fileSignature = 0;
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            fileSignature = raf.readInt();
        } catch (IOException e) {}
        return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
    }


    public static List<String> getUnqualifiedJavaClassNames(Path zipOrFolder) {
        try {
            return listJavaSources(zipOrFolder,true).stream()
                .map(f -> f.getFileName().toString())
                .map(n -> n.replace(".java",""))
                .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error collecting Java class names from source code",e);
            throw new RuntimeException(e);
        }
    }

    public static List<Path> listJavaSources(Path zipOrFolder,boolean excludePackageInfo) throws IOException {
        Predicate<Path> filter = path -> path.toString().endsWith(".java");
        if (excludePackageInfo) {
            filter = filter.and(p -> !p.toString().endsWith("package-info.java"));
        }
        return listContent(zipOrFolder,filter);
    }

    public static List<Path> listContent(Path zipOrFolder, Predicate<Path> filter) throws IOException {
        if (zipOrFolder.toFile().isDirectory()) {
            return Files.walk(zipOrFolder)
                .filter(file -> !Files.isDirectory(file))
                .filter(filter)
                .collect(Collectors.toList());
        }
        else {

            Map<String, String> env = new HashMap<>();
            FileSystem fs = FileSystems.newFileSystem(zipOrFolder, env, null);
            return Streams.stream(fs.getRootDirectories())
                .flatMap(root -> {
                    try {
                        return Files.walk(root);
                    }
                    catch (IOException x) {
                        LOGGER.error("Error extracting content of file system",x);
                        throw new RuntimeException(x);
                    }
                })
                .filter(filter)
                .collect(Collectors.toList());

        }

    }


    public static NodeList evalXPath(File file, String xpath) throws Exception {
        Preconditions.checkArgument(isXML(file),"file is not an xml file: " + file.getAbsolutePath());
        FileInputStream fileIS = new FileInputStream(file);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(fileIS);
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (NodeList) xPath.compile(xpath).evaluate(xmlDocument, XPathConstants.NODESET);
    }

    /**
     * Evaluate xpath, assume that the expression yield a single node.
     * If no node is in the result set, return null.
     * If multiple nodes are returned, throw an IllegalArgumentException.
     * @param file
     * @param xpath
     * @return
     * @throws Exception
     */
    public static String evalXPathSingleNode(File file, String xpath) throws Exception {
        Preconditions.checkArgument(isXML(file),"file is not an xml file: " + file.getAbsolutePath());
        FileInputStream fileIS = new FileInputStream(file);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(fileIS);
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = evalXPath(file,xpath);
        if (nodeList.getLength()==0) {
            return null;
        }
        else if (nodeList.getLength()>1) {
            throw new IllegalArgumentException("XPath query too general, resulted in multiple nodes in result set");
        }
        else {
            return nodeList.item(0).getTextContent();
        }
    }

    /**
     * Evaluate xpath, assume that the expression yield a single node.
     * If no node is in the result set, multiple nodes are returned, or the value cannot be converted to an int,
     * throws a runtime exception
     * @param file
     * @param xpath
     * @return
     * @throws Exception
     */
    public static int evalXPathSingleNodeAsInt(File file, String xpath) throws Exception {
        Preconditions.checkArgument(isXML(file),"file is not an xml file: " + file.getAbsolutePath());
        FileInputStream fileIS = new FileInputStream(file);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(fileIS);
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = evalXPath(file,xpath);
        if (nodeList.getLength()==0) {
            throw new IllegalArgumentException("XPath query too specific, resulted in empty result set");
        }
        else if (nodeList.getLength()>1) {
            throw new IllegalArgumentException("XPath query too general, resulted in multiple nodes in result set");
        }
        else {
            String value = nodeList.item(0).getTextContent();
            return Integer.parseInt(value);
        }
    }

    /**
     * Check whether this is an xml file.
     * @param file
     * @return
     * @throws Exception
     */
    public static boolean isXML(File file) throws Exception {
        Preconditions.checkArgument(file.exists(),"file does not exist: " + file.getAbsolutePath());
        try {
            FileInputStream fileIS = new FileInputStream(file);
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            builder.parse(fileIS);
            return true;
        }
        catch (SAXException e) {
            return false;
        }
    }

    public static boolean isValidXML(File file) throws Exception {
        Preconditions.checkArgument(isXML(file),"file is not an xml file: " + file.getAbsolutePath());
        try {
            Source xmlInput=new StreamSource(new FileReader(file));
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema();
            Validator validator = schema.newValidator();
            validator.validate(xmlInput);
            return true;
        }
        catch (SAXException e) {
            return false;
        }
    }

    public static String printStacktrace(Throwable x) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        x.printStackTrace(pw);
        return sw.toString();
    }
}
