package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.util.TypeNormalizer;

import java.util.List;
import java.util.Map;

public final class TypeNormalizerTest {
    private TypeNormalizerTest() {
    }

    public static void run() {
        TypeNormalizer normalizer = new TypeNormalizer();

        TestSupport.assertEquals("DECIMAL", normalizer.normalize(DatabaseType.DB2, "decimal"),
                "raw types should stay as-is before user rules are applied");
        TestSupport.assertEquals("INT4", normalizer.normalize(DatabaseType.GAUSS, "int4"),
                "database-specific aliases should not be silently normalized anymore");
        TestSupport.assertEquals("GRAPHIC", normalizer.normalize(DatabaseType.AS400, "graphic"),
                "as400 raw types should stay original before user rules are applied");
        TestSupport.assertEquals("NUMERIC", normalizer.normalize(DatabaseType.AS400, "decimal",
                        Map.of("NUMERIC", List.of("DECIMAL"))),
                "user-defined rules should decide whether DECIMAL and NUMERIC are treated as equal");
        TestSupport.assertEquals("DATE", normalizer.normalize(DatabaseType.GAUSS, "timestamp",
                        Map.of("DATE", List.of("TIMESTAMP"))),
                "custom type mappings should collapse configured aliases to the configured canonical type");
        TestSupport.assertNull(normalizer.normalize(DatabaseType.SNAPSHOT, null),
                "null raw type should stay null");
    }
}
