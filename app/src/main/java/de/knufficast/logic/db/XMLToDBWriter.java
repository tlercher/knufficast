/*******************************************************************************
 * Copyright 2012 Crazywater
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.knufficast.logic.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.knufficast.App;
import de.knufficast.logic.xml.FeedPostProcessor;
import de.knufficast.logic.xml.XMLEpisode;
import de.knufficast.logic.xml.XMLFeed;

/**
 * A writer that gets feeds and adds them to the database or merges them with
 * the existing feeds in the database. (i.e. here happens the conversion from
 * XMLFeed/XMLEpisode to DBFeed/DBEpisode)
 * 
 * @author crazywater
 * 
 */
public class XMLToDBWriter {
  private final Database db = App.get().getDB();
  private final FeedPostProcessor postProcessor = new FeedPostProcessor();

  /**
   * Newly adds feeds to the database.
   */
  public void addFeeds(List<XMLFeed> xmlFeeds) {
    postProcessor.process(xmlFeeds);
    for (XMLFeed xmlFeed : xmlFeeds) {
      // check if this feed already exists
      List<Long> ids = db.query(SQLiteHelper.TABLE_FEEDS,
          SQLiteHelper.C_FD_FEED_URL, xmlFeed.getDataUrl());
      if (ids.isEmpty()) {
        DBFeed feed = createFeed(xmlFeed);
        // reverse the episodes, so we insert the oldest first
        List<XMLEpisode> episodes = xmlFeed.getEpisodes();
        Collections.reverse(episodes);
        for (XMLEpisode tempEpisode : episodes) {
          DBEpisode ep = createEpisode(feed, tempEpisode);
          ep.setNew(false);
        }
      }
    }
  }

  /**
   * Merges feeds with existing feeds in the database.
   */
  public void mergeFeeds(List<XMLFeed> tempFeeds) {
    postProcessor.process(tempFeeds);
    for (XMLFeed tempFeed : tempFeeds) {
      List<Long> feedIds = db.query(SQLiteHelper.TABLE_FEEDS, SQLiteHelper.C_FD_FEED_URL, tempFeed.getDataUrl());
      DBFeed feed;
      if (feedIds.isEmpty()) {
        feed = createFeed(tempFeed);
      } else {
        feed = new DBFeed(feedIds.get(0));
        // update the etag
        String eTag = tempFeed.getETag();
        if (eTag != null) {
          feed.setETag(eTag);
        }
      }
      List<XMLEpisode> newEpisodes = new ArrayList<XMLEpisode>();
      for (XMLEpisode tempEpisode : tempFeed.getEpisodes()) {
        // get all episode ids with this guid
        List<Long> ids = db.query(SQLiteHelper.TABLE_EPISODES,
            SQLiteHelper.C_EP_GUID, tempEpisode.getGuid());
        boolean found = false;
        for (long id : ids) {
          DBEpisode ep = new DBEpisode(id);
          DBFeed dbFeed = ep.getFeed();
          // check if this episode's feed url matches ours
          if (dbFeed.getFeedUrl().equals(tempFeed.getDataUrl())) {
            found = true;
            break;
          }
        }
        if (!found) {
          newEpisodes.add(tempEpisode);
        }
      }
      // reverse so we have the newest episodes first
      Collections.reverse(newEpisodes);
      for (XMLEpisode tempEpisode : newEpisodes) {
        DBEpisode ep = createEpisode(feed, tempEpisode);
        ep.setNew(true);
      }
    }
  }

  private DBEpisode createEpisode(DBFeed feed, XMLEpisode tempEpisode) {
    String[] values = { tempEpisode.getDataUrl(), tempEpisode.getDescription(),
        tempEpisode.getFlattrUrl(), tempEpisode.getGuid(),
        tempEpisode.getImgUrl(), tempEpisode.getTitle(),
        String.valueOf(feed.getId()), tempEpisode.getContent() };
    String[] columns = { SQLiteHelper.C_EP_DATA_URL,
        SQLiteHelper.C_EP_DESCRIPTION, SQLiteHelper.C_EP_FLATTR_URL,
        SQLiteHelper.C_EP_GUID, SQLiteHelper.C_EP_IMG_URL,
        SQLiteHelper.C_EP_TITLE, SQLiteHelper.C_EP_FEED_ID,
        SQLiteHelper.C_EP_CONTENT };
    long episodeId = db.create(SQLiteHelper.TABLE_EPISODES,
        Arrays.asList(columns), Arrays.asList(values));
    return new DBEpisode(episodeId);
  }

  private DBFeed createFeed(XMLFeed tempFeed) {
    String[] columns = { SQLiteHelper.C_FD_FEED_URL,
        SQLiteHelper.C_FD_DESCRIPTION, SQLiteHelper.C_FD_ENCODING,
        SQLiteHelper.C_FD_ETAG, SQLiteHelper.C_FD_IMG_URL,
        SQLiteHelper.C_FD_TITLE, SQLiteHelper.C_FD_LAST_UPDATED };
    String[] values = { tempFeed.getDataUrl(), tempFeed.getDescription(),
        tempFeed.getEncoding(), tempFeed.getETag(), tempFeed.getImgUrl(),
        tempFeed.getTitle(), String.valueOf(tempFeed.getLastUpdated()) };
    long feedId = db.create(SQLiteHelper.TABLE_FEEDS, Arrays.asList(columns),
        Arrays.asList(values));
    return new DBFeed(feedId);
  }
}
