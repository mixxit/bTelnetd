/* $Id: Welcome.java,v 1.5 2006/05/01 15:45:16 m31 Exp $
 *
 * This file is part of the project "Poor Woman's Telnet Server".
 *
 *   http://pwts.sourceforge.net/
 *
 * Copyright 2006,  Michael Meyling <michael@meyling.com>.
 *
 * "PWTS" is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package com.meyling.telnet.shell;

import java.util.Random;

/**
 * Welcome picture producer. This class provides ascii images.
 * 
 *
 * @author Michael Meyling
 */
public class Welcome {

    private static final String[] eyes = new String[] {
        "  _-~~-.        .-~~-_",
        " ,^( O )\\      /( O )^\"",
        "  ^^~~~^^      ^^~~~~^"
    };

    private static final String[] running = new String[] {
        "         _",
        "       _( }",
        "     /_  '>",
        "      /\" /~\"",
        "     / -;\\",
        " ;--'    /'",
        "        <_"
    };

    private static final String[] bull = new String[] {
        "       (_)",
        "    _ / \",",
        " ./( (  )",
        "   f^~Y^|",
        "   ~  ^ ^"
    };

    private static final String[] jumping = new String[] {
        " 7_O_/",
        "  (/",
        "  /\\/'",
        "  7"
    };


    private static final String[] dancing = new String[] {
        ",   ",
        "|_,O ",
        " (/\\/\"",
        " |\\",
        "  \\`\\",
        "  '  '"
    };

    private static final String[] dancing2 = new String[] {
        "  O  ",
        " <V\\/",
        " '|\\",
        "  /~> ",
        " '  ~ "       
    };
    
    private static final String[] running2 = new String[] {
        " o",
        "\\/>",
        " /\\",
        " > >"
    };
    
    private static final String[] declare = new String[] {
        "      ,",
        "   (}_/ ",
        "  /,; ",
        "\"^ / \\",
        "   \\T/",
        "   //",
        "   \"\""
    };
    
    private static final String[][] all = new String[][] {
        eyes, running, bull, jumping, dancing, dancing2, running2, declare
    };

    private static final StringBuffer spaces = new StringBuffer("                   ");

    private static final String[][] getPictures() {
        return all;
    }

    /**
     * Get maximum length of <code>String</code> array.
     *
     * @param   arg Array to work on. Must not be <code>null</code>.
     * @return  Maximum length. Maybe <code>-1</code> if all entries
     *          are <code>null</code>.
     */
    public static final int getMaximumLength(final String[] arg) {
        int maximum = -1;
        for (int i = 0; i < arg.length; i++) {
            if (arg[i] != null && arg[i].length() > maximum) {
                maximum = arg[i].length();
            }
        }
        return maximum;
    }

    /**
     * Get required number of spaces.
     *
     * @param   length  Number of spaces wanted.
     * @return  Spaces.
     */
    public static final String getSpaces(final int length) {
        while (length > spaces.length()) {
            spaces.append(spaces);
        }
        return spaces.substring(0, length);
    }

    /**
     * Get random picture string.
     *
     * @param   right   Right edge position in characters.
     * @return  Right aligned ascii picture. Each <code>String</code>
     *          is already terminated by <code>\r\n</code>.
     */
    public static final String[] getRandomPicture(final int right) {
        final Random random = new Random();
        final String[] welcome = getPictures()[
           (int) (random.nextInt(Welcome.getPictures().length))];
        final String[] result = new String[welcome.length];
        final String spaces = Welcome.getSpaces(right - getMaximumLength(welcome) - 1);
        for (int i = 0; i < welcome.length; i++) {
            result[i] = spaces + welcome[i] + "\r\n";
        }
        return result;
    }

}
