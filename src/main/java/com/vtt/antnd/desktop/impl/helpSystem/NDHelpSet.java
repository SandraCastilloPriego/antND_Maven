/*
 * Copyright 2007-2012 
 * This file is part of AntND.
 *
 * AntND is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * AntND is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * AntND; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.vtt.antnd.desktop.impl.helpSystem;

import javax.help.HelpSet;

/**
 * @author Taken from MZmine2
 * http://mzmine.sourceforge.net/
 *
 */
public class NDHelpSet extends HelpSet {

        public NDHelpSet() {
                super();
        }

        public void addTOCView(NDTOCView TOCView) {
                this.addView(TOCView);
        }
}
