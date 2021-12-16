/*
 * Copyright 2021 Daniel Gultsch
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

package rs.ltt.jmap.mua.service;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.mua.MuaSession;
import rs.ltt.jmap.mua.plugin.EmailBuildStagePlugin;
import rs.ltt.jmap.mua.plugin.EmailCacheStagePlugin;

public class PluginService extends MuaService {

    private final ArrayList<EmailBuildStagePlugin> emailBuildStagePlugins = new ArrayList<>();
    private final ArrayList<EmailCacheStagePlugin> emailCacheStagePlugins = new ArrayList<>();
    private final ClassToInstanceMap<Plugin> plugins;

    public PluginService(MuaSession muaSession, final ClassToInstanceMap<Plugin> plugins) {
        super(muaSession);
        this.plugins = plugins;
        this.install(muaSession);
    }

    public ListenableFuture<Email> executeEmailBuildStagePlugins(final Email email) {
        ListenableFuture<Email> currentFuture = Futures.immediateFuture(email);
        for (final EmailBuildStagePlugin plugin : emailBuildStagePlugins) {
            currentFuture =
                    Futures.transformAsync(
                            currentFuture, plugin::onBuildEmail, MoreExecutors.directExecutor());
        }
        return currentFuture;
    }

    public void executeEmailCacheStagePlugins(final Email[] emails) {
        for (final Email email : emails) {
            executeEmailCacheStagePlugins(email);
        }
    }

    private void executeEmailCacheStagePlugins(final Email email) {
        for (final EmailCacheStagePlugin plugin : emailCacheStagePlugins) {
            plugin.onCacheEmail(email);
        }
    }

    private void install(final MuaSession muaSession) {
        for (final Plugin plugin : plugins.values()) {
            plugin.setMuaSession(muaSession);
            if (plugin instanceof EmailBuildStagePlugin) {
                this.emailBuildStagePlugins.add((EmailBuildStagePlugin) plugin);
            }
            if (plugin instanceof EmailCacheStagePlugin) {
                this.emailCacheStagePlugins.add((EmailCacheStagePlugin) plugin);
            }
        }
    }

    public <T extends Plugin> T getPlugin(final Class<T> plugin) {
        return this.plugins.getInstance(plugin);
    }

    public abstract static class Plugin {

        protected MuaSession muaSession;

        private synchronized void setMuaSession(final MuaSession muaSession) {
            if (this.muaSession != null) {
                throw new IllegalStateException("This plugin has already been installed");
            }
            this.muaSession = muaSession;
        }
    }
}
