package com.example.dbcompare.tests;

public class AllTests {
    public static void main(String[] args) throws Exception {
        run("TypeNormalizerTest", TypeNormalizerTest::run);
        run("MappingServiceTest", MappingServiceTest::run);
        run("ConfigLoaderTest", ConfigLoaderTest::run);
        run("TableCompareServiceTest", TableCompareServiceTest::run);
        run("CompareOrchestratorSmokeTest", CompareOrchestratorSmokeTest::run);
        System.out.println("All tests passed.");
    }

    private static void run(String name, ThrowingRunnable runnable) throws Exception {
        runnable.run();
        System.out.println("[PASS] " + name);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
