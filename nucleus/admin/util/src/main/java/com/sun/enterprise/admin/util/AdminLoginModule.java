/*
 * Copyright (c) 2023, 2023 Contributors to the Eclipse Foundation. All rights reserved.
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.util;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.config.serverbeans.SecureAdminPrincipal;

import jakarta.inject.Inject;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.glassfish.common.util.admin.AdminAuthenticator;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.common.util.admin.RestSessionManager;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.LocalPassword;
import org.jvnet.hk2.annotations.Service;

/**
 * Handles the non-username/password ways an admin user can authenticate.
 * <p>
 * As specified by the LoginModule contract, the login method creates lists of principals or credentials to be added to
 * the Subject during commit. Only if commit is invoked does the module actually add them to the Subject.
 *
 * @author tjquinn
 */
@Service
@PerLookup
public class AdminLoginModule implements LoginModule {

    private static final Logger logger = GenericAdminAuthenticator.ADMSEC_LOGGER;
    private static final Level PROGRESS_LEVEL = Level.FINE;

    @Inject
    private Domain domain;

    @Inject
    private AuthTokenManager authTokenManager;

    @Inject
    private LocalPassword localPassword;

    @Inject
    private RestSessionManager restSessionManager;

    private SecureAdmin secureAdmin = null;

    private boolean isAuthenticated;

    private Subject subject;
    private CallbackHandler callbackHandler;

    // Holds principals and credentials that should be added to the real
    // subject during commit, if it is ever invoked.
    private final Subject subjectToAssemble = new Subject();

    private final UsernamePasswordAuthenticator usernamePasswordAuth = new UsernamePasswordAuthenticator();
    private final PrincipalAuthenticator principalAuth = new PrincipalAuthenticator();
    private final AdminIndicatorAuthenticator adminIndicatorAuth = new AdminIndicatorAuthenticator();
    private final AdminTokenAuthenticator adminTokenAuth = new AdminTokenAuthenticator();
    private final RemoteHostAuthenticator remoteHostAuth = new RemoteHostAuthenticator();
    private final RestAdminAuthenticator restTokenAuthenticator = new RestAdminAuthenticator();

    private List<Callback> callbacks = new ArrayList<Callback>(Arrays.asList(

            usernamePasswordAuth.nameCB, usernamePasswordAuth.pwCB, principalAuth.cb, adminIndicatorAuth.cb, adminTokenAuth.cb,
            remoteHostAuth.cb, restTokenAuthenticator.restTokenCB, restTokenAuthenticator.remoteAddrCB));

    private List<AdminAuthenticator> authenticators = new ArrayList<AdminAuthenticator>(
            Arrays.asList(usernamePasswordAuth, principalAuth, adminIndicatorAuth, adminTokenAuth, restTokenAuthenticator));

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        if (callbackHandler instanceof AdminCallbackHandler) {
            ServiceLocator sl = ((AdminCallbackHandler) callbackHandler).getServiceLocator();
            findServices(sl);
        }

        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    private void findServices(final ServiceLocator sl) {
        domain = sl.getService(Domain.class);
        secureAdmin = domain.getSecureAdmin();
        authTokenManager = sl.getService(AuthTokenManager.class);
        localPassword = sl.getService(LocalPassword.class);
        restSessionManager = sl.getService(RestSessionManager.class);
    }

    @Override
    public boolean login() throws LoginException {
        /*
        * Without a callback handler we cannot find out what we need about
        * the incoming request.
        */
        if (callbackHandler == null) {
            throw new LoginException(Strings.get("secure.admin.noCallbackHandler"));
        }
        try {
            callbackHandler.handle(callbacks.toArray(new Callback[callbacks.size()]));
        } catch (Exception ex) {
            final LoginException lex = new LoginException();
            lex.initCause(ex);
            throw lex;
        }

        /*
        * Make sure this login module has some way of authenticating this user.
        * Otherwise we don't need it to be invoked during commit or logout.
        */
        isAuthenticated = false;
        for (AdminAuthenticator auth : authenticators) {
            isAuthenticated |= auth.identify(subjectToAssemble);
        }

        logger.log(PROGRESS_LEVEL, "login returning {0}", isAuthenticated);

        if (!isAuthenticated) {
            throw new LoginException();
        }
        return isAuthenticated;
    }

