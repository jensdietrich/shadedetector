package nz.ac.wgtn.shadedetector;

import nz.ac.wgtn.shadedetector.classselectors.SelectClassesWithComplexNames;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClassSelectorFactoryTest {

    @Test
    public void testSelectAll() {
        ClassSelector selector = new ClassSelectorFactory().create("all");
        assertNotNull(selector);
        assertEquals("all",selector.name());
    }

    @Test
    public void testSelectClassesWithComplexNames() {
        ClassSelector selector = new ClassSelectorFactory().create("complexnames");
        assertNotNull(selector);
        assertEquals("complexnames",selector.name());
    }

    @Test
    public void testSelectClassesWithComplexNamesMax42() {
        ClassSelector selector = new ClassSelectorFactory().create("complexnames?maxSize=42");
        assertNotNull(selector);
        assertEquals("complexnames",selector.name());
        assertTrue(selector instanceof SelectClassesWithComplexNames);
        SelectClassesWithComplexNames selectorX = (SelectClassesWithComplexNames)selector;
        assertEquals(42,selectorX.getMaxSize());
    }

    @Test
    public void testSelectClassesWithComplexNamesMax43() {
        ClassSelector selector = new ClassSelectorFactory().create("complexnames?maxSize=43");
        assertNotNull(selector);
        assertEquals("complexnames",selector.name());
        assertTrue(selector instanceof SelectClassesWithComplexNames);
        SelectClassesWithComplexNames selectorX = (SelectClassesWithComplexNames)selector;
        assertEquals(43,selectorX.getMaxSize());
    }

    @Test
    public void testNonExisting() {
        assertThrows(IllegalArgumentException.class, () -> new ClassSelectorFactory().create("foo"));
    }


}
