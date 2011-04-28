/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.inject.client.assistedinject;

import com.google.gwt.inject.client.GinModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import java.lang.annotation.Annotation;

/**
 * Copied and modified from
 * {@link com.google.inject.assistedinject.FactoryModuleBuilder}. Usage is
 * mostly the same, with the exception of forwarded bindings (see at the
 * bottom of this documentation).
 *
 * Provides a factory that combines the caller's arguments with
 * injector-supplied values to construct objects.
 *
 * <h3>Defining a factory</h3>
 * Create an interface whose methods return the constructed type, or any of its
 * supertypes. The method's parameters are the arguments required to build the
 * constructed type.
 *
 * <pre>public interface PaymentFactory {
 *   Payment create(Date startDate, Money amount);
 * }</pre>
 *
 * You can name your factory methods whatever you like, such as <i>create</i>,
 * <i>createPayment</i> or <i>newPayment</i>.
 *
 * <h3>Creating a type that accepts factory parameters</h3>
 * {@code constructedType} is a concrete class with an
 * {@literal @}{@link Inject}-annotated constructor. In addition to injector-
 * supplied parameters, the constructor should have parameters that match each
 * of the factory method's parameters. Each factory-supplied parameter requires
 * an {@literal @}{@link Assisted} annotation. This serves to document that the
 * parameter is not bound by your application's modules.
 *
 * <pre>public class RealPayment implements Payment {
 *   {@literal @}Inject
 *   public RealPayment(
 *      CreditService creditService,
 *      AuthService authService,
 *      <strong>{@literal @}Assisted Date startDate</strong>,
 *      <strong>{@literal @}Assisted Money amount</strong>) {
 *     ...
 *   }
 * }</pre>
 *
 * <h3>Multiple factory methods for the same type</h3>
 * If the factory contains many methods that return the same type, you can
 * create multiple constructors in your concrete class, each constructor
 * marked with with {@literal @}{@link AssistedInject}, in order to match the
 * different parameters types of the factory methods.
 *
 * <pre>public interface PaymentFactory {
 *    Payment create(Date startDate, Money amount);
 *    Payment createWithoutDate(Money amount);
 * }
 *
 * public class RealPayment implements Payment {
 *  {@literal @}AssistedInject
 *   public RealPayment(
 *      CreditService creditService,
 *      AuthService authService,
 *     <strong>{@literal @}Assisted Date startDate</strong>,
 *     <strong>{@literal @}Assisted Money amount</strong>) {
 *     ...
 *   }
 *
 *  {@literal @}AssistedInject
 *   public RealPayment(
 *      CreditService creditService,
 *      AuthService authService,
 *     <strong>{@literal @}Assisted Money amount</strong>) {
 *     ...
 *   }
 * }</pre>
 *
 * <h3>Configuring simple factories</h3>
 * In your {@link Module module}, install a {@code GinFactoryModuleBuilder}
 * that creates the factory:
 *
 * <pre>install(new GinFactoryModuleBuilder()
 *     .implement(Payment.class, RealPayment.class)
 *     .build(PaymentFactory.class);</pre>
 *
 * As a side-effect of this binding, Gin will inject the factory to initialize
 * it for use. The factory cannot be used until the injector has been
 * initialized.
 *
 * <h3>Using the factory</h3>
 * Inject your factory into your application classes. When you use the factory,
 * your arguments will be combined with values from the injector to construct
 * an instance.
 *
 * <pre>public class PaymentAction {
 *   {@literal @}Inject private PaymentFactory paymentFactory;
 *
 *   public void doPayment(Money amount) {
 *     Payment payment = paymentFactory.create(new Date(), amount);
 *     payment.apply();
 *   }
 * }</pre>
 *
 * <h3>Making parameter types distinct</h3>
 * The types of the factory method's parameters must be distinct. To use
 * multiple parameters of the same type, use a named
 * {@literal @}{@link Assisted} annotation to disambiguate the parameters. The
 * names must be applied to the factory method's parameters:
 *
 * <pre>public interface PaymentFactory {
 *   Payment create(
 *       <strong>{@literal @}Assisted("startDate")</strong> Date startDate,
 *       <strong>{@literal @}Assisted("dueDate")</strong> Date dueDate,
 *       Money amount);
 * } </pre>
 *
 * ...and to the concrete type's constructor parameters:
 *
 * <pre>public class RealPayment implements Payment {
 *   {@literal @}Inject
 *   public RealPayment(
 *      CreditService creditService,
 *      AuthService authService,
 *      <strong>{@literal @}Assisted("startDate")</strong> Date startDate,
 *      <strong>{@literal @}Assisted("dueDate")</strong> Date dueDate,
 *      <strong>{@literal @}Assisted</strong> Money amount) {
 *     ...
 *   }
 * }</pre>
 *
 * <h3>Values are created by Gin</h3>
 * Returned factories use child injectors to create values. The values are
 * eligible for method interception. In addition, {@literal @}{@link Inject}
 * members will be injected before they are returned.
 *
 * <h3>More configuration options</h3>
 * In addition to simply specifying an implementation class for any returned
 * type, factories' return values can be automatic or can be configured to use
 * annotations:
 * <p/>
 * If you just want to return the types specified in the factory, do not
 * configure any implementations:
 *
 * <pre>public interface FruitFactory {
 *   Apple getApple(Color color);
 * }
 * ...
 * protected void configure() {
 *   install(new GinFactoryModuleBuilder().build(FruitFactory.class));
 * }</pre>
 *
 * Note that any type returned by the factory in this manner needs to be an
 * implementation class.
 * <p/>
 * To return two different implementations for the same interface from your
 * factory, use binding annotations on your return types:
 *
 * <pre>interface CarFactory {
 *   {@literal @}Named("fast") Car getFastCar(Color color);
 *   {@literal @}Named("clean") Car getCleanCar(Color color);
 * }
 * ...
 * protected void configure() {
 *   install(new GinFactoryModuleBuilder()
 *       .implement(Car.class, Names.named("fast"), Porsche.class)
 *       .implement(Car.class, Names.named("clean"), Prius.class)
 *       .build(CarFactory.class));
 * }</pre>
 * <p/>
 *
 * <strong>In difference to regular Guice Assisted Inject</strong>, in Gin,
 * return types in your factory are <strong>not</strong> further resolved using
 * your regular injector configuration. This means that in the following
 * example you'll still get a {@code Chicken} and not a {@code Rooster}:
 *
 * <pre>interface Animal {}
 * public class Chicken implements Animal {}
 * public class Rooster extends Chicken {}
 * interface AnimalFactory {
 *   Animal getAnimal();
 * }
 * ...
 * protected void configure() {
 *   bind(Chicken.class).to(Rooster.class);
 *   install(new GinFactoryModuleBuilder()
 *       .implement(Animal.class, Chicken.class)
 *       .build(AnimalFactory.class));
 * }</pre>
 *
 */
