/** 
 * Copyright 2011 John M. Schanck
 * 
 * ScannerForZotero is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ScannerForZotero is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ScannerForZotero.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ale.scanner.zotero.web;

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

import android.os.Bundle;
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

    private Bundle mExtra = null;

    public APIRequest(APIHandler handler, HttpClient client){
        mHandler = handler;
        mHttpsClient = client;
    }

    // The Bundle extra is used to identify this request and provide 
    // its handler w/ the information needed to process the result.
    public void setExtra(Bundle extra){
        mExtra = extra;
    }

    public Bundle getExtra(){
        return mExtra;
    }

    public void setHttpMethod(int type){
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
            ((HttpPost)mRequest).setEntity(new StringEntity(content, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(CLASS_TAG, "Problem encoding: "+content);
        }
    }

    public void run() {
        mHandler.sendMessage(
                Message.obtain(mHandler, APIHandler.START, new APIResponse(this, null)));
        HttpResponse response = null;
        try {
            response = mHttpsClient.execute(mRequest);

            StatusLine status = response.getStatusLine();
            mHandler.sendMessage(Message.obtain(mHandler, 
                        APIHandler.STATUSLINE, new APIResponse(this, status)));

            // Check the status code if it's 400 or higher then we don't need to
            // finish reading the response, the APIHandler should know what to do.
            if(status.getStatusCode() <= 399){
                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();

                ByteArrayOutputStream content = new ByteArrayOutputStream();

                // Read response into a buffered stream
                int readBytes = 0;
                byte[] sBuffer = new byte[BUFFER_SIZE];
                while ((readBytes = inputStream.read(sBuffer)) != -1) {
                   content.write(sBuffer, 0, readBytes);
                }

                // Return result from buffered stream
                String dataAsString = new String(content.toByteArray());
                mHandler.sendMessage(Message.obtain(mHandler, 
                        APIHandler.SUCCESS, new APIResponse(this, dataAsString)));
            }
        } catch (Exception e) {
            mHandler.sendMessage(Message.obtain(mHandler,
                        APIHandler.EXCEPTION, new APIResponse(this, e)));
        } finally {
            mHandler.sendMessage(Message.obtain(mHandler, 
                        APIHandler.FINISH, new APIResponse(this, null)));
        }
    }

    public class APIResponse {
        private APIRequest mRequest;
        private Object mData;
        public APIResponse(APIRequest req, Object data){
            mRequest = req;
            mData = data;
        }
        public APIRequest getRequest(){ return mRequest; }
        public Object getData(){ return mData; }
    }
}
