/*
 * Copyright 2019 Daniel Gultsch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rs.ltt.jmap.annotation.processor;


import com.google.auto.service.AutoService;
import rs.ltt.jmap.annotation.JmapEntity;
import rs.ltt.jmap.common.Utils;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.filter.FilterCondition;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("rs.ltt.jmap.annotation.JmapEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class JmapEntityProcessor extends AbstractProcessor {

    private static final Class<AbstractIdentifiableEntity> INTERFACE = AbstractIdentifiableEntity.class;

    private Filer filer;
    private TypeMirror abstractIdMirror;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.filer = processingEnvironment.getFiler();
        this.typeUtils = processingEnvironment.getTypeUtils();
        this.abstractIdMirror = processingEnvironment.getElementUtils().getTypeElement(INTERFACE.getName()).asType();
    }

    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
        final Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(JmapEntity.class);
        final List<TypeElement> classes = new ArrayList<>();
        for (final Element element : elements) {
            if (element instanceof TypeElement) {
                final TypeElement typeElement = (TypeElement) element;
                if (typeUtils.isAssignable(element.asType(), abstractIdMirror)) {
                    classes.add(typeElement);
                } else {
                    System.err.println(typeElement.getQualifiedName() + " does not implement " + abstractIdMirror);
                }
            }
        }

        if (classes.size() == 0) {
            return true;
        }

        try {
            writeFilterConditions(classes);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void writeFilterConditions(final List<TypeElement> classes) throws IOException {
        final FileObject resourceFile = filer.createResource(StandardLocation.CLASS_OUTPUT, "", Utils.getFilenameFor(FilterCondition.class));
        final PrintWriter printWriter = new PrintWriter(resourceFile.openOutputStream());
        for (final TypeElement typeElement : classes) {
            final TypeMirror typeMirror = getFilterCondition(typeElement);
            if (typeMirror.toString().equals(JmapEntity.NoFilterCondition.class.getCanonicalName())) {
                System.out.println("skipping "+typeMirror);
                continue;
            }
            System.out.println(typeMirror);
            printWriter.println(String.format("%s %s", typeElement.getQualifiedName(), typeMirror));
        }
        printWriter.flush();
        printWriter.close();
        System.out.println("done writing entity filter for " + classes.size() + " classes");
    }

    private static TypeMirror getFilterCondition(final TypeElement typeElement) {
        final JmapEntity annotation = typeElement.getAnnotation(JmapEntity.class);
        try {
            final Class<? extends FilterCondition<? extends AbstractIdentifiableEntity>> fc = annotation.filterCondition();
            throw new IllegalStateException("Getting Filter condition from annotation did not throw");
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }
}
