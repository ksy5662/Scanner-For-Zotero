package org.ale.scan2zotero.web;

import org.apache.http.HttpVersion;
import org.apache.http.conn.scheme.PlainSocketFactory;
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
    
    public static HttpsClient getHttpsClientInstance() {
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

        // XXX: Disallow HTTP in production release
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

        SSLSocketFactory sf = SSLSocketFactory.getSocketFactory();
        sf.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        registry.register(new Scheme("https", (SocketFactory) sf, 443));

        return new ThreadSafeClientConnManager(params, registry);
    }

}
