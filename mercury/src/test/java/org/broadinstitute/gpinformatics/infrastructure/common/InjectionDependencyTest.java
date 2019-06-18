package org.broadinstitute.gpinformatics.infrastructure.common;

import net.sourceforge.stripes.action.ActionBean;
import org.broadinstitute.gpinformatics.infrastructure.Resources;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * CDI automatic bean discovery parsing of all classes results in lots of log errors, warnings, and startup overhead due to newer rigid CDI API. </br>
 * For example, should CDI injection of org.apache.commons.logging.Log instantiate an bean instance using org.apache.commons.logging.Log
 *   or should it use org.broadinstitute.gpinformatics.infrastructure.Resources producer class?
 * Setting CDI property bean-discovery-mode="annotated" requires all injected beans to be annotated with
 * javax.enterprise.context scope annotations. </br >
 * This test locates all injected classes and ensures that they are annotated properly.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class InjectionDependencyTest {

    private Index index;

    private final Set<ClassInfo> cdiClasses = new HashSet<>();
    private Set<Class> cdiProducedClasses = new HashSet<>();
    private Set<Class> cdiExternalClasses = new HashSet<>();

    public InjectionDependencyTest(){}

    @Test
    public void validateCDI() throws Exception {

        index = indexMercuryClasses();

        // Get all injected and producer classes out of indexed Mercury classes
        List<AnnotationInstance> annotationInstances = new ArrayList<>();

        DotName injectAttribute = DotName.createSimple(Inject.class.getName());
        annotationInstances.addAll(index.getAnnotations(injectAttribute));

        DotName producesAttribute = DotName.createSimple(Produces.class.getName());
        annotationInstances.addAll(index.getAnnotations(producesAttribute));

        // Populate instance variables with all CDI class types tagged with annotations
        getCdiClasses(annotationInstances);

        // Write list to a file if required for reference
        Writer typeList = new BufferedWriter( new FileWriter( new File(System.getProperty("java.io.tmpdir"), "injectedTypes.txt"),false));
        typeList.write("*** CDI Classes:\n");
        for( ClassInfo type : cdiClasses) {
            typeList.write(type.name().toString());
            typeList.write("\n");
        }
        typeList.write("*** CDI Produced Classes:\n");
        for( Class type : cdiProducedClasses) {
            typeList.write(type.getName());
            typeList.write("\n");
        }
        typeList.close();

        // Lots of Injection points are interfaces - interfaces are not tagged with attributes, but all implementations must be
        // If they're tagged with @Default or @Alternative()
        Set<ClassInfo> implementations = new HashSet<>();
        DotName defaultAttribute = DotName.createSimple(Default.class.getName());
        DotName altAttribute = DotName.createSimple(Alternative.class.getName());
        for(Iterator<ClassInfo> fieldTypesIter = cdiClasses.iterator(); fieldTypesIter.hasNext(); ) {
            ClassInfo type = fieldTypesIter.next();

            if( Class.forName(type.name().toString()).isInterface() ) {
                fieldTypesIter.remove();
                for( ClassInfo classInfo : index.getAllKnownImplementors(type.name()) ) {
                    if (classInfo.classAnnotations().contains(defaultAttribute) || classInfo.classAnnotations().contains(altAttribute)) {
                        implementations.add(classInfo);
                    }
                }
            } else if( Modifier.isAbstract(Class.forName(type.name().toString()).getModifiers())){
                fieldTypesIter.remove();
            }
        }
        cdiClasses.addAll(implementations);

        // Same with producer methods
        Set<Class> producerImpls = new HashSet<>();
        for(Iterator<Class> fieldTypesIter = cdiProducedClasses.iterator(); fieldTypesIter.hasNext(); ) {
            Class type = fieldTypesIter.next();

            if( type.isInterface() ) {
                fieldTypesIter.remove();
                for( ClassInfo classInfo : index.getAllKnownImplementors(DotName.createSimple(type.getName())) ){
                    producerImpls.add(Class.forName(classInfo.name().toString()));
                }
            } else if( Modifier.isAbstract(type.getModifiers())){
                fieldTypesIter.remove();
            }
        }
        cdiProducedClasses.addAll(producerImpls);

        // All classes handled by CDI need to be tagged with at least one of these attributes
        Set<String> cdiAnnotations = new HashSet<>();
        cdiAnnotations.add(Dependent.class.getName());
        cdiAnnotations.add(RequestScoped.class.getName());
        cdiAnnotations.add(ApplicationScoped.class.getName());
        cdiAnnotations.add(SessionScoped.class.getName());
        cdiAnnotations.add(ConversationScoped.class.getName());
        cdiAnnotations.add(Stateless.class.getName());
        cdiAnnotations.add(Stateful.class.getName());
        cdiAnnotations.add(Singleton.class.getName());
        // Hoping MDBs are not tagged with a scope
        cdiAnnotations.add(MessageDriven.class.getName());
        // Why does CDI work on these with no annotation?
        cdiAnnotations.add(javax.ws.rs.Path.class.getName());

        Set<Class> notAnnotated = new HashSet<>();

        cdiLoop :
        for( ClassInfo type : cdiClasses) {
            // Constructed as non-bean, extended as bean and non-bean, easier to ignore than code around
            if( "org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPWorkRequestClientService".equals(type.name().toString())) {
                continue;
            }

            DotName injectedType = type.name();
            ClassInfo classInfo = index.getClassByName(injectedType);
            if( classInfo != null ) {
                for (AnnotationInstance annotationInstance : classInfo.classAnnotations()) {
                    if (cdiAnnotations.contains(annotationInstance.name().toString())) {
                        continue cdiLoop;
                    }
                }
            }

            // Now see if the non-annotated classes are created by producers
            if( cdiProducedClasses.contains(Class.forName(injectedType.toString()))) {
                continue cdiLoop;
            }

            // Still here?  Missing annotation
            notAnnotated.add(Class.forName(type.name().toString()));
        }

        if( notAnnotated.size() > 0 ) {
            StringBuilder annotationMissingString = new StringBuilder();
            annotationMissingString.append("Injected class(es) missing CDI annotations\n");
            for( Class clazz : notAnnotated ) {
                annotationMissingString.append(clazz.getName()).append("\n");
            }
            Assert.fail( annotationMissingString.toString());
        }
    }

    /**
     * Populates instance variables with all CDI class types tagged with annotations
     */
    private void getCdiClasses( List<AnnotationInstance> annotationInstances ) throws Exception {

        DotName producesAttribute = DotName.createSimple(Produces.class.getName());

        Writer annotatedListFileWriter = new BufferedWriter( new FileWriter( new File( System.getProperty("java.io.tmpdir"), "injectionPoints.txt"),false));

        for (AnnotationInstance annotationInstance : annotationInstances) {

            annotatedListFileWriter.write(annotationInstance.target().kind().toString());
            annotatedListFileWriter.write(":  ");
            annotatedListFileWriter.write(annotationInstance.target().toString());
            annotatedListFileWriter.write("\n");

            switch(annotationInstance.target().kind()) {
                case FIELD:
                    FieldInfo fieldInfo = annotationInstance.target().asField();

                    if( ActionBean.class.isAssignableFrom(Class.forName(fieldInfo.declaringClass().name().toString()))
                            || Filter.class.isAssignableFrom(Class.forName(fieldInfo.declaringClass().name().toString()))
                            || TagSupport.class.isAssignableFrom(Class.forName(fieldInfo.declaringClass().name().toString()))
                            || CoreActionBean.class.isAssignableFrom(Class.forName(fieldInfo.declaringClass().name().toString()))
                            ) {
                        break;
                    } else {
                        cdiClasses.add(fieldInfo.declaringClass());
                    }
                    Type fieldType = fieldInfo.type();
                    ClassInfo fieldClassInfo = index.getClassByName(fieldType.name());
                    if( fieldClassInfo != null ) {
//                        if( Modifier.isAbstract( fieldClassInfo.flags() ) ) {
//                            cdiClasses.addAll( index.getAllKnownImplementors(fieldClassInfo.name()));
//                        } else {
                            cdiClasses.add(fieldClassInfo);
//                        }
                    } else {
                        cdiExternalClasses.add(Class.forName(fieldType.name().toString()));
                    }
                    break;
                case METHOD:
                    ClassInfo declClassInfo = annotationInstance.target().asMethod().declaringClass();

                    // Class needs annotation
                    if( !ActionBean.class.isAssignableFrom(Class.forName(declClassInfo.name().toString()))
                            && !Filter.class.isAssignableFrom(Class.forName(declClassInfo.name().toString()))
                            && !TagSupport.class.isAssignableFrom(Class.forName(declClassInfo.name().toString()))
                            && !CoreActionBean.class.isAssignableFrom(Class.forName(declClassInfo.name().toString()))
                            ) {
                        cdiClasses.add(declClassInfo);
                    }

                    // Ignore args to a Producer method
                    if( !annotationInstance.target().asMethod().hasAnnotation(producesAttribute) ) {
                        // Parameter classes need annotations
                        for (Type paramType : annotationInstance.target().asMethod().parameters()) {
                            if (paramType.kind() == Type.Kind.CLASS) {
                                ClassInfo paramClassInfo = index.getClassByName(paramType.name());
                                // Might be an external class (which SHOULD be handled by a producer)
                                if (paramClassInfo != null) {
                                    cdiClasses.add(paramClassInfo);
                                } else {
                                    cdiExternalClasses.add(Class.forName(paramType.name().toString()));
                                }
                            }
                        }
                    } else {
                        // Register return type of producer
                        // Return type must not be a CDI bean (but if it is, container will blow up at start with ambiguous dependency error)
                        DotName returnDotName = annotationInstance.target().asMethod().returnType().name();
                        Class returnClass = Class.forName(returnDotName.toString());
                        if( returnClass.isInterface() ) {
                            for( ClassInfo implClassInfo : index.getAllKnownImplementors(returnDotName) ) {
                                cdiProducedClasses.add(Class.forName(implClassInfo.name().toString()));
                            }
                        } else {
                            cdiProducedClasses.add(returnClass);
                        }
                    }

                    break;
                case METHOD_PARAMETER:
                    cdiClasses.add(annotationInstance.target().asMethodParameter().asClass());
                    break;
                default:  // For safety, but CLASS and TYPE are not injected
                    break;
            }
        }

        annotatedListFileWriter.close();
    }

    private Index indexMercuryClasses() throws Exception {

        Indexer indexer = new Indexer();

        Path targetPath = getRootMercuryClasspath();

        // Adds all Mercury classes to the index
        Files.walkFileTree(targetPath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                if( path.toString().endsWith(".class")) {
                    indexer.index(Files.newInputStream(path, StandardOpenOption.READ));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        return indexer.complete();

    }

    /**
     * Gets the root of the Mercury codebase classes (assumes this is not run using an archive file
     */
    private Path getRootMercuryClasspath() throws URISyntaxException {
        // file:/C:/projects/IdeaProjects/mercury/mercury/target/classes/org/broadinstitute/gpinformatics/infrastructure/Resources.class
        URL pathURL = Resources.class.getResource("Resources.class");
        Path deepPath = Paths.get( pathURL.toURI() );
        // ( Hopefully Resources package name is not changed )
        return deepPath.getParent().getParent().getParent().getParent().getParent();
    }

}
