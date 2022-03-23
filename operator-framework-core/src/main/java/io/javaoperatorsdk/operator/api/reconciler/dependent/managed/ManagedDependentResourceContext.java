package io.javaoperatorsdk.operator.api.reconciler.dependent.managed;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.javaoperatorsdk.operator.OperatorException;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;

/**
 * Contextual information related to {@link DependentResource} either to retrieve the actual
 * implementations to interact with them or to pass information between them and/or the reconciler
 */
@SuppressWarnings("rawtypes")
public class ManagedDependentResourceContext {

  private final List<DependentResource> dependentResources;
  private final ConcurrentHashMap attributes = new ConcurrentHashMap();
  private final ConcurrentHashMap<DependentResource<?, ?>, ReconcileResult<?>> reconcileResults =
      new ConcurrentHashMap<>();

  /**
   * Retrieve a contextual object, if it exists and is of the specified expected type, associated
   * with the specified key. Contextual objects can be used to pass data between the reconciler and
   * dependent resources and are scoped to the current reconciliation.
   *
   * @param key the key identifying which contextual object to retrieve
   * @param expectedType the class representing the expected type of the contextual object
   * @param <T> the type of the expected contextual object
   * @return an Optional containing the contextual object or {@link Optional#empty()} if no such
   *         object exists or doesn't match the expected type
   */
  public <T> Optional<T> get(Object key, Class<T> expectedType) {
    return Optional.ofNullable(attributes.get(key))
        .filter(expectedType::isInstance)
        .map(expectedType::cast);
  }

  /**
   * Associates the specified contextual value to the specified key. If the value is {@code null},
   * the semantics of this operation is defined as removing the mapping associated with the
   * specified key.
   *
   * @param key the key identifying which contextual object to add or remove from the context
   * @param value the value to add to the context or {@code null} to remove an existing entry
   *        associated with the specified key
   * @return an Optional containing the previous value associated with the key or
   *         {@link Optional#empty()} if none existed
   */
  @SuppressWarnings("unchecked")
  public Optional put(Object key, Object value) {
    if (value == null) {
      return Optional.ofNullable(attributes.remove(key));
    }
    return Optional.ofNullable(attributes.put(key, value));
  }

  /**
   * Retrieves the value associated with the key or fail with an exception if none exists.
   *
   * @param key the key identifying which contextual object to retrieve
   * @param expectedType the expected type of the value to retrieve
   * @param <T> the type of the expected contextual object
   * @return the contextual object value associated with the specified key
   * @see #get(Object, Class)
   */
  public <T> T getMandatory(Object key, Class<T> expectedType) {
    return get(key, expectedType).orElseThrow(() -> new IllegalStateException(
        "Mandatory attribute (key: " + key + ", type: " + expectedType.getName()
            + ") is missing or not of the expected type"));
  }

  public ManagedDependentResourceContext(List<DependentResource> dependentResources) {
    this.dependentResources = Collections.unmodifiableList(dependentResources);
  }

  /**
   * Retrieve all the known {@link DependentResource} implementations
   *
   * @return a list of known {@link DependentResource} implementations
   */
  public List<DependentResource> getDependentResources() {
    return dependentResources;
  }

  /**
   * Retrieve the dependent resource implementation associated with the specified resource type.
   *
   * @param resourceClass the dependent resource class for which we want to retrieve the associated
   *        dependent resource implementation
   * @param <T> the type of the resources for which we want to retrieve the associated dependent
   *        resource implementation
   * @return the associated dependent resource implementation if it exists or an exception if it
   *         doesn't or several implementations are associated with the specified resource type
   */
  @SuppressWarnings("unchecked")
  public <T extends DependentResource> T getDependentResource(Class<T> resourceClass) {
    var resourceList =
        dependentResources.stream()
            .filter(dr -> dr.getClass().equals(resourceClass))
            .collect(Collectors.toList());
    if (resourceList.isEmpty()) {
      throw new OperatorException(
          "No dependent resource found for class: " + resourceClass.getName());
    }
    if (resourceList.size() > 1) {
      throw new OperatorException(
          "More than one dependent resource found for class: " + resourceClass.getName());
    }
    return (T) resourceList.get(0);
  }

  /**
   * Retrieve a reconcile result of the specified dependent resource implementation.
   *
   * @param dependentResource the dependent resource implementation
   * @return an Optional containing the reconcile result or {@link Optional#empty()} if no such
   *         object exists
   */
  @SuppressWarnings("unchecked")
  public <R, T extends DependentResource<R, ?>> Optional<ReconcileResult<R>> getReconcileResult(
      DependentResource<R, ?> dependentResource) {
    return Optional.ofNullable(reconcileResults.get(dependentResource))
        .map(r -> (ReconcileResult<R>) r);
  }

  /**
   * Set a reconcile result of the specified dependent resource implementation.
   *
   * @param dependentResource the dependent resource implementation
   * @param reconcileResult the reconcile result to add to the context associated with the specified
   *        dependent resource
   */
  public <R, T extends DependentResource<R, ?>> void setReconcileResult(
      DependentResource<R, ?> dependentResource, ReconcileResult<R> reconcileResult) {
    reconcileResults.put(dependentResource, reconcileResult);
  }
}
