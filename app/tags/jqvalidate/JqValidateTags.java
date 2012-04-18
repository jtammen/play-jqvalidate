package tags.jqvalidate;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import play.data.validation.Email;
import play.data.validation.Max;
import play.data.validation.MaxSize;
import play.data.validation.Min;
import play.data.validation.MinSize;
import play.data.validation.Range;
import play.data.validation.Required;
import play.data.validation.URL;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Router.ActionDefinition;
import play.mvc.Scope.Flash;
import play.templates.FastTags;
import play.templates.GroovyTemplate.ExecutableTemplate;
import play.templates.JavaExtensions;

@FastTags.Namespace("jqvalid")
public class JqValidateTags extends FastTags {

	public static void _form(final Map<?, ?> args, final Closure body,
			final PrintWriter out, final ExecutableTemplate template,
			final int fromLine) {
		ActionDefinition actionDef = (ActionDefinition) args.get("arg");
		if (actionDef == null) {
			actionDef = (ActionDefinition) args.get("action");
		}
		String enctype = (String) args.get("enctype");
		if (enctype == null) {
			enctype = "application/x-www-form-urlencoded";
		}
		if (actionDef.star) {
			actionDef.method = "POST"; // prefer POST for form ....
		}
		if (args.containsKey("method")) {
			actionDef.method = args.get("method").toString();
		}
		if (!("GET".equals(actionDef.method) || "POST".equals(actionDef.method))) {
			String separator = actionDef.url.indexOf('?') != -1 ? "&" : "?";
			actionDef.url += separator + "x-http-method-override="
					+ actionDef.method.toUpperCase();
			actionDef.method = "POST";
		}
		String id = args.containsKey("id") ? (String) args.get("id")
				: "play-jqvalid-form__" + UUID.randomUUID();
		out.println("<form id=\""
				+ id
				+ "\" action=\""
				+ actionDef.url
				+ "\" method=\""
				+ actionDef.method.toUpperCase()
				+ "\" accept-charset=\"utf-8\" enctype=\""
				+ enctype
				+ "\" "
				+ serialize(args, "action", "method", "accept-charset",
						"enctype") + ">");
		if (!("GET".equals(actionDef.method))) {
			_authenticityToken(args, body, out, template, fromLine);
		}
		out.println(JavaExtensions.toString(body));
		out.println("</form>");
	}

	private static String buildValidationDataString(final Field f)
			throws Exception {
		StringBuilder result = new StringBuilder("{");
		List<String> rules = new ArrayList<String>();
		Map<String, String> messages = new HashMap<String, String>();
		Required required = f.getAnnotation(Required.class);
		if (required != null) {
			rules.add("required:true");
			if (required.message() != null) {
				messages.put("required", Messages.get(required.message()));
			}
		}
		Min min = f.getAnnotation(Min.class);
		if (min != null) {
			rules.add("min:" + new Double(min.value()).toString());
			if (min.message() != null) {
				messages.put("min",
						Messages.get(min.message(), null, min.value()));
			}
		}
		Max max = f.getAnnotation(Max.class);
		if (max != null) {
			rules.add("max:" + new Double(max.value()).toString());
			if (max.message() != null) {
				messages.put("max",
						Messages.get(max.message(), null, max.value()));
			}
		}
		Range range = f.getAnnotation(Range.class);
		if (range != null) {
			rules.add("range:[" + new Double(range.min()).toString() + ", "
					+ new Double(range.max()).toString() + "]");
			if (range.message() != null) {
				messages.put(
						"range",
						Messages.get(range.message(), null, range.min(),
								range.max()));
			}
		}
		MaxSize maxSize = f.getAnnotation(MaxSize.class);
		if (maxSize != null) {
			rules.add("maxlength:" + new Integer(maxSize.value()).toString());
			if (maxSize.message() != null) {
				messages.put("maxlength",
						Messages.get(maxSize.message(), null, maxSize.value()));
			}
		}
		MinSize minSize = f.getAnnotation(MinSize.class);
		if (minSize != null) {
			rules.add("minlength:" + new Integer(minSize.value()).toString());
			if (minSize.message() != null) {
				messages.put("minlength",
						Messages.get(minSize.message(), null, minSize.value()));
			}
		}
		URL url = f.getAnnotation(URL.class);
		if (url != null) {
			rules.add("url:true");
			if (url.message() != null) {
				messages.put("url", Messages.get(url.message()));
			}
		}
		Email email = f.getAnnotation(Email.class);
		if (email != null) {
			rules.add("email:true");
			if (email.message() != null) {
				messages.put("email", Messages.get(email.message()));
			}
		}
		if (rules.size() > 0) {
			boolean first = true;
			for (String rule : rules) {
				if (first) {
					first = false;
				} else {
					result.append(",");
				}
				result.append(rule);
			}
		}
		if (messages.size() > 0) {
			result.append(",messages:{");
			boolean first = true;
			for (String key : messages.keySet()) {
				if (first) {
					first = false;
				} else {
					result.append(",");
				}
				result.append("\"");
				result.append(key);
				result.append("\"");
				result.append(":");
				result.append("\"");
				result.append(messages.get(key));
				result.append("\"");
			}
			result.append("}");
		}
		result.append("}");
		return result.toString();
	}