    private void updateFromSubject(final Subject subjectToAddTo, final Subject subjectToAddFrom) {
        subjectToAddTo.getPrincipals().addAll(subjectToAddFrom.getPrincipals());
        subjectToAddTo.getPrivateCredentials().addAll(subjectToAddFrom.getPrivateCredentials());
        subjectToAddTo.getPublicCredentials().addAll(subjectToAddFrom.getPublicCredentials());
    }

    @Override
    public boolean commit() throws LoginException {
        if (!isAuthenticated) {
            return false;
        }
        updateFromSubject(subject, subjectToAssemble);
        logger.log(PROGRESS_LEVEL, "commiting");
        final Level dumpLevel = Level.FINER;
        if (logger.isLoggable(dumpLevel)) {
            logger.log(dumpLevel, "Following identity attached to subject: {0} principals, {1} private credentials, {2} public credentials",
                    new Object[] { subjectToAssemble.getPrincipals().size(), subjectToAssemble.getPrivateCredentials().size(),
                            subjectToAssemble.getPublicCredentials().size() });
            for (Principal p : subjectToAssemble.getPrincipals()) {
                logger.log(dumpLevel, "  principal: {0}", p.getName());
            }
            for (Object c : subjectToAssemble.getPrivateCredentials()) {
                logger.log(dumpLevel, "  private credential: {0}", c.toString());
            }
            for (Object c : subjectToAssemble.getPublicCredentials()) {
                logger.log(dumpLevel, "  public credential: {0}", c.toString());
            }
        }

        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (!isAuthenticated) {
            return false;
        }
        logger.log(PROGRESS_LEVEL, "aborting");
        removeAddedInfo();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        logger.log(PROGRESS_LEVEL, "logging out");
        removeAddedInfo();
        return true;
    }

    private void removeAddedInfo() {
        subject.getPrincipals().removeAll(subjectToAssemble.getPrincipals());
        subject.getPrivateCredentials().removeAll(subjectToAssemble.getPrivateCredentials());
        subject.getPublicCredentials().removeAll(subjectToAssemble.getPublicCredentials());
    }

    static class PrincipalCallback implements Callback, Serializable {

        private static final long serialVersionUID = 1L;

        private Principal p;

        public void setPrincipal(final Principal p) {
            this.p = p;
        }

        public Principal getPrincipal() {
            return p;
        }
    }

    /*
    * If the admin client sent the unique domain identifier in a header then
    * that should mean the request came from another GlassFish server in this
    * domain. Make sure that the value, if present, matches the one in
    * this server's domain config.  If they do not match then reject the
    * message - it came from a domain other than this server's.
    *
    * Note that we don't insist that every request have the domain identifier.  For
    * example, requests from asadmin will not include the domain ID.  But if
    * the domain ID is present in the request it needs to match the
    * configured ID.
    */
    private static class SpecialAdminIndicatorChecker {

        private static enum Result {
            NOT_IN_REQUEST, MATCHED, MISMATCHED
        }

        private final SpecialAdminIndicatorChecker.Result _result;

        private SpecialAdminIndicatorChecker(final String actualIndicator, final String expectedIndicator, final String originHost) {
            final Level dumpLevel = Level.FINER;
            if (actualIndicator != null) {
                if (actualIndicator.equals(expectedIndicator)) {
                    _result = SpecialAdminIndicatorChecker.Result.MATCHED;
                    logger.log(dumpLevel, "Admin request contains expected domain ID");
                } else {
                    logger.log(Level.WARNING, AdminLoggerInfo.mForeignDomainID,
                            new Object[] { originHost, actualIndicator, expectedIndicator });
                    _result = SpecialAdminIndicatorChecker.Result.MISMATCHED;
                }
            } else {
                logger.log(dumpLevel, "Admin request contains no domain ID; this is OK - continuing");
                _result = SpecialAdminIndicatorChecker.Result.NOT_IN_REQUEST;
            }
        }

