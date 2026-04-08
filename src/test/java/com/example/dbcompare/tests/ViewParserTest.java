package com.example.dbcompare.tests;

import com.example.dbcompare.service.ViewParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewParserTest {
    private final ViewParser viewParser = new ViewParser();

    @Test
    void fallsBackToViewNameWhenDefinitionIsMissing() {
        assertEquals("CUSTOMER_VIEW", viewParser.resolveBaseTableName("customer_view", null));
    }

    @Test
    void extractsBaseTableNameFromViewDefinition() {
        assertEquals("BASE_TABLE", viewParser.resolveBaseTableName("ignored_view",
                "select a.id from \"APP\".\"BASE_TABLE\" a where a.flag = 'Y'"));
    }
}