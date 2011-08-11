/** 
 * Copyright 2011 John M. Schanck
 * 
 * Scan2Zotero is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Scan2Zotero is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Scan2Zotero.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ale.scan2zotero.web;

import org.apache.http.HttpVersion;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.os.Build;

public class HttpsClient extends DefaultHttpClient {

    private static final String USER_AGENT = "Scan2Zotero/0.1 Android/"+Build.VERSION.RELEASE;

    private static HttpsClient mInstance;
    
    public static HttpsClient getInstance() {
        if(mInstance == null){
            HttpParams mHttpParams = setupHttpParams();
            ThreadSafeClientConnManager mConnMan = setupSSLConnMan(mHttpParams);
            mInstance = new HttpsClient(mConnMan, mHttpParams);
        }
        return mInstance;
    }

    public HttpsClient(ThreadSafeClientConnManager connMan, HttpParams httpParams) {
        super(connMan, httpParams);
    }

    public static HttpParams setupHttpParams(){
        HttpParams params = new BasicHttpParams();
        // Protocol Parameters
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "utf-8");
        HttpProtocolParams.setUseExpectContinue(params, false);
        HttpProtocolParams.setUserAgent(params, USER_AGENT);

        // Connection Parameters
        // HttpConnectionParams.setConnectionTimeout(params, 5000);
        return params;
    }

    public static ThreadSafeClientConnManager setupSSLConnMan(HttpParams params){
        SchemeRegistry registry = new SchemeRegistry();

        SSLSocketFactory sf = SSLSocketFactory.getSocketFactory();
        sf.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        registry.register(new Scheme("https", (SocketFactory) sf, 443));

        return new ThreadSafeClientConnManager(params, registry);
    }

}
