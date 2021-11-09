package at.splendit.minimalm2esample.osgi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class BundleStarter {

	protected static final String ORG_APACHE_FELIX_SCR = "org.apache.felix.scr"; 
	
	private Log log;
	
	public BundleStarter(Log log) {
		this.log = log;
	}
	
	public void runOsgiStandaloneApp(Map<String, String> configuration) throws BundleException, MojoExecutionException, InterruptedException {
		log.debug("Starting Eclipse Equinox framework");
		ServiceLoader<FrameworkFactory> ffs = ServiceLoader.load(FrameworkFactory.class);
		FrameworkFactory frameworkFactory = ffs.iterator().next();
		Framework framework = frameworkFactory.newFramework(configuration);
		framework.start();
		BundleContext bundleContext = framework.getBundleContext();
		List<Bundle> bundles = loadBundles(bundleContext);
		startBundles(bundles);

		framework.stop();
		framework.waitForStop(0);

		log.debug("Equinox stopped");

		String exitMessage = bundleContext.getProperty("standalone.exit.message");
		if (exitMessage != null && !exitMessage.isEmpty()) {
			throw new MojoExecutionException(exitMessage);
		}
	}
	
	protected void startBundles(List<Bundle> bundles) {
		// Load apache felix first. It is needed for discovering OSGi services. 
		startApacheFelixSCR(bundles);

		bundles.stream()
			.filter(bundle -> bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null)
			.filter(bundle -> bundle.getSymbolicName() != null)
			.filter(bundle -> bundle.getSymbolicName().startsWith("at.splendit.standalone"))
			.forEach(bundle -> {
				try {
					String loggerInfo = String.format("Starting bundle %s:%s [s]", bundle.getSymbolicName(), bundle.getVersion(), bundle.getState());
					log.debug(loggerInfo);
					bundle.start();
				} catch (Exception e) {
					log.debug(e.getMessage(), e);
					log.error(e.getMessage());
				}
			});
	}
	
	protected void startApacheFelixSCR(List<Bundle> bundles) {

		for (Bundle bundle : bundles) {
			String symbolicName = bundle.getSymbolicName();
			if (symbolicName.startsWith(ORG_APACHE_FELIX_SCR)) {
				try {
					String message = String.format("Starting bundle %s:%s [%d]", symbolicName, bundle.getVersion(), bundle.getState()); 
					log.debug(message);
					bundle.start();
				} catch (BundleException e) {
					log.debug(e.getMessage(), e);
					log.error(e.getMessage());
				}
			}
		}
	}
	
	protected List<Bundle> loadBundles(BundleContext bundleContext) throws BundleException, MojoExecutionException {
		log.debug("Loading bundles from resources");
		final List<Bundle> bundles = new ArrayList<>();
		try (InputStream is = getClass().getResourceAsStream("/manifest.standalone")) {
			if (is != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
					String line = ""; 
					while ((line = reader.readLine()) != null) {
						InputStream fileStream = getClass().getResourceAsStream("/" + line);
						if (!line.startsWith("org.eclipse.osgi_")) { // OSGi is already loaded.
							Bundle bundle = bundleContext.installBundle("file://" + line, fileStream); 
							bundles.add(bundle);
						}
					}
				}
			} else {
				throw new MojoExecutionException("The standalone manifest file could not be found. Please read the readme-file."); 
			}
		} catch (IOException e) {
			log.debug(e.getMessage(), e);
			log.error(e.getMessage());
		}

		return bundles;
	}

}
