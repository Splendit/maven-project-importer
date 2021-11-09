package at.splendit.standalone;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class Activator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class); 
	
	@Override
	public void start(BundleContext context) throws Exception {
		configureLogbackInBundle(context.getBundle());
		ProxyUtils.configureProxy(context);
		MavenProjectImporter projectImporter = new MavenProjectImporter();
		File workspaceRoot = ResourcesPlugin.getWorkspace()
				.getRoot()
				.getLocation()
				.toFile();
		String rootPath = context.getProperty("ROOT.PROJECT.BASE.PATH");
		List<String> projectPath = Collections.singletonList(rootPath);
		try {
			List<IJavaProject> importedProjects = projectImporter.importProjects(workspaceRoot, projectPath);
			logger.debug("Imported {} projects.", importedProjects.size());
		} catch (InterruptedException | CoreException e) {
			String key = "standalone.exit.message";
			String exitMessage = e.getMessage();
			EnvironmentInfo envInfo = getEnvironmentInfo(context);
			if (envInfo != null) {
				envInfo.setProperty(key, exitMessage);
			} else {
				System.setProperty(key, exitMessage);
			}
		}
		
	}
	
	private EnvironmentInfo getEnvironmentInfo(BundleContext ctx) {
		if (ctx == null) {
			return null;
		}

		ServiceReference<?> infoRev = ctx.getServiceReference(EnvironmentInfo.class.getName());
		if (infoRev == null) {
			return null;
		}

		EnvironmentInfo envInfo = (EnvironmentInfo) ctx.getService(infoRev);
		if (envInfo == null) {
			return null;
		}
		ctx.ungetService(infoRev);

		return envInfo;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (ResourcesPlugin.getWorkspace() != null) {
			ResourcesPlugin.getWorkspace()
				.forgetSavedTree("standalone");
			ResourcesPlugin.getWorkspace()
				.removeSaveParticipant("standalone");
		}
		
	}
	
    private void configureLogbackInBundle(Bundle bundle) throws JoranException, IOException {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(context);
        context.reset();

        // this assumes that the logback.xml file is in the root of the bundle.
        URL logbackConfigFileUrl = FileLocator.find(bundle, new Path("logback.xml"),null);
        jc.doConfigure(logbackConfigFileUrl.openStream());
    }

}
