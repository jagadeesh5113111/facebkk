/**
 * Copyright 2015 Santhosh Kumar Tekuri
 *
 * The JLibs authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package jlibs.wamp4j;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jlibs.wamp4j.msg.AbortMessage;
import jlibs.wamp4j.msg.ErrorMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * @author Santhosh Kumar Tekuri
 */
public class ErrorCode{
    private static final Pattern regex = Pattern.compile("\\{(.*?)\\}");

    public final String uri;
    public final ArrayNode arguments;
    public final ObjectNode argumentsKw;

    private ErrorCode(String uri, String message, Object... params){
        this.uri = uri;
        if(params.length==0)
            argumentsKw = null;
        else{
            argumentsKw = instance.objectNode();
            StringBuilder buff = new StringBuilder();
            Matcher matcher = regex.matcher(message);
            int cursor = 0;
            int iparam = 0;
            while(cursor<message.length() && matcher.find(cursor)){
                buff.append(message.subSequence(cursor, matcher.start()));
                String name = matcher.group(1);
                Object value = params[iparam++];
                if(value instanceof String)
                    argumentsKw.put(name, (String)value);
                else if(value instanceof Long)
                    argumentsKw.put(name, (Long)value);
                else
                    argumentsKw.putPOJO(name, value);
                buff.append(value);
                cursor = matcher.end();
            }
            buff.append(message.subSequence(cursor, message.length()));
            message = buff.toString();
        }

        if(message==null)
            arguments = null;
        else{
            arguments = instance.arrayNode();
            arguments.add(message);
        }
    }

    public ErrorCode(String uri){
        this(uri, null);
    }

    public ErrorCode(ErrorMessage msg){
        uri = msg.error;
        arguments = msg.arguments;
        argumentsKw = msg.argumentsKw;
    }

    public ErrorCode(AbortMessage msg){
        uri = msg.reason;
        arguments = null;
        argumentsKw = null;
    }

    public String message(){
        return arguments!=null && arguments.size()>0 && arguments.get(0).isTextual() ? arguments.get(0).asText() : uri;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        ErrorCode errorCode = (ErrorCode)o;

        if(uri != null ? !uri.equals(errorCode.uri) : errorCode.uri != null) return false;
        if(arguments != null ? !arguments.equals(errorCode.arguments) : errorCode.arguments != null) return false;
        return !(argumentsKw != null ? !argumentsKw.equals(errorCode.argumentsKw) : errorCode.argumentsKw != null);

    }

    @Override
    public int hashCode(){
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        result = 31 * result + (argumentsKw != null ? argumentsKw.hashCode() : 0);
        return result;
    }

/*-------------------------------------------------[ Interactions ]---------------------------------------------------*/

    /**
     * Peer provided an incorrect URI for any URI-based attribute of WAMP message, such as realm, topic or procedure
     */
    public static ErrorCode invalidURI(String uri, String entity){
        return new ErrorCode("wamp.error.invalid_uri", "invalid uri {uri} for {entity}", uri, entity);
    }

    /**
     * A Dealer could not perform a call, since no procedure is currently registered under the given URI
     */
    public static ErrorCode noSuchProcedure(String procedure){
        return new ErrorCode("wamp.error.no_such_procedure", "no such procedure {procedure}", procedure);
    }

    /**
     * A procedure could not be registered, since a procedure with the given URI is already registered
     */
    public static ErrorCode procedureAlreadyExists(String procedure){
        return new ErrorCode("procedure_already_exists", "procedure {procedure} already exists", procedure);
    }

    /**
     * A Dealer could not perform an unregister, since the given registration is not active
     */
    public static ErrorCode noSuchRegistration(long registrationID){
        return new ErrorCode("wamp.error.no_such_registration", "no such registion {registrationID}", registrationID);
    }

    /**
     * A Broker could not perform an unsubscribe, since the given subscription is not active
     */
    public static ErrorCode noSuchSubscription(long subscriptionID){
        return new ErrorCode("wamp.error.no_such_subscription", "no such subscription {subscriptionID}", subscriptionID);
    }

    /**
     * A call failed, since the given argument types or values are not acceptable to the called procedure.
     * In which case the Callee may throw this error.
     * Or
     * a Router performing payload validation checked the payload (args / kwargs) of a call, call result, call error
     * or publish, and the payload did not conform - in which case the Router may throw this error
     */
    public static ErrorCode invalidArgument(){
        return new ErrorCode("wamp.error.invalid_argument");
    }

    /*-------------------------------------------------[ Session Close ]---------------------------------------------------*/

    /**
     * The Peer is shutting down completely - used as a GOODBYE (or ABORT) reason
     */
    public static ErrorCode systemShutdown(){
        return new ErrorCode("wamp.error.system_shutdown");
    }

    /**
     * The Peer want to leave the realm - used as a GOODBYE reason
     */
    public static final String CLOSE_REALM = "wamp.error.close_realm";

    /**
     * A Peer acknowledges ending of a session - used as a GOODBYE reply reason
     */
    public static final String GOODBYE_AND_OUT = "wamp.error.goodbye_and_out";

    /*-------------------------------------------------[ Authorization ]---------------------------------------------------*/

    /**
     * A join, call, register, publish or subscribe failed, since the Peer is not authorized to perform the operation
     */
    public static ErrorCode notAuthorized(){
        return new ErrorCode("wamp.error.not_authorized");
    }

    /**
     * A Dealer or Broker could not determine if the Peer is authorized to perform a join, call, register, publish or subscribe,
     * since the authorization operation itself failed. E.g. a custom authorizer did run into an error
     */
    public static ErrorCode authorizationFailed(){
        return new ErrorCode("wamp.error.authorization_failed");
    }

    /**
     * Peer wanted to join a non-existing realm (and the Router did not allow to auto-create the realm)
     */
    public static ErrorCode noSuchRealm(String realm){
        return new ErrorCode("wamp.error.no_such_realm", "no such realm {realm}", realm);
    }

    /**
     * A Peer was to be authenticated under a Role that does not (or no longer) exists on the Router.
     * For example, the Peer was successfully authenticated, but the Role configured does not exists - hence
     * there is some misconfiguration in the Router
     */
    public static ErrorCode noSuchRole(String role){
        return new ErrorCode("wamp.error.no_such_role", "no such role {role}", role);
    }

    /*-------------------------------------------------[ Custom ]---------------------------------------------------*/

    public static ErrorCode invalidMessage(){
        return new ErrorCode("jlibs.wamp4j.invalid_message");
    }

    public static ErrorCode serializationFailed(){
        return new ErrorCode("jlibs.wamp4j.serialization_failed");
    }

    public static ErrorCode notConnected(){
        return new ErrorCode("jlibs.wamp4j.not_connected");
    }

    public static ErrorCode unexpectedError(){
        return new ErrorCode("jlibs.wamp4j.unexpected_error");
    }

    public static ErrorCode wrongThread(){
        return new ErrorCode("jlibs.wamp4j.wrong_thread");
    }
}
