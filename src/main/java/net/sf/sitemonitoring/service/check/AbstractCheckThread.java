package net.sf.sitemonitoring.service.check;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import net.sf.sitemonitoring.entity.Check;
import net.sf.sitemonitoring.event.AbortCheckEvent;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.common.eventbus.Subscribe;

@Slf4j
public abstract class AbstractCheckThread extends Thread {

	protected boolean abort = false;

	protected String output;

	protected Check check;

	protected CloseableHttpClient httpClient;

	public AbstractCheckThread(Check check) {
		this.check = check;
	}

	@Subscribe
	public void abort(AbortCheckEvent abortCheckEvent) throws IOException {
		log.debug("called abort " + abortCheckEvent.getCheckId());
		if (check.getId() == abortCheckEvent.getCheckId()) {
			log.debug("aborted check " + check.getId());
			abort = true;
			httpClient.close();
		}
	}

	public abstract void performCheck();

	protected CloseableHttpClient buildHttpClient() {
		if (check.getHttpProxyUsername() != null && !check.getHttpProxyPassword().isEmpty()) {
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(check.getHttpProxyServer(), check.getHttpProxyPort()),
					new UsernamePasswordCredentials(check.getHttpProxyUsername(), check.getHttpProxyPassword()));
			return HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
		} else {
			return HttpClients.createDefault();
		}
	}

	public void run() {
		httpClient = buildHttpClient();
		try {
			performCheck();
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error("Error closing HTTP client", e);
			}
		}
	}

	public String getOutput() {
		return output;
	}

	protected void appendMessage(String message) {
		if (output == null) {
			output = "";
		}
		if (message != null && !message.trim().isEmpty()) {
			output += message;
		}
	}

	protected void copyConnectionSettings(Check original, Check result) {
		result.setConnectionTimeout(original.getConnectionTimeout());
		result.setSocketTimeout(original.getSocketTimeout());
		result.setUserAgent(original.getUserAgent());
		result.setHttpProxyServer(original.getHttpProxyServer());
		result.setHttpProxyPort(original.getHttpProxyPort());
		result.setHttpProxyUsername(original.getHttpProxyUsername());
		result.setHttpProxyPassword(original.getHttpProxyPassword());
	}

}
