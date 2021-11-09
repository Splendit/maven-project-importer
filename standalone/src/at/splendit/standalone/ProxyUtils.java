package at.splendit.standalone;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ProxyUtils {
	private static final String PROXY_SETTINGS = "PROXY.SETTINGS"; 

	private ProxyUtils() {

	}

	public static void configureProxy(BundleContext context) throws IOException, CoreException {

		Queue<ProxySettings> proxySettingsList = parseProxySettingsFromBundleContext(
				context.getProperty(PROXY_SETTINGS));

		if (proxySettingsList.isEmpty()) {
			return;
		}

		setProxy(context, proxySettingsList);
	}

	private static Queue<ProxySettings> parseProxySettingsFromBundleContext(String settingsString) throws IOException {

		if (settingsString == null || settingsString.isEmpty()) {
			return new LinkedList<>();
		}

		String[] splitSettingsString = settingsString.split("§"); 
		Queue<ProxySettings> proxySettingsList = new LinkedList<>();

		for (String proxySettingsString : splitSettingsString) {
			String proxySettingsPropertyString = proxySettingsString.replace("^", "\n");  
			Properties prop = new Properties();
			prop.load(new StringReader(proxySettingsPropertyString));

			ProxySettings proxySettings = new ProxySettings();
			proxySettings.setType(prop.getProperty("type")); 
			proxySettings.setHost(prop.getProperty("host")); 
			proxySettings.setPort(Integer.parseInt(prop.getProperty("port"))); 
			proxySettings.setUserId(prop.getProperty("username", null)); 
			proxySettings.setPassword(prop.getProperty("password", null)); 

			String nonProxyHosts = prop.getProperty("nonProxyHosts"); 
			if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
				String[] nonProxyHostsArray = nonProxyHosts.split("|"); 
				List<String> nonProxyHostsList = Arrays.asList(nonProxyHostsArray);
				proxySettings.setNonProxyHosts(nonProxyHostsList);
			}

			proxySettingsList.add(proxySettings);
		}

		return proxySettingsList;
	}

	/**
	 * Use the given {@link ProxySettings} to configure the equinox proxy
	 * 
	 * @param settings
	 *            object containing the proxy settings
	 * @throws StandaloneException
	 *             when the proxy couldn't be set
	 */
	private static void setProxy(BundleContext bundleContext, Queue<ProxySettings> settings) throws CoreException {
		ServiceReference<IProxyService> proxyServiceReference;
		IProxyService proxyService;
		proxyServiceReference = bundleContext.getServiceReference(IProxyService.class);
		proxyService = (IProxyService) bundleContext.getService(proxyServiceReference);

		if (proxyService == null) {
//			throw new StandaloneException(Messages.ProxyConfiguration_CouldNotGetProxyServiceInstance);
		}

		IProxyData[] proxyData = proxyService.getProxyData();

		List<String> nonProxyHosts = new LinkedList<String>();
		for (IProxyData proxy : proxyData) {
			for (ProxySettings setting : settings) {
				boolean httpOrHttps = !IProxyData.SOCKS_PROXY_TYPE.equals(proxy.getType());
				if (httpOrHttps && proxy.getType()
					.equals(setting.getType())) {
					proxy.setHost(setting.getHost());
					proxy.setPort(setting.getPort());
					proxy.setUserid(setting.getUserId());
					proxy.setPassword(setting.getPassword());
					nonProxyHosts.addAll(setting.getNonProxyHosts());
				}
			}
		}


		if (!nonProxyHosts.isEmpty()) {
			proxyService.setNonProxiedHosts(nonProxyHosts.toArray(new String[0]));
		}
		proxyService.setProxyData(proxyData);
		proxyService.setSystemProxiesEnabled(false);
		proxyService.setProxiesEnabled(true);

		bundleContext.ungetService(proxyServiceReference);
		proxyService = null;
		proxyServiceReference = null;
	}
}
