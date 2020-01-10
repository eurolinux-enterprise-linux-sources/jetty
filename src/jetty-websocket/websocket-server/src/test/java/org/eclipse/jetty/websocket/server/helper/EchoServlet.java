//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.server.helper;

import org.eclipse.jetty.websocket.common.extensions.compress.FrameCompressionExtension;
import org.eclipse.jetty.websocket.common.extensions.compress.MessageCompressionExtension;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * Initialize a simple Echo websocket
 */
@SuppressWarnings("serial")
public class EchoServlet extends WebSocketServlet
{
    @Override
    public void configure(WebSocketServletFactory factory)
    {
        // Setup some extensions we want to test against
        factory.getExtensionFactory().register("x-webkit-deflate-frame",FrameCompressionExtension.class);
        factory.getExtensionFactory().register("permessage-compress",MessageCompressionExtension.class);

        // Setup the desired Socket to use for all incoming upgrade requests
        factory.register(EchoSocket.class);
    }
}
