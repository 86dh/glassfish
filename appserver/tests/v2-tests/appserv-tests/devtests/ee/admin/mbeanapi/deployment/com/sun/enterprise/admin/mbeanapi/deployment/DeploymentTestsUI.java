/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation. All rights reserved.
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.mbeanapi.deployment;

import java.io.*;
import java.net.*;
import java.util.*;


/**
 */

public class DeploymentTestsUI
{
    public DeploymentTestsUI()
    {
        System.setProperty(Env.USE_TLS_SYSTEM_PROPERTY, "true");

        persist = new SimplePersistence(this);
        getArgs();
        setPort();
        getSamplesDir();
        getSamples();
    }

    ///////////////////////////////////////////////////////////////////////////

    public void deployAll()
    {
        for(int i = 0; i < archives.length; i++)
        {
            File f = archives[i];
            setName(f);
            print("Deploying " + name + " (" + f + ")");

            if(!deployOne(f))
                return;
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    private boolean deployOne(File f)
    {
        // return true to continue, false to abort...
        try
        {
            DeploymentTest test = new DeploymentTest(user.value, password.value,
                host.value, portInt, f.getAbsolutePath(), name, contextRoot, enable, appservTarget.value);

            System.out.println("################################################");
            System.out.println("#########   Deploying  " + name + "  ##########");
            System.out.println("################################################");
            test.run();

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        DeployedAppInspector list = null;

        try
        {
            list = new DeployedAppInspector(user.value, password.value,
                host.value, portInt,  appservTarget.value);

            System.out.println("################################################");
            System.out.println("#########   AppList: ");
            System.out.println("################################################");
            //list.run();
            //String[] results = list.getResults();

            //for(int i = 0; i < results.length; i++)
                //System.out.println("" + i + ": " + results[i]);

            System.out.println("isDeployed(): " + list.isDeployed(name));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        String response = "";

        try
        {
            response = Console.readLine("Go check on deployed App.  Press <return> when ready to undeploy it. 'q' to quit");
            UndeploymentTest test = new UndeploymentTest(user.value, password.value,
                host.value, portInt,  name, appservTarget.value);

            System.out.println("################################################");
            System.out.println("#########   Un-Deploying  " + name + "  ##########");
            System.out.println("################################################");
            test.run();

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println("################################################");
            System.out.println("#########   AppList: ");
            System.out.println("################################################");
            //list.run();
            //String[] results = list.getResults();

            //for(int i = 0; i < results.length; i++)
                //System.out.println("" + i + ": " + results[i]);

            System.out.println("isDeployed(): " + list.isDeployed(name));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return !response.toLowerCase().startsWith("q");
    }

    ///////////////////////////////////////////////////////////////////////////

    private void getArgs()
    {
        getArg(user);
        getArg(password);
        getArg(host);
        getArg(port);
        getArg(appservTarget);
    }

    ///////////////////////////////////////////////////////////////////////////

    private void getArg(NameValue nv)
    {
        nv.value = persist.getProperty(nv.name);

        if(!ok(nv.value))
            nv.value = null;

        String prompt = nv.name + ": ";

        if(ok(nv.value))
            prompt += "[" + nv.value + "]";

        String reply = Console.readLine(prompt);

        if(ok(reply))
        {

            nv.value = reply;
            persist.setProperty(nv.name, nv.value);
        }
        else if(!ok(nv.value))
            throw new IllegalArgumentException("No arg supplied for: " + nv.name);
        // else -- it is already set...
    }

    ///////////////////////////////////////////////////////////////////////////

    boolean ok(String s)
    {
        return s != null && s.length() > 0;
    }

    ///////////////////////////////////////////////////////////////////////////

    private void getSamplesDir()
    {
        Class    clazz    = getClass();
        URL url = clazz.getResource("samples");
        String filename = url.getPath();
        samplesDir = new File(filename);
        System.out.println("Samples Dir: " + filename);
    }

    ///////////////////////////////////////////////////////////////////////////

    private void getSamples()
    {
        archives = samplesDir.listFiles(new ArchiveFilter());
    }

    ///////////////////////////////////////////////////////////////////////////

    private void setName(File f)
    {
        // note that ArchiveFilter guarantees that there will be a 4-character extension...
        name = f.getName();
        name = name.substring(0, name.length() - 4);
    }

    ///////////////////////////////////////////////////////////////////////////

    private void setPort()
    {
        try
        {
            portInt = Integer.parseInt(port.value);
        }
        catch(NumberFormatException nfe)
        {
            throw new IllegalArgumentException("Bad port number: " + port.value);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    private void print(String s)
    {
        System.out.println(s);
    }

    ///////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) throws Exception
    {
        DeploymentTestsUI tests = new DeploymentTestsUI();
        tests.deployAll();
    }

    ///////////////////////////////////////////////////////////////////////////

    private        File[]                archives;
    private        Cmd                    target;
    private        SimplePersistence    persist;
    private        File                archive;
    private        String                name;
    private        String                contextRoot;
    private        boolean                enable = true;
    private        File                samplesDir;
    private        int                    portInt        = -1;
    private        NameValue            user        = new NameValue("user");
    private        NameValue            password    = new NameValue("password");
    private        NameValue            host        = new NameValue("host");
    private        NameValue            port        = new NameValue("port");
    private        NameValue            appservTarget= new NameValue("target");

    ///////////////////////////////////////////////////////////////////////////

    private static class NameValue
    {
        private NameValue(String name)
        {
            this.name = name;
        }
        private final String name;
        private String value;
    }

    private static class ArchiveFilter implements FileFilter
    {
        public boolean accept(File f)
        {
            // must end in .jar/.war/.rar/.ear

            String name = f.getName();

            return name.endsWith(".ear") || name.endsWith(".jar") || name.endsWith(".rar") || name.endsWith(".war");
        }
    }
}

