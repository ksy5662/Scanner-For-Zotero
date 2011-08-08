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

    protected ArrayList<Integer> mResponseTypes; // Make static in implementing classes
    protected ArrayList<APIResponse> mResponses; // Make static in implementing classes

    protected static S2ZMainActivity mActivity = null;

    protected abstract void dequeueMessages();

    protected abstract void onStart(String id);
    protected abstract void onProgress(String id, int percent);
    protected abstract void onStatusLine(String id, StatusLine reason);
    protected abstract void onException(String id, Exception exc);
    protected abstract void onSuccess(String id, String res);

    public void registerActivity(S2ZMainActivity activity){
        if(APIHandler.mActivity != null || activity == null)
            return;
        APIHandler.mActivity = activity;
        dequeueMessages();
    }

    public void unregisterActivity(){
        APIHandler.mActivity = null;
    }

    public void handleMessage(Message msg){
        APIResponse resp = (APIResponse)msg.obj;

        if(APIHandler.mActivity == null){
            mResponseTypes.add(new Integer(msg.what));
            mResponses.add(resp);
        }else{
            handleMessage(msg.what, resp);
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
}
