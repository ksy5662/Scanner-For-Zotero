package org.ale.scan2zotero;

import java.net.URI;
import java.net.URISyntaxException;

import org.ale.scan2zotero.data.Account;
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

import android.net.Uri;
import android.os.Build;
import android.os.Handler;

public class ZoteroAPIClient {
    private static final boolean DEBUG = false;

    private static final String CLASS_TAG = ZoteroAPIClient.class.getCanonicalName();

    private static final String USER_AGENT = "Scan2Zotero/0.1 Android/"+Build.VERSION.RELEASE;

    private static final String ZOTERO_BASE_URL = DEBUG ? "http://10.13.37.64/" : "https://api.zotero.org/";

    private Account mAccount;

    private String mPrefix;

    private HttpParams mHttpParams;

    private ThreadSafeClientConnManager mConnMan;

    private DefaultHttpClient mHttpsClient;

    private RequestQueue mRequestQueue;

    protected ZoteroAPIClient() {
        mHttpParams = setupHttpParams();
        mConnMan = setupSSLConnMan(mHttpParams);
        mHttpsClient = new DefaultHttpClient(mConnMan, mHttpParams);

        mRequestQueue = RequestQueue.getInstance();
    }

    protected ZoteroAPIClient(Account acct) {
        this();
        mPrefix = "/users/";
        mAccount = acct;
    }

    protected void setAccount(Account acct){
        mAccount = acct;
    }
    
    private HttpParams setupHttpParams(){
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

    private ThreadSafeClientConnManager setupSSLConnMan(HttpParams params){
        SchemeRegistry registry = new SchemeRegistry();

        if(DEBUG){ // Debug with HTTP
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        }else{ // But normally only allow HTTPS
            SSLSocketFactory sf = SSLSocketFactory.getSocketFactory();
            sf.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
            registry.register(new Scheme("https", (SocketFactory) sf, 443));
        }

        return new ThreadSafeClientConnManager(params, registry);
    }

    protected void addItem(Handler resultHandler, String jsonContent){
        ZoteroAPIRequest r = new ZoteroAPIRequest(resultHandler, mHttpsClient, ZoteroAPIRequest.POST);
        try {
            r.setURI(new URI(ZOTERO_BASE_URL + "/users/" + mAccount.getUid() + "/items?key=" + mAccount.getKey()));
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        r.setContent(jsonContent, "application/json");

        mRequestQueue.enqueue(r);
    }

    protected void getUsersGroups(Handler resultHandler){
/*        Uri.Builder ub = new Uri.Builder();
        ub.authority(ZOTERO_BASE_URL);
        ub.path("users");
        ub.appendPath(mAccount.getUid());
        ub.appendPath("groups");
        ub.appendQueryParameter("key", mAccount.getKey());
*/
        ZoteroAPIRequest r = new ZoteroAPIRequest(resultHandler, mHttpsClient, ZoteroAPIRequest.GET);
        try {
            r.setURI(new URI(ZOTERO_BASE_URL + "users/" + mAccount.getUid() + "/groups?key=" + mAccount.getKey()));
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mRequestQueue.enqueue(r);
    }

/*    protected boolean doRequest(){
        try {
            HttpGet request = new HttpGet();
            request.setURI(new URI(ZOTERO_BASE_URL));

            HttpResponse response = mHttpsClient.execute(request);

            StatusLine status = response.getStatusLine();
            if(status.getStatusCode() != 200){
                throw new IOException("Invalid response from server: " + status.toString());
            }

            // Pull content stream from response
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();

            ByteArrayOutputStream content = new ByteArrayOutputStream();

            // Read response into a buffered stream
            int readBytes = 0;
            byte[] sBuffer = new byte[512];
            while ((readBytes = inputStream.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }

            // Return result from buffered stream
            String dataAsString = new String(content.toByteArray());
            Log.d(CLASS_TAG, dataAsString);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }*/
}
