package debloater.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles import/export of action selection profiles.
 * A profile is a JSON file containing an array of selected action IDs.
 * On import, IDs are validated against the ActionRegistry — unknown IDs are ignored and warned.
 * No raw PowerShell is ever imported.
 */
public class ProfileService {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ActionRegistry registry;

    public ProfileService(ActionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Wrapper for profile import result.
     */
    public static class ImportResult {
        public final List<String> validIds;
        public final List<String> unknownIds;

        public ImportResult(List<String> validIds, List<String> unknownIds) {
            this.validIds = validIds;
            this.unknownIds = unknownIds;
        }
    }

    /**
     * Exports the given list of selected action IDs to a JSON file.
     */
    public String exportProfile(List<String> selectedIds, Path filePath) throws IOException {
        // Only export valid IDs
        List<String> validIds = registry.validateIds(selectedIds);
        String json = gson.toJson(validIds);
        Files.writeString(filePath, json, StandardCharsets.UTF_8);
        return json;
    }

    /**
     * Returns the profile JSON string for the given selected IDs (for in-memory use).
     */
    public String exportProfileToString(List<String> selectedIds) {
        List<String> validIds = registry.validateIds(selectedIds);
        return gson.toJson(validIds);
    }

    /**
     * Imports a profile from a JSON file.
     * Validates all IDs against the registry.
     * Returns both valid and unknown IDs for user warning.
     */
    public ImportResult importProfile(Path filePath) throws IOException {
        String json = Files.readString(filePath, StandardCharsets.UTF_8);
        return importProfileFromString(json);
    }

    /**
     * Imports a profile from a JSON string.
     */
    public ImportResult importProfileFromString(String json) {
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> importedIds;

        try {
            importedIds = gson.fromJson(json, listType);
        } catch (JsonSyntaxException e) {
            System.err.println("ProfileService: Invalid profile JSON: " + e.getMessage());
            return new ImportResult(new ArrayList<>(), new ArrayList<>());
        }

        if (importedIds == null) {
            return new ImportResult(new ArrayList<>(), new ArrayList<>());
        }

        Set<String> knownIds = registry.getAllActionIds();
        List<String> validIds = new ArrayList<>();
        List<String> unknownIds = new ArrayList<>();

        for (String id : importedIds) {
            if (knownIds.contains(id)) {
                validIds.add(id);
            } else {
                unknownIds.add(id);
                System.err.println("ProfileService: Unknown action ID in profile (ignored): " + id);
            }
        }

        return new ImportResult(validIds, unknownIds);
    }
}
