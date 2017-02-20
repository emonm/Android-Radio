/**
 *  Radio for android, internet radio.
 *
 * Copyright (C) 2016 Old Geek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.oucho.radio.domain.async_tasks;

import android.net.Uri;
import android.os.AsyncTask;
import android.webkit.URLUtil;

import org.oucho.radio.domain.helper.Counter;
import org.oucho.radio.domain.network.HttpGetter;
import org.oucho.radio.domain.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Playlist extends AsyncTask<Void, Void, String>
{
   private static final int max_ttl = 10;

   private static final int NONE    = 0;
   private static final int M3U     = 1;
   private static final int PLS     = 2;

   private Player player = null;
   private String start_url = null;
   private int then = 0;

   private static Random random = null;

    private static final String url_regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
    private static Pattern url_pattern = null;

   public Playlist(Player a_player, String a_url)
   {
      super();
      player = a_player;
      start_url = a_url;
      then = Counter.now();
   }

   public Playlist start()
   {
      return (Playlist) executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
   }

   protected String doInBackground(Void... args)
   {
      String url = start_url;
      int ttl = max_ttl;
      int type = NONE;

      if ( url != null && url.length() != 0 && URLUtil.isValidUrl(url) )
         type = playlistType(url);
      else
         url = null;

       //noinspection ConstantConditions
       if ( 0 < ttl && url != null && type != NONE )
      {
         ttl -= 1;

         url = selectUrlFromPlaylist(url,type);
         if ( url != null && url.length() != 0 && URLUtil.isValidUrl(url) )
             //noinspection UnusedAssignment
             type = playlistType(url);
         else
            url = null;
      }

      if (ttl  == 0)
           url = null;

      return url;
   }

   protected void onPostExecute(String url) {
      if ( url != null && player != null && ! isCancelled() && Counter.still(then) )
         player.playLaunch(url);
     }

   private static String filter(String line, int type)
   {
      switch (type)
      {
         //
         case M3U:
            return line.indexOf('#') == 0 ? "" : line;
         //
         case PLS:
            if ( line.startsWith("File") && 0 < line.indexOf('=') )
               return line;
            return "";
         //
         default:
            return line;
      }
   }


   private String selectUrlFromPlaylist(String url, int type)
   {
      List<String> lines = HttpGetter.httpGet(url);

      for (int i=0; i<lines.size(); i+= 1)
      {
         String line = lines.get(i);
         line = filter(line.trim(),type);
         lines.set(i, line);
      }

      @SuppressWarnings("unchecked") List<String> links = selectUrlsFromList(lines);
      if ( links.size() == 0 )
         return null;

      for (int i=0; i<links.size(); i+= 1)

      if ( random == null )
         random = new Random();

      return links.get(random.nextInt(links.size()));
   }



   private static ArrayList selectUrlsFromList(List<String> lines)
   {
      ArrayList links = new ArrayList<>();

      if ( url_pattern == null )
         url_pattern = Pattern.compile(url_regex);

      for (int i=0; i<lines.size(); i+=1)
      {
         String line = lines.get(i);
         if ( 0 < line.length() )
         {
            Matcher matcher = url_pattern.matcher(line);
            if ( matcher.find() )
            {
               String link = matcher.group();
               if (link.startsWith("(") && link.endsWith(")"))
                  link = link.substring(1, link.length() - 1);

                //noinspection unchecked
                links.add(link);
            }
         }
      }

      return links;
   }

   private static Uri parseUri(String url)
      { return Uri.parse(url); }

   private static boolean isSuffix(String text, String suffix)
      { return text != null && text.endsWith(suffix) ; }

   private static boolean isSomeSuffix(String url, String suffix) {
       return isSuffix(url, suffix) || isSuffix(parseUri(url).getPath(), suffix);

   }

   private static int playlistType(String url) {

       //String URL;

       String URL = url.toLowerCase();
      if ( isSomeSuffix(URL,".m3u"  ) ) return M3U;
      if ( isSomeSuffix(URL,".m3u8" ) ) return M3U;
      if ( isSomeSuffix(URL,".pls"  ) ) return PLS;
      return NONE;
   }

   public static boolean isPlaylistMimeType(String mime) {

       return mime != null && ("audio/x-scpls".equals(mime)
               || "audio/scpls".equals(mime)
               || "audio/x-mpegurl".equals(mime)
               || "audio/mpegurl".equals(mime)
               || "audio/mpeg-url".equals(mime)
               || "application/vnd.apple.mpegurl".equals(mime)
               || "application/x-winamp-playlist".equals(mime)
               || mime.contains("mpegurl")
               || mime.contains("mpeg-url")
               || mime.contains("scpls")
               || mime.indexOf("text/") == 0);

   }

}
