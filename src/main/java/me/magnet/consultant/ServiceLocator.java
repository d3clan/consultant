package me.magnet.consultant;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A class which implements an Iterator style pattern allowing the user of this class to fetch service instances in a
 * particular order one-by-one.
 */
public class ServiceLocator {

	private final Supplier<Iterator<ServiceInstance>> instanceSupplier;
	private final Supplier<ServiceLocator> fallbackSupplier;

	private Iterator<ServiceInstance> instances;
	private ServiceLocator fallback;

	private Consumer<ServiceInstance> listener;

	/**
	 * Creates a new ServiceLocator object returning only service instances which are generated by the
	 * instanceSupplier.
	 *
	 * @param instanceSupplier A Supplier of a ServiceInstance Iterator returning service instances to emit.
	 */
	ServiceLocator(Supplier<Iterator<ServiceInstance>> instanceSupplier) {
		this(instanceSupplier, (ServiceLocator) null);
	}

	/**
	 * Creates a new ServiceLocator object returning only service instances which are generated by the instanceSupplier
	 * and then falls back onto another ServiceLocator for emitting service instances.
	 *
	 * @param instanceSupplier A Supplier of a ServiceInstance Iterator returning service instances to emit.
	 * @param fallback         The ServiceLocator to use as when all instances of the instanceSupplier were emitted.
	 */
	ServiceLocator(Supplier<Iterator<ServiceInstance>> instanceSupplier, ServiceLocator fallback) {
		this(instanceSupplier, () -> fallback);
	}

	/**
	 * Creates a new ServiceLocator object returning only service instances which are generated by the instanceSupplier
	 * and then falls back onto another ServiceLocator for emitting service instances.
	 *
	 * @param instanceSupplier A Supplier of a ServiceInstance Iterator returning service instances to emit.
	 * @param fallbackSupplier The ServiceLocator to use as when all instances of the instanceSupplier were emitted.
	 */
	ServiceLocator(Supplier<Iterator<ServiceInstance>> instanceSupplier,
			Supplier<ServiceLocator> fallbackSupplier) {
		this.instanceSupplier = instanceSupplier;
		this.fallbackSupplier = fallbackSupplier;
	}

	/**
	 * Ensure that a certain callback is called when emitting a particular service instance. This can be used to keep
	 * track of which service instances have been emitted in a RoutingStrategy implementation.
	 *
	 * @param listener The consumer of the service instance emit event.
	 *
	 * @return This ServiceLocator object.
	 */
	ServiceLocator setListener(Consumer<ServiceInstance> listener) {
		this.listener = listener;
		if (fallback != null) {
			fallback.setListener(listener);
		}
		return this;
	}

	/**
	 * Maps the batches of service instances emitted by this ServiceLocator, to a differently ordered batch of those
	 * same service instances. This can be used to reorder the service instances, and ensure that with regards to the
	 * client-side load balancing a different instance is used first.
	 *
	 * @param mapper Function which maps one ordered batch of instances to a differently ordered batch of those same
	 *               instances.
	 *
	 * @return The newly created ServiceLocator object.
	 */
	ServiceLocator map(Function<Iterator<ServiceInstance>, Iterator<ServiceInstance>> mapper) {
		if (fallback != null) {
			return new ServiceLocator(() -> mapper.apply(instanceSupplier.get()), fallback.map(mapper));
		}
		return new ServiceLocator(() -> mapper.apply(instanceSupplier.get()));
	}

	/**
	 * @return The next ServiceInstance which can be used for client-side load balancing. Will return an empty
	 * Optional in case no service instance is available, or all service instances have been fetched already.
	 */
	public Optional<ServiceInstance> next() {
		if (instances == null) {
			instances = instanceSupplier.get();
		}
		if (instances.hasNext()) {
			ServiceInstance instance = instances.next();
			if (listener != null) {
				listener.accept(instance);
			}
			return Optional.of(instance);
		}

		if (fallback == null) {
			fallback = fallbackSupplier.get();
		}
		if (fallback != null) {
			return fallback.next();
		}

		return Optional.empty();
	}

}
