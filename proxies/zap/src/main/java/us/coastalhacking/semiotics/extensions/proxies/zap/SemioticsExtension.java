/**
 * 
 */
package us.coastalhacking.semiotics.extensions.proxies.zap;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.parosproxy.paros.extension.ExtensionAdaptor;

/**
 * @author jonpasski
 *
 */
public class SemioticsExtension extends ExtensionAdaptor  {

	/* (non-Javadoc)
	 * @see org.parosproxy.paros.extension.Extension#getAuthor()
	 */
	public String getAuthor() {
		return "Coastal Hacking";
	}

	/* (non-Javadoc)
	 * @see org.parosproxy.paros.extension.ExtensionAdaptor#init()
	 */
	@Override
	public void init() {

		FrameworkFactory frameworkFactory = getFrameworkFactory();
		if (frameworkFactory != null) {
			Map<String, String> config = new HashMap<String, String>();
			// TODO: add some config properties
			Framework framework = frameworkFactory.newFramework(config);
			try {
				System.out.println("Starting OSGi...");
				framework.start();
			} catch (BundleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// TODO log
		}
	}

	private FrameworkFactory getFrameworkFactory() {
		FrameworkFactory frameworkFactory = null;
		// http://stackoverflow.com/a/1983870
		URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
		// http://stackoverflow.com/a/60775
		URLClassLoader shadedLoader = new URLClassLoader(new URL[] { location }, getClass().getClassLoader());
		try {
			Iterator<FrameworkFactory> iter = ServiceLoader.load(FrameworkFactory.class, shadedLoader).iterator();
			if (iter.hasNext()) {
				frameworkFactory = iter.next();
			}
		} catch (Exception ex) {
			// ZAP throws an exception if no implementation is found
			// via org.parosproxy.paros.extension.ExtensionLoader
		}
		return frameworkFactory;
	}
}
