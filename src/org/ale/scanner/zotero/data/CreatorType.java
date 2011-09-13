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

package org.ale.scanner.zotero.data;

import java.util.ArrayList;

public final class CreatorType {

    public static final String type = "creatorType";
    
    public static final ArrayList<String> Book;
    static{
        Book = new ArrayList<String>();
        Book.add("author");
        Book.add("contributor");
        Book.add("editor");
        Book.add("seriesEditor");
        Book.add("translator");
    }

    public static final ArrayList<String> LocalizedBook;
    static{
        LocalizedBook = new ArrayList<String>();
        LocalizedBook.add("Author");
        LocalizedBook.add("Contributor");
        LocalizedBook.add("Editor");
        LocalizedBook.add("Series Editor");
        LocalizedBook.add("Translator");
    }
}
