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
package de.knufficast.search;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.knufficast.util.BooleanCallback;
import de.knufficast.util.HttpUtil;

/**
 * Searches GPodder for podcasts.
 * 
 * @author crazywater
 * 
 */
public class GPodderSearch implements PodcastSearch {
  private static final String SEARCH_URL = "http://gpodder.net/search.json?q=%s";
  private static final String ERROR_CONNECTION = "No connection";
  private static final String ERROR_JSON = "JSON Error";
  private final HttpUtil httpUtil = new HttpUtil();

  public class GPodderResult implements PodcastSearch.Result {
    private String title = "";
    private String website = "";
    private String description = "";
    private String feedUrl = "";
    private String imgUrl = "";

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public String getFeedUrl() {
      return feedUrl;
    }

    public String getImgUrl() {
      return imgUrl;
    }

    public String getWebsite() {
      return website;
    }
  };

  public void search(final String query,
      final BooleanCallback<List<Result>, String> callback) {
    runOnThread(new Runnable() {
      @Override
      public void run() {
        String queryUrl = String.format(SEARCH_URL, URLEncoder.encode(query));
        HttpGet request = new HttpGet(queryUrl);
        try {
          List<Result> results = new ArrayList<Result>();
          JSONArray response = httpUtil.getJsonArray(request);
          int max = response.length();
          for (int i = 0; i < max; i++) {
            JSONObject jsonResult = response.getJSONObject(i);
            GPodderResult result = new GPodderResult();
            result.website = jsonResult.optString("website");
            result.description = jsonResult.optString("description");
            result.title = jsonResult.optString("title");
            result.feedUrl = jsonResult.optString("url");
            result.imgUrl = jsonResult.optString("scaled_logo_url");
            results.add(result);
          }
          callback.success(results);
        } catch (IOException e) {
          callback.fail(ERROR_CONNECTION);
        } catch (JSONException e) {
          callback.fail(ERROR_JSON);
        }
      }
    });
  }
  
  private void runOnThread(Runnable runnable) {
    Thread thread = new Thread(runnable);
    thread.start();
  }
}
