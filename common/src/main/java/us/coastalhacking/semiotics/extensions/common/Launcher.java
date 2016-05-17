package us.coastalhacking.semiotics.extensions.common;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class Launcher {

	private Framework framework;
	/**
	 * The location of the shaded extension jar
	 */
	private URL location;


	public Launcher(URL location) {
		super();
		this.location = location;
	}

	public void startFramework() throws LauncherException {
		FrameworkFactory frameworkFactory = getFrameworkFactory();
		if (frameworkFactory != null) {
			Map<String, String> config = new HashMap<String, String>();
			config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, equinox());
			config.put(Constants.FRAMEWORK_STORAGE_CLEAN, "true");
			config.put("osgi.console", ""); // use stdin for console in equinox
			framework = frameworkFactory.newFramework(config);
			try {
				// TODO log
				System.out.println("Starting OSGi framework...");
				framework.start();
				System.out.println(String.format("OSGi framework state: %s", framework.getState()));
			} catch (BundleException e) {
				// TODO Auto-generated catch block
				System.out.println("Error starting OSGi framework");
				e.printStackTrace();
				throw new LauncherException(e);
			}
		} else {
			// TODO log
		}
	}

	public void startBundles() throws LauncherException {
		if (framework != null) {
			BundleContext context = framework.getBundleContext();
			Bundle[] bundles = context.getBundles();

			List<Bundle> installedBundles = new LinkedList<Bundle>();
			Map<String, Bundle> installedBundleLocations = new HashMap<String, Bundle>();
			for (Bundle bundle : bundles) {
				installedBundleLocations.put(bundle.getLocation(), bundle);
			}

			for (String jar : jarsInJar()) {
				// May have been previously installed
				// Still needs to be started
				if (installedBundleLocations.keySet().contains(jar)) {
					installedBundles.add(installedBundleLocations.get(jar));
					continue;
				}

				Bundle bundle;
				try {
					System.out.println(String.format("Installing bundle: %s", jar));
					bundle = context.installBundle(jar);
					System.out.println(String.format("Adding installed bundle to be started: %s", bundle));
					installedBundles.add(bundle);
				} catch (BundleException e) {
					System.out.println(String.format("Could not install bundle: %s", jar));
					// TODO : handle better
					// A bundle may have been already installed
					// via a transitive load
					e.printStackTrace();
					// throw new LauncherException(e);
				}
			}

			for (Bundle bundle : installedBundles) {
				try {
					if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
						System.out.println(String.format("Starting bundle: %s", bundle));
						bundle.start();
					} else {
						System.out.println(String.format("Ignoring starting bundle: %s", bundle));
					}
				} catch (BundleException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					// throw new LauncherException(e);
					try {
						// TODO log
						System.out.println(String.format("Attempting to uninstall bundle: %s", bundle));
						bundle.uninstall();
						System.out.println("Uninstall successful.");
					} catch (BundleException e1) {
						// TODO Auto-generated catch block
						System.out.println("Uninstall failed.");
						e1.printStackTrace();
					}
				}
			}
		} else {
			// TODO log
		}
	}

	public void stopFramework() throws LauncherException {
		if (framework != null) {
			try {
				System.out.println("Stopping OSGi framework...");
				framework.stop();
			} catch (BundleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				// throw new LauncherException(e);
			}
		}
	}

	private FrameworkFactory getFrameworkFactory() {
		FrameworkFactory frameworkFactory = null;
		try {
			ClassLoader loader = getClass().getClassLoader();
			Iterator<FrameworkFactory> iter = ServiceLoader.load(FrameworkFactory.class, loader).iterator();
			if (iter.hasNext()) {
				frameworkFactory = iter.next();
			} else {
				// TODO log
				System.out.println("Could not find factory via loader: " + loader.toString());
			}
		} catch (Exception ex) {
			// ZAP throws an exception if no implementation is found
			// via org.parosproxy.paros.extension.ExtensionLoader
			// TODO: log
			ex.printStackTrace();
		}
		return frameworkFactory;
	}

	private List<String> jarsInJar() {
		List<String> results = new ArrayList<String>();
		// http://stackoverflow.com/a/15331935
		JarURLConnection urlcon;
		try {
			// TODO hack: find a better home for this re-cast
			if (location.getProtocol().equals("file")) {
				location = new URL(String.format("jar:%s!/", location.toString()));
			}
			urlcon = (JarURLConnection) (location.openConnection());

			try (JarFile jar = urlcon.getJarFile()) {
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					String entry = entries.nextElement().getName();
					if (entry.endsWith(".jar")) {
						String jarEntry = String.format("%s%s", location.toString(), entry);
						results.add(jarEntry);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

	private static String equinox() {
		// TODO return this via the OSGi dependency versus hardcoded here
		// Taken from Export-Package from org.eclipse.osgi
		// We need to export the OSGi dependencies aren't since they're not on
		// the system classpath. That is, the
		// 'org.osgi.framework.system.packages'
		// property won't have the OSGi packages exported.
		return "org.eclipse.core.runtime.adaptor;x-friends:=\"org.eclipse.core.runtime\",org.eclipse.core.runtime.internal.adaptor;x-internal:=true,org.eclipse.equinox.log;version=\"1.0\",org.eclipse.osgi.container;version=\"1.0\",org.eclipse.osgi.container.builders;version=\"1.0\",org.eclipse.osgi.container.namespaces;version=\"1.0\",org.eclipse.osgi.framework.console;version=\"1.1\",org.eclipse.osgi.framework.eventmgr;version=\"1.2\",org.eclipse.osgi.framework.internal.reliablefile;x-internal:=true,org.eclipse.osgi.framework.log;version=\"1.1\",org.eclipse.osgi.framework.util;x-internal:=true,org.eclipse.osgi.internal.debug;x-internal:=true,org.eclipse.osgi.internal.framework;x-internal:=true,org.eclipse.osgi.internal.hookregistry;x-friends:=\"org.eclipse.osgi.tests\",org.eclipse.osgi.internal.loader;x-internal:=true,org.eclipse.osgi.internal.loader.buddy;x-internal:=true,org.eclipse.osgi.internal.loader.classpath;x-internal:=true,org.eclipse.osgi.internal.loader.sources;x-internal:=true,org.eclipse.osgi.internal.location;x-internal:=true,org.eclipse.osgi.internal.messages;x-internal:=true,org.eclipse.osgi.internal.provisional.service.security;version=\"1.0.0\";x-friends:=\"org.eclipse.equinox.security.ui\",org.eclipse.osgi.internal.provisional.verifier;x-friends:=\"org.eclipse.update.core,org.eclipse.ui.workbench,org.eclipse.equinox.p2.artifact.repository\",org.eclipse.osgi.internal.service.security;x-friends:=\"org.eclipse.equinox.security.ui\",org.eclipse.osgi.internal.serviceregistry;x-internal:=true,org.eclipse.osgi.internal.signedcontent;x-internal:=true,org.eclipse.osgi.internal.url;x-internal:=true,org.eclipse.osgi.storage.url,org.eclipse.osgi.storage.url.bundleresource,org.eclipse.osgi.storage.url.bundleentry,org.eclipse.osgi.storage.url.bundleresource,org.eclipse.osgi.launch;version=\"1.0\",org.eclipse.osgi.report.resolution;version=\"1.0\",org.eclipse.osgi.service.datalocation;version=\"1.3\",org.eclipse.osgi.service.debug;version=\"1.2\",org.eclipse.osgi.service.environment;version=\"1.3\",org.eclipse.osgi.service.localization;version=\"1.1\",org.eclipse.osgi.service.pluginconversion;version=\"1.0\",org.eclipse.osgi.service.resolver;version=\"1.6\",org.eclipse.osgi.service.runnable;version=\"1.1\",org.eclipse.osgi.service.security;version=\"1.0\",org.eclipse.osgi.service.urlconversion;version=\"1.0\",org.eclipse.osgi.signedcontent;version=\"1.0\",org.eclipse.osgi.storage;x-friends:=\"org.eclipse.osgi.tests\",org.eclipse.osgi.storage.bundlefile;x-internal:=true,org.eclipse.osgi.storage.url.reference;x-internal:=true,org.eclipse.osgi.storagemanager;version=\"1.0\",org.eclipse.osgi.util;version=\"1.1\",org.osgi.dto;version=\"1.0\",org.osgi.framework;version=\"1.8\",org.osgi.framework.dto;version=\"1.8\",org.osgi.framework.hooks.bundle;version=\"1.1\",org.osgi.framework.hooks.resolver;version=\"1.0\",org.osgi.framework.hooks.service;version=\"1.1\",org.osgi.framework.hooks.weaving;version=\"1.1\",org.osgi.framework.launch;version=\"1.2\",org.osgi.framework.namespace;version=\"1.1\",org.osgi.framework.startlevel;version=\"1.0\",org.osgi.framework.startlevel.dto;version=\"1.0\",org.osgi.framework.wiring;version=\"1.2\",org.osgi.framework.wiring.dto;version=\"1.2\",org.osgi.resource;version=\"1.0\",org.osgi.resource.dto;version=\"1.0\",org.osgi.service.condpermadmin;version=\"1.1.1\",org.osgi.service.log;version=\"1.3\",org.osgi.service.packageadmin;version=\"1.2\",org.osgi.service.permissionadmin;version=\"1.2\",org.osgi.service.resolver;version=\"1.0.1\",org.osgi.service.startlevel;version=\"1.1\",org.osgi.service.url;version=\"1.0\",org.osgi.util.tracker;version=\"1.5.1\"";
	}
}
