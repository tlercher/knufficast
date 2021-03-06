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
package de.knufficast.logic;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import de.knufficast.logic.db.DBFeed;
import de.knufficast.logic.xml.XMLEpisode;
import de.knufficast.logic.xml.XMLFeed;

/**
 * A parser for XML podcast feeds. Produces {@link DBFeed} objects.
 * 
 * @author crazywater
 * 
 */
public class RssParser extends XmlParser {
  private List<XMLFeed> feeds;
  private String feedUrl;
  private long timestamp;
  private String eTag;

  private XMLFeed feed;
  private XMLEpisode episode;

  private static final String FEED_TAG = "channel";
  private static final String ENCLOSURE_TAG = "enclosure";
  private static final String DESCRIPTION_TAG = "description";
  private static final String TITLE_TAG = "title";
  private static final String GUID_TAG = "guid";
  private static final String EPISODE_TAG = "item";
  private static final String LINK_TAG = "link";
  private static final String IMAGE_TAG = "image";
  private static final String CONTENT_TAG = "encoded";
  private static final String LOCATION_ATTRIBUTE = "href";
  private static final String URL_ATTRIBUTE = "url";
  private static final String REL_ATTRIBUTE = "rel";

  public void parse(InputStream xml, String feedUrl, long timestamp, String eTag)
      throws XmlPullParserException, IOException {
    feeds = new ArrayList<XMLFeed>();
    this.feedUrl = feedUrl;
    this.timestamp = timestamp;
    this.eTag = eTag;
    this.parseFrom(xml);
  }

  /**
   * Returns the parsed feeds from a previous call to {@link #parseFrom} or null
   * if no call was made.
   */
  public List<XMLFeed> getFeeds() {
    return feeds;
  }

  /**
   * Called upon parsing an opening tag.
   * 
   * @param tag
   *          the tag name
   * @param attributes
   *          the attributes of the tag (name->value) mapping
   */
  protected void openTag(String tag, Map<String, String> attributes) {
    if (tag.equals(FEED_TAG)) {
      feed = new XMLFeed();
      feed.setDataUrl(feedUrl);
      feed.setLastUpdated(timestamp);
      feed.setETag(eTag);
      feed.setEncoding(getEncoding());
    } else if (tag.equals(EPISODE_TAG)) {
      episode = new XMLEpisode();
    } else if (tag.equals(IMAGE_TAG)) {
      String location = attributes.get(LOCATION_ATTRIBUTE);
      if (location != null) {
        if (FEED_TAG.equals(getParentTag())) {
          feed.setImgUrl(location);
        } else if (EPISODE_TAG.equals(getParentTag())) {
          episode.setImgUrl(location);
        }
      }
    } else if (tag.equals(ENCLOSURE_TAG) && EPISODE_TAG.equals(getParentTag())) {
      episode.setDataUrl(attributes.get(URL_ATTRIBUTE));
    } else if (tag.equals(LINK_TAG) && EPISODE_TAG.equals(getParentTag())) {
      if ("payment".equals(attributes.get(REL_ATTRIBUTE))) {
        String paymentLocation = attributes.get(LOCATION_ATTRIBUTE);
        if (paymentLocation != null && paymentLocation.contains("flattr")) {
          episode.setFlattrUrl(paymentLocation);
        }
      }
    }
  }

  /**
   * Called upon parsing a closing tag
   * 
   * @param tag
   *          the tag name
   */
  protected void closeTag(String tag) {
    if (tag.equals(FEED_TAG)) {
      feeds.add(feed);
      feed = null;
    }
    if (tag.equals(EPISODE_TAG)) {
      feed.addEpisode(episode);
      episode = null;
    }
  }

  /**
   * Called upon parsing text between an opening and a closing tag. Parent tags
   * can be found in {@link #currentTags}, but include the current tag at the
   * top.
   * 
   * @param text
   *          the text that is encountered
   */
  protected void tagText(String text) {
    String tag = getCurrentTag();
    if (tag.equals(TITLE_TAG) && EPISODE_TAG.equals(getParentTag())) {
      episode.setTitle(text);
    } else if (tag.equals(GUID_TAG) && EPISODE_TAG.equals(getParentTag())) {
      episode.setGuid(text);
    } else if (tag.equals(CONTENT_TAG) && EPISODE_TAG.equals(getParentTag())) {
      episode.setContent(text);
    } else if (tag.equals(TITLE_TAG) && FEED_TAG.equals(getParentTag())) {
      feed.setTitle(text);
    } else if (tag.equals(DESCRIPTION_TAG)
        && EPISODE_TAG.equals(getParentTag())) {
      episode.setDescription(text);
    } else if (tag.equals(DESCRIPTION_TAG) && FEED_TAG.equals(getParentTag())) {
      feed.setDescription(text);
    }
  }
}
