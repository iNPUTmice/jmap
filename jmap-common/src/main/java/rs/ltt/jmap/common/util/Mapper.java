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

package rs.ltt.jmap.common.util;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.annotation.JmapEntity;
import rs.ltt.jmap.common.Utils;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.AccountCapability;
import rs.ltt.jmap.common.entity.Capability;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.entity.filter.FilterCondition;
import rs.ltt.jmap.common.entity.filter.MailboxFilterCondition;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.MethodResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class Mapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mapper.class);
    public static final ImmutableBiMap<String, Class<? extends MethodCall>> METHOD_CALLS = Mapper.get(MethodCall.class);
    public static final ImmutableBiMap<String, Class<? extends MethodResponse>> METHOD_RESPONSES = Mapper.get(MethodResponse.class);
    public static final ImmutableBiMap<String, Class<? extends MethodErrorResponse>> METHOD_ERROR_RESPONSES = Mapper.get(MethodErrorResponse.class);
    public static final ImmutableBiMap<String, Class<? extends Capability>> CAPABILITIES = Mapper.get(Capability.class);
    public static final ImmutableBiMap<String, Class<? extends AccountCapability>> ACCOUNT_CAPABILITIES = Mapper.get(AccountCapability.class);
    public static final ImmutableBiMap<String, Class<? extends AbstractIdentifiableEntity>> ENTITIES = Mapper.get(AbstractIdentifiableEntity.class);
    public static final ImmutableMap<Class<? extends AbstractIdentifiableEntity>, Class<FilterCondition<? extends AbstractIdentifiableEntity>>> ENTITY_TO_FILTER_CONDITION = getEntityToFilterConditionMap();
    public static final ImmutableMap<Type, Class<? extends AbstractIdentifiableEntity>> TYPE_TO_ENTITY_CLASS;

    static {
        final ImmutableMap.Builder<Type, Class<? extends AbstractIdentifiableEntity>> typeMapBuilder = new ImmutableMap.Builder<>();
        for(final Class<? extends AbstractIdentifiableEntity> clazz : ENTITY_TO_FILTER_CONDITION.keySet()) {
            typeMapBuilder.put(TypeToken.getParameterized(Filter.class, clazz).getType(), clazz);
        }
        TYPE_TO_ENTITY_CLASS = typeMapBuilder.build();
    }

    private Mapper() {

    }

    private static <T> ImmutableBiMap<String, Class<? extends T>> get(Class<T> type) {
        final ImmutableBiMap.Builder<String, Class<? extends T>> builder = new ImmutableBiMap.Builder<>();
        for (final BufferedReader bufferedReader : getSystemResources(type)) {
            if (bufferedReader == null) {
                continue;
            }
            try {
                for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                    final String[] parts = line.split(" ", 2);
                    if (parts.length == 2) {
                        final String name = parts[0];
                        try {
                            Class<? extends T> clazz = Class.forName(name).asSubclass(type);
                            builder.put(parts[1], clazz);
                        } catch (ClassNotFoundException | ClassCastException e) {
                            LOGGER.warn("Mapping points to a class that doesn't exist {}", name);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Unable to read system resource", e);
            }
        }
        final ImmutableBiMap<String, Class<? extends T>> map = builder.build();
        if (LOGGER.isWarnEnabled() && MapperLoggingUtils.isMissingWellKnown(map, type)) {
            LOGGER.warn(
                    "Some well known mappings are missing. Have you enabled resource merging for {}?",
                    Utils.getFilenameFor(type)
            );
        }
        return map;
    }

    private static <T> Iterable<BufferedReader> getSystemResources(final Class<T> type) {
        final List<URL> urls = getSystemResourceUrls(type);
        if (urls.size() == 0) {
            final InputStream is = Mapper.class.getClassLoader().getResourceAsStream(Utils.getFilenameFor(type));
            if (is == null) {
                LOGGER.error("Unable to find resources for type {}", type.getName());
                return Collections.emptyList();
            }
            return Collections.singletonList(new BufferedReader(new InputStreamReader(is)));
        } else {
            return urls.stream().map(url -> {
                try {
                    return new BufferedReader(Resources.asCharSource(url, Charsets.UTF_8).openStream());
                } catch (IOException e) {
                    LOGGER.warn("Unable to to read mappings for type {} from url {}", type.getName(), url.toString());
                    return null;
                }
            }).collect(Collectors.toList());
        }
    }

    private static <T> List<URL> getSystemResourceUrls(Class<T> type) {
        try {
            return Collections.list(ClassLoader.getSystemResources(Utils.getFilenameFor(type)));
        } catch (IOException e) {
            LOGGER.warn("Unable to get SystemResources from ClassLoader", e);
            return Collections.emptyList();
        }
    }

    private static ImmutableBiMap<Class<? extends AbstractIdentifiableEntity>, Class<FilterCondition<? extends AbstractIdentifiableEntity>>> getEntityToFilterConditionMap() {
        final ImmutableBiMap.Builder<Class<? extends AbstractIdentifiableEntity>, Class<FilterCondition<? extends AbstractIdentifiableEntity>>> builder = new ImmutableBiMap.Builder<>();
        for (final BufferedReader bufferedReader : getSystemResources(FilterCondition.class)) {
            if (bufferedReader == null) {
                continue;
            }
            try {
                for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                    final String[] parts = line.split(" ", 2);
                    if (parts.length == 2) {
                        try {
                            final Class<? extends AbstractIdentifiableEntity> entityClass = Class.forName(parts[0]).asSubclass(AbstractIdentifiableEntity.class);
                            final Class<FilterCondition<? extends AbstractIdentifiableEntity>> filterConditionClass = (Class<FilterCondition<? extends AbstractIdentifiableEntity>>) Class.forName(parts[1]).asSubclass(FilterCondition.class);
                            builder.put(entityClass, filterConditionClass);
                        } catch (ClassNotFoundException | ClassCastException e) {
                            LOGGER.warn("Unable to create Entity to FilterCondition mapping for {} and {}", parts[0], parts[1]);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Unable to read system resource", e);
            }
        }
        final ImmutableBiMap<Class<? extends AbstractIdentifiableEntity>, Class<FilterCondition<? extends AbstractIdentifiableEntity>>> map = builder.build();
        if (LOGGER.isWarnEnabled() && !map.containsValue(MailboxFilterCondition.class)) {
            LOGGER.warn(
                    "Some well known mappings are missing. Have you enabled resource merging for {}?",
                    Utils.getFilenameFor(AbstractIdentifiableEntity.class)
            );
        }
        return builder.build();
    }

}
