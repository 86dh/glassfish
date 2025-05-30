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

package org.glassfish.appclient.client.acc.callbackhandler;

import com.sun.enterprise.security.GUILoginDialog;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.glassfish.appclient.client.acc.callbackhandler.CallbackGUIBindings.MessageType;
import org.glassfish.main.jdke.i18n.LocalStringsImpl;

/**
 * Example callback handler for displaying and gathering information from the
 * user on behalf of the security system.
 * <p>
 * This callback handler works with all known types of callback handlers (as of
 * this writing).  It basically builds a component for each callback that the
 * caller provides and passes them all to a JOptionPane, which displays all the
 * components and accepts the user's ending action (such as OK, Cancel, etc.).
 * <p>
 * Normally the caller passes a ConfirmationCallback to tell what kind of
 * responses should be expected and accepted from the user (OK/Cancel, Yes/No,
 * Yes/No/Cancel, etc.).  If the caller does not provide one this handler
 * uses a default ConfirmationCallback with "OK" as the only choice.
 * In such a case the user has no option to cancel, although he or she can
 * always simply close the option dialog box.
 * <p>
 * The dialog box includes a system-selected icon based on the message type
 * specified when the JOptionPane is created.  The message type for the dialog
 * box (INFORMATION, WARNING, ERROR) is computed by choosing the most severe
 * message type from ConfirmationCallbacks or TextOutputCallbacks passed by the
 * caller.
 * <p>
 * Whenever the user dismisses the dialog box, whether by choosing one of the
 * confirmation choices (such as OK or Cancel) or by closing the window, the
 * handler updates each of the callbacks as appropriate with information from
 * the corresponding U/I components.  The caller should provide a
 * ConfirmationCallback if it needs to distinguish among the possible responses
 * from the user.
 * <p>
 * Each type of callback is associated with a corresponding type of
 * callback-to-U/I binding.  Each binding creates a JComponent,
 * specific to its type of callback, that displays information from the callback
 * or collects information from the user, perhaps using a prompt or initial
 * value from the callback.  Each type of binding also implements the finish
 * method which updates the corresponding callback, if appropriate, with
 * data from the U/I component.
 *
 * @author tjquinn
 */
public class DefaultGUICallbackHandler implements javax.security.auth.callback.CallbackHandler {
    /* records which confirmation callback binding to use - either the last
     * one passed by the caller or the default */
    private CallbackGUIBindings.Confirmation confirmationCallbackGUIBinding = null;

    /* most severe message type found among the callbacks with a message type,
     * such as ConfirmationCallback and TextOutputCallback
     */
    private MessageType messageType;

    private LocalStringsImpl localStrings = new LocalStringsImpl(DefaultGUICallbackHandler.class);

