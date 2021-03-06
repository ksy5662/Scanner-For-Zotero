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

// XXX: This is temporary, ultimately we should fetch all ItemTypes
// through api.zotero.org/itemTypes?locale=xxx, and store the result in a db

// We also only support "book" right now.

package org.ale.scanner.zotero.data;

public class ItemType {
    public static final String type = "itemType";

    public static final String artwork = "artwork";
    public static final String audioRecording = "audioRecording";
    public static final String bill = "bill";
    public static final String blogPost = "blogPost";
    public static final String book = "book";
    public static final String bookSection = "bookSection";
    public static final String _case = "case";
    public static final String computerProgram = "computerProgram";
    public static final String conferencePaper = "conferencePaper";
    public static final String dictionaryEntry = "dictionaryEntry";
    public static final String document = "document";
    public static final String email = "email";
    public static final String encyclopediaArticle = "encyclopediaArticle";
    public static final String film = "film";
    public static final String forumPost = "forumPost";
    public static final String hearing = "hearing";
    public static final String instantMessage = "instantMessage";
    public static final String interview = "interview";
    public static final String journalArticle = "journalArticle";
    public static final String letter = "letter";
    public static final String magazineArticle = "magazineArticle";
    public static final String manuscript = "manuscript";
    public static final String map = "map";
    public static final String newspaperArticle = "newspaperArticle";
    public static final String note = "note";
    public static final String patent = "patent";
    public static final String podcast = "podcast";
    public static final String presentation = "presentation";
    public static final String radioBroadcast = "radioBroadcast";
    public static final String report = "report";
    public static final String statute = "statute";
    public static final String tvBroadcast = "tvBroadcast";
    public static final String thesis = "thesis";
    public static final String videoRecording = "videoRecording";
    public static final String webpage = "webpage";
}
