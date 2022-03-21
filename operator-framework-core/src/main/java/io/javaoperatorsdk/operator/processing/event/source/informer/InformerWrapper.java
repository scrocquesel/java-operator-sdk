package io.javaoperatorsdk.operator.processing.event.source.informer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.javaoperatorsdk.operator.OperatorException;
import io.javaoperatorsdk.operator.processing.LifecycleAware;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.ResourceCache;
import io.javaoperatorsdk.operator.processing.event.source.UpdatableCache;

class InformerWrapper<T extends HasMetadata>
    implements LifecycleAware, ResourceCache<T>, UpdatableCache<T> {
  private final Supplier<SharedIndexInformer<T>> informerSupplier;
  private final List<ResourceEventHandler<T>> eventHandlers = new ArrayList<>();
  private InformerResourceCache<T> cache;
  private SharedIndexInformer<T> informer;

  public InformerWrapper(Supplier<SharedIndexInformer<T>> informerSupplier) {
    this.informerSupplier = informerSupplier;
  }

  @Override
  public void start() throws OperatorException {
    informer = informerSupplier.get();
    eventHandlers.forEach(eh -> informer.addEventHandler(eh));
    cache = new InformerResourceCache<>(informer);
    informer.run();
  }

  @Override
  public void stop() throws OperatorException {
    if (informer != null) {
      informer.stop();
      informer = null;
      cache = null;
    }
  }

  @Override
  public Optional<T> get(ResourceID resourceID) {
    return cache.get(resourceID);
  }

  @Override
  public boolean contains(ResourceID resourceID) {
    return cache.contains(resourceID);
  }

  @Override
  public Stream<ResourceID> keys() {
    return cache.keys();
  }

  @Override
  public Stream<T> list() {
    return cache.list();
  }

  @Override
  public Stream<T> list(Predicate<T> predicate) {
    return cache.list(predicate);
  }

  @Override
  public Stream<T> list(String namespace) {
    return cache.list(namespace);
  }

  @Override
  public Stream<T> list(String namespace, Predicate<T> predicate) {
    return cache.list(namespace, predicate);
  }

  public void addEventHandler(ResourceEventHandler<T> eventHandler) {
    eventHandlers.add(eventHandler);
    if (informer != null) {
      informer.addEventHandler(eventHandler);
    }
  }

  @Override
  public T remove(ResourceID key) {
    return cache.remove(key);
  }

  @Override
  public void put(ResourceID key, T resource) {
    cache.put(key, resource);
  }
}
