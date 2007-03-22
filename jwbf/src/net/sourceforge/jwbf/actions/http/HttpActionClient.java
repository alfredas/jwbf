/*
 * Copyright 2007 Thomas Stock.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors:
 * 
 */

package net.sourceforge.jwbf.actions.http;

import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 * The main interaction class.
 * 
 * @author Thomas Stock
 * 
 */
public class HttpActionClient {

	private HttpClient client;

	private String path = "";

	private Logger log = Logger.getLogger(Action.class);

	/**
	 * 
	 * @param client
	 *            a
	 * @param path
	 *            like "/w/index.php"
	 */
	public HttpActionClient(final HttpClient client, final String path) {
		/*
		 * see for docu
		 * http://jakarta.apache.org/commons/httpclient/preference-api.html
		 */

		this.client = client;

		this.path = path;
	}

	/**
	 * 
	 * @param client
	 *            a
	 */
	public HttpActionClient(final HttpClient client) {
		this(client, "");

	}

	/**
	 * 
	 * @param a
	 *            a
	 * @return message, never null
	 * @throws ActionException
	 *             on problems
	 */
	public String performAction(Action a) throws ActionException {
		List<HttpMethod> msgs = a.getMessages();
		String out = "";
		Iterator<HttpMethod> it = msgs.iterator();
		while (it.hasNext()) {
			HttpMethod e = it.next();
			if (path.length() > 1) {
				e.setPath(path);
			}
			try {
				if (e instanceof GetMethod) {
					out = get(e, a);
				} else {
					out = post(e, a);
				}
			} catch (Exception ex) {
				throw new ActionException(ex);
			}
		}
		return out;

	}

	/**
	 * Process a POST Message.
	 * 
	 * @param authpost
	 *            a
	 * @param cp
	 *            a
	 * @return a returning message, not null
	 * @throws Exception
	 *             on problems or if document can't be found
	 */
	protected String post(HttpMethod authpost, ContentProcessable cp)
			throws Exception {
		showCookies(client);

		String out = "";

		client.executeMethod(authpost);
		cp.validateReturningCookies(client.getState().getCookies(), authpost);
		out = new String(authpost.getResponseBody());
		out = cp.processReturningText(out, authpost);

		authpost.releaseConnection();
		log.debug(authpost.getURI());
		log.debug("POST: " + authpost.getStatusLine().toString());

		// Usually a successful form-based login results in a redicrect to
		// another url
		// int statuscode = authpost.getStatusCode();
		// if ((statuscode == HttpStatus.SC_MOVED_TEMPORARILY)
		// || (statuscode == HttpStatus.SC_MOVED_PERMANENTLY)
		// || (statuscode == HttpStatus.SC_SEE_OTHER)
		// || (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
		// Header header = authpost.getResponseHeader("location");
		// if (header != null) {
		// String newuri = header.getValue();
		// if ((newuri == null) || (newuri.equals(""))) {
		// newuri = "/";
		// }
		// log.debug("Redirect target: " + newuri);
		// GetMethod redirect = new GetMethod(newuri);
		//
		// client.executeMethod(redirect);
		// log.debug("Redirect: " + redirect.getStatusLine().toString());
		// // release any connection resources used by the method
		// redirect.releaseConnection();
		// } else {
		// throw new Exception("Invalid redicet");
		// }
		// }

		return out;
	}

	/**
	 * Process a GET Message.
	 * 
	 * @param authgets
	 *            a
	 * @param cp
	 *            a
	 * @return a returning message, not null
	 * @throws Exception
	 *             on problems or if document can't be found
	 */
	protected String get(HttpMethod authgets, ContentProcessable cp)
			throws Exception {
		showCookies(client);
		String out = "";

		client.executeMethod(authgets);
		cp.validateReturningCookies(client.getState().getCookies(), authgets);
		log.debug(authgets.getURI());
		log.debug("GET: " + authgets.getStatusLine().toString());

		out = new String(authgets.getResponseBody());
		out = cp.processReturningText(out, authgets);
		// release any connection resources used by the method
		authgets.releaseConnection();
		int statuscode = authgets.getStatusCode();

		if (statuscode == HttpStatus.SC_NOT_FOUND) {
			log.warn("Not Found: " + authgets.getQueryString());

			throw new FileNotFoundException(authgets.getQueryString());
		}

		return out;
	}

	/**
	 * send the cookies to the logger.
	 * 
	 * @param client
	 *            a
	 */
	protected void showCookies(HttpClient client) {
		Cookie[] cookies = client.getState().getCookies();
		if (cookies.length > 0) {
			for (int i = 0; i < cookies.length; i++) {
				log.trace("cookie: " + cookies[i].toString());
			}
		}
	}
}