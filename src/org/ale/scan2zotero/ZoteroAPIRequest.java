package org.ale.scan2zotero;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ZoteroAPIRequest implements Runnable {
    private static final String CLASS_TAG = ZoteroAPIRequest.class.getCanonicalName();

    protected static final int GET = 0;
    protected static final int POST = 1;
    protected static final int PUT = 2;
    protected static final int DELETE = 3;

    protected static final int START = 0;
    protected static final int FAILURE = 1;
    protected static final int EXCEPTION = 2;
    protected static final int SUCCESS = 3;
    
    private String mWriteToken;
    
    private HttpClient mHttpsClient;
    
    private Handler mHandler;
    
    private HttpRequestBase mRequest;
    
    private Uri mURI; 
    
    private String mContent;
    
    private String mContentType;
    
    public ZoteroAPIRequest(Handler handler, HttpClient client, int type){
        // Make 16 hex character write token
        Random rng = new Random();
        mWriteToken = Integer.toHexString(rng.nextInt()) + Integer.toHexString(rng.nextInt());
        mHandler = handler;
        mHttpsClient = client;
        setRequestType(type);
    }
    
    private void setRequestType(int type){
        switch(type){
            case GET:
                mRequest = new HttpGet();
                break;
            case POST:
                mRequest = new HttpPost();
                break;
            case PUT:
                //mRequest = new HttpPut();
                break;
            case DELETE:
                //mRequest = new HttpDelete();
                break;
        }
    }
    
    public void setURI(URI uri) {
        Log.d(CLASS_TAG, uri.toString());
        mRequest.setURI(uri);
    }
    
    public void setContent(String content, String contentType){
        mContent = content;
        mContentType = contentType;
    }

    @Override
    public void run() {
        mHandler.sendMessage(
                Message.obtain(mHandler, ZoteroAPIRequest.START, null));
        HttpResponse response = null;
        try {
            response = mHttpsClient.execute(mRequest);
            
            StatusLine status = response.getStatusLine();
            if(status.getStatusCode() != 200){
                mHandler.sendMessage(
                        Message.obtain(mHandler, ZoteroAPIRequest.FAILURE, status));
            }
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
            mHandler.sendMessage(
                    Message.obtain(mHandler, ZoteroAPIRequest.SUCCESS, dataAsString));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            mHandler.sendMessage(
                    Message.obtain(mHandler, ZoteroAPIRequest.EXCEPTION, e));
            return;
        }
    }
}
