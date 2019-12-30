/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.document;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TextEditorTest {

    private static final String FILE_PATH = "psvm.java";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private Path temporaryFile;

    @Before
    public void setUpTemporaryFiles() throws IOException {
        temporaryFile = temporaryFolder.newFile(FILE_PATH).toPath();
    }


    @Test
    public void insertAtStartOfTheFileWithOffsetShouldSucceed() throws IOException {
        TextDocument doc = tempFile("static void main(String[] args) {}");

        try (TextEditor editor = doc.newEditor()) {
            editor.insert(0, "public ");
        }

        assertFinalFileIs(doc, "public static void main(String[] args) {}");
    }


    @Test
    public void shouldPreserveNewlinesLf() throws IOException {

        final String testFileContent =
            "class ShouldPreserveNewlines {\n"
                + "    public static void main(String[] args) {\n"
                + "        System.out.println(\"Test\");\n"
                + "    }\n"
                + "}\n"
                + "// note: multiple empty lines at the end\n"
                + "\n"
                + "\n";

        TextDocument doc = tempFile(testFileContent);

        try (TextEditor editor = doc.newEditor()) {
            editor.insert(0, "public ");
        }

        assertFinalFileIs(doc, "public " + testFileContent);
    }

    @Test
    public void shouldPreserveNewlinesCrLf() throws IOException {

        final String testFileContent =
            "class ShouldPreserveNewlines {\r\n"
                + "    public static void main(String[] args) {\r\n"
                + "        System.out.println(\"Test\");\r\n"
                + "    }\r\n"
                + "}\r\n"
                + "// note: multiple empty lines at the end\r\n"
                + "\r\n"
                + "\r\n";

        TextDocument doc = tempFile(testFileContent);

        try (TextEditor editor = doc.newEditor()) {
            editor.insert(0, "public ");
        }

        assertFinalFileIs(doc, "public " + testFileContent);
    }

    @Test
    public void insertVariousTokensIntoTheFileShouldSucceed() throws IOException {
        TextDocument doc = tempFile("static void main(String[] args) {}");

        try (TextEditor editor = doc.newEditor()) {
            editor.insert(0, "public ");
            editor.insert(17, "final ");
        }

        assertFinalFileIs(doc, "public static void main(final String[] args) {}");

        try (TextEditor editor = doc.newEditor()) {
            editor.replace(doc.createRegion(30, 6), "int[]");
        }

        assertFinalFileIs(doc, "public static void main(final int[][] args) {}");
    }

    @Test
    public void insertAtTheEndOfTheFileShouldSucceed() throws IOException {
        final String code = "public static void main(String[] args)";
        TextDocument doc = tempFile(code);

        try (TextEditor editor = doc.newEditor()) {
            editor.insert(code.length(), "{}");
        }

        assertFinalFileIs(doc, "public static void main(String[] args){}");
    }

    @Test
    public void removeTokenShouldSucceed() throws IOException {
        final String code = "public static void main(final String[] args) {}";
        TextDocument doc = tempFile(code);

        try (TextEditor editor = doc.newEditor()) {
            editor.delete(doc.createRegion(1, 25, 1, 31));
        }

        assertFinalFileIs(doc, "public static void main(String[] args) {}");
    }

    @Test
    public void insertAndRemoveTokensShouldSucceed() throws IOException {
        final String code = "static void main(final String[] args) {}";
        TextDocument doc = tempFile(code);

        try (TextEditor editor = doc.newEditor()) {
            editor.insert(0, "public ");
            editor.delete(doc.createRegion("static void main(".length(), "final ".length()));
        }

        assertFinalFileIs(doc, "public static void main(String[] args) {}");
    }

    @Test
    public void insertAndDeleteVariousTokensShouldSucceed() throws IOException {
        final String code = "void main(String[] args) {}";
        TextDocument doc = tempFile(code);

        try (TextEditor editor = doc.newEditor()) {
            editor.insert(0, "public ");
            editor.insert(0, "static ");
            // delete "void"
            editor.delete(doc.createRegion(0, 4));
            editor.insert(10, "final ");
            // delete "{}"
            editor.delete(doc.createRegion("void main(String[] args) ".length(), 2));
        }

        assertFinalFileIs(doc, "public static  main(final String[] args) ");
    }

    @Test
    public void replaceATokenShouldSucceed() throws IOException {
        final String code = "int main(String[] args) {}";
        TextDocument doc = tempFile(code);

        try (TextEditor editor = doc.newEditor()) {
            editor.replace(doc.createRegion(0, 3), "void");
        }

        assertFinalFileIs(doc, "void main(String[] args) {}");
    }

    @Test
    public void replaceVariousTokensShouldSucceed() throws IOException {
        final String code = "int main(String[] args) {}";
        TextDocument doc = tempFile(code);

        try (TextEditor editor = doc.newEditor()) {
            editor.replace(doc.createRegion(1, 1, 1, 1 + "int".length()), "void");
            editor.replace(doc.createRegion(1, 1 + "int ".length(), 1, 1 + "int main".length()), "foo");
            editor.replace(doc.createRegion("int main(".length(), "String".length()), "CharSequence");
        }

        assertFinalFileIs(doc, "void foo(CharSequence[] args) {}");
    }

    @Test
    public void insertDeleteAndReplaceVariousTokensShouldSucceed() throws IOException {
        final String code = "static int main(CharSequence[] args) {}";
        TextDocument doc = tempFile(code);

        try (TextEditor editor = doc.newEditor()) {
            editor.insert(0, "public");
            // delete "static "
            editor.delete(doc.createRegion(1, 1, 1, 7));
            // replace "int"
            editor.replace(doc.createRegion(1, 8, 1, 8 + "int".length()), "void");
            editor.insert(16, "final ");
            editor.replace(doc.createRegion(1, 17, 1, 17 + "CharSequence".length()), "String");
        }

        assertFinalFileIs(doc, "public void main(final String[] args) {}");
    }

    private void assertFinalFileIs(TextDocument doc, String expected) throws IOException {
        final String actualContent = new String(Files.readAllBytes(temporaryFile), StandardCharsets.UTF_8);
        assertEquals(expected, actualContent);
        assertEquals(expected, doc.getText().toString()); // getText() is not necessarily a string
    }

    private TextDocument tempFile(final String content) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
        return TextDocument.forFile(temporaryFile, StandardCharsets.UTF_8);
    }

}
