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

import android.os.AsyncTask;

import org.oucho.radio.domain.helper.Counter;

public abstract class Later extends AsyncTask<Integer, Void, Void>
{
   private static final int default_seconds = 120;
   private int seconds = default_seconds;

   private final int then;

   public abstract void later();

   // secs <  0: execute immédiatement
   // secs == 0: délais pour default_seconds
   // otherwise: délais pour sec

   public Later(int secs) {
      super();

      int secondes  = secs;

      if ( secondes == 0 )
         secondes = default_seconds;
      seconds = secondes;
      then = Counter.now();
   }

   public  Later()
      { this(default_seconds); }

   protected Void doInBackground(Integer... args)
   {
      try { if ( 0 < seconds ) Thread.sleep(seconds * 1000); }
      catch ( Exception ignored) { }
      return null;
   }

   protected void onPostExecute(Void ignored)
   {
      if ( ! isCancelled() && Counter.still(then) )
         later();
   }

   public AsyncTask<Integer,Void,Void> start()
      { return executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); }
}