public class GinFactoryModuleBuilder {
  private final BindingCollector bindings = new BindingCollector();

  /**
   * See the factory configuration examples at {@link GInFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(Class<T> source, Class<? extends T> target) {
    return implement(source, TypeLiteral.get(target));
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(Class<T> source, TypeLiteral<? extends T> target) {
    return implement(TypeLiteral.get(source), target);
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(TypeLiteral<T> source, Class<? extends T> target) {
    return implement(source, TypeLiteral.get(target));
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(TypeLiteral<T> source,
      TypeLiteral<? extends T> target) {
    return implement(Key.get(source), target);
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(Class<T> source, Annotation annotation,
      Class<? extends T> target) {
    return implement(source, annotation, TypeLiteral.get(target));
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(Class<T> source, Annotation annotation,
      TypeLiteral<? extends T> target) {
    return implement(TypeLiteral.get(source), annotation, target);
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(TypeLiteral<T> source, Annotation annotation,
      Class<? extends T> target) {
    return implement(source, annotation, TypeLiteral.get(target));
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(TypeLiteral<T> source, Annotation annotation,
      TypeLiteral<? extends T> target) {
    return implement(Key.get(source, annotation), target);
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(Class<T> source,
      Class<? extends Annotation> annotationType, Class<? extends T> target) {
    return implement(source, annotationType, TypeLiteral.get(target));
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(Class<T> source,
      Class<? extends Annotation> annotationType, TypeLiteral<? extends T> target) {
    return implement(TypeLiteral.get(source), annotationType, target);
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(TypeLiteral<T> source,
      Class<? extends Annotation> annotationType, Class<? extends T> target) {
    return implement(source, annotationType, TypeLiteral.get(target));
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(TypeLiteral<T> source,
      Class<? extends Annotation> annotationType, TypeLiteral<? extends T> target) {
    return implement(Key.get(source, annotationType), target);
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(Key<T> source, Class<? extends T> target) {
    return implement(source, TypeLiteral.get(target));
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <T> GinFactoryModuleBuilder implement(Key<T> source, TypeLiteral<? extends T> target) {
    bindings.addBinding(source, target);
    return this;
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <F> GinModule build(Class<F> factoryInterface) {
    return build(TypeLiteral.get(factoryInterface));
  }

  /**
   * See the factory configuration examples at {@link GinFactoryModuleBuilder}.
   */
  public <F> GinModule build(TypeLiteral<F> factoryInterface) {
    return build(Key.get(factoryInterface));
  }

  public <F> GinModule build(Key<F> factoryInterface) {
    return new FactoryModule<F>(
        bindings.getBindings(),
        factoryInterface,
        findCaller(factoryInterface));
  }

  /**
   * Find the topmost stack element that's not a method of this class, which is
   * presumably the location in a Gin module that invoked build().
   *
   * @param factoryInterface An object identifying the factory interface; used
   *     to generate a fallback message if we can't determine the caller.
   */
  private String findCaller(Object factoryInterface) {
    Throwable dummyThrowableForStackTrace = new Throwable();

    StackTraceElement[] stackTrace = dummyThrowableForStackTrace.getStackTrace();

    for (StackTraceElement element : stackTrace) {
      if (!element.getClassName().equals(GinFactoryModuleBuilder.class.getName())) {
        return element.toString();
      }
    }

    return "definition of factory " + factoryInterface;
  }
}
