package org.apache.cxf.transport.play;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.apache.ws.security.WSPasswordCallback;
 
public class ServerPasswordCallback implements CallbackHandler {
 
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

        String data = pc.getRequestData().getMsgContext().toString();        
        Pattern pattern = Pattern.compile("<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">(.*?)<");
        Matcher matcher = pattern.matcher(data);

        if (matcher.find())
        {
             pc.setPassword( matcher.group(1) );
        }
    }
 
}
