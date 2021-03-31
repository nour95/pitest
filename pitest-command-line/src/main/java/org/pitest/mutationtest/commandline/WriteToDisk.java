package org.pitest.mutationtest.commandline;

import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.gregor.GregorMutater;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.config.Mutator;
import org.pitest.plugin.export.MutantExportInterceptor;
import org.pitest.util.FileUtil;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.Collection;

public class WriteToDisk {

    private final ClassLoader classLoader;
    ClassloaderByteArraySource source;
    MutantExportInterceptor testee;
    FileSystem fileSystem;
    GregorMutater mutator;

    public WriteToDisk(String source, String target) throws MalformedURLException {
        this.classLoader = new URLClassLoader(new URL[]{new File(source).toURI().toURL()});
        this.source = new ClassloaderByteArraySource(classLoader);
        final Collection<MethodMutatorFactory> mutators = Mutator.newDefaults();
        this.mutator = new GregorMutater(this.source, m -> true, mutators);
        this.testee = new MutantExportInterceptor(this.fileSystem, this.source, target);
    }

    /**
     * class name of format package.class($asd)
     */
    public void mutate(String className) throws ClassNotFoundException {
        final Class<?> clazz = classLoader.loadClass(className);
        final Collection<MutationDetails> mutations = this.mutator.findMutations(
                ClassName.fromClass(clazz)
        );

        this.testee.begin(ClassTree.fromBytes(this.source.getBytes(clazz.getName()).get()));
        this.testee.intercept(mutations, this.mutator);
        this.testee.end();
    }

    public void mutateAllIn(String fileName) throws IOException, ClassNotFoundException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),
                StandardCharsets.UTF_8));

        String line;
        while ((line = br.readLine()) != null) {
            mutate(line);
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        WriteToDisk wtd = new WriteToDisk(args[0], args[1]);
        wtd.mutateAllIn(args[3]);
    }
}