        private SpecialAdminIndicatorChecker.Result result() {
            return _result;
        }
    }

    abstract class Authenticator implements AdminAuthenticator {
        private final AuthenticatorType type;
        final Callback cb;

        Authenticator(final AuthenticatorType type, final Callback cb) {
            this.type = type;
            this.cb = cb;
        }

        @Override
        public List<Callback> callbacks() {
            return new ArrayList<Callback>(Arrays.asList(cb));
        }

        @Override
        public AuthenticatorType type() {
            return type;
        }

    }

    abstract class TextAuthenticator extends Authenticator {
        final TextInputCallback textCB;

        TextAuthenticator(final AuthenticatorType type) {
            super(type, new TextInputCallback(type.name()));
            textCB = (TextInputCallback) cb;
        }
    }

    class PrincipalAuthenticator extends Authenticator {

        final private PrincipalCallback pcb;

        PrincipalAuthenticator() {
            super(AuthenticatorType.PRINCIPAL, new PrincipalCallback());
            pcb = (PrincipalCallback) cb;
        }

        @Override
        public boolean identify(Subject subject) {
            /*
             * There are three ways certs can be attached to the request:
             * 1. A non-GlassFish client (a human) actually sent a cert.
             *
             * 2. One server is sending another server a request.  In this case
             * the cert's DN should be among the secure-admin-principals.
             *
             * 3. The request came from the console and secure admin is enabled,
             * in which case (because the console runs in the DAS
             * and sends ReST requests over the net to the DAS) the request
             * will have both the DAS's cert and the user's username and password.
             */

            final Principal p = pcb.getPrincipal();
            if (p != null) {
                if (isPrincipalFromGlassFish(p) && usernamePasswordAuth.isActive()) {
                    /*
                     * This is the console case. Do not indicate that the client
                     * is identifiable using an SSL cert.  Instead rely on
                     * the username/password authentication code path.
                     */
                    logger.log(PROGRESS_LEVEL, "Detected console request - not adding SSL principal to the subject");
                    return false;
                }
                /*
                 * In all other cases add the principal to the tentative subject.
                 */
                subject.getPrincipals().add(p);
                logger.log(PROGRESS_LEVEL, "Attaching Principal {0}", p.getName());

            }
            return p != null;
        }

        private boolean isPrincipalFromGlassFish(final Principal p) {
            for (SecureAdminPrincipal sap : secureAdmin.getSecureAdminPrincipal()) {
                if (sap.getDn().equals(p.getName())) {
                    return true;
                }
            }
            return false;
        }

    }

    class AdminIndicatorAuthenticator extends TextAuthenticator {

        AdminIndicatorAuthenticator() {
            super(AuthenticatorType.ADMIN_INDICATOR);
        }

        @Override
        public boolean identify(Subject subject) throws LoginException {
            if (secureAdmin == null) {
                return false;
            }
            final String providedIndicator = textCB.getText();
            final SpecialAdminIndicatorChecker checker = new SpecialAdminIndicatorChecker(providedIndicator,
                    secureAdmin.getSpecialAdminIndicator(), remoteHostAuth.textCB.getText());
            if (checker.result() == SpecialAdminIndicatorChecker.Result.MISMATCHED) {
                throw new LoginException();
            }
            /*
                * Either there was no special indicator or there was one and
                * it matched what we expect.
                */
            if (checker.result() == SpecialAdminIndicatorChecker.Result.MATCHED) {
                /*
                 * Add a principal indicating that this subject represents
                 * another server in the domain and so we
                 * can trust it completely, even if the request is remote and secure
                 * admin is disabled.
                 */
                subject.getPrincipals().add(new AdminIndicatorPrincipal(providedIndicator));
                return true;
            }
            return false;
        }
    }

