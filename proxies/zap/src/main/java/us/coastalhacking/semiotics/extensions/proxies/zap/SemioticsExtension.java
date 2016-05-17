/**
 * 
 */
package us.coastalhacking.semiotics.extensions.proxies.zap;

import org.parosproxy.paros.extension.ExtensionAdaptor;

import us.coastalhacking.semiotics.extensions.common.Launcher;
import us.coastalhacking.semiotics.extensions.common.LauncherException;

/**
 * @author jonpasski
 *
 */
public class SemioticsExtension extends ExtensionAdaptor  {

	Launcher launcher;

	public SemioticsExtension() {
		super();
		launcher = new Launcher(getClass().getProtectionDomain().getCodeSource().getLocation());
	}

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
		try {
			launcher.startFramework();
		} catch (LauncherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void start() {
		try {
			launcher.startBundles();
		} catch (LauncherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		try {
			launcher.stopFramework();
		} catch (LauncherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
