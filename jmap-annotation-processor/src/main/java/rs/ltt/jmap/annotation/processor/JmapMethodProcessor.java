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
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Utils;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodResponse;

@SupportedAnnotationTypes("rs.ltt.jmap.annotation.JmapMethod")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@AutoService(Processor.class)
public class JmapMethodProcessor extends AbstractProcessor {

    private static Class[] INTERFACES = {MethodCall.class, MethodResponse.class};

    private Filer filer;
    private TypeMirror[] typeMirrors;
    private Types types;
    private Elements elements;
    private HashMap<Class, List<TypeElement>> typeElementMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.filer = processingEnvironment.getFiler();
        this.types = processingEnvironment.getTypeUtils();
        this.elements = processingEnvironment.getElementUtils();
        this.typeMirrors = new TypeMirror[INTERFACES.length];
        for (int i = 0; i < INTERFACES.length; ++i) {
            this.typeMirrors[i] =
                    processingEnvironment
                            .getElementUtils()
                            .getTypeElement(INTERFACES[i].getName())
                            .asType();
        }
    }

    public boolean process(
            Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        Set<? extends Element> elements =
                roundEnvironment.getElementsAnnotatedWith(JmapMethod.class);
        boolean emptyPass = true;
        for (Element element : elements) {
            if (element instanceof TypeElement) {
                final TypeElement typeElement = (TypeElement) element;
                for (int i = 0; i < INTERFACES.length; ++i) {
                    if (types.isAssignable(element.asType(), typeMirrors[i])) {
                        if (!typeElementMap.containsKey(INTERFACES[i])) {
                            typeElementMap.put(INTERFACES[i], new ArrayList<TypeElement>());
                        }
                        typeElementMap.get(INTERFACES[i]).add(typeElement);
                        emptyPass = false;
                    }
                }
            }
        }
        if (emptyPass) {
            return true;
        }
        for (Map.Entry<Class, List<TypeElement>> entry : typeElementMap.entrySet()) {
            createSourceFile(entry.getKey(), entry.getValue());
        }
        return true;
    }

    private void createSourceFile(Class clazz, Collection<TypeElement> classes) {

        try {
            FileObject resourceFile =
                    filer.createResource(
                            StandardLocation.CLASS_OUTPUT, "", Utils.getFilenameFor(clazz));
            PrintWriter printWriter = new PrintWriter(resourceFile.openOutputStream());
            for (TypeElement typeElement : classes) {
                JmapMethod annotation = typeElement.getAnnotation(JmapMethod.class);
                printWriter.println(
                        String.format("%s %s", typeElement.getQualifiedName(), annotation.value()));
            }
            printWriter.flush();
            printWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
