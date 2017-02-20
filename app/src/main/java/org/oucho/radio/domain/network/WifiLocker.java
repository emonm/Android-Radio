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

package org.oucho.radio.domain.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;

public class WifiLocker
{
   private static WifiLock lock = null;
   private static WifiManager manager = null;

   public static void lock(Context context, String app_name)
   {
      if ( manager == null )
         manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

      if ( manager != null && lock == null )
         lock = manager.createWifiLock(WifiManager.WIFI_MODE_FULL, app_name);

      if ( lock == null )
         return;

      if ( ! Connectivity.onWifi() )
         { unlock(); return; }

      if ( lock.isHeld() )
         return;

      lock.acquire();
   }

   public static void unlock()
   {
      if ( lock != null && lock.isHeld() )
      {
         lock.release();
      }
   }
}
