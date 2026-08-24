package servlet;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;

class InventoryEndpointMappingPolicyTest {
    @Test void legacyWasteAndVariantMutationRoutesAreAbsent() throws Exception {
        Path root=Path.of("src/main/java");
        String legacy=Files.readString(root.resolve("servlet/AdminInventoryAdjustmentServlet.java"));
        assertFalse(legacy.contains("@WebServlet"));
        assertFalse(legacy.contains("/waste"));
        assertFalse(Files.readString(root.resolve("servlet/AdminProductServlet.java")).contains("setManagedQuantity("));
        assertFalse(Files.readString(root.resolve("servlet/AdminVariantServlet.java")).contains("setManagedQuantity("));
    }

    @Test void inventoryServletMappingsAreUniqueAndNonOverlapping() throws Exception {
        Pattern annotation=Pattern.compile("@WebServlet\\(\"([^\"]+)\"\\)");
        List<String> mappings=new ArrayList<>();
        try(var files=Files.list(Path.of("src/main/java/servlet"))){for(Path file:files.toList()){Matcher matcher=annotation.matcher(Files.readString(file));while(matcher.find())if(matcher.group(1).contains("/inventory/"))mappings.add(matcher.group(1));}}
        assertEquals(mappings.size(),new HashSet<>(mappings).size());
        for(String mapping:mappings)if(mapping.endsWith("/*")){String prefix=mapping.substring(0,mapping.length()-1);assertFalse(mappings.stream().anyMatch(other->!other.equals(mapping)&&other.startsWith(prefix)),mapping);}
    }
}
