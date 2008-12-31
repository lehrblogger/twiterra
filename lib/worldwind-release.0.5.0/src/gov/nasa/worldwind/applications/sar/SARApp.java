/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.Configuration;

/**
 * @author tag
 * @version $Id: SARApp.java 4996 2008-04-09 18:31:43Z dcollins $
 */
public class SARApp
{
    public static final String APP_NAME = "World Wind Search and Rescue Prototype";
    public static final String APP_VERSION = "(Version 4.3 released 4/9/08)";
    public static final String APP_NAME_AND_VERSION = APP_NAME + " " + APP_VERSION;

    static
    {
        System.setProperty("gov.nasa.worldwind.config.file", "config/SAR.properties");
        if (Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME);
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
        }
    }

    private static boolean checkLicenseAgreement()
    {
        NOSALicenseAgreement licenseAgreement = new NOSALicenseAgreement(APP_NAME_AND_VERSION);
        String status = licenseAgreement.checkForLicenseAgreement(null);
        return (status.equals(NOSALicenseAgreement.LICENSE_ACCEPTED)
             || status.equals(NOSALicenseAgreement.LICENSE_ACCEPTED_AND_INSTALLED));
    }

    public static void main(String[] args)
    {
        boolean licenseStatus = checkLicenseAgreement();
        if (licenseStatus)
        {
            SAR2 appFrame = new SAR2();
            appFrame.setVisible(true);
        }
        else
        {
            System.exit(0);
        }
    }
}
