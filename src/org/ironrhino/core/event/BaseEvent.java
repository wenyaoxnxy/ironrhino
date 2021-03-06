package org.ironrhino.core.event;

import org.ironrhino.core.util.AppInfo;
import org.springframework.context.ApplicationEvent;

public class BaseEvent<T> extends ApplicationEvent {

	private static final long serialVersionUID = -2892858943541156897L;

	private String instanceId = AppInfo.getInstanceId();

	protected T source;

	public BaseEvent(T source) {
		super(source);
		this.source = source;
	}

	@Override
	public T getSource() {
		return source;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public boolean isLocal() {
		return getInstanceId().equals(AppInfo.getInstanceId());
	}

}
