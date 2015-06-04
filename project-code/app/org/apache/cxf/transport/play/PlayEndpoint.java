package org.apache.cxf.transport.play;

public interface PlayEndpoint {
	PlayEndpoint getEndpoint();
	Class getInterface();
	Object getImplementor();
	String getTransportId();
	String getAddress();
	String getName();
	Boolean useWSSecurity();
}