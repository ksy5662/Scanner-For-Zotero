package org.ale.scan2zotero.web;

import java.util.ArrayList;

import org.ale.scan2zotero.S2ZMainActivity;
import org.ale.scan2zotero.web.APIRequest.APIResponse;
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

    protected static S2ZMainActivity mActivity = null;

    protected abstract void dequeueMessages();

    protected abstract void onStart(String id);
    protected abstract void onProgress(String id, int percent);
    protected abstract void onStatusLine(String id, StatusLine reason);
    protected abstract void onException(String id, Exception exc);
    protected abstract void onSuccess(String id, String res);

    public synchronized void registerActivity(S2ZMainActivity activity){
        if(APIHandler.mActivity != null || activity == null)
            return;
        APIHandler.mActivity = activity;
        dequeueMessages();
        for(Runnable r : mUIThreadEvents){
            mActivity.post(r);
        }
        mUIThreadEvents.clear();
    }

    public synchronized void unregisterActivity(){
        APIHandler.mActivity = null;
    }

    public static synchronized boolean hasActivity() {
        return APIHandler.mActivity != null;
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
        String id = resp.getId();

        switch(type) {
        case APIHandler.START:
            onStart(id);
            break;
        case APIHandler.PROGRESS:
            onProgress(id, ((Integer)resp.getData()).intValue());
            break;
        case APIHandler.EXCEPTION:
            onException(id, (Exception)resp.getData());
            break;
        case APIHandler.STATUSLINE:
            onStatusLine(id, (StatusLine)resp.getData());
            break;
        case APIHandler.SUCCESS:
            onSuccess(id, (String) resp.getData());
            break;
        case APIHandler.FINISH:
            RequestQueue.getInstance().taskComplete((APIRequest)resp.getData());
            break;
        }
    }

    public void checkActivityAndRun(Runnable r){
        if(APIHandler.hasActivity()){
            mActivity.post(r);
        }else{
            mUIThreadEvents.add(r);
        }
    }
}
