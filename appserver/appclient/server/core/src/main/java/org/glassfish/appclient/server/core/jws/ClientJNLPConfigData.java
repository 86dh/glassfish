/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.appclient.server.core.jws;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Encapsulates the logic related to choosing which of the two possible
 * config files to use and handling the data within the selected file.
 */
class ClientJNLPConfigData {

    private static final String CONFIG_FILE_NAME = "client-jnlp-config.properties";
    private File previousConfigFileUsed = null;
    private File configFileFromDomain;
    private File configFileFromInstall;
    private long lastModified = -1;

    private static Logger logger = Logger.getLogger(JavaWebStartInfo.APPCLIENT_SERVER_MAIN_LOGGER,
                JavaWebStartInfo.APPCLIENT_SERVER_LOGMESSAGE_RESOURCE);

    @LogMessageInfo(
            message = "Config file for client JNLP not found: {0}",
            cause = "The config file does not exist.",
            action = "The file is part of the installation so this might be an internal error.  Make sure you have not accidentally removed or renamed the config file.  If not, please file an error with a reproducible test case.")
    public static final String NO_CONFIG_FILE = "AS-ACDEPL-00108";

    @LogMessageInfo(
            message = "The config file for client JNLP {0} exists but could not be read.",
            cause = "The config file might be protected from reading.",
            action = "Make sure the config file protection settings permit the server to read it.")
    public static final String CONFIG_UNREADABLE = "AS-ACDEPL-00109";

    /**
     * Easier for the caller if we return empty lists rather than nulls if we cannot
     * locate either config file.
     */
    private List<XPathToDeveloperProvidedContentRefs> xPathsToDevContentRefs = Collections.EMPTY_LIST;
    private List<CombinedXPath> xPathsToCombinedContent = Collections.EMPTY_LIST;


    ClientJNLPConfigData(final File installConfigDir, final File domainConfigDir) {
        super();
        configFileFromInstall = new File(installConfigDir, CONFIG_FILE_NAME);
        configFileFromDomain = new File(domainConfigDir, CONFIG_FILE_NAME);
        ensureCurrent();
    }

    /**
     * Makes sure that the in-memory data is up-to-date wrt the on-disk
     * properties file - whether that's the domain's config file or the
     * installation's config file.
     */
    private void ensureCurrent() {
        final File configFile = chooseConfigFile();
        if (configFile == null) {
            lastModified = -1;
            xPathsToDevContentRefs = Collections.EMPTY_LIST;
            xPathsToCombinedContent = Collections.EMPTY_LIST;
            return;
        }
        if (lastModified < configFile.lastModified() || previousConfigFileUsed != configFile) {
            processConfigFile(configFile);
        }
    }

    /**
     * Chooses which config file to use, preferring the domain's config
     * file (if present and readable) and falling back to the
     * installation's file.
     * <p>
     * The method logs a warning if finds but cannot read the domain's
     * config file (we assume in that case that the administrator intended for that file
     * to be used but, alas, we cannot read it).  It logs a severe error
     * if there is no JNLP config file for the domain and it cannot locate
     * or read the installation's file, which should always be usable.
     *
     * @return the config file to be used; null if neither the domain's nor
     * the installation's config file exists and is readable
     */
    private File chooseConfigFile() {
        File result = checkExistenceAndReadability(configFileFromDomain, Level.WARNING);
        if (result == null) {
            result = checkExistenceAndReadability(configFileFromInstall, Level.SEVERE);
            if (result == null && !configFileFromInstall.exists()) {
                /*
                 * Complain separately if the installation's config file
                 * does not exist.
                 */
                logErrorNonExistentConfig(configFileFromInstall);
            }
        }
        if (result != null && previousConfigFileUsed != null && result != previousConfigFileUsed) {
            logUsingDifferentConfigFile(result);
        }
        return result;
    }

    private File checkExistenceAndReadability(final File f, final Level logLevel) {
        File result = null;
        if (f.exists()) {
            if (f.canRead()) {
                result = f;
            } else {
                logErrorConfigExistsButCannotRead(logLevel, f);
            }
        }
        return result;
    }

    private void logErrorConfigExistsButCannotRead(final Level level, final File f) {
        logger.log(level, CONFIG_UNREADABLE, f.getAbsolutePath());
    }

    private void logErrorNonExistentConfig(final File f) {
        logger.log(Level.SEVERE, NO_CONFIG_FILE, f.getAbsolutePath());
    }

    private void logUsingDifferentConfigFile(final File f) {
        logger.log(Level.FINE, "Changing file for client JNLP configuration; now using {0}", f.getAbsolutePath());
    }

    List<XPathToDeveloperProvidedContentRefs> xPathsToDevContentRefs() {
        ensureCurrent();
        return xPathsToDevContentRefs;
    }

    List<CombinedXPath> xPathsToCombinedContent() {
        ensureCurrent();
        return xPathsToCombinedContent;
    }

    private void processConfigFile(final File configFile) {
        final Properties p = new Properties();
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(configFile));
            p.load(is);
            final List<XPathToDeveloperProvidedContentRefs> newRefsToContent = prepareRefsToContent(p);
            final List<CombinedXPath> newCombinedXPaths = prepareCombinedXPaths(p);
            /*
             * Now that everything seems to have worked, assign the newly-computed
             * values to the fields.
             */
            xPathsToDevContentRefs = newRefsToContent;
            xPathsToCombinedContent = newCombinedXPaths;
            lastModified = configFile.lastModified();
            previousConfigFileUsed = configFile;
            logger.log(Level.FINE, "enterprise.deployment.appclient.jws.clientJNLPConfigLoad", configFile.getAbsolutePath());
        } catch (Exception e) {
            final String fmt = logger.getResourceBundle().getString("enterprise.deployment.appclient.jws.clientJNLPConfigProcError");
            final String msg = MessageFormat.format(fmt, configFile.getAbsolutePath());
            logger.log(Level.SEVERE, msg, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private List<XPathToDeveloperProvidedContentRefs> prepareRefsToContent(final Properties p) {
        final List<XPathToDeveloperProvidedContentRefs> result =
                XPathToDeveloperProvidedContentRefs.parse(p);
        return result;
    }

    private List<CombinedXPath> prepareCombinedXPaths(final Properties p) {
        final List<CombinedXPath> result = CombinedXPath.parse(p);
        return result;
    }


}
