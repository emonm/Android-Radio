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

package org.oucho.radio.domain.helper;

public class Counter
{
   private static volatile int counter = 1;

   public static int now()
      { return counter; }

   public static void timePasses()
      { counter += 1; }

   public static boolean still(int then)
      { return then == now(); }
}

