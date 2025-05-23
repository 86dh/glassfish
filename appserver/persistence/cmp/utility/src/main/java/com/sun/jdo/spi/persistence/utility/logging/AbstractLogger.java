/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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

/*
 * AbstractLogger.java
 *
 * Created on May 14, 2002, 12:35 PM
 */

package com.sun.jdo.spi.persistence.utility.logging;

import com.sun.jdo.spi.persistence.utility.StringHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.glassfish.persistence.common.I18NHelper;

import static org.glassfish.embeddable.GlassFishVariable.JAVA_HOME;

/**
 * This class provides a default implementation of the
 * com.sun.jdo.spi.persistence.utility.logging.Logger interface which
 * implements most methods and/or delegates them to a few
 * abstract methods which subclasses must override.
 *
 * @author Rochelle Raccah
 * @version %I%
 */
abstract public class AbstractLogger implements Logger
{
    /** I18N message handler for this class */
    private static final ResourceBundle _messages =
        I18NHelper.loadBundle(AbstractLogger.class);

    /** Default level if none supplied */
    private static final int _defaultLevel = INFO;

    /** Level Separator for Logger Level specification */
    private static final char _levelSeparator='.';

    private static Map _levelNamesToValues;

    private static boolean _hasLoggingProperties = true;
    private static Properties _loggingProperties;

    /** Resource bundle for this (named) logger */
    private final ResourceBundle _bundle;

    /** Name for this logger */
    private final String _loggerName;

    /** Level for this logger */
    private final int _level;

    static
    {
        _levelNamesToValues = new HashMap();
        _levelNamesToValues.put("ALL", Integer.valueOf(ALL));        // NOI18N
        _levelNamesToValues.put("FINEST", Integer.valueOf(FINEST));    // NOI18N
        _levelNamesToValues.put("FINER", Integer.valueOf(FINER));    // NOI18N
        _levelNamesToValues.put("FINE", Integer.valueOf(FINE));        // NOI18N
        _levelNamesToValues.put("CONFIG", Integer.valueOf(CONFIG));    // NOI18N
        _levelNamesToValues.put("INFO", Integer.valueOf(INFO));        // NOI18N
        _levelNamesToValues.put("WARNING", Integer.valueOf(WARNING)); // NOI18N
        _levelNamesToValues.put("SEVERE", Integer.valueOf(SEVERE));    // NOI18N
        _levelNamesToValues.put("OFF", Integer.valueOf(OFF));        // NOI18N
    }

    /** Creates a new AbstractLogger.  The supplied class loader or the
     * loader which loaded this class must be able to load the bundle.
     * @param loggerName the full domain name of this logger
     * @param bundleName the bundle name for message translation
     * @param loader the loader used for looking up the bundle file
     * and possibly the logging.properties or alternative file
     */
    public AbstractLogger (String loggerName, String bundleName,
        ClassLoader loader)
    {
        _loggerName = loggerName;
        _level = readLoggingLevel(loggerName);
        _bundle = I18NHelper.loadBundle(bundleName, loader);
    }

    /** Get the message bundle for the AbstractLogger itself.
     */
    protected static ResourceBundle getMessages () { return _messages; }

    private static Map getLevelNameMap () { return _levelNamesToValues; }

    // mostly copied from LogManager's readConfiguration() in jdk1.4
    private static synchronized Properties getLoggingProperties ()
    {
        if (_hasLoggingProperties && (_loggingProperties == null))
        {
            String fileName =
                System.getProperty("java.util.logging.config.file");

            if (fileName == null)
            {
                fileName = System.getProperty(JAVA_HOME.getSystemPropertyName());

                if (fileName != null)
                {
                    File file = new File(fileName, "lib");

                    file = new File(file, "logging.properties");

                    try
                    {
                        fileName = file.getCanonicalPath();
                    }
                    catch (IOException ioe)
                    {
                        fileName = null;
                    }
                }
            }

            if (fileName != null)
            {
                InputStream inputStream = null;

                try
                {
                    Properties properties = new Properties();
                    BufferedInputStream bufferedInputStream = null;

                    inputStream = new FileInputStream(fileName);
                    bufferedInputStream = new BufferedInputStream(inputStream);
                    properties.load(bufferedInputStream);
                    _loggingProperties = properties;
                }
                catch (Exception e)
                {
                    _hasLoggingProperties = false;
                }
                finally
                {
                    if (inputStream != null)
                    {
                        try
                        {
                            inputStream.close();
                        }
                        catch (IOException ioe)
                        {
                            // couldn't close it for some reason
                        }
                    }
                }
            } else {
                _hasLoggingProperties = false;
            }
        }

        return _loggingProperties;
    }

