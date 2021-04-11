import com.beust.jcommander.ParameterException;
import com.company.Main;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {
        private static final String TEST_DATA_PATH = "src/test/resources/testData";
        private static final String TEMPORARY_DIRECTORY = "temp_test_data";
        private static final String BZIP2_SUFFIX = ".bz2";
        private static final long TIMEOUT = 30L;

        @BeforeAll
        static void createArchives() {
            var temp = new File(TEMPORARY_DIRECTORY);
            if (!temp.exists()){
                temp.mkdirs();
            }
            var testDir = new File(TEST_DATA_PATH);
            for(File file : testDir.listFiles()) {
                if (file.getName().endsWith(".xml")) {
                    try {
                        createTemporaryBzip2File(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // create invalid Bzip2 file
            File invalid = new File(toBzip2Inputs("invalid"));
            try {
                FileWriter fw = new FileWriter(invalid);
                fw.append("Превед, Медвед!");
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @AfterAll
        static void cleanUp() {
            File file = new File(TEMPORARY_DIRECTORY);
            for (File listFile : file.listFiles()) {
                listFile.delete();
            }
            file.delete();
        }

        private static String toBzip2Inputs(String filename) {
            //TODO fix
            String[] files = filename.split(",");
            for (int i = 0; i < files.length; i++) {
                files[i] = Paths.get(TEMPORARY_DIRECTORY).resolve(files[i] + BZIP2_SUFFIX).toString();
            }
            StringBuilder result = new StringBuilder();
            for(String s : files) {
                result.append(s).append(",");
            }
            result.deleteCharAt(result.length()-1);
            return result.toString();
        }

        private static void createTemporaryBzip2File(File file) throws IOException {
            InputStream in = Files.newInputStream(file.toPath());
            File outputFile = new File (toBzip2Inputs(file.getName()));
            OutputStream fout = Files.newOutputStream((outputFile.toPath()));
            BufferedOutputStream out = new BufferedOutputStream(fout);
            BZip2CompressorOutputStream bzOut = new BZip2CompressorOutputStream(out);
            final byte[] buffer = new byte[4096];
            int n = 0;
            while (-1 != (n = in.read(buffer))) {
                bzOut.write(buffer, 0, n);
            }
            bzOut.close();
            in.close();
        }

    @Test
    @Timeout(TIMEOUT)
    void goodXML() {
        testInputs("simple.xml",1);
    }

    @Test
    @Timeout(TIMEOUT)
    void notWellFormedXml() {
            assertThrows(Throwable.class, () -> testInputs("not-well-formed.xml", 1));
    }

    @Test
    @Timeout(TIMEOUT)
    void missedTagsInXml() {
        testInputs("missed-tags.xml", 1);
    }

    @Test
    @Timeout(TIMEOUT)
    void XMLWithoutPages() {
        testInputs("no-pages.xml", 1);
    }

    @Disabled
    @Test
    @Timeout(TIMEOUT)
    void WrongNestingOfTagsInXml() {
        testInputs("wrong-nesting.xml", 1);
    }

    @Test
    @Timeout(TIMEOUT)
    void incorrectBzip2() {
            //TODO FIX always true
        assertThrows(Throwable.class, () -> testInputs("invalid", 1));
    }

    @Test
    @Timeout(TIMEOUT)
    void incorrectInput() {
       assertThrows(ParameterException.class, () -> testInputs("nonexistent", 1));
    }

    @Test
    @Timeout(TIMEOUT)
    void multipleInputs() {
       testInputs("simple.xml,second.xml", 1);
    }

    @Test
    @Timeout(TIMEOUT)
    void bigXmlSingleThread() {
        testInputs("big.xml", 1);
    }

    @Test
    @Timeout(TIMEOUT)
    void bigXmlMultipleThreads() {
        testInputs("big.xml", 4);
    }

    void testInputs(String xmlInputs, int threads) {
        var outputPrefix = xmlInputs.replace(",", "__");
        var outputFileName = Paths.get(TEMPORARY_DIRECTORY).resolve(outputPrefix + ".actual.txt").toString();
        String[] args = {
                "--threads", Integer.toString(threads),
                "--input", toBzip2Inputs(xmlInputs),
                "--output", outputFileName
        };
        Main.main(args);
        String expectedFileName = outputPrefix + ".expected.txt";
        try {
            assertFilesHaveSameContent(expectedFileName, outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void assertFilesHaveSameContent(String expectedFileName, String actualFileName) throws IOException {
        File actual = new File(actualFileName);
        File expected = Paths.get(TEST_DATA_PATH).resolve(expectedFileName).toFile();
        assertTrue(FileUtils.contentEquals(expected, actual));
    }
}
