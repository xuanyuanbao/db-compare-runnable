package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.util.TypeNormalizer;

public final class TypeNormalizerTest {
    private TypeNormalizerTest() {
    }

    public static void run() {
        TypeNormalizer normalizer = new TypeNormalizer();

        TestSupport.assertEquals("NUMERIC", normalizer.normalize(DatabaseType.DB2, "decimal"),
                "common aliases should be normalized first");
        TestSupport.assertEquals("INTEGER", normalizer.normalize(DatabaseType.GAUSS, "int4"),
                "gauss-specific aliases should be normalized");
        TestSupport.assertEquals("CHAR", normalizer.normalize(DatabaseType.AS400, "graphic"),
                "as400 graphic should map to char");
        TestSupport.assertEquals("VARCHAR", normalizer.normalize(DatabaseType.AS400, "vargraphic"),
                "as400 vargraphic should map to varchar");
        TestSupport.assertNull(normalizer.normalize(DatabaseType.SNAPSHOT, null),
                "null raw type should stay null");
    }
}