    /** Return the string name of a level given its int value.
     * @return a string representing the level
     */
    public static String toString (int level)
    {
        String bundleKey = null;

        switch (level)
        {
            case Logger.OFF:
                bundleKey = "utility.level_off";    // NOI18N
                break;
            case Logger.SEVERE:
                bundleKey = "utility.level_severe";    // NOI18N
                break;
            case Logger.WARNING:
                bundleKey = "utility.level_warning";    // NOI18N
                break;
            case Logger.INFO:
                bundleKey = "utility.level_info";    // NOI18N
                break;
            case Logger.CONFIG:
                bundleKey = "utility.level_config";    // NOI18N
                break;
            case Logger.FINE:
                bundleKey = "utility.level_fine";    // NOI18N
                break;
            case Logger.FINER:
                bundleKey = "utility.level_finer";    // NOI18N
                break;
            case Logger.FINEST:
                bundleKey = "utility.level_finest";    // NOI18N
                break;
            case Logger.ALL:
                bundleKey = "utility.level_all";    // NOI18N
                break;
        }

        return ((bundleKey != null) ?
            I18NHelper.getMessage(getMessages(), bundleKey) : null);
    }

    /** Get the message bundle for the named instance of the logger.
     */
    protected ResourceBundle getBundle () { return _bundle; }

    public int getLevel () { return _level; }

    private int readLoggingLevel (String loggerName)
    {
        String value =  findLevelMatch(loggerName);
        int level = _defaultLevel;

        if (value != null)
        {
            Object lookupValue = null;

            value = value.trim();
            lookupValue = getLevelNameMap().get(value);

            if (lookupValue != null) {
                level = ((Integer)lookupValue).intValue();
            } else        // maybe a number was specified
            {
                try
                {
                    level = Integer.parseInt(value);
                }
                catch (NumberFormatException nfe)
                {
                    // level will be the default
                }
            }
        }

        return level;
    }

    /** Find the best matched logging level from the logging properties
     *@param loggerName the name of the hierarchical
     *@return a String representing the level value
     */
    private String findLevelMatch (String loggerName)
    {
        Properties properties = getLoggingProperties();
        String value = null;

        if (properties != null)
        {
            while (value == null)
            {
                int lastIndex = loggerName.lastIndexOf(_levelSeparator);

                value = properties.getProperty(loggerName + ".level"); // NOI18N

                if (loggerName.trim().length() > 0)
                {
                    loggerName = ((lastIndex == -1) ? "" :        // NOI18N
                        loggerName.substring(0, lastIndex));
                } else {
                    return value;
                }
            }
        }

        return value;
    }

    /** Return whether logging is enabled at the FINE level.  This method
     * is not exact because to make it accurately reflect the logging level
     * we would have to include the JDK 1.4 java.util.logging.Level class.
     * This method does not delegate to isLoggable(FINE) for performance
     * reasons.
     * @return whether logging is enabled at the fine level.
     */
    @Override
    public boolean isLoggable () { return (FINE >= getLevel()); }

    /**
     * Check if a message of the given level would actually be logged
     * by this logger.  This check is based on the Logger's effective level,
     * which may be inherited from its parent.
     *
     * @return true if the given message level is currently being logged.
     * @param levelValue the level to check
     */
    @Override
    public boolean isLoggable (int levelValue)
    {
        return (levelValue >= getLevel());
    }

    /**
     * Log a method entry.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     * <p>
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of method that is being entered
     */
    @Override
    public void entering (String sourceClass, String sourceMethod)
    {
        entering(sourceClass, sourceMethod, (Object[])null);
    }

    /**
     * Log a method entry, with one parameter.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY {0}", log level
     * FINER, and the given sourceMethod, sourceClass, and parameter
     * is logged.
     * <p>
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of method that is being entered
     * @param param1 parameter to the method being entered
     */
    @Override
    public void entering (String sourceClass, String sourceMethod,
        Object param1)
    {
        if (isLoggable(FINER)) {
            entering(sourceClass, sourceMethod, new Object[]{param1});
        }
    }

