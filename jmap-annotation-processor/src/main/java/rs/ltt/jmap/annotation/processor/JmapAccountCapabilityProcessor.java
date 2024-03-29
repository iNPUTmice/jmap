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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import rs.ltt.jmap.annotation.JmapAccountCapability;
import rs.ltt.jmap.common.Utils;
import rs.ltt.jmap.common.entity.AccountCapability;

@SupportedAnnotationTypes("rs.ltt.jmap.annotation.JmapAccountCapability")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@AutoService(Processor.class)
public class JmapAccountCapabilityProcessor extends AbstractProcessor {

    private static final Class<AccountCapability> INTERFACE = AccountCapability.class;

    private Filer filer;
    private TypeMirror accountCapability;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.filer = processingEnvironment.getFiler();
        this.typeUtils = processingEnvironment.getTypeUtils();
        this.accountCapability =
                processingEnvironment
                        .getElementUtils()
                        .getTypeElement(INTERFACE.getName())
                        .asType();
    }

    public boolean process(
            Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        Set<? extends Element> elements =
                roundEnvironment.getElementsAnnotatedWith(JmapAccountCapability.class);
        final List<TypeElement> classes = new ArrayList<>();
        for (Element element : elements) {
            if (element instanceof TypeElement) {
                final TypeElement typeElement = (TypeElement) element;
                if (typeUtils.isAssignable(element.asType(), accountCapability)) {
                    classes.add(typeElement);
                } else {
                    System.out.println(
                            typeElement.getQualifiedName()
                                    + " does not implement "
                                    + accountCapability
                                    + " but "
                                    + typeElement.getInterfaces());
                }
            }
        }
        for (TypeElement typeElement : classes) {
            System.out.println(typeElement.getQualifiedName());
        }

        System.out.println("creating for " + classes.size() + " classes");
        if (classes.size() == 0) {
            return true;
        }

        try {
            FileObject resourceFile =
                    filer.createResource(
                            StandardLocation.CLASS_OUTPUT, "", Utils.getFilenameFor(INTERFACE));
            PrintWriter printWriter = new PrintWriter(resourceFile.openOutputStream());
            for (TypeElement typeElement : classes) {
                JmapAccountCapability annotation =
                        typeElement.getAnnotation(JmapAccountCapability.class);
                printWriter.println(
                        String.format(
                                "%s %s", typeElement.getQualifiedName(), annotation.namespace()));
            }
            printWriter.flush();
            printWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
}
