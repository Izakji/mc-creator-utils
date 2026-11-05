package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class CurseForgeReportTest {

    @Test
    void formatsSearchResults() {
        JsonObject root = JsonParser.parseString("""
                {"data":[{"id":238222,"name":"JEI","slug":"jei","downloadCount":100,
                "summary":"Item viewer","authors":[{"name":"mezz"}],
                "links":{"websiteUrl":"https://www.curseforge.com/minecraft/mc-mods/jei"},
                "latestFilesIndexes":[{"fileId":42,"filename":"jei.jar","gameVersion":"1.21.1","modLoader":6}]}],
                "pagination":{"totalCount":1}}
                """).getAsJsonObject();
        String report = CurseForgeReport.search(root);
        assertTrue(report.contains("JEI"));
        assertTrue(report.contains("id=238222"));
        assertTrue(report.contains("mezz"));
        assertTrue(report.contains("fileId=42"));
        assertTrue(report.contains("https://www.curseforge.com/minecraft/mc-mods/jei"));
    }

    @Test
    void formatsModFilesAndFile() {
        JsonObject mod = JsonParser.parseString("""
                {"data":{"id":238222,"name":"JEI","slug":"jei","downloadCount":100,
                "dateModified":"2026-01-01T00:00:00Z","authors":[{"name":"mezz"}],
                "categories":[{"name":"Utility"}],
                "links":{"sourceUrl":"https://github.com/mezz/JustEnoughItems"},
                "latestFiles":[{"id":42,"displayName":"JEI 1.0","fileName":"jei.jar",
                "releaseType":1,"gameVersions":["1.21.1"]}]}}
                """).getAsJsonObject();
        String modReport = CurseForgeReport.mod(mod, false, "");
        assertTrue(modReport.contains("CurseForge mod 238222"));
        assertTrue(modReport.contains("Utility"));
        assertTrue(modReport.contains("fileId=42") || modReport.contains("id=42"));

        JsonObject files = JsonParser.parseString("""
                {"data":[{"id":42,"displayName":"JEI 1.0","fileName":"jei.jar","releaseType":1,
                "fileDate":"2026-01-01T00:00:00Z","downloadCount":10,"gameVersions":["1.21.1"],
                "downloadUrl":"https://example.invalid/jei.jar","dependencies":[{"modId":1,"relationType":3}]}],
                "pagination":{"totalCount":1}}
                """).getAsJsonObject();
        String filesReport = CurseForgeReport.files(files);
        assertTrue(filesReport.contains("fileId=42"));
        assertTrue(filesReport.contains("required"));

        JsonObject file = JsonParser.parseString("""
                {"data":{"id":42,"modId":238222,"displayName":"JEI 1.0","fileName":"jei.jar",
                "releaseType":1,"fileDate":"2026-01-01T00:00:00Z","fileLength":123,
                "gameVersions":["1.21.1"],"downloadCount":10,
                "hashes":[{"value":"abc","algo":1}]}}
                """).getAsJsonObject();
        String fileReport = CurseForgeReport.file(file, false, "");
        assertTrue(fileReport.contains("sha1=abc"));
        assertTrue(fileReport.contains("Size: 123"));
    }

    @Test
    void stripsHtmlAndNormalizesEnums() {
        assertEquals("Hello world", CurseForgeReport.stripHtml("<p>Hello <b>world</b></p>"));
        assertEquals("6", CurseForgeReport.normalizeModLoader("neoforge"));
        assertEquals("0", CurseForgeReport.normalizeModLoader("any"));
        assertEquals("2", CurseForgeReport.normalizeSort("popularity"));
        assertEquals("11", CurseForgeReport.normalizeSort("released"));
    }
}