    /**
     * Log a method entry, with an array of parameters.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY" (followed by a
     * format {N} indicator for each entry in the parameter array),
     * log level FINER, and the given sourceMethod, sourceClass, and
     * parameters is logged.
     * <p>
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of method that is being entered
     * @param params array of parameters to the method being entered
     */
    @Override
    public void entering (String sourceClass, String sourceMethod,
        Object params[])
    {
        if (isLoggable(FINER))
        {
            MessageFormat messageFormat = null;
            String messageKey = null;
            String[] args = null;

            if ((params != null) && (params.length > 0))
            {
                messageKey = "entering_method_params";    // NOI18N
                args = new String[]{sourceClass, sourceMethod,
                    StringHelper.arrayToSeparatedList(Arrays.asList(params))};
            }
            else
            {
                messageKey = "entering_method";    // NOI18N
                args = new String[]{sourceClass, sourceMethod};
            }

            messageFormat = new MessageFormat(
                getMessages().getString(messageKey));
            finer(messageFormat.format(args));
        }
    }

    /**
     * Log a method return.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     * <p>
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of the method
     */
    @Override
    public void exiting (String sourceClass, String sourceMethod)
    {
        exiting(sourceClass, sourceMethod, null);
    }

    /**
     * Log a method return, with result object.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN {0}", log level
     * FINER, and the gives sourceMethod, sourceClass, and result
     * object is logged.
     * <p>
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of the method
     * @param result Object that is being returned
     */
    @Override
    public void exiting (String sourceClass, String sourceMethod, Object result)
    {
        if (isLoggable(FINER))
        {
            MessageFormat messageFormat = null;
            String messageKey = null;
            Object[] args = null;

            if (result != null)
            {
                messageKey = "exiting_method_return";    // NOI18N
                args = new Object[]{sourceClass, sourceMethod, result};
            }
            else
            {
                messageKey = "exiting_method";    // NOI18N
                args = new Object[]{sourceClass, sourceMethod};
            }

            messageFormat = new MessageFormat(
                getMessages().getString(messageKey));
            finer(messageFormat.format(args));
        }
    }

    /**
     * Log throwing an exception.
     * <p>
     * This is a convenience method to log that a method is
     * terminating by throwing an exception.  The logging is done
     * using the FINER level.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.  The
     * LogRecord's message is set to "THROW".
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus is it
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of the method.
     * @param thrown The Throwable that is being thrown.
     */
    @Override
    public void throwing (String sourceClass, String sourceMethod,
        Throwable thrown)
    {
        if (isLoggable(FINER))
        {
            MessageFormat messageFormat = new MessageFormat(
                getMessages().getString("throwing_method"));

            log(FINER, messageFormat.format(
                new String[]{sourceClass, sourceMethod}), thrown);
        }
    }

    /**
     * Log a SEVERE message.
     * <p>
     * If the logger is currently enabled for the SEVERE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     */
    @Override
    public void severe (String msg) { log(SEVERE, msg); }

    /**
     * Log a WARNING message.
     * <p>
     * If the logger is currently enabled for the WARNING message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     */
    @Override
    public void warning (String msg) { log(WARNING, msg); }

    /**
     * Log an INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     */
    @Override
    public void info (String msg) { log(INFO, msg); }

