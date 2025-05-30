/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

package org.glassfish.main.test.app.security.jakartasecurity;

import jakarta.annotation.security.DeclareRoles;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import static jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters.withParams;

/**
 * Test Servlet that authenticates that authenticates the request and verifies that GlassFish SecurityContext can be serialized
 */
@DeclareRoles("admin")
@WebServlet("/serializableCoreSecurityContext")
public class SerializableCoreSecurityContextServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private SecurityContext securityContext;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        securityContext.authenticate(request, response, withParams());

        response.setContentType("text/plain");

        final ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());

        try {
        // verify internal core security context is serializable so that it can be used in SSO
        oos.writeObject(com.sun.enterprise.security.SecurityContext.getCurrent());
        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().write("Exception " + e);
            return;
        }

        response.getWriter().write("OK");
    }

}