    /**
     * Handles the caller-requested callbacks.
     * @param callbacks the Callback objects to be processed
     * @throws java.io.IOException
     * @throws UnsupportedCallbackException if this handler does not support the
     * specified callback
     */
    public void handle(Callback[] callbacks)
            throws IOException, UnsupportedCallbackException {
        new GUILoginDialog(localStrings.get("dialog.user"), callbacks);
//        messageType = MessageType.PLAIN;
//
//        /*
//         * Record all the callback-to-U/I bindings created, as well as all the
//         * components in a separate data structure so an array of JComponents
//         * can be passed to the JOptionPane.
//         */
//        ArrayList<CallbackGUIBindings.Binding> bindings = new ArrayList<CallbackGUIBindings.Binding>();
//        ArrayList<JComponent> components = new ArrayList<JComponent>();
//        try {
//            prepareComponentBindings(callbacks, bindings, components);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        /*
//         * Decide what the message type should be for the dialog box, based on
//         * the message types from the callbacks that have one.
//         */
//        for (CallbackGUIBindings.Binding binding : bindings) {
//            MessageType bindingType = binding.getMessageType();
//            if ( ! messageType.exceeds(bindingType)) {
//                messageType = bindingType;
//            }
//        }
//
//        /*
//         * Create the JOptionPane using the assembled U/I components for
//         * the callbacks requested.  By this time the binding for either a
//         * caller-specified ConfirmationCallback or the handler-supplied
//         * default one has been assigned to confirmationCallbackUIBinding.  Let
//         * it map from the Callback-defined option types to the JOptionPane
//         * option types.  (The option types indicate whether to display
//         * OK/Cancel, Yes/No, etc.)  Also use the most severe message type
//         * specified by any of the callbacks capable of doing so.
//         */
//        JOptionPane optionPane = new JOptionPane(
//                components.toArray(new JComponent[components.size()]),
//                confirmationCallbackGUIBinding.getOptionPaneOptionType(),
//                messageType.getOptionPaneMessageType(),
//                null,
//                confirmationCallbackGUIBinding.getOptions(),
//                confirmationCallbackGUIBinding.getDefaultOption()
//                );
//
//        JDialog dialog = optionPane.createDialog(
//                null,
//                StringManager.getString("dialog.title"));
//        dialog.setResizable(true);
//        /*
//         * The setVisible invocation blocks until the user clicks on one of
//         * the buttons or closes the window
//         */
//        dialog.setVisible(true);
//
//        int response = computeResponseValue(optionPane);
//        dialog.setVisible(false);
//        dialog.dispose();
//
//        /*
//         * Give each binding a chance to update the callback with information
//         * now available from its corresponding U/I component.
//         */
//        for (CallbackGUIBindings.Binding binding : bindings) {
//            binding.finish();
//        }
//
//        /*
//         * Convert the JOptionPane's result value to one appropriate to the
//         * ConfirmationCallback's set of possible values.
//         */
//        confirmationCallbackGUIBinding.setResult(response);
//    }
//
//    private int computeResponseValue(JOptionPane pane) {
//        Object selectedValue = pane.getValue();
//        if(selectedValue == null)
//            return JOptionPane.CLOSED_OPTION;
//        if(pane.getOptions() == null) {
//            if(selectedValue instanceof Integer)
//                return ((Integer)selectedValue).intValue();
//            return JOptionPane.CLOSED_OPTION;
//        }
//
//        for(int counter = 0, maxCounter = pane.getOptions().length;
//            counter < maxCounter; counter++) {
//            if(pane.getOptions()[counter].equals(selectedValue))
//                return counter;
//        }
//        return JOptionPane.CLOSED_OPTION;
//    }
//
//    /**
//     * Populates the collection of bindings and components for each callback.
//     * @param callbacks the Callbacks provided by the caller
//     * @param bindings - Collection of bindings of callbacks to U/I components
//     * @param components - Collection of U/I components for display in the JOptionPane
//     * @throws javax.security.auth.callback.UnsupportedCallbackException
//     */
//    private void prepareComponentBindings(
//            Callback[] callbacks,
//            Collection<CallbackGUIBindings.Binding> bindings,
//            Collection<JComponent> components) throws UnsupportedCallbackException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
//
//        CallbackGUIBindings factory = new CallbackGUIBindings();
//        for (Callback callback : callbacks) {
//            CallbackGUIBindings.Binding binding = factory.createCallbackGUIBinding(callback);
//            bindings.add(binding);
//            if (binding instanceof CallbackGUIBindings.Confirmation) {
//                /*
//                 * Do not add the confirmation callback's component to the
//                 * list of components to be part of the option dialog that
//                 * will be displayed.  Instead the option dialog itself will
//                 * be set up to use the options specified by the (last)
//                 * ConfirmationCallback.
//                 */
//                confirmationCallbackGUIBinding =
//                        (CallbackGUIBindings.Confirmation) binding;
//            } else {
//                components.add(binding.getComponent());
//            }
//
//            /*
//             * Make sure there is at least one confirmation callback binding
//             * so we know what choices to offer the user if the caller did not
//             * specify them.
//             */
//            if (confirmationCallbackGUIBinding == null) {
//                confirmationCallbackGUIBinding =
//                        factory.getDefaultConfirmationCallbackUIBinding();
//            }
//        }
    }
}
