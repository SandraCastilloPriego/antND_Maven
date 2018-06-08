/*
 * Copyright 2007-2012 
 *
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
package com.vtt.antnd.modules.configuration.general;


import com.vtt.antnd.desktop.numberFormat.NumberFormatParameter;
import com.vtt.antnd.desktop.preferences.NumOfThreadsParameter;
import com.vtt.antnd.desktop.preferences.ProxySettings;
import com.vtt.antnd.desktop.preferences.WindowStateParameter;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.parameters.Parameter;
import com.vtt.antnd.parameters.ParameterSet;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.parameters.parametersType.OptionalModuleParameter;
import com.vtt.antnd.util.dialogs.ExitCode;
import java.text.DecimalFormat;

/**
 *
 */
public class GeneralconfigurationParameters extends SimpleParameterSet {
       
        public static final NumberFormatParameter intensityFormat = new NumberFormatParameter(
                "Intensity format",
                "Format of intensity values. Please check the help file for details.",
                new DecimalFormat("0.0E0"));
        public static final NumOfThreadsParameter numOfThreads = new NumOfThreadsParameter();
        public static final OptionalModuleParameter proxySettings = new OptionalModuleParameter(
                "Use proxy", "Use proxy for internet connection",
                new ProxySettings());
        public static final WindowStateParameter windowState = new WindowStateParameter();

        public GeneralconfigurationParameters() {
                super(new Parameter[]{numOfThreads, proxySettings, windowState});
        }

        @Override
        public ExitCode showSetupDialog() {

                ExitCode retVal = super.showSetupDialog();

                if (retVal == ExitCode.OK) {

                        // Update system proxy settings
                        if (getParameter(proxySettings).getValue()) {
                                ParameterSet proxyParams = getParameter(proxySettings).getEmbeddedParameters();
                                String address = proxyParams.getParameter(
                                        ProxySettings.proxyAddress).getValue();
                                String port = proxyParams.getParameter(ProxySettings.proxyPort).getValue();
                                System.setProperty("http.proxyHost", address);
                                System.setProperty("http.proxyPort", port);
                        } else {
                                System.clearProperty("http.proxyHost");
                                System.clearProperty("http.proxyPort");
                        }

                        // Repaint windows to update number formats
                        NDCore.getDesktop().getMainFrame().repaint();
                }

                return retVal;
        }

        public void setProxy() {
                try {
                        // Update system proxy settings
                        if (getParameter(proxySettings).getValue()) {
                                ParameterSet proxyParams = getParameter(proxySettings).getEmbeddedParameters();
                                String address = proxyParams.getParameter(
                                        ProxySettings.proxyAddress).getValue();
                                String port = proxyParams.getParameter(ProxySettings.proxyPort).getValue();
                                System.setProperty("http.proxyHost", address);
                                System.setProperty("http.proxyPort", port);
                        } else {
                                System.clearProperty("http.proxyHost");
                                System.clearProperty("http.proxyPort");
                        }
                } catch (NullPointerException e) {
                }
        }
}
