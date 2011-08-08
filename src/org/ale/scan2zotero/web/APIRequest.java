package org.ale.scan2zotero.web;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import android.os.Message;
import android.util.Log;

public class APIRequest implements Runnable {

    private static final String CLASS_TAG = APIRequest.class.getCanonicalName();
    public static final int GET = 0;
    public static final int POST = 1;
    public static final int PUT = 2;
    public static final int DELETE = 3;

    private static final int BUFFER_SIZE = 512;

    private HttpClient mHttpsClient;

    private APIHandler mHandler;

    private HttpRequestBase mRequest;

    private String mId;

    public APIRequest(APIHandler handler, HttpClient client){
        mHandler = handler;
        mHttpsClient = client;
    }

    /* Returned with API response for handler to identify request */
    public void setReturnIdentifier(String id){
        mId = id;
    }
    
    public void setRequestType(int type){
        switch(type){
            case GET:
                mRequest = new HttpGet();
                break;
            case POST:
                mRequest = new HttpPost();
                break;
        }
    }

    public void setURI(URI uri) {
        mRequest.setURI(uri);
        Log.d("APIREQUEST", uri.toString());
    }

    public void addHeader(String name, String value){
        mRequest.addHeader(name, value);
    }

    public void setContent(String content, String contentType) {
        mRequest.setHeader("Content-Type", contentType);
        //mRequest.setHeader("Content-Length", String.valueOf(content.length()));
        try {
            ((HttpPost)mRequest).setEntity(new StringEntity(content));
        } catch (UnsupportedEncodingException e) {
            Log.e(CLASS_TAG, "Problem encoding: "+content);
        }
    }

    @Override
    public void run() {
        mHandler.sendMessage(
                Message.obtain(mHandler, APIHandler.START, new APIResponse(mId, null)));
        HttpResponse response = null;
        try {
            response = mHttpsClient.execute(mRequest);

            StatusLine status = response.getStatusLine();
            mHandler.sendMessage(Message.obtain(mHandler, 
                        APIHandler.STATUSLINE, new APIResponse(mId, status)));

            // Check the status code if it's 400 or higher then we don't need to
            // finish reading the response, the APIHandler should know what to do.
            if(status.getStatusCode() <= 399){
                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();

                ByteArrayOutputStream content = new ByteArrayOutputStream();

                // This progress stuff isn't really worth it
                //int percentPerSeg = Math.max(1, 
                //        Math.round(((float)BUFFER_SIZE)/entity.getContentLength()));
                // APIResponse prog = new APIResponse(mId, new Integer(percentPerSeg));

                // Read response into a buffered stream
                int readBytes = 0;
                byte[] sBuffer = new byte[BUFFER_SIZE];
                while ((readBytes = inputStream.read(sBuffer)) != -1) {
                   content.write(sBuffer, 0, readBytes);
                   //mHandler.sendMessage(Message.obtain(mHandler,
                   //      APIHandler.PROGRESS, prog));
                }

                // Return result from buffered stream
                String dataAsString = new String(content.toByteArray());
                mHandler.sendMessage(Message.obtain(mHandler, 
                        APIHandler.SUCCESS, new APIResponse(mId, dataAsString)));
            }
        } catch (Exception e) {
            mHandler.sendMessage(Message.obtain(mHandler,
                        APIHandler.EXCEPTION, new APIResponse(mId, e)));
        } finally {
            mHandler.sendMessage(Message.obtain(mHandler, 
                        APIHandler.FINISH, new APIResponse(mId, this)));
        }
    }

    public class APIResponse {
        private String mId;
        private Object mData;
        public APIResponse(String id, Object data){
            mId = id;
            mData = data;
        }
        public String getId(){ return mId; }
        public Object getData(){ return mData; }
    }
}
