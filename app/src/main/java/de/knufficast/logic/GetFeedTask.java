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

import android.os.AsyncTask;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import de.knufficast.App;
import de.knufficast.logic.db.XMLToDBWriter;
import de.knufficast.logic.xml.XMLFeed;

/**
 * A 'background' task that download a RSS feed, parses them present the result to the Presenter
 *
 * @author TAL
 */
public class GetFeedTask extends AsyncTask<String, Void, Void> {
  private ResultCallback callback;

  public GetFeedTask(ResultCallback callback) {
    this.callback = callback;
  }

  @Override
  protected Void doInBackground(String... urls) {
    int count = urls.length;
    for (int i = 0; i < count; i++) {
      Log.d("GetFeedTask", "Fetching Feed url " + urls[i]);
      try {
        String url = urls[i];
        // add http in the front - otherwise we get invalid protocol
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
          url = "http://" + url;
        }
        if (isCancelled()) {
          return null;
        }
        HttpURLConnection con = (HttpURLConnection) new URL(url)
            .openConnection();
        if (isCancelled()) {
          return null;
        }
        List<XMLFeed> feeds = new FeedDownloader().getFeeds(con);
        if (isCancelled()) {
          return null;
        }

        for(XMLFeed feed : feeds) {
          App.get().getImageCache().getResource(feed.getImgUrl());
          callback.onFeedFound(feed);
        }
      } catch (IOException e) {
        callback.onError(e.getMessage());
      } catch (XmlPullParserException e) {
        callback.onError(e.getMessage());
      }
    }
    return null;
  }

  public interface ResultCallback {
    void onFeedFound(XMLFeed feed);
    void onError(String message);
  }
}
