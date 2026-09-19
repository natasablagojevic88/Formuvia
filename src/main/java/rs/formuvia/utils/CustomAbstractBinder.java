package rs.formuvia.utils;

import org.glassfish.jersey.inject.hk2.AbstractBinder;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Singleton;

public class CustomAbstractBinder extends AbstractBinder {

	@SuppressWarnings("unchecked")
	@Override
	protected void configure() {
		for (Class<?> inClass : StaticData.allClasses) {
			if (!inClass.isAnnotationPresent(Service.class)) {
				continue;
			}

			Class<?>[] interfaceClasses = inClass.getInterfaces();
			for (Class<?> interfaceClass : interfaceClasses) {
				bind(inClass).to(interfaceClass).in(Singleton.class);
			}
		}

	}

}
