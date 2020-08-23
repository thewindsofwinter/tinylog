/*
 * Copyright 2020 Martin Winandy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.tinylog.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import org.tinylog.core.providers.BundleLoggingProvider;
import org.tinylog.core.providers.LoggingProvider;
import org.tinylog.core.providers.LoggingProviderBuilder;
import org.tinylog.core.providers.NopLoggingProvider;
import org.tinylog.core.providers.NopLoggingProviderBuilder;

/**
 * Storage for {@link Configuration}, {@link Hook Hooks}, and {@link LoggingProvider}.
 */
public final class Framework {

	private final Configuration configuration;
	private final Collection<Hook> hooks;
	private final Object loggingProviderMutex = new Object();

	private LoggingProvider loggingProvider;
	private boolean running;

	/**
	 * Loads the configuration from default properties file and hooks from service files.
	 */
	public Framework() {
		this(loadConfiguration(), loadHooks());
	}

	/**
	 * Initializes the framework with a custom configuration and custom hooks.
	 *
	 * @param configuration Configuration to store
	 * @param hooks Hooks to store
	 */
	public Framework(Configuration configuration, Collection<Hook> hooks) {
		this.configuration = configuration;
		this.hooks = hooks;
	}

	/**
	 * Gets the class loader for loading resources and services from the classpath.
	 *
	 * @return A valid and existing class loader instance
	 */
	public ClassLoader getClassLoader() {
		return loadClassLoader();
	}

	/**
	 * Gets the stored configuration.
	 *
	 * @return The stored configuration
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Gets the logging provider from the stored configuration.
	 *
	 * @return The logging provider implementation
	 */
	public LoggingProvider getLoggingProvider() {
		if (loggingProvider == null) {
			synchronized (loggingProviderMutex) {
				loadLoggingProvider();
			}
		}

		return loggingProvider;
	}

	/**
	 * Registers a new {@link Hook}.
	 *
	 * @param hook Hook to register
	 */
	public void registerHook(Hook hook) {
		hooks.add(hook);
	}

	/**
	 * Removes a registered {@link Hook}.
	 *
	 * @param hook Hook to unregister
	 */
	public void removeHook(Hook hook) {
		hooks.remove(hook);
	}

	/**
	 * Starts the framework and calls the start up method on all registered hooks, if the framework is not yet started.
	 */
	public void startUp() {
		synchronized (hooks) {
			running = true;

			for (Hook hook : hooks) {
				hook.startUp();
			}
		}

	}

	/**
	 * Stops the framework and calls the shut down method on all registered hooks, if the framework is not yet shut
	 * down.
	 */
	public void shutDown() {
		synchronized (hooks) {
			if (running) {
				running = false;

				for (Hook hook : hooks) {
					hook.shutDown();
				}
			}
		}
	}

	/**
	 * Gets the class loader for loading resources and services from the classpath.
	 *
	 * @return A valid and existing class loader instance
	 */
	private static ClassLoader loadClassLoader() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return classLoader == null ? Framework.class.getClassLoader() : classLoader;
	}

	/**
	 * Creates a new {@link Configuration} and loads the settings from default properties file if available.
	 *
	 * @return The created and pre-filled configuration
	 */
	private static Configuration loadConfiguration() {
		Configuration configuration = new Configuration();
		configuration.loadPropertiesFile();
		return configuration;
	}

	/**
	 * Loads all hooks that are registered as {@link java.util.ServiceLoader service} in {@code META-INF/services}.
	 *
	 * @return All found hooks
	 */
	private static Collection<Hook> loadHooks() {
		Collection<Hook> hooks = new ArrayList<>();
		for (Hook hook : ServiceLoader.load(Hook.class, loadClassLoader())) {
			hooks.add(hook);
		}
		return hooks;
	}

	/**
	 * Freezes the stored configuration and loads the logging provider.
	 */
	private void loadLoggingProvider() {
		configuration.freeze();

		List<String> names = configuration.getList("backend");
		List<LoggingProvider> providers = new ArrayList<>();

		for (LoggingProviderBuilder builder : ServiceLoader.load(LoggingProviderBuilder.class, getClassLoader())) {
			if ((names.isEmpty() && !(builder instanceof NopLoggingProviderBuilder))
					|| names.contains(builder.getName())) {
				providers.add(builder.create(this));
			}
		}

		if (providers.isEmpty()) {
			System.err.println("No logging back end could be found in the classpath. Therefore, no log entries will be "
					+ "output. Please add tinylog-impl or any other logging back end for outputting log entries.");
			loggingProvider = new NopLoggingProvider();
		} else if (providers.size() == 1) {
			loggingProvider = providers.get(0);
		} else {
			loggingProvider = new BundleLoggingProvider(providers);
		}
	}

}
