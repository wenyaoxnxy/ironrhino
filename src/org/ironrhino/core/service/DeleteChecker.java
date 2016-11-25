package org.ironrhino.core.service;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class DeleteChecker {

	@Autowired
	private SessionFactory sessionFactory;

	private Map<Class<?>, List<Tuple<Class<?>, String>>> mapping = new HashMap<>();

	private Map<Class<?>, List<Tuple<Class<?>, Tuple<String, String>>>> componentMapping = new HashMap<>();

	private Map<Class<?>, List<Tuple<Class<?>, Tuple<String, String>>>> collectionMapping = new HashMap<>();

	@PostConstruct
	public void init() {
		Metamodel metamodel = ((EntityManagerFactory) sessionFactory).getMetamodel();
		Set<EntityType<?>> entities = metamodel.getEntities();
		for (EntityType<?> type : entities) {
			Class<?> entityClass = type.getJavaType();
			for (Attribute<?, ?> attr : type.getAttributes()) {
				String name = attr.getName();
				Class<?> referrer = attr.getJavaType();
				switch (attr.getPersistentAttributeType()) {
				case MANY_TO_ONE:
					if (BaseTreeableEntity.class.isAssignableFrom(entityClass) && name.equals("parent"))
						continue;
					List<Tuple<Class<?>, String>> list1 = mapping.get(referrer);
					if (list1 == null) {
						list1 = new ArrayList<>();
						mapping.put(referrer, list1);
					}
					list1.add(new Tuple<>(entityClass, name));
					break;
				case ONE_TO_ONE:
					PrimaryKeyJoinColumn pkjc = null;
					PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(entityClass, name);
					if (pd != null) {
						Class<?> declaredClass = pd.getReadMethod() != null ? pd.getReadMethod().getDeclaringClass()
								: entityClass;
						if (pd.getReadMethod() != null)
							pkjc = pd.getReadMethod().getAnnotation(PrimaryKeyJoinColumn.class);
						if (pkjc == null)
							try {
								Field f = declaredClass.getDeclaredField(name);
								if (f != null)
									pkjc = f.getAnnotation(PrimaryKeyJoinColumn.class);
							} catch (Exception e) {
							}
					}
					if (pkjc == null) {
						List<Tuple<Class<?>, String>> list2 = mapping.get(referrer);
						if (list2 == null) {
							list2 = new ArrayList<>();
							mapping.put(referrer, list2);
						}
						list2.add(new Tuple<>(entityClass, name));
					}
					break;
				case ONE_TO_MANY:
				case MANY_TO_MANY:
					if (BaseTreeableEntity.class.isAssignableFrom(entityClass) && name.equals("children"))
						continue;
					Class<?> componentClass = ((PluralAttribute<?, ?, ?>) attr).getElementType().getJavaType();
					try {
						Class<?> clz = componentClass;
						while (true) {
							for (Field f : clz.getDeclaredFields()) {
								if (f.getAnnotation(ManyToOne.class) != null
										|| f.getAnnotation(OneToOne.class) != null) {
									referrer = f.getType();
									List<Tuple<Class<?>, Tuple<String, String>>> list3 = collectionMapping
											.get(referrer);
									if (list3 == null) {
										list3 = new ArrayList<>();
										collectionMapping.put(referrer, list3);
									}
									list3.add(new Tuple<>(entityClass, new Tuple<>(name, f.getName())));
								}
							}
							clz = clz.getSuperclass();
							if (clz.equals(Object.class) || clz.getAnnotation(MappedSuperclass.class) == null)
								break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case EMBEDDED:
					try {
						Class<?> clz = attr.getJavaType();
						while (true) {
							for (Field f : clz.getDeclaredFields()) {
								if (f.getAnnotation(ManyToOne.class) != null
										|| f.getAnnotation(OneToOne.class) != null) {
									referrer = f.getType();
									List<Tuple<Class<?>, Tuple<String, String>>> list4 = componentMapping.get(referrer);
									if (list4 == null) {
										list4 = new ArrayList<>();
										componentMapping.put(referrer, list4);
									}
									list4.add(new Tuple<>(entityClass, new Tuple<>(name, f.getName())));
								}
							}
							clz = clz.getSuperclass();
							if (clz.equals(Object.class) || clz.getAnnotation(MappedSuperclass.class) == null)
								break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				default:
					break;
				}
			}
		}
	}

	public void check(Persistable<?> entity) {
		if (entity instanceof Enableable) {
			Enableable enableable = (Enableable) entity;
			if (enableable.isEnabled())
				throw new ErrorMessage("delete.forbidden", new Object[] { entity }, "delete.forbidden.notdisabled");
		}
		List<Tuple<Class<?>, String>> references = mapping.get(ReflectionUtils.getActualClass(entity));
		if (references != null && references.size() > 0) {
			Session session = sessionFactory.getCurrentSession();
			for (Tuple<Class<?>, String> tuple : references) {
				CriteriaBuilder cb = session.getCriteriaBuilder();
				CriteriaQuery<Long> cq = cb.createQuery(Long.class);
				Root<?> root = cq.from(tuple.getKey());
				cq.select(cb.count(root)).where(cb.equal(root.get(tuple.getValue()), entity));
				long count = session.createQuery(cq).getSingleResult();
				if (count > 0)
					throw new ErrorMessage("delete.forbidden", new Object[] { entity }, "delete.forbidden.referrer");
			}
		}
		List<Tuple<Class<?>, Tuple<String, String>>> componentReferences = componentMapping
				.get(ReflectionUtils.getActualClass(entity));
		if (componentReferences != null && componentReferences.size() > 0) {
			Session session = sessionFactory.getCurrentSession();
			for (Tuple<Class<?>, Tuple<String, String>> tuple : componentReferences) {
				Tuple<String, String> value = tuple.getValue();
				CriteriaBuilder cb = session.getCriteriaBuilder();
				CriteriaQuery<Long> cq = cb.createQuery(Long.class);
				Root<?> root = cq.from(tuple.getKey());
				cq.select(cb.count(root)).where(cb.equal(root.get(value.getKey() + "." + value.getValue()), entity));
				long count = session.createQuery(cq).getSingleResult();
				if (count > 0)
					throw new ErrorMessage("delete.forbidden", new Object[] { entity }, "delete.forbidden.referrer");
			}
		}
		List<Tuple<Class<?>, Tuple<String, String>>> collectionReferences = collectionMapping
				.get(ReflectionUtils.getActualClass(entity));
		if (collectionReferences != null && collectionReferences.size() > 0) {
			Session session = sessionFactory.getCurrentSession();
			for (Tuple<Class<?>, Tuple<String, String>> tuple : collectionReferences) {
				Tuple<String, String> value = tuple.getValue();
				CriteriaBuilder cb = session.getCriteriaBuilder();
				CriteriaQuery<Long> cq = cb.createQuery(Long.class);
				Root<?> root = cq.from(tuple.getKey());
				root.get(value.getKey()).alias(value.getKey());
				cq.select(cb.count(root)).where(cb.equal(root.get(value.getKey() + "." + value.getValue()), entity));
				long count = session.createQuery(cq).getSingleResult();
				if (count > 0)
					throw new ErrorMessage("delete.forbidden", new Object[] { entity }, "delete.forbidden.referrer");
			}
		}
	}

}