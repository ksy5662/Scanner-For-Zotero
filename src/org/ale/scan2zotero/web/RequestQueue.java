/** 
 * Copyright 2011 John M. Schanck
 * 
 * Scan2Zotero is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Scan2Zotero is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Scan2Zotero.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ale.scan2zotero.web;

import java.util.ArrayList;
import java.util.LinkedList;

public class RequestQueue {

    private static final int CAPACITY = 5;
    
    private static RequestQueue instance;
    
    private static ArrayList<APIRequest> active =  new ArrayList<APIRequest>();

    private static LinkedList<APIRequest> queue = new LinkedList<APIRequest>();

    public static RequestQueue getInstance(){
        if(instance == null)
            instance = new RequestQueue();
        return instance;
    }

    public synchronized void enqueue(APIRequest task){
        queue.add(task);
        if(active.size() < CAPACITY){
            startNext();
        }
    }

    private synchronized void startNext() {
        if(!queue.isEmpty()){
            APIRequest next = queue.poll();
            active.add(next);
            Thread task = new Thread(next);
            task.start();
        }
    }

    public synchronized void taskComplete(APIRequest task){
        active.remove(task);
        startNext();
    }
}
