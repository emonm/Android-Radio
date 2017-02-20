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

package org.oucho.radio.domain.player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.webkit.URLUtil;

import org.oucho.radio.domain.helper.Counter;
import org.oucho.radio.domain.async_tasks.Later;
import org.oucho.radio.R;
import org.oucho.radio.domain.helper.State;
import org.oucho.radio.domain.async_tasks.Playlist;
import org.oucho.radio.domain.network.Connectivity;
import org.oucho.radio.domain.network.WifiLocker;

public class Player extends Service
   implements
      OnBufferingUpdateListener,
      OnInfoListener,
      OnErrorListener,
      OnPreparedListener,
      OnAudioFocusChangeListener,
      OnCompletionListener
{

   private static final String fichier_préférence = "org.oucho.radio_preferences";
   private static SharedPreferences préférences = null;

   private static Context context = null;

   private static String app_name = null;
   private static String intent_play = null;
   private static String intent_stop = null;
   private static String intent_pause = null;
   private static String intent_restart = null;

   private static final String default_url = null;
   private static final String default_name = null;
   public  static String name = null;
   public  static String url = null;

   private static MediaPlayer player = null;

   private static AudioManager audio_manager = null;

   private static Playlist playlist_task = null;
   private static AsyncTask<Integer,Void,Void> pause_task = null;

   private static Connectivity connectivity = null;
   private static final int initial_failure_ttl = 5;
   private static int failure_ttl = 0;

   private static String launch_url = null;

   private Later start_buffering_task = null;

   private Later stopSoonTask = null;

    @Override
   public void onCreate() {
      context = getApplicationContext();

      app_name = getString(R.string.app_name);
      intent_play = "play";
      intent_stop = "stop";
      intent_pause = "pause";
      intent_restart = "restart";

      préférences = getSharedPreferences(fichier_préférence, MODE_PRIVATE);
      url = préférences.getString("url", default_url);
      name = préférences.getString("name", default_name);

      audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
      connectivity = new Connectivity(context,this);
   }

   public void onDestroy()
   {
      stop();

      if ( player != null )
      {
         player.release();
         player = null;
      }

      if ( connectivity != null )
      {
         connectivity.destroy();
         connectivity = null;
      }

      super.onDestroy();
   }


   @Override
   public int onStartCommand(Intent intent, int flags, int startId)
   {
      if ( intent == null || ! intent.hasExtra("action") )
         return done();


      if ( ! Counter.still(intent.getIntExtra("counter", Counter.now())) )
         return done();

      String action = intent.getStringExtra("action");

      if (action.equals(intent_stop))
         return stop();
      
      if (action.equals(intent_pause)) 
         return pause();
      
      if (action.equals(intent_restart))
         return restart();

      if (action.equals(intent_play))
          intentPlay(intent);

      return done();
   }

    public int intentPlay(Intent intent) {

        if ( intent.hasExtra("url") )
            url = intent.getStringExtra("url");

        if ( intent.hasExtra("name") )
            name = intent.getStringExtra("name");

        Editor editor = préférences.edit();
        editor.putString("url", url);
        editor.putString("name", name);
        editor.apply();

        failure_ttl = initial_failure_ttl;
        return play(url);

    }

   public int play()
      { return play(url); }

   private int play(String url)
   {
      stop(false);


      if ( ! URLUtil.isValidUrl(url) )
      {
         return stop();
      }

      if ( isNetworkUrl(url) && ! Connectivity.isConnected(context) )
      {
         connectivity.dropped_connection();
         return done();
      }

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
      {
         return stop();
      }


      if ( player == null )
      {
         player = new MediaPlayer();
         player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
         player.setAudioStreamType(AudioManager.STREAM_MUSIC);
         player.setOnPreparedListener(this);
         player.setOnBufferingUpdateListener(this);
         player.setOnInfoListener(this);
         player.setOnErrorListener(this);
         player.setOnCompletionListener(this);
      }

      if ( isNetworkUrl(url) )
         WifiLocker.lock(context, app_name);

      playlist_task = new Playlist(this,url).start();

      start_buffering();
      return done(State.STATE_BUFFER);
   }


   public int playLaunch(String url)
   {

      launch_url = null;
      if ( ! URLUtil.isValidUrl(url) )
      {
         return stop();
      }

      launch_url = url;

      WifiLocker.unlock();
      if ( isNetworkUrl(url) )
         WifiLocker.lock(context, app_name);

      try
      {
         player.setVolume(1.0f, 1.0f);
         player.setDataSource(context, Uri.parse(url));
         player.prepareAsync();
      }
      catch (Exception e)
         { return stop(); }

      start_buffering();
      return done(State.STATE_BUFFER);
   }

   @Override
   public void onPrepared(MediaPlayer mp)
   {
      if ( mp.equals(player) )
      {
         player.start();
         Counter.timePasses();
         failure_ttl = initial_failure_ttl;
         State.setState(context, State.STATE_PLAY);
      }
   }

   public boolean isNetworkUrl()
      { return isNetworkUrl(launch_url); }

   public boolean isNetworkUrl(String check_url)
      { return ( check_url != null && URLUtil.isNetworkUrl(check_url) ); }

   public int stop()
      { return stop(true); }

   private int stop(boolean update_state)
   {

      Counter.timePasses();
      launch_url = null;
      audio_manager.abandonAudioFocus(this);
      WifiLocker.unlock();

      if ( player != null )
      {
         if ( player.isPlaying() )
            player.stop();
         player.reset();
         player.release();
         player = null;
      }

      if ( playlist_task != null )
      {
         playlist_task.cancel(true);
         playlist_task = null;
      }

      if ( update_state )
         return done(State.STATE_STOP);
      else
         return done();
   }

/*   private int duck(String msg)
   {
      if ( State.is(State.STATE_DUCK) || ! State.is_playing() )
         return done();

      player.setVolume(0.1f, 0.1f);
      return done(State.STATE_DUCK);
   }*/

   public int pause()
   {
      if ( player == null || State.is(State.STATE_PAUSE) || ! State.isPlaying() )
         return done();

      if ( pause_task != null )
         pause_task.cancel(true);

      pause_task =
         new Later()
         {
            @Override
            public void later()
            {
               pause_task = null;
               stop();
            }
         }.start();

      player.pause();
      return done(State.STATE_PAUSE);
   }

   public int restart()
   {
      if ( player == null || State.isStopped() )
         return play();

      player.setVolume(1.0f, 1.0f);

      if ( State.is(State.STATE_PLAY) || State.is(State.STATE_BUFFER) )
         return done();

/*      if ( State.is(State.STATE_DUCK) )
         return done(State.STATE_PLAY);*/

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
      {
         return done();
      }

      if ( pause_task != null )
         { pause_task.cancel(true); pause_task = null; }

      player.start();
      return done(State.STATE_PLAY);
   }

   private int done(String state)
   {
      if ( state != null )
         State.setState(context, state);
      return done();
   }

   private int done()
      { return START_NOT_STICKY; }

   @Override
   public void onBufferingUpdate(MediaPlayer player, int percent)
   {
      /*
      // Notifications of buffer state seem to be unreliable.
      if ( 0 <= percent && percent <= 100 )
         log("Buffering: ", ""+percent, "%"); 
         */
   }

   @Override
   public boolean onInfo(MediaPlayer player, int what, int extra)
   {

      switch (what) {
         case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            State.setState(context, State.STATE_BUFFER);
            break;

         case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            failure_ttl = initial_failure_ttl;
            State.setState(context, State.STATE_PLAY);
             break;

          default: //do nothing
              break;
      }
      return true;
   }


   private void start_buffering()
   {
      if ( start_buffering_task != null )
         start_buffering_task.cancel(true);

      // We'll give it 90 seconds for the stream to start.  Otherwise, we'll
      // declare an error.  onError() tries to restart, in some cases.
      start_buffering_task = (Later)
         new Later(90)
         {
            @Override
            public void later()
            {
               start_buffering_task = null;
               onError(null,0,0);
            }
         }.start();
   }


   private void stop_soon()
   {
      if ( stopSoonTask != null )
          stopSoonTask.cancel(true);

       stopSoonTask = (Later)
         new Later(300)
         {
            @Override
            public void later()
            {
                stopSoonTask = null;
               stop();
            }
         }.start();
   }

   private void tryRecover()
   {
      stop_soon();
      if ( isNetworkUrl() && 0 < failure_ttl )
      {
         failure_ttl -= 1;
         if ( Connectivity.isConnected(context) )
            play();
         else
            connectivity.dropped_connection();
      }
   }

   @Override
   public boolean onError(MediaPlayer player, int what, int extra)
   {
      State.setState(context,State.STATE_ERROR);
       tryRecover(); // This calls stop_soon().

      // Returning true, here, prevents the onCompletionlistener from being called.
      return true;
   }

   @Override
   public void onCompletion(MediaPlayer mp)
   {

      if ( ! isNetworkUrl() && (State.is(State.STATE_PLAY)) )
         State.setState(context, State.STATE_COMPLETE);

      stop_soon();
   }

   @Override
   public void onAudioFocusChange(int change)
   {

      if ( player != null )
         switch (change)
         {
            case AudioManager.AUDIOFOCUS_GAIN:
               restart();
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
               // pause();
               // break;
               // Drop through.

            case AudioManager.AUDIOFOCUS_LOSS:
               pause();
               break;

             default: //do nothing
                 break;

         }
   }

   @Override
   public IBinder onBind(Intent intent)
      { return null; }
}
