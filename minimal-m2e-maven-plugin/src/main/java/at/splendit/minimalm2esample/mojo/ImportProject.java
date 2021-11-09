package at.splendit.minimalm2esample.mojo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import at.splendit.minimalm2esample.osgi.BundleStarter;

@Mojo(name = "import", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.INITIALIZE, aggregator = true)
public class ImportProject extends AbstractMojo {

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${maven.home}", readonly = true)
	private String mavenHome;

	@Parameter(defaultValue = "", property = "tempWorkspace")
	private String tempWorkspacePath;
	
	private static final ComparableVersion VERSION_11 = new ComparableVersion("11"); 

	public void execute() throws MojoExecutionException, MojoFailureException {
		Log log = getLog();

		if (!isAtLeastJava11()) {
			log.error("This sample maven plugin requires Java 11");
			throw new MojoExecutionException("This sample maven plugin requires Java 11");
		}
		
		String workspacePathStr = tempWorkspacePath == null || "".equals(tempWorkspacePath) ? 
				".." + File.separator + ".." +  File.separator + "tempWorkspace" : 
					tempWorkspacePath;
		File tempWorkspace = new File(workspacePathStr).getAbsoluteFile();
		String tempWorkspacePath = tempWorkspace.getAbsolutePath();
		System.setProperty("user.dir", tempWorkspacePath);
		Map<String, String> configuration = new HashMap<>();
		configuration.put("PROXY.SETTINGS", getSettingsStringFrom(mavenSession));
		configuration.put("ROOT.PROJECT.BASE.PATH", project.getBasedir().getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, "target/bundlecache");
		configuration.put("osgi.instance.area.default", tempWorkspacePath);
		configuration.put(Constants.FRAMEWORK_BOOTDELEGATION,
				"javax.*,org.xml.*,sun.*,com.sun.*,jdk.internal.reflect,jdk.internal.reflect.*");
		configuration.put("debug.enabled", Boolean.toString(log.isDebugEnabled()));

		BundleStarter bundleStarter = new BundleStarter(log);
		try {
			bundleStarter.runOsgiStandaloneApp(configuration);
		} catch (MojoExecutionException | BundleException | InterruptedException e) {
			log.error(e.getMessage(), e);
		}
	}

	public static boolean isAtLeastJava11() {
		String javaVersion = System.getProperty("java.version");
		ComparableVersion currentVersion = new ComparableVersion(javaVersion);
		return currentVersion.compareTo(VERSION_11) >= 0;
	}

	public static String getSettingsStringFrom(MavenSession mavenSession) {

		return mavenSession.getSettings().getProxies().stream().filter(Proxy::isActive)
				.filter(p -> "https".equalsIgnoreCase(p.getProtocol()) || "http".equalsIgnoreCase(p.getProtocol()))  
				.filter(p -> p.getHost() != null && !p.getHost().isEmpty())
				.filter(p -> p.getPort() > 0 && p.getPort() < 65535).map(p -> {
					StringBuilder proxySB = new StringBuilder();

					proxySB.append("type=").append(p.getProtocol()).append("^") // delimiter
							.append("host=").append(p.getHost()).append("^") // delimiter
							.append("port=").append(String.valueOf(p.getPort())).append("^"); // delimiter

					String username = p.getUsername();
					if (username != null && !username.isEmpty()) {
						proxySB.append("username=").append(username).append("^");
					}

					String password = p.getPassword();
					if (password != null && !password.isEmpty()) {
						proxySB.append("password=").append(password).append("^");
					}

					String nonProxyHosts = p.getNonProxyHosts();
					if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
						proxySB.append("nonProxyHosts=").append(nonProxyHosts).append("^");
					}
					return proxySB.toString();
				}).collect(Collectors.joining("^"));
	}

}