	public static void _field(final Map<?, ?> args, final Closure body,
			final PrintWriter out, final ExecutableTemplate template,
			final int fromLine) {
		Map<String, Object> field = new HashMap<String, Object>();
		String _arg = args.get("arg").toString();
		field.put("name", _arg);
		field.put("label", _arg.replaceAll("\\[\\d*\\]", ""));
		field.put("id", StringUtils.replaceEach(_arg, new String[] { ".", "[",
				"]" }, new String[] { "_", "_", "" }));
		field.put("flash", Flash.current().get(_arg));
		field.put("flashArray",
				field.get("flash") != null
						&& !field.get("flash").toString().isEmpty() ? field
						.get("flash").toString().split(",") : new String[0]);
		field.put("error", Validation.error(_arg));
		field.put("errorClass", field.get("error") != null ? "hasError" : "");

		String[] pieces = _arg.split("\\.");
		Object obj = body.getProperty(pieces[0]);
		if (obj != null) {
			if (pieces.length > 1) {
				for (int i = 1; i < pieces.length; i++) {
					try {
						String rawFieldName = pieces[i];
						String fieldName = rawFieldName.replaceAll("\\W", "")
								.replaceAll("[0-9]", "");
						Field f = obj.getClass().getField(fieldName);
						if (i == (pieces.length - 1)) {
							field.put("validationData",
									buildValidationDataString(f));

							try {
								Method getter = obj.getClass().getMethod(
										"get"
												+ JavaExtensions.capFirst(f
														.getName()));
								field.put("value",
										getter.invoke(obj, new Object[0]));
							} catch (NoSuchMethodException e) {
								field.put("value", f.get(obj).toString());
							}
						} else {
							// check whether we have a nested collection (list)
							boolean isCollection = rawFieldName.contains("[");
							if (isCollection) {
								int index = Integer.valueOf(rawFieldName
										.substring(rawFieldName.length() - 2,
												rawFieldName.length() - 1));
								Type type = f.getGenericType();
								if (type instanceof ParameterizedType) {
									ParameterizedType pt = (ParameterizedType) type;
									Type collectionType = pt
											.getActualTypeArguments()[0];
									// get the actual object from the list!
									List object = (List) f.get(obj);
									if (object != null && object.size() > 0) {
										obj = object.get(index);
									} else {
										// ... or create a new one if the list
										// is empty
										obj = ((Class<? extends Object>) collectionType)
												.newInstance();
									}
								}
							} else {
								// nested object
								obj = f.get(obj);
							}
						}
					} catch (Exception e) {
						// if there is a problem reading the field we dont set
						// any value
					}
				}
			} else {
				field.put("value", obj);
			}
		}
		body.setProperty("field", field);
		body.call();
	}
}
