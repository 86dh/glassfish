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

/*
 * WebAppHandler.java
 *
 * Created on August 10, 2006, 2:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *
 * @author anilam
 */
package org.glassfish.admingui.common.handlers;

import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.glassfish.admingui.common.util.DeployUtil;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestResponse;
import org.glassfish.admingui.common.util.RestUtil;



public class WebAppHandlers {

    /** Creates a new instance of ApplicationsHandler */
    public WebAppHandlers() {
    }

   //This is called when user change the default web module of a VS.
   //Need to ensure this VS is in the application-ref virtual server list. If not add it and restart the app for
   //change to take into effect.  refer to issue#8671
   @Handler(id = "gf.ensureDefaultWebModule",
        input = {
            @HandlerInput(name = "endpoint", type = String.class, required = true),
            @HandlerInput(name = "vsName", type = String.class, required = true),
            @HandlerInput(name = "instanceList", type=List.class, required=true)
        })
    public static void ensureDefaultWebModule(HandlerContext handlerCtx) throws Exception {
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        String vsName = (String) handlerCtx.getInputValue("vsName");
        String encodedName = URLEncoder.encode(vsName, "UTF-8");
        List<String> instanceList = (List) handlerCtx.getInputValue("instanceList");

        Map vsAttrs = RestUtil.getAttributesMap(endpoint + "/" + encodedName);
        String webModule= (String) vsAttrs.get("defaultWebModule");
        if (GuiUtil.isEmpty(webModule))
            return;
        String appName = webModule;
        int index = webModule.indexOf("#");
        if (index != -1){
            appName=webModule.substring(0, index);
        }
        String serverEndPoint = GuiUtil.getSessionValue("REST_URL") + "/servers/server/";
        for (String serverName : instanceList) {
            String encodedAppName = URLEncoder.encode(appName, "UTF-8");
            String encodedServerName = URLEncoder.encode(serverName, "UTF-8");
            String apprefEndpoint = serverEndPoint + encodedServerName + "/application-ref/" + encodedAppName;
            Map apprefAttrs = RestUtil.getAttributesMap(apprefEndpoint);
            String vsStr = (String) apprefAttrs.get("virtualServers");
            //Add to the vs list of this application-ref, then restart the app.
            if (GuiUtil.isEmpty(vsStr)){
                vsStr = vsName;
            }else{
                List vsList = GuiUtil.parseStringList(vsStr, ",");
                if (vsList.contains(vsName)){
                    continue;   //the default web module app is already deployed to this vs, no action needed
                }else{
                    vsStr=vsStr+","+vsName;
                }
            }
            apprefAttrs.put("virtualServers", vsStr);
            RestResponse response = RestUtil.sendUpdateRequest(apprefEndpoint, apprefAttrs, null, null, null);
            if (!response.isSuccess()) {
                GuiUtil.getLogger().severe("Update virtual server failed.  parent=" + apprefEndpoint + "; attrsMap =" + apprefAttrs);
                GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.error.checkLog"));
                return;
            }
            List targets = new ArrayList();
            targets.add("domain");
            DeployUtil.reloadApplication(appName, targets , handlerCtx);
        }
   }



   //This handler is called after user deleted one more more VS from the VS table.
   //We need to go through all the application-ref to see if the VS specified still exist.  If it doesn't, we need to
   //remove that from the vs list.
   @Handler(id = "checkVsOfAppRef")
   public static void checkVsOfAppRef(HandlerContext handlerCtx) throws Exception{
       String configUrl = GuiUtil.getSessionValue("REST_URL") + "/configs/config/";
       List configs = new ArrayList(RestUtil.getChildMap(configUrl).keySet());
       ArrayList vsList = new ArrayList();
       for (Object cfgName : configs) {
           String vsUrl = configUrl + cfgName + "/http-service/virtual-server";
           List vsNames = new ArrayList(RestUtil.getChildMap(vsUrl).keySet());
           for (Object str : vsNames) {
               if (!vsList.contains(str))
                   vsList.add(str);
           }
       }
       List servers = new ArrayList(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/servers/server").keySet());
       for (Object svrName : servers) {
           String serverEndpoint = GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + svrName;
           List appRefs = new ArrayList(RestUtil.getChildMap(serverEndpoint + "/application-ref").keySet());
           for (Object appRef : appRefs) {
               String apprefEndpoint = serverEndpoint + "/application-ref/" + appRef;
               Map apprefAttrs = RestUtil.getAttributesMap(apprefEndpoint);
               String vsStr = (String) apprefAttrs.get("VirtualServers");
               List<String> lvsList = GuiUtil.parseStringList(vsStr, ",");
               boolean changed = false;
               for(String oneVs: lvsList ){
                   if (! vsList.contains(oneVs)){
                       changed = true;
                       continue;
                   }
               }
               if (changed){
                   apprefAttrs.put("VirtualServers", vsStr);
                   RestResponse response = RestUtil.sendUpdateRequest(apprefEndpoint, apprefAttrs, null, null, null);
                   if (!response.isSuccess()) {
                       GuiUtil.getLogger().severe("Update virtual server failed.  parent=" + apprefEndpoint + "; attrsMap =" + apprefAttrs);
                       GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.error.checkLog"));
                       return;
                   }
               }
           }
       }
   }

   @Handler(id="gf.getProtocols",
        input={
            @HandlerInput(name="configName", type=String.class, required=true)
        },
        output={
            @HandlerOutput(name = "result", type = java.util.List.class)
        })
   public static void getProtocols(HandlerContext handlerCtx) {
       String configName = (String) handlerCtx.getInputValue("configName");
       String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName + "/network-config/protocols/protocol";
       List<Map> result = new ArrayList<Map>();
       try {
            List<Map> childElements = (List<Map>) RestUtil.buildChildEntityList(endpoint, "", null, null, "name");
            for (Map m: childElements) {
                String name = (String) m.get("name");
                if (!(name.equals(ServerTags.PORT_UNIF_PROTOCOL_NAME) || name.equals(ServerTags.REDIRECT_PROTOCOL_NAME))) {
                    result.add(m);
                }
            }
            handlerCtx.setOutputValue("result", result);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
   }

   @Handler(id="gf.getNetworkListeners",
        input={
            @HandlerInput(name="configName", type=String.class, required=true)
        },
        output={
            @HandlerOutput(name = "result", type = java.util.List.class)
        })
   public static void getNetworkListeners(HandlerContext handlerCtx) {
       String configName = (String) handlerCtx.getInputValue("configName");
       String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName + "/network-config/network-listeners/network-listener";
       try {
            List<Map> childElements = (List<Map>) RestUtil.buildChildEntityList(endpoint, "", null, null, "name");
            for (Map m: childElements) {
                String name = (String) m.get("protocol");
                if (name.equals(ServerTags.PORT_UNIF_PROTOCOL_NAME)) {
                    m.put("protocol", ServerTags.SEC_ADMIN_LISTENER_PROTOCOL_NAME);
                }
            }
            handlerCtx.setOutputValue("result", childElements);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
   }
}
