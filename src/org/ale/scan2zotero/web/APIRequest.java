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

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class APIRequest implements Runnable {

    private static final String CLASS_TAG = APIRequest.class.getCanonicalName();
    public static final int GET = 0;
    public static final int POST = 1;
    public static final int PUT = 2;
    public static final int DELETE = 3;

    public static final int START = 0;
    public static final int PROGRESS = 1;
    public static final int FAILURE = 2;
    public static final int EXCEPTION = 3;
    public static final int SUCCESS = 4;
    public static final int FINISH = 5;

    private static final int BUFFER_SIZE = 512;

    private HttpClient mHttpsClient;

    private Handler mHandler;

    private HttpRequestBase mRequest;

    private String mId;

    public APIRequest(Handler handler, HttpClient client){
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
                Message.obtain(mHandler, APIRequest.START, new APIResponse(mId, null)));
        HttpResponse response = null;
        try {
            response = mHttpsClient.execute(mRequest);

            StatusLine status = response.getStatusLine();
            if(status.getStatusCode() != 200){
                mHandler.sendMessage(
                        Message.obtain(mHandler, APIRequest.FAILURE, new APIResponse(mId, status)));
            }
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            int percentPerSeg = Math.max(1, 
                    Math.round(((float)BUFFER_SIZE)/entity.getContentLength()));

            ByteArrayOutputStream content = new ByteArrayOutputStream();

            APIResponse prog = new APIResponse(mId, new Integer(percentPerSeg));
            // Read response into a buffered stream
            int readBytes = 0;
            byte[] sBuffer = new byte[BUFFER_SIZE];
            while ((readBytes = inputStream.read(sBuffer)) != -1) {
               content.write(sBuffer, 0, readBytes);
               mHandler.sendMessage(
                       Message.obtain(mHandler, APIRequest.PROGRESS, prog));
            }

            // Return result from buffered stream
            String dataAsString = new String(content.toByteArray());
            mHandler.sendMessage(
                    Message.obtain(mHandler, 
                                   APIRequest.SUCCESS,
                                   new APIResponse(mId, dataAsString)));
        } catch (Exception e) {
            mHandler.sendMessage(
                    Message.obtain(mHandler,
                                   APIRequest.EXCEPTION,
                                   new APIResponse(mId, e)));
        } finally {
            mHandler.sendMessage(
                    Message.obtain(mHandler, APIRequest.FINISH, new APIResponse(mId, this)));
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