    /**
     * Log a CONFIG message.
     * <p>
     * If the logger is currently enabled for the CONFIG message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     */
    @Override
    public void config (String msg) { log(CONFIG, msg); }

    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param level The level for this message
     * @param msg The string message (or a key in the message catalog)
     */
    @Override
    public void log (int level, String msg)
    {
        if (isLoggable(level)) {
            logInternal(level, getMessage(msg));
        }
    }

    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param level The level for this message
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     */
    @Override
    public void log (int level, String msg, Object o1)
    {
        if (isLoggable(level)) {
            log(level, msg, new Object[]{o1});
        }
    }

    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the  message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param level The level for this message
     * @param msg The string message (or a key in the message catalog)
     * @param o Objects to be inserted into the message
     */
    @Override
    public void log (int level, String msg, Object[] o)
    {
        if (isLoggable(level))
        {
            int count = ((o == null) ? 0 : o.length);
            String formattedMessage = msg;

            if (count > 0)
            {
                MessageFormat messageFormat =
                    new MessageFormat(getBundle().getString(msg));

                if (messageFormat != null) {
                    formattedMessage = messageFormat.format(o);
                }
            } else {
                formattedMessage = getMessage(msg);
            }

            logInternal(level, formattedMessage);
        }
    }

    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the  message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param level The level for this message
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     * @param o2 A parameter to be inserted into the message
     */
    @Override
    public void log (int level, String msg, Object o1, Object o2)
    {
        if (isLoggable(level)) {
            log(level, msg, new Object[]{o1, o2});
        }
    }

    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the  message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param level The level for this message
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     * @param o2 A parameter to be inserted into the message
     * @param o3 A parameter to be inserted into the message
     */
    @Override
    public void log (int level, String msg, Object o1, Object o2, Object o3)
    {
        if (isLoggable(level)) {
            log(level, msg, new Object[]{o1, o2, o3});
        }
    }

    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the message
     * level then the given message, and the exception dump,
     * is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param level The level for this message
     * @param msg The string message (or a key in the message catalog)
     * @param thrown The exception to log
     */
    @Override
    abstract public void log (int level, String msg, Throwable thrown);

    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the  message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     */
    @Override
    public void fine (String msg) { log(FINE, msg); }

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     */
    @Override
    public void fine (String msg, Object o1) { log(FINE, msg, o1); }

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o Objects to be inserted into the message
     */
    @Override
    public void fine (String msg, Object[] o) { log(FINE, msg, o); }

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     * @param o2 A parameter to be inserted into the message
     */
    @Override
    public void fine (String msg, Object o1, Object o2)
    {
        log(FINE, msg, o1, o2);
    }

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     * @param o2 A parameter to be inserted into the message
     * @param o3 A parameter to be inserted into the message
     */
    @Override
    public void fine (String msg, Object o1, Object o2, Object o3)
    {
        log(FINE, msg, o1, o2, o3);
    }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     */
    @Override
    public void finer (String msg) { log(FINER, msg); }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o Objects to be inserted into the message
     */
    @Override
    public void finer (String msg, Object[] o) { log(FINER, msg, o); }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     */
    @Override
    public void finer (String msg, Object o1) { log(FINER, msg, o1); }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     * @param o2 A parameter to be inserted into the message
     */
    @Override
    public void finer (String msg, Object o1, Object o2)
    {
        log(FINER, msg, o1, o2);
    }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     * @param o2 A parameter to be inserted into the message
     * @param o3 A parameter to be inserted into the message
     */
    @Override
    public void finer (String msg, Object o1, Object o2, Object o3)
    {
        log(FINER, msg, o1, o2, o3);
    }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     */
    @Override
    public void finest (String msg) { log(FINEST, msg); }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o Objects to be inserted into the message
     */
    @Override
    public void finest (String msg, Object[] o) { log(FINEST, msg, o); }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     */
    @Override
    public void finest (String msg, Object o1) { log(FINEST, msg, o1); }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     * @param o2 A parameter to be inserted into the message
     */
    @Override
    public void finest (String msg, Object o1, Object o2)
    {
        log(FINEST, msg, o1, o2);
    }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The string message (or a key in the message catalog)
     * @param o1 A parameter to be inserted into the message
     * @param o2 A parameter to be inserted into the message
     * @param o3 A parameter to be inserted into the message
     */
    @Override
    public void finest (String msg, Object o1, Object o2, Object o3)
    {
        log(FINEST, msg, o1, o2, o3);
    }

    /**
     * Get the name for this logger.
     * @return logger name.  Will be null for anonymous Loggers.
     */
    @Override
    public String getName () { return _loggerName; }

    /** Prepare a printable version of this instance.
     * @return the String representation of this object
     */
    @Override
    public String toString ()
    {
        StringBuffer buffer = new StringBuffer(getClass().getName());

        buffer.append(": ");                //NOI18N
        buffer.append(" name: ");            //NOI18N
        buffer.append(getName());
        buffer.append(", logging level: ");    //NOI18N
        buffer.append(toString(getLevel()));

        return buffer.toString();
    }

    /**
     * This method returns a string from the bundle file if possible,
     * treating the message argument as the key.  If no such key is found
     * in the bundle, the message itself is returned.
     *
     * @return a message either used as itself or searched in the bundle file.
     * @param message the message which is used as a key to search the bundle
     */
    protected String getMessage (String message)
    {
        try
        {
            return getBundle().getString(message);
        }
        catch (MissingResourceException e)
        {
            return message;
        }

    }

    /**
     * This method returns a string with a formatted prefix which
     * depends on the level.
     *
     * @return a formatted string with a level prefix.
     * @param level the level to print
     * @param message the message to print
     * @see AbstractLogger#toString
     */
    protected String getMessageWithPrefix (int level, String message)
    {
        MessageFormat messageFormat = new MessageFormat(
            getMessages().getString("logging_prefix"));    // NOI18N

        return messageFormat.format(
            new String[]{toString(level), message});
    }

    /**
     * This method does the actual logging.  It is expected that if a
     * check for isLoggable is desired for performance reasons, it has
     * already been done, as it should not be done here.
     * @param level the level to print
     * @param message the message to print
     */
    abstract protected void logInternal (int level, String message);
}
