package at.splendit.standalone;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.net.proxy.IProxyData;

public class ProxySettings {

	private String type;
	private String host;
	private int port;
	private String userId;
	private String password;
	private boolean requiresAuthentication;
	private List<String> nonProxyHosts = new ArrayList<>();

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		if (port < 0 || port > 65535) {
			throw new RuntimeException("Proxy port number must be between 0 and 65535");
		}

		this.port = port;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isRequiresAuthentication() {
		return requiresAuthentication;
	}

	public void setRequiresAuthentication(boolean requiresAuthentication) {
		this.requiresAuthentication = requiresAuthentication;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		if ("http".equalsIgnoreCase(type)) { 
			this.type = IProxyData.HTTP_PROXY_TYPE;
		} else if ("https".equalsIgnoreCase(type)) { 
			this.type = IProxyData.HTTPS_PROXY_TYPE;
		} else {
			throw new RuntimeException("Proxy only supports HTTP or HTTPS");
		}
	}

	public List<String> getNonProxyHosts() {
		return nonProxyHosts;
	}

	public void setNonProxyHosts(List<String> nonProxyHosts) {
		this.nonProxyHosts = nonProxyHosts;
	}
}
