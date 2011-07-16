package org.ale.scan2zotero;

import java.util.ArrayList;
import java.util.LinkedList;

public class RequestQueue {
    private static final int CAPACITY = 5;
    
    private static RequestQueue instance;
    
    private ArrayList<ZoteroAPIRequest> active =  new ArrayList<ZoteroAPIRequest>();
    private LinkedList<ZoteroAPIRequest> queue = new LinkedList<ZoteroAPIRequest>();

    public static RequestQueue getInstance(){
        if(instance == null)
            instance = new RequestQueue();
        return instance;
    }

    protected void enqueue(ZoteroAPIRequest task){
        queue.add(task);
        if(active.size() < CAPACITY){
            startNext();
        }
    }

    private void startNext() {
        if(!queue.isEmpty()){
            ZoteroAPIRequest next = queue.poll();
            active.add(next);
            Thread task = new Thread(next);
            task.start();
        }
    }

    private void taskComplete(ZoteroAPIRequest task){
        active.remove(task);
        startNext();
    }
}
