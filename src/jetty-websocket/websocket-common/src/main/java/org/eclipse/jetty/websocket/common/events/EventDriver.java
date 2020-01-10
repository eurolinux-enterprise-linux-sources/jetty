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

package org.eclipse.jetty.websocket.common.events;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.Utf8Appendable.NotUtf8Exception;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.CloseException;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.api.extensions.IncomingFrames;
import org.eclipse.jetty.websocket.common.CloseInfo;
import org.eclipse.jetty.websocket.common.OpCode;
import org.eclipse.jetty.websocket.common.WebSocketFrame;
import org.eclipse.jetty.websocket.common.WebSocketSession;

/**
 * EventDriver is the main interface between the User's WebSocket POJO and the internal jetty implementation of WebSocket.
 */
public abstract class EventDriver implements IncomingFrames
{
    private static final Logger LOG = Log.getLogger(EventDriver.class);
    protected final WebSocketPolicy policy;
    protected final Object websocket;
    protected WebSocketSession session;

    public EventDriver(WebSocketPolicy policy, Object websocket)
    {
        this.policy = policy;
        this.websocket = websocket;
    }

    public WebSocketPolicy getPolicy()
    {
        return policy;
    }

    public WebSocketSession getSession()
    {
        return session;
    }

    @Override
    public final void incomingError(WebSocketException e)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("incoming(WebSocketException)",e);
        }

        if (e instanceof CloseException)
        {
            CloseException close = (CloseException)e;
            terminateConnection(close.getStatusCode(),close.getMessage());
        }

        onError(e);
    }

    @Override
    public void incomingFrame(Frame frame)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("{}.onFrame({})",websocket.getClass().getSimpleName(),frame);
        }

        onFrame(frame);

        try
        {
            switch (frame.getType().getOpCode())
            {
                case OpCode.CLOSE:
                {
                    boolean validate = true;
                    CloseInfo close = new CloseInfo(frame,validate);

                    // notify user websocket pojo
                    onClose(close);

                    // process handshake
                    session.getConnection().getIOState().onCloseRemote(close);

                    return;
                }
                case OpCode.PING:
                {
                    byte pongBuf[] = new byte[0];
                    if (frame.hasPayload())
                    {
                        pongBuf = BufferUtil.toArray(frame.getPayload());
                    }
                    session.getRemote().sendPong(ByteBuffer.wrap(pongBuf));
                    break;
                }
                case OpCode.BINARY:
                {
                    onBinaryFrame(frame.getPayload(),frame.isFin());
                    return;
                }
                case OpCode.TEXT:
                {
                    onTextFrame(frame.getPayload(),frame.isFin());
                    return;
                }
            }
        }
        catch (NotUtf8Exception e)
        {
            terminateConnection(StatusCode.BAD_PAYLOAD,e.getMessage());
        }
        catch (CloseException e)
        {
            terminateConnection(e.getStatusCode(),e.getMessage());
        }
        catch (Throwable t)
        {
            unhandled(t);
        }
    }

    public abstract void onBinaryFrame(ByteBuffer buffer, boolean fin) throws IOException;

    public abstract void onBinaryMessage(byte[] data);

    public abstract void onClose(CloseInfo close);

    public abstract void onConnect();

    public abstract void onError(Throwable t);

    public abstract void onFrame(Frame frame);

    public abstract void onTextFrame(ByteBuffer buffer, boolean fin) throws IOException;

    public abstract void onTextMessage(String message);

    public void openSession(WebSocketSession session)
    {
        LOG.debug("openSession({})",session);
        this.session = session;
        this.onConnect();
    }

    protected void terminateConnection(int statusCode, String rawreason)
    {
        String reason = rawreason;
        reason = StringUtil.truncate(reason,(WebSocketFrame.MAX_CONTROL_PAYLOAD - 2));
        LOG.debug("terminateConnection({},{})",statusCode,rawreason);
        session.close(statusCode,reason);
    }

    private void unhandled(Throwable t)
    {
        LOG.warn("Unhandled Error (closing connection)",t);

        // Unhandled Error, close the connection.
        switch (policy.getBehavior())
        {
            case SERVER:
                terminateConnection(StatusCode.SERVER_ERROR,t.getClass().getSimpleName());
                break;
            case CLIENT:
                terminateConnection(StatusCode.POLICY_VIOLATION,t.getClass().getSimpleName());
                break;
        }
    }
}