    class AdminTokenAuthenticator extends TextAuthenticator {
        AdminTokenAuthenticator() {
            super(AuthenticatorType.ADMIN_TOKEN);
        }

        @Override
        public boolean identify(Subject subject) throws LoginException {
            if (authTokenManager == null) {
                return false;
            }
            Subject s = null;
            final String token = textCB.getText();
            if (token != null) {
                s = authTokenManager.findToken(token);
                if (s != null) {

                    /*
                    * The token manager knows which Subject was effective when the token
                    * was created.  We add those to the lists we'll add if this module's
                    * commit is invoked.
                    */
                    logger.log(PROGRESS_LEVEL, "Recognized valid limited-use token");
                    updateFromSubject(subject, s);
                    /*
                     * Add an additional principal indicating that we trust this
                     * subject to make remote requests.  Otherwise we would
                     * reject attempts to use a token from off-node, and that's
                     * partly the whole point of tokens.
                     */
                    subject.getPrincipals().add(new AdminTokenPrincipal(token));

                }
            }
            return s != null;
        }
    }

    class UsernamePasswordAuthenticator extends Authenticator {
        final NameCallback nameCB = new NameCallback("username");
        final PasswordCallback pwCB = new PasswordCallback("password", false);

        UsernamePasswordAuthenticator() {
            super(AuthenticatorType.USERNAME_PASSWORD, null);
        }

        boolean isActive() {
            return nameCB.getName() != null || pwCB.getPassword() != null;
        }

        @Override
        public boolean identify(final Subject subject) throws LoginException {
            /*
                * Note that this LoginModule does not authenticate the normal
                * username/password pairs.  That's left to another one.  This module
                * checks for the local password.
                */
            if (localPassword == null) {
                return false;
            }
            final boolean result = localPassword.isLocalPassword(new String(pwCB.getPassword()));
            if (result) {
                subject.getPrincipals().add(new AdminLocalPasswordPrincipal());
                logger.log(PROGRESS_LEVEL, "AdminLoginModule detected local password");
            }
            return result;
        }

        @Override
        public List<Callback> callbacks() {
            return new ArrayList<Callback>(Arrays.asList(nameCB, pwCB));
        }
    }

    class RemoteHostAuthenticator extends TextAuthenticator {
        RemoteHostAuthenticator() {
            super(AuthenticatorType.REMOTE_HOST);
        }

        @Override
        public boolean identify(final Subject subject) throws LoginException {
            return false;
        }
    }

    class RestAdminAuthenticator extends Authenticator {

        private TextInputCallback restTokenCB = new TextInputCallback(AdminAuthenticator.REST_TOKEN_NAME);
        private TextInputCallback remoteAddrCB = new TextInputCallback(AdminAuthenticator.REMOTE_ADDR_NAME);

        RestAdminAuthenticator() {
            super(AuthenticatorType.REST_TOKEN, null);
        }

        @Override
        public List<Callback> callbacks() {
            return new ArrayList<Callback>(Arrays.asList(cb, remoteAddrCB));
        }

        @Override
        public boolean identify(final Subject subject) throws LoginException {
            if (restSessionManager == null) {
                return false;
            }
            boolean result = false;
            final String token = restTokenCB.getText();
            final String remoteAddr = remoteAddrCB.getText();
            if (token != null && remoteAddr != null) {
                final Subject s = restSessionManager.authenticate(token, remoteAddr);
                if (s != null) {
                    result = true;
                    updateFromSubject(subject, s);
                    logger.log(PROGRESS_LEVEL, "Detected ReST token");
                }
            }
            return result;
        }
    }
}
