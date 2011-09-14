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

import java.util.ArrayList;

import org.ale.scanner.zotero.MainActivity;
import org.ale.scanner.zotero.web.APIRequest.APIResponse;
import org.apache.http.StatusLine;

import android.os.Handler;
import android.os.Message;

public abstract class APIHandler extends Handler {

    public static final String CLASS_TAG = APIHandler.class.getCanonicalName();

    public static final int START = 0;
    public static final int STATUSLINE = 1;
    public static final int PROGRESS = 2;
    public static final int EXCEPTION = 3;
    public static final int SUCCESS = 4;
    public static final int FINISH = 5;

    protected ArrayList<Integer> mResponseTypes = new ArrayList<Integer>();
    protected ArrayList<APIResponse> mResponses = new ArrayList<APIResponse>();
    protected ArrayList<Runnable> mUIThreadEvents = new ArrayList<Runnable>();

    protected static MainActivity MAIN = null;
    protected static ArrayList<APIHandler> HANDLERS = new ArrayList<APIHandler>();

    protected abstract void onStart(APIRequest req);
    protected abstract void onProgress(APIRequest req, int percent);
    protected abstract void onStatusLine(APIRequest req, StatusLine reason);
    protected abstract void onException(APIRequest req, Exception exc);
    protected abstract void onSuccess(APIRequest req, String res);

    public static void globalBindActivity(MainActivity activity) {
        for(APIHandler handler : HANDLERS){
            handler.bindActivity(activity);
        }
    }

    public static void globalUnbindActivity() {
        for(APIHandler handler : HANDLERS){
            handler.unbindActivity();
        }
    }

    public synchronized void bindActivity(MainActivity activity){
        if(APIHandler.MAIN != null || activity == null)
            return;
        APIHandler.MAIN = activity;
        dequeueMessages();
        for(Runnable r : mUIThreadEvents){
            APIHandler.MAIN.postToUIThread(r);
        }
        mUIThreadEvents.clear();
    }

    public synchronized void unbindActivity(){
        APIHandler.MAIN = null;
    }

    public static synchronized boolean hasActivity() {
        return APIHandler.MAIN != null;
    }

    public void handleMessage(Message msg){
        APIResponse resp = (APIResponse)msg.obj;

        if(APIHandler.hasActivity()){ // Process
            handleMessage(msg.what, resp);
        }else{ // Or queue
            mResponseTypes.add(new Integer(msg.what));
            mResponses.add(resp);
        }
    }

    public void handleMessage(int type, APIResponse resp){
        APIRequest req = resp.getRequest();

        switch(type) {
        case APIHandler.START:
            onStart(req);
            break;
        case APIHandler.STATUSLINE:
            onStatusLine(req, (StatusLine)resp.getData());
            break;
        case APIHandler.PROGRESS:
            onProgress(req, ((Integer)resp.getData()).intValue());
            break;
        case APIHandler.EXCEPTION:
            onException(req, (Exception)resp.getData());
            break;
        case APIHandler.SUCCESS:
            onSuccess(req, (String) resp.getData());
            break;
        case APIHandler.FINISH:
            RequestQueue.getInstance().taskComplete(req);
            break;
        }
    }

    public void checkActivityAndRun(Runnable r) {
        // Post a runnable to the activity UI thread if our activity reference
        // is non-null. Otherwise queue the runnable until activity registration.
        if(APIHandler.hasActivity()){
            APIHandler.MAIN.postToUIThread(r);
        }else{
            mUIThreadEvents.add(r);
        }
    }

    private void dequeueMessages(){
        for(int i=0; i<mResponses.size(); i++){
            handleMessage(mResponseTypes.get(i).intValue(), mResponses.get(i));
        }
        mResponseTypes.clear();
        mResponses.clear();
    }
}
