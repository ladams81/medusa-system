package com.sarxos.medusa.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MedusaHttpClient extends DefaultHttpClient {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(MedusaHttpClient.class.getSimpleName());

	/**
	 * Proxy host.
	 */
	private static String PROXY_HOST = (String) System.getProperties().get("http.proxyHost");

	/**
	 * Proxy port number.
	 */
	private static String PROXY_PORT = (String) System.getProperties().get("http.proxyPort");

	static {
		if (PROXY_HOST != null && PROXY_PORT != null) {
			LOG.info("Setting proxy '" + PROXY_HOST + ":" + PROXY_PORT + "'");
		}
	}

	/**
	 * HTTP proxy.
	 */
	private HttpHost proxy = null;

	public MedusaHttpClient(ClientConnectionManager conman, HttpParams params) {
		super(conman, params);
		init();
	}

	public MedusaHttpClient(ClientConnectionManager conman) {
		super(conman);
		init();
	}

	public MedusaHttpClient(HttpParams params) {
		super(params);
		init();
	}

	public MedusaHttpClient() {
		super();
		init();
	}

	/**
	 * Initialize client.
	 */
	private void init() {

		// set proxy
		if (PROXY_HOST != null && PROXY_PORT != null) {
			proxy = new HttpHost(PROXY_HOST, Integer.parseInt(PROXY_PORT), "http");
			getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}

		// do not allow circular redirects
		getParams().setBooleanParameter("http.protocol.allow-circular-redirects", false);

		// accept all cookies - even incorrect ones
		getCookieSpecs().register("naive", new NaiveCookieSpecFactory());
		getParams().setParameter(ClientPNames.COOKIE_POLICY, "naive");

		// redirect 30X all requests (GET, POST, HEAD)
		setRedirectStrategy(new BrowserRedirectStrategy());
	}

	/**
	 * @return Return proxy used by this client or null if no proxy is used
	 */
	public HttpHost getProxy() {
		return proxy;
	}

	protected HttpUriRequest affect(HttpUriRequest req) {

		req.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:2.0.1) Gecko/20100101 Firefox/4.0.1");
		req.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		req.setHeader("Accept-Language", "pl,en-us;q=0.7,es;q=0.3");
		req.setHeader("Accept-Charset", "ISO-8859-2,utf-8;q=0.7,*;q=0.7");
		req.setHeader("Keep-Alive", "115");
		req.setHeader("Accept-Encoding", "gzip,deflate");
		req.setHeader("Connection", "keep-alive");

		if (req instanceof HttpPost) {
			req.getParams().setBooleanParameter("http.protocol.expect-continue", false);
		}
		if (getProxy() != null) {
			req.setHeader("Proxy-Connection", "keep-alive");
		}

		return req;
	}

	public void executeVoid(HttpUriRequest req) {
		affect(req);
		HttpResponse response = null;
		try {
			response = execute(req);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (response != null) {
				try {
					response.getEntity().getContent().close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Download file from specific URL.
	 * 
	 * @param from - file to download
	 * @param to - file to be stored locally
	 * @throws HttpException when something wrong happens
	 */
	public void download(String from, File to) throws HttpException {
		int attempts = 0;
		int max = 5;
		do {
			try {
				download0(from, to);
				return;
			} catch (HttpException e) {
				LOG.error(
						"Invalid download attempt. " +
						(attempts < max - 1 ? " Trying one more time" : "Fatal."), e);
			}
		} while (attempts++ < max);
	}

	private void download0(String url, File f) throws HttpException {

		HttpEntity entity = null;
		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(f);

			HttpGet get = new HttpGet(url);
			HttpResponse response = execute(get);
			entity = response.getEntity();
			entity.writeTo(fos);

		} catch (Exception e) {
			throw new HttpException(
					"Cannot download file from '" + url + "' to '" +
					f.getPath() + "'", e);
		} finally {

			if (entity != null) {
				try {
					entity.getContent().close();
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}

			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
	}
}