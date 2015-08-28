package org.apache.cxf.transport.play

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import javax.inject.Inject
import javax.xml.namespace.QName
import java.util.logging.Logger
import java.util.HashMap

import org.apache.cxf.common.logging.LogUtils
import org.apache.cxf.message.{MessageImpl, Message}
import org.apache.cxf.jaxws.JaxWsServerFactoryBean
import org.apache.ws.security.WSConstants
import org.apache.ws.security.handler.WSHandlerConstants
import org.apache.cxf.interceptor.LoggingInInterceptor
import org.apache.cxf.interceptor.LoggingOutInterceptor
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Enumerator
import play.api.mvc._

import scala.concurrent.Promise
import scala.collection.JavaConverters._


object CxfController extends Controller {

  def LOG = LogUtils.getL7dLogger(CxfController.getClass);
  /**
   * Factory method for Spring.
   */
  def getInstance() = this

  /**
   * Apache CXF transport factory, set by Spring.
   */
  var transportFactory: PlayTransportFactory = null

  var endpoints: java.util.List[PlayEndpoint] = null

  val maxRequestSize = 1024 * 1024

  def handle(path: String) = Action.async(parse.raw(maxRequestSize)) { implicit request =>
    val delayedOutput = new DelayedOutputStream
    val replyPromise: Promise[Message] = Promise.apply()
    dispatchMessage(extractMessage, delayedOutput, replyPromise)

    val resultEnumerator = Enumerator.outputStream { os =>
      delayedOutput.setTarget(os)
    }
    replyPromise.future.map { outMessage =>
      Ok.chunked(resultEnumerator >>> Enumerator.eof) withHeaders(
        Message.CONTENT_TYPE -> outMessage.get(Message.CONTENT_TYPE).asInstanceOf[String]
      )
    }
  }

  private def extractMessage()(implicit request: Request[RawBuffer]) = {
    val msg: Message = new MessageImpl
    msg.put(Message.HTTP_REQUEST_METHOD, request.method)
    msg.put(Message.REQUEST_URL, request.path)
    msg.put(Message.QUERY_STRING, request.rawQueryString)
    msg.put(Message.PROTOCOL_HEADERS, headersAsJava)
    msg.put(Message.CONTENT_TYPE, request.headers.get(Message.CONTENT_TYPE) getOrElse null)
    msg.put(Message.ACCEPT_CONTENT_TYPE, request.headers.get(Message.ACCEPT_CONTENT_TYPE) getOrElse null)
    msg.put("Remote-Address", request.remoteAddress)

    request.body.asBytes() foreach { arr: Array[Byte] =>
      msg.setContent(classOf[InputStream], new ByteArrayInputStream(arr))
    }

    msg
  }

  private def endpointAddress()(implicit request: Request[RawBuffer]) = "play://" + request.host + request.path

  private def headersAsJava()(implicit request: Request[RawBuffer]) =
    request.headers.toMap.mapValues { s: Seq[String] =>
      s.asJava
    }.asJava

  private def dispatchMessage(inMessage: Message,
                              output: OutputStream,
                              replyPromise: Promise[Message])
                             (implicit request: Request[RawBuffer]) {
    val dOpt = Option(transportFactory.getDestination(endpointAddress)).orElse(
        Option(transportFactory.getDestination(request.path)))
    dOpt match {
      case Some(destination) => {
        inMessage.put(Message.ENDPOINT_ADDRESS, destination.getFactoryKey)
        destination.dispatchMessage(inMessage, output, replyPromise)
      }
      case _ =>
        replyPromise.failure(new IllegalArgumentException("Destination not found: [" + endpointAddress +
          "] " + transportFactory.getDestinationsDebugInfo))
    }
  }

  @Inject
  def setTransportFactory(factory: PlayTransportFactory) {
    this.transportFactory = factory
  }

  @Inject
  def setEndpoints(endpoints: java.util.List[PlayEndpoint]) = {
    this.endpoints = endpoints
    
    for (endpoint: PlayEndpoint <- endpoints.asScala.toSet) {
      var sf: JaxWsServerFactoryBean = new JaxWsServerFactoryBean()
      sf.setServiceClass(endpoint.getInterface)
      sf.setAddress(endpoint.getAddress)
      sf.setTransportId(endpoint.getTransportId)
      var cxfEndpoint = sf.create.getEndpoint

      if (endpoint.useWSSecurity) {
        var inProps = new HashMap[String,Object]()
        inProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN)
        inProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT)
        inProps.put(WSHandlerConstants.PW_CALLBACK_REF, new ServerPasswordCallback())
        inProps.put(WSHandlerConstants.ACTOR, WSConstants.URI_SOAP11_NEXT_ACTOR)
        
        var wssIn = new WSS4JInInterceptor(inProps)

        // Do not let this go out of order.
        cxfEndpoint.getInInterceptors().add(new WSSecurityNamespaceInInterceptor())
        cxfEndpoint.getInInterceptors().add(wssIn)
        cxfEndpoint.getInInterceptors().add(new LoggingInInterceptor())
        cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor())
      }
    }
  }

}
