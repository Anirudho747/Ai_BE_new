package com.testleaf.runner;

import org.springframework.stereotype.Service;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class TestExecutor {

    public TestResult runTests(String javaCode, String className) {
        TestResult result = new TestResult();
        try {
            // 1. Create Temporary Directory for Compiled Files
            Path tempDir = Files.createTempDirectory("dynamic-tests-");
            File sourceFile = new File(tempDir.toFile(), className + ".java");
            
            // 2. Write the Java Code to a File
            try (PrintWriter out = new PrintWriter(sourceFile)) {
                out.println(javaCode);
            }
            System.out.println("✅ Java source file created: " + sourceFile.getAbsolutePath());

            // 3. Prepare the Java Compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                result.setError("❌ No Java compiler available. Ensure you run on a JDK, not a JRE.");
                return result;
            }

            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(sourceFile);

            // 4. Build a full classpath string
            //    (1) system classpath
            //    (2) plus URLs from the current class loader (Spring Boot libs, Maven deps, etc.)
            StringBuilder cpBuilder = new StringBuilder();

            // (A) Start with the system classpath
            String sysCp = System.getProperty("java.class.path");
            cpBuilder.append(sysCp);

            // (B) Add jars/urls from the current thread context or current class loader
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader) cl).getURLs()) {
                    cpBuilder.append(File.pathSeparator).append(url.getFile());
                }
            } else {
                // fallback: try getClass().getClassLoader() if thread context is not a URLClassLoader
                ClassLoader thisCl = getClass().getClassLoader();
                if (thisCl instanceof URLClassLoader) {
                    for (URL url : ((URLClassLoader) thisCl).getURLs()) {
                        cpBuilder.append(File.pathSeparator).append(url.getFile());
                    }
                }
            }

            // 5. Set compiler options
            List<String> optionList = new ArrayList<>();
            optionList.add("-classpath");
            optionList.add(cpBuilder.toString());
            optionList.add("-d");
            optionList.add(tempDir.toFile().getAbsolutePath());

            // 6. Compile the Java File
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, null, optionList, null, compilationUnits);
            boolean success = task.call();
            fileManager.close();

            if (!success) {
                result.setError("❌ Compilation failed. Check your generated code for errors.");
                return result;
            }
            System.out.println("✅ Compilation successful! Compiled class stored at: " + tempDir);

            // 7. Load and Run Tests
            URLClassLoader testClassLoader = new URLClassLoader(
            	    new URL[]{tempDir.toUri().toURL()},
            	    getClass().getClassLoader()
            	);

        	Class<?> testClass;
        	try {
        	    testClass = Class.forName("automation.tests." + className, true, testClassLoader);
        	    System.out.println("✅ Successfully loaded class: " + testClass.getName());
        	} catch (ClassNotFoundException e) {
        	    result.setError("❌ Class not found in classpath: " + className);
        	    return result;
        	}

        	// Create TestNG
        	TestNG testng = new TestNG();
        	testng.setOutputDirectory(tempDir.toFile().getAbsolutePath());

        	// ⚠️ Instead of using XmlSuite/XmlTest:
        	testng.setTestClasses(new Class[]{ testClass });

        	// Add listener for capturing results
        	TestListenerAdapter testListener = new TestListenerAdapter();
        	testng.addListener(testListener);
        	testng.run();

            // 8. Gather Results
            result.setPassedTests(testListener.getPassedTests().size());
            result.setFailedTests(testListener.getFailedTests().size());
            result.setSkippedTests(testListener.getSkippedTests().size());

            if (!testListener.getFailedTests().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                testListener.getFailedTests().forEach(failedTest -> {
                    sb.append("\nTest Method: ").append(failedTest.getName())
                      .append("\nException: ").append(failedTest.getThrowable())
                      .append("\n-----------------------------------");
                });
                result.setError(sb.toString());
            }

        } catch (IOException e) {
            result.setError("❌ Error during execution: " + e.getMessage());
        }

        return result;
    }

    // Simple result structure
    public static class TestResult {
        private int passedTests;
        private int failedTests;
        private int skippedTests;
        private String error;

        public int getPassedTests() { return passedTests; }
        public void setPassedTests(int passedTests) { this.passedTests = passedTests; }
        public int getFailedTests() { return failedTests; }
        public void setFailedTests(int failedTests) { this.failedTests = failedTests; }
        public int getSkippedTests() { return skippedTests; }
        public void setSkippedTests(int skippedTests) { this.skippedTests = skippedTests; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
